package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.{ Actor, ActorLogging, Props }
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorker.{ GenerateId, IdGenerated }
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.Random

@SerialVersionUID(1L)
class InvalidSystemClock(message: String) extends Exception(message)

case class IdWorkerConfig(dataCenterId: Long,
                          workerIdBits: Long = 5L,
                          dataCenterIdBits: Long = 5L,
                          sequenceBits: Long = 12L,
                          twepoch: Long = 1288834974657L)

class Snowfalke(val idWorkerConfig: IdWorkerConfig) {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  private[this] var sequence: Long  = 0L
  private[this] val maxWorkerId     = -1L ^ (-1L << idWorkerConfig.workerIdBits)
  private[this] val maxDataCenterId = -1L ^ (-1L << idWorkerConfig.dataCenterIdBits)

  private[this] val workerIdShift      = idWorkerConfig.sequenceBits
  private[this] val dataCenterIdShift  = idWorkerConfig.sequenceBits + idWorkerConfig.workerIdBits
  private[this] val timestampLeftShift = idWorkerConfig.sequenceBits + idWorkerConfig.workerIdBits + idWorkerConfig.dataCenterIdBits
  private[this] val sequenceMask       = -1L ^ (-1L << idWorkerConfig.sequenceBits)

  private[this] var lastTimestamp = -1L

  if (idWorkerConfig.dataCenterId > maxDataCenterId || idWorkerConfig.dataCenterId < 0) {
    throw new IllegalArgumentException("datacenter Id can't be greater than %d or less than 0".format(maxDataCenterId))
  }

  def generateId(workerId: Long): Long = {
    if (workerId > maxWorkerId || workerId < 0) {
      throw new IllegalArgumentException(
        "worker Id (%d) can't be greater than %d or less than 0".format(workerId, maxWorkerId)
      )
    }

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

  def props(idWorkerConfig: IdWorkerConfig) =
    Props(new IdWorker(idWorkerConfig))

  def name(id: Long): String = s"id-worker-$id"

  trait CommandRequest {
    def idWorkerId: Long
  }
  case class GenerateId() extends CommandRequest {
    override val idWorkerId: Long = Random.nextInt(32).toLong
  }
  case class IdGenerated(id: Long)

}

class IdWorker(val idWorkerConfig: IdWorkerConfig, heartbeatDuration: FiniteDuration = 1 seconds)
    extends Actor
    with ActorLogging {

  private val snowfalke: Snowfalke = new Snowfalke(idWorkerConfig)

  override def receive: Receive = {
    case m: GenerateId =>
      sender() ! IdGenerated(snowfalke.generateId(m.idWorkerId))
  }

}
