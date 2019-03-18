package com.github.j5ik2o.mbcs.adaptor.aggregate

import akka.actor.Actor

object ThreadAggregate {

  object Protocol {
    sealed trait ThreadCommand
//    case class CreateThread(id: ) extends ThreadCommand
  }

}

class ThreadAggregate extends Actor {
  override def receive: Receive = {
    case _ =>
  }
}
