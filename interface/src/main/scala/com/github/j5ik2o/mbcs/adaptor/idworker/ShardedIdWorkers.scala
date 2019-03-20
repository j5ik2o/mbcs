package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.{ ActorRef, ActorSystem, Props, ReceiveTimeout, SupervisorStrategy }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import akka.cluster.sharding.ShardRegion.Passivate
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorker.GenerateId
import com.github.j5ik2o.mbcs.adaptor.idworker.ShardedIdWorkers.StopIdWorker
import com.github.j5ik2o.mbcs.adaptor.utils.SupervisorActor

object ShardedIdWorkers {
  def props(idWorkerConfig: IdWorkerConfig): Props =
    Props(new ShardedIdWorkers(idWorkerConfig))

  val shardName = "id-worker"

  case object StopIdWorker

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: GenerateId =>
      (s"${cmd.dataCenterId}-${cmd.workerId}", cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: GenerateId =>
      cmd.dataCenterId.toString
  }

  def startShardRegion(idWorkerConfig: IdWorkerConfig)(implicit system: ActorSystem): ActorRef = {
    val actorRef = ClusterSharding(system).start(
      shardName,
      Props(new SupervisorActor(ShardedIdWorkers.props(idWorkerConfig), SupervisorStrategy.defaultStrategy)),
      ClusterShardingSettings(system),
      extractEntityId,
      extractShardId
    )
    actorRef
  }

}

class ShardedIdWorkers(idWorkerConfig: IdWorkerConfig) extends IdWorkers(idWorkerConfig) {

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = StopIdWorker)
    case StopIdWorker =>
      context.stop(self)
    case any =>
      super.unhandled(any)
  }

}
