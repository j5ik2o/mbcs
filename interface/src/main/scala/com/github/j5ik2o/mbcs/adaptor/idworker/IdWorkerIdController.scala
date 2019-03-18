package com.github.j5ik2o.mbcs.adaptor.idworker
import java.time.Instant
import java.util.concurrent.TimeUnit

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorkerIdController.Event.{IdBorrowed, IdReturned}
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorkerIdController.Protocol._
import com.google.common.cache.{Cache, CacheBuilder}
import scala.collection.JavaConverters._

import scala.util.Random

object IdWorkerIdController {

  def props(minId: Long, maxId: Long): Props = Props(new IdWorkerIdController(minId, maxId))

  def name: String = "id-worker-id-controller"

  object Event {
    sealed trait Event
    case class IdBorrowed(id: Long, createAt: Instant) extends Event
    case class IdReturned(id: Long, createAt: Instant) extends Event
  }

  object Protocol {

    sealed trait Request
    sealed trait Response

    case class Ping(id: Long) extends Request
    case class Pong(id: Long) extends Response

    case object BorrowId                       extends Request
    case class BorrowIdSucceeded(id: Long)     extends Response
    case class BorrowIdFailed(message: String) extends Response

    case class ReturnId(id: Long, noReply: Boolean = true) extends Request
    case object ReturnIdSucceeded                          extends Response
    case class ReturnIdFailed(message: String)             extends Response

    case object GetBorrowIds                                        extends Request
    case class GetBorrowIdsSucceeded(idWorkers: Map[Long, Instant]) extends Response
    case class GetBorrowIdsFailed(message: String)                  extends Response
  }

}

class IdWorkerIdController(minId: Long, maxId: Long) extends PersistentActor with ActorLogging {

  private val cache: Cache[Long, Instant] = CacheBuilder.newBuilder()
    .expireAfterWrite(30, TimeUnit.SECONDS).build().asInstanceOf[Cache[Long, Instant]]

  private def scalaMap = cache.asMap().asScala.toMap

  private var counter: Long                   = 1L

  override def persistenceId: String = IdWorkerIdController.name

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, ids) =>
      cache.putAll(ids.asInstanceOf[Map[Long, Instant]].asJava)
    case IdBorrowed(id, createdAt) =>
      cache.put(id, createdAt)
    case IdReturned(id, _) =>
      cache.invalidate(id)
  }

  override def receiveCommand: Receive = {
    case GetBorrowIds =>
      sender() ! GetBorrowIdsSucceeded(scalaMap)
    case Ping(id) =>
      log.debug(s"Ping($id)")
      cache.put(id, Instant.now())
      sender() ! Pong(id)
    case BorrowId =>
      Random.shuffle((minId to maxId).toList).find { v =>
        !scalaMap.keys.exists(_ == v)
      } match {
        case Some(id) =>
          persist(IdBorrowed(id, Instant.now())) { event =>
            cache.put(event.id, event.createAt)
            sender() ! BorrowIdSucceeded(id)
          }
        case None =>
          sender() ! BorrowIdFailed("Not Found Id Error")
      }
      counter += 1
      if (counter % 100 == 0) {
        saveSnapshot(scalaMap)
      }
    case ReturnId(id, noReply) =>
      scalaMap.get(id) match {
        case Some(_) =>
          persist(IdReturned(id, Instant.now())) { _ =>
            cache.invalidate(id)
            if (!noReply)
              sender() ! ReturnIdSucceeded
          }
        case None =>
          sender() ! ReturnIdFailed("Not Found Id Error")
      }
      counter += 1
      if (counter % 100 == 0) {
        saveSnapshot(scalaMap)
      }
  }

}
