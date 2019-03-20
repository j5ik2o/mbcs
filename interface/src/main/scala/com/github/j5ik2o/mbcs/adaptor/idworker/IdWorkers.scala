package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.{ Actor, ActorLogging, Props }
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorker.GenerateId
import com.github.j5ik2o.mbcs.adaptor.utils.ChildActorLookup
case class DataCenterIdWithWorkerId(dId: Long, wId: Long)

object IdWorkers {

  def props(idWorkerConfig: IdWorkerConfig) = Props(new IdWorkers(idWorkerConfig))

}

class IdWorkers(idWorkerConfig: IdWorkerConfig) extends Actor with ChildActorLookup with ActorLogging {
  override type ID             = DataCenterIdWithWorkerId
  override type CommandRequest = GenerateId
  override protected def childName(childId: ID): String = childId.dId + "-" + childId.wId
  override protected def childProps(childId: ID): Props = IdWorker.props(idWorkerConfig, childId.dId, childId.wId)
  override protected def toCommandRequestId(commandRequest: GenerateId): ID =
    DataCenterIdWithWorkerId(commandRequest.dataCenterId, commandRequest.workerId)

  private[this] val maxWorkerId     = -1L ^ (-1L << idWorkerConfig.workerIdBits)
  private[this] val maxDataCenterId = -1L ^ (-1L << idWorkerConfig.dataCenterIdBits)

  override def receive: Receive = {
    case cmd @ GenerateId(dataCenterId, workerId) =>
      require(
        dataCenterId >= 0L && dataCenterId < maxDataCenterId &&
        workerId >= 0L && workerId < maxWorkerId
      )
      forwardToActor(cmd)
  }
}
