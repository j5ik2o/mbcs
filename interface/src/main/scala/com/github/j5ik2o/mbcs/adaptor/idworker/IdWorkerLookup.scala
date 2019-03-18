package com.github.j5ik2o.mbcs.adaptor.idworker
import akka.actor.{Actor, ActorContext, ActorRef}
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorker.CommandRequest
import com.github.j5ik2o.mbcs.domain.model.ULID

trait IdWorkerLookup {
  implicit def context: ActorContext
  def idWorkerConfig: IdWorkerConfig
  def idWorkerIdController: ActorRef

  def forwardToActor: Actor.Receive = {
    case cmd: CommandRequest =>
      context
        .child(IdWorker.name(cmd.idWorkerId))
        .fold(createAndForward(cmd, cmd.idWorkerId))(forwardCommand(cmd))
  }

  def forwardCommand(cmd: CommandRequest)(idWorkerRef: ActorRef): Unit =
    idWorkerRef forward cmd

  def createAndForward(cmd: CommandRequest, idWorkerId: ULID): Unit = {
    createActor(idWorkerId) forward cmd
  }

  def createActor(idWorkerId: ULID): ActorRef =
    context.actorOf(IdWorker.props(idWorkerConfig, idWorkerIdController), IdWorker.name(idWorkerId))

}
