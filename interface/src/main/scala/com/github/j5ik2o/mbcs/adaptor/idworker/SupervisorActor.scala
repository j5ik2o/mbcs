package com.github.j5ik2o.mbcs.adaptor.idworker
import akka.actor.{ Actor, Props, SupervisorStrategy }

class SupervisorActor(childProps: Props, override val supervisorStrategy: SupervisorStrategy) extends Actor {
  val child = context.actorOf(childProps, "supervised-child")

  override def receive: Receive = {
    case msg => child forward msg
  }
}
