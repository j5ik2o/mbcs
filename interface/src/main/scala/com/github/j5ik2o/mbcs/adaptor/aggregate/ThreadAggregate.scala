package com.github.j5ik2o.mbcs.adaptor.aggregate

import java.time.Instant

import akka.actor.{ Actor, ActorLogging, Props, SupervisorStrategy }
import akka.persistence.PersistentActor
import com.github.j5ik2o.mbcs.adaptor.aggregate.ThreadAggregate.Protocol._
import com.github.j5ik2o.mbcs.adaptor.aggregate.ThreadAggregate.ThreadCreated
import com.github.j5ik2o.mbcs.adaptor.utils.SupervisorActor
import com.github.j5ik2o.mbcs.domain.model.{ MemberId, Thread, ThreadId, ThreadRef, ThreadTitle, UserAccountId }

object ThreadAggregate {

  def props(threadId: ThreadId): Props               = propWithSupervisor(Props(new PersistentThreadAggregate(threadId)))
  def propsWithoutPersist(threadId: ThreadId): Props = Props(new ThreadAggregate(threadId))

  def propWithSupervisor(p: => Props) =
    Props(new SupervisorActor(p, SupervisorStrategy.defaultStrategy))

  def name(threadId: ThreadId): String = s"thread-${threadId.asString}"

  sealed trait Event
  case class ThreadCreated(threadId: ThreadId,
                           title: ThreadTitle,
                           parentThreadRef: Option[ThreadRef],
                           createdAt: Instant)
      extends Event

  object Protocol {
    sealed trait CommandRequest {
      val threadId: ThreadId
    }
    trait CommandResponse

    case class CreateThread(threadId: ThreadId, title: ThreadTitle, parentThreadRef: Option[ThreadRef])
        extends CommandRequest
    sealed trait CreateThreadResponse                                  extends CommandResponse
    case class CreateThreadSucceeded(threadId: ThreadId)               extends CreateThreadResponse
    case class CreateThreadFailed(threadId: ThreadId, message: String) extends CreateThreadResponse

    case class GetThread(threadId: ThreadId)                        extends CommandRequest
    sealed trait GetThreadResponse                                  extends CommandResponse
    case class GetThreadSucceeded(thread: Thread)                   extends GetThreadResponse
    case class GetThreadFailed(threadId: ThreadId, message: String) extends GetThreadResponse

    case class AddMembers(threadId: ThreadId, memberIds: Seq[UserAccountId]) extends CommandRequest
    case class RemoveMembers(threadId: ThreadId, memberIds: Seq[MemberId])   extends CommandRequest
  }

}

class PersistentThreadAggregate(threadId: ThreadId) extends PersistentActor with ActorLogging {

  private val childRef = context.actorOf(ThreadAggregate.propsWithoutPersist(threadId), "child")

  override def persistenceId: String = ThreadAggregate.name(threadId)

  override def receiveRecover: Receive = {
    case e: ThreadCreated =>
      childRef ! CreateThread(e.threadId, e.title, e.parentThreadRef)
  }

  override def receiveCommand: Receive = {
    case m: CreateThread =>
      persist(ThreadCreated(m.threadId, m.title, m.parentThreadRef, Instant.now())) { _ =>
        childRef forward m
      }
  }

}

class ThreadAggregate(threadId: ThreadId) extends Actor with ActorLogging {

  override def receive: Receive = onMessage(None)

  private def onMessage(state: Option[Thread]): Receive = {
    case m: GetThread =>
      require(threadId == m.threadId)
      state match {
        case None =>
          sender() ! GetThreadFailed(threadId, "Failed to get status, because it has not been initialized")
        case Some(s) =>
          sender() ! GetThreadSucceeded(s)
      }
    case m: CreateThread =>
      require(threadId == m.threadId)
      context.become(onMessage(Some(Thread(m.threadId, m.title, m.parentThreadRef))))
      sender() ! CreateThreadSucceeded(m.threadId)
  }
}
