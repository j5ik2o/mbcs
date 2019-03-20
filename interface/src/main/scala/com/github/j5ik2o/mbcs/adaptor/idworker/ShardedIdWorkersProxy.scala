package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.cluster.sharding.ClusterSharding

object ShardedIdWorkersProxy {
  def props: Props = Props(new ShardedIdWorkersProxy())
  def name: String = "sharded-id-workers-proxy"

  def getShardRegionRef(system: ActorSystem): ActorRef =
    ClusterSharding(system).shardRegion(ShardedIdWorkers.shardName)

}

class ShardedIdWorkersProxy extends Actor {
  import ShardedIdWorkersProxy._

  override def receive: Receive = {
    case msg =>
      getShardRegionRef(context.system) forward msg
  }

}
