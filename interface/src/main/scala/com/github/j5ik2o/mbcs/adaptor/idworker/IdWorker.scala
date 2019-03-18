package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorker.{GenerateId, IdGenerated}
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorkerIdController.Protocol._
import com.github.j5ik2o.mbcs.domain.model.ULID
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

@SerialVersionUID(1L)
class InvalidSystemClock(message: String) extends Exception(message)

case class IdWorkerConfig(dataCenterId: Long,
                          workerIdBits: Long = 5L,
                          dataCenterIdBits: Long = 5L,
                          sequenceBits: Long = 12L,
                          twepoch: Long = 1288834974657L)

class Snowfalke(val idWorkerConfig: IdWorkerConfig, val workerId: Long) {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  private[this] var sequence: Long  = 0L
  private[this] val maxWorkerId     = -1L ^ (-1L << idWorkerConfig.workerIdBits)
  private[this] val maxDataCenterId = -1L ^ (-1L << idWorkerConfig.dataCenterIdBits)

  private[this] val workerIdShift      = idWorkerConfig.sequenceBits
  private[this] val dataCenterIdShift  = idWorkerConfig.sequenceBits + idWorkerConfig.workerIdBits
  private[this] val timestampLeftShift = idWorkerConfig.sequenceBits + idWorkerConfig.workerIdBits + idWorkerConfig.dataCenterIdBits
  private[this] val sequenceMask       = -1L ^ (-1L << idWorkerConfig.sequenceBits)

  private[this] var lastTimestamp = -1L

  if (workerId > maxWorkerId || workerId < 0) {
    throw new IllegalArgumentException("worker Id can't be greater than %d or less than 0".format(maxWorkerId))
  }

  if (idWorkerConfig.dataCenterId > maxDataCenterId || idWorkerConfig.dataCenterId < 0) {
    throw new IllegalArgumentException("datacenter Id can't be greater than %d or less than 0".format(maxDataCenterId))
  }

  def generateId(): Long = {
    var timestamp = timeGen()

    if (timestamp < lastTimestamp) {
      logger.error("clock is moving backwards.  Rejecting requests until %d.", lastTimestamp)
      throw new InvalidSystemClock(
        "Clock moved backwards.  Refusing to generate id for %d milliseconds".format(lastTimestamp - timestamp)
      )
    }

    if (lastTimestamp == timestamp) {
      sequence = (sequence + 1) & sequenceMask
      if (sequence == 0) {
        timestamp = tilNextMillis(lastTimestamp)
      }
    } else {
      sequence = 0
    }

    lastTimestamp = timestamp
    ((timestamp - idWorkerConfig.twepoch) << timestampLeftShift) |
    (idWorkerConfig.dataCenterId << dataCenterIdShift) |
    (workerId << workerIdShift) |
    sequence
  }

  protected def tilNextMillis(lastTimestamp: Long): Long = {
    var timestamp = timeGen()
    while (timestamp <= lastTimestamp) {
      timestamp = timeGen()
    }
    timestamp
  }

  protected def timeGen(): Long = System.currentTimeMillis()

}

object IdWorker {

  def props(idWorkerConfig: IdWorkerConfig, idWorkerIdController: ActorRef) =
    Props(new IdWorker(idWorkerConfig, idWorkerIdController))

  case class GenerateId(id: ULID)
  case class IdGenerated(id: Long)

}

class IdWorker(val idWorkerConfig: IdWorkerConfig, idWorkerIdController: ActorRef)
    extends Actor
    with ActorLogging
    with Stash {
  import context.dispatcher

  private var snowfalke: Snowfalke = _

  override def preStart(): Unit = {
    super.preStart()
    idWorkerIdController ! BorrowId
  }

  override def postStop(): Unit = {
    idWorkerIdController ! ReturnId(snowfalke.workerId)
    super.postStop()
  }

  def ready: Receive = {
    case GenerateId(_) =>
      sender() ! IdGenerated(snowfalke.generateId())
  }

  override def receive: Receive = {
    case BorrowIdSucceeded(workerId) =>
      log.debug(s"BorrowIdSucceeded($workerId)")
      snowfalke = new Snowfalke(idWorkerConfig, workerId)
      unstashAll()
      context.become(ready)
      context.system.scheduler.schedule(0 seconds, 3 seconds, self, Ping(workerId))
    case m: Pong =>
      log.debug(m.toString)
    case m: Ping =>
      log.debug(m.toString)
      idWorkerIdController ! m
    case msg =>
      stash()
  }

}
