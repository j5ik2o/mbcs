package com.github.j5ik2o.mbcs.adaptor.idworker
import java.nio.charset.StandardCharsets

import akka.actor.{ ActorRef, Props, ReceiveTimeout }
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorker.GenerateId
import com.github.j5ik2o.mbcs.adaptor.idworker.ShardedIdWorker.StopIdWorker
import net.boeckling.crc.CRC64

import scala.util.Random

object ShardedIdWorker {
  def props(idWorkerConfig: IdWorkerConfig, idWorkerIdController: ActorRef): Props =
    Props(new ShardedIdWorker(idWorkerConfig, idWorkerIdController))

  val shardName = "id-worker"

  case object StopIdWorker

  val random = new Random(32)

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: GenerateId =>
      ((random.nextLong() % 32).toString, cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: GenerateId =>
      (CRC64.fromBytes(cmd.id.value.getBytes(StandardCharsets.UTF_8)).getValue % 8).toString
  }

}

class ShardedIdWorker(override val idWorkerConfig: IdWorkerConfig, idWorkerIdController: ActorRef)
    extends IdWorker(idWorkerConfig, idWorkerIdController) {

  override def unhandled(message: Any): Unit = message match {
    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = StopIdWorker)
    case StopIdWorker =>
      context.stop(self)
  }

}
