package com.github.j5ik2o.mbcs.adaptor.utils
import akka.actor.{ Actor, ActorContext, ActorRef, Props }

trait ChildActorLookup {
  implicit def context: ActorContext
  type ID
  type CommandRequest

  protected def childName(childId: ID): String
  protected def childProps: Props
  protected def toCommandRequestId(commandRequest: CommandRequest): ID

  protected def forwardToActor: Actor.Receive = {
    case _cmd =>
      val cmd = _cmd.asInstanceOf[CommandRequest]
      context
        .child(childName(toCommandRequestId(cmd)))
        .fold(createAndForward(cmd, toCommandRequestId(cmd)))(forwardCommand(cmd))
  }

  protected def forwardCommand(cmd: CommandRequest)(childRef: ActorRef): Unit =
    childRef forward cmd

  protected def createAndForward(cmd: CommandRequest, childId: ID): Unit = {
    createActor(childId) forward cmd
  }

  protected def createActor(childId: ID): ActorRef =
    context.actorOf(childProps, childName(childId))
}
