package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }

object ShardedIdWorkers {
  def props: Props = Props(new ShardedIdWorkers())
  def name: String = "sharded-id-workers"

  def start(idWorkerConfig: IdWorkerConfig, idWorkerIdController: ActorRef)(implicit system: ActorSystem): ActorRef = {
    val actorRef = ClusterSharding(system).start(
      ShardedIdWorker.shardName,
      ShardedIdWorker.props(idWorkerConfig, idWorkerIdController),
      ClusterShardingSettings(system),
      ShardedIdWorker.extractEntityId,
      ShardedIdWorker.extractShardId
    )
    actorRef
  }

  def shardRegion(system: ActorSystem): ActorRef =
    ClusterSharding(system).shardRegion(ShardedIdWorker.shardName)

}

class ShardedIdWorkers extends Actor {

  private val shardRegion = ShardedIdWorkers.shardRegion(context.system)

  override def receive: Receive = {
    case msg =>
      shardRegion forward msg
  }

}
