package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.{ ActorRef, Props }
import com.github.j5ik2o.mbcs.adaptor.utils.ChildActorLookup

trait IdWorkerLookup extends ChildActorLookup {
  override type ID             = Long
  override type CommandRequest = IdWorker.CommandRequest
  def idWorkerConfig: IdWorkerConfig
  def idWorkerIdController: ActorRef
  override protected def childName(childId: ID): String                                  = IdWorker.name(childId)
  override protected def childProps: Props                                               = IdWorker.props(idWorkerConfig)
  override protected def toCommandRequestId(commandRequest: IdWorker.CommandRequest): ID = commandRequest.idWorkerId
}
