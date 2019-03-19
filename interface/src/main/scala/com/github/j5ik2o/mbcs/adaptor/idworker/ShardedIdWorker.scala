package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.{ Props, ReceiveTimeout }
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorker.GenerateId
import com.github.j5ik2o.mbcs.adaptor.idworker.ShardedIdWorker.StopIdWorker

object ShardedIdWorker {
  def props(idWorkerConfig: IdWorkerConfig): Props =
    Props(new ShardedIdWorker(idWorkerConfig))

  val shardName = "id-worker"

  case object StopIdWorker

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: GenerateId =>
      ((cmd.idWorkerId % 32).toString, cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: GenerateId =>
      (cmd.idWorkerId % 8).toString
  }

}

class ShardedIdWorker(override val idWorkerConfig: IdWorkerConfig) extends IdWorker(idWorkerConfig) {

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = StopIdWorker)
    case StopIdWorker =>
      context.stop(self)
  }

}
