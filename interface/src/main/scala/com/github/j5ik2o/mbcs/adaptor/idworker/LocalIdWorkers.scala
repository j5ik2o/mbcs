package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.{ Actor, ActorRef, Props }

object LocalIdWorkers {

  def props: Props = Props[LocalIdWorkers]

  def name = "local-id-worker"

}

class LocalIdWorkers(override val idWorkerConfig: IdWorkerConfig, override val idWorkerIdController: ActorRef)
    extends Actor
    with IdWorkerLookup {
  override def receive: Receive = forwardToActor
}
