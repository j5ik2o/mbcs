package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props }
import akka.cluster.singleton.{
  ClusterSingletonManager,
  ClusterSingletonManagerSettings,
  ClusterSingletonProxy,
  ClusterSingletonProxySettings
}
import com.github.j5ik2o.mbcs.domain.model.ULID

object IdWorkerIdControllerSingleton {

  def singletonManager(minId: Long, maxId: Long)(implicit system: ActorSystem): ActorRef = system.actorOf(
    ClusterSingletonManager.props(
      IdWorkerIdController.props(minId, maxId),
      PoisonPill,
      ClusterSingletonManagerSettings(system).withRole(None)
    ),
    s"${IdWorkerIdController.name}-singleton"
  )

}

object IdWorkerIdControllerProxy {

  def props(id: ULID): Props = Props(new IdWorkerIdControllerProxy(id))

}

class IdWorkerIdControllerProxy(id: ULID) extends Actor with ActorLogging {

  private var idWorkerIdController: ActorRef = _

  override def preStart(): Unit = {
    super.preStart()
    idWorkerIdController = context.system.actorOf(
      ClusterSingletonProxy.props(
        s"/user/${IdWorkerIdController.name}-singleton",
        ClusterSingletonProxySettings(context.system).withRole(None)
      ),
      s"${IdWorkerIdController.name}-singleton-proxy-${id.value}"
    )
  }

  override def receive: Receive = {
    case msg =>
      log.debug(s"msg = $msg")
      idWorkerIdController forward msg
  }

}
