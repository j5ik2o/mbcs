package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.{ Actor, ActorRef, ActorSystem, Props, SupervisorStrategy }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import com.github.j5ik2o.mbcs.adaptor.utils.SupervisorActor

object ShardedIdWorkers {
  def props: Props = Props(new ShardedIdWorkers())
  def name: String = "sharded-id-workers"

  def startShardRegion(idWorkerConfig: IdWorkerConfig)(implicit system: ActorSystem): ActorRef = {
    val actorRef = ClusterSharding(system).start(
      ShardedIdWorker.shardName,
      Props(new SupervisorActor(ShardedIdWorker.props(idWorkerConfig), SupervisorStrategy.defaultStrategy)),
      ClusterShardingSettings(system),
      ShardedIdWorker.extractEntityId,
      ShardedIdWorker.extractShardId
    )
    actorRef
  }

  def getShardRegionRef(system: ActorSystem): ActorRef =
    ClusterSharding(system).shardRegion(ShardedIdWorker.shardName)

}

class ShardedIdWorkers extends Actor {

  private val shardRegion = ShardedIdWorkers.getShardRegionRef(context.system)

  override def receive: Receive = {
    case msg =>
      shardRegion forward msg
  }

}
