package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.{ Actor, ActorLogging, Props }
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorker.{ GenerateId, IdGenerated }
import org.slf4j.LoggerFactory

@SerialVersionUID(1L)
case class InvalidSystemClock(message: String, timestamp: Long, lastSequence: Long) extends Exception(message)

case class IdWorkerConfig(workerIdBits: Long = 5L,
                          dataCenterIdBits: Long = 5L,
                          sequenceBits: Long = 12L,
                          twepoch: Long = 1288834974657L)

case class NextId(id: Option[Long], lastTimestamp: Long = -1L, sequence: Long = 0L)

class Snowfalke(val idWorkerConfig: IdWorkerConfig, val dataCenterId: Long, val workerId: Long) {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  private[this] val maxWorkerId     = -1L ^ (-1L << idWorkerConfig.workerIdBits)
  private[this] val maxDataCenterId = -1L ^ (-1L << idWorkerConfig.dataCenterIdBits)

  private[this] val workerIdShift      = idWorkerConfig.sequenceBits
  private[this] val dataCenterIdShift  = idWorkerConfig.sequenceBits + idWorkerConfig.workerIdBits
  private[this] val timestampLeftShift = idWorkerConfig.sequenceBits + idWorkerConfig.workerIdBits + idWorkerConfig.dataCenterIdBits
  private[this] val sequenceMask       = -1L ^ (-1L << idWorkerConfig.sequenceBits)

  if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
    throw new IllegalArgumentException(
      "datacenter Id (%d) can't be greater than %d or less than 0".format(dataCenterId, maxDataCenterId)
    )
  }

  if (workerId > maxWorkerId || workerId < 0) {
    throw new IllegalArgumentException(
      "worker Id (%d) can't be greater than %d or less than 0".format(workerId, maxWorkerId)
    )
  }

  def generateId(lastTimestamp: Long, currentSequence: Long): NextId = {
    val timestamp = timeGen()

    if (timestamp < lastTimestamp) {
      logger.error("clock is moving backwards.  Rejecting requests until %d.", lastTimestamp)
      throw new InvalidSystemClock(
        "Clock moved backwards.  Refusing to generate id for %d milliseconds".format(lastTimestamp - timestamp),
        lastTimestamp,
        currentSequence
      )
    }

    require(currentSequence < sequenceMask + 1)

    val nextSequence: Option[Long] = if (lastTimestamp == timestamp) {
      val s = (currentSequence + 1) & sequenceMask
      if (s == 0) None else Some(s)
    } else {
      Some(0)
    }

    val id = nextSequence.map { s =>
      ((timestamp - idWorkerConfig.twepoch) << timestampLeftShift) |
      (dataCenterId << dataCenterIdShift) |
      (workerId << workerIdShift) | s
    }

    NextId(id, timestamp, nextSequence.getOrElse(0))
  }

  protected def timeGen(): Long = System.currentTimeMillis()

}

object IdWorker {

  def props(idWorkerConfig: IdWorkerConfig, dataCenterId: Long, workerId: Long) =
    Props(new IdWorker(idWorkerConfig, dataCenterId, workerId))

  def name(dataCenterId: Long, workerId: Long): String = s"id-worker-$dataCenterId-$workerId"

  trait CommandRequest {
    def dataCenterId: Long
    def workerId: Long
  }
  case class GenerateId(dataCenterId: Long, workerId: Long) extends CommandRequest

  case class IdGenerated(id: Long)

}

class IdWorker(val idWorkerConfig: IdWorkerConfig, val dataCenterId: Long, val workerId: Long)
    extends Actor
    with ActorLogging {

  private val snowfalke: Snowfalke = new Snowfalke(idWorkerConfig, dataCenterId, workerId)

  override def receive: Receive = onMessage(sequenceNumber = 0L, lastTimestamp = -1L)

  private def onMessage(sequenceNumber: Long, lastTimestamp: Long): Receive = {
    case m: GenerateId =>
      require(m.dataCenterId == dataCenterId, s"Invalid dataCenterId: ${m.dataCenterId}")
      require(m.workerId == workerId, s"Invalid workerId: ${m.workerId}")
      val NextId(idOpt, timestamp, nextSequence) = snowfalke.generateId(lastTimestamp, sequenceNumber)
      context.become(onMessage(nextSequence, timestamp))
      idOpt match {
        case Some(id) =>
          sender() ! IdGenerated(id)
        case None =>
          log.debug("retrying to generate id...")
          self.tell(m, sender())
      }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    message match {
      case Some(msg: GenerateId) =>
        self forward msg
      case _ =>
    }
    super.preRestart(reason, message)
  }

  override def postRestart(reason: Throwable): Unit = {
    reason match {
      case InvalidSystemClock(_, timestamp, lastSequence) =>
        context.become(onMessage(lastSequence, timestamp))
      case _ =>
    }
    super.postRestart(reason)
  }

}
