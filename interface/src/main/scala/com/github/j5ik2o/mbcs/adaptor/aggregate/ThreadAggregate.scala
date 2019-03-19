package com.github.j5ik2o.mbcs.adaptor.aggregate

import akka.actor.{ ActorLogging, Props }
import akka.persistence.PersistentActor
import com.github.j5ik2o.mbcs.adaptor.aggregate.ThreadAggregate.Protocol.CreateThread
import com.github.j5ik2o.mbcs.domain.model.{ MemberId, Thread, ThreadId, ThreadRef, ThreadTitle, UserAccountId }

object ThreadAggregate {

  def props(threadId: ThreadId): Props = Props(new ThreadAggregate(threadId))

  def name(threadId: ThreadId): String = s"thread-${threadId.asString}"

  object Protocol {
    sealed trait ThreadCommand {
      val threadId: ThreadId
    }
    case class CreateThread(threadId: ThreadId, title: ThreadTitle, parentThreadRef: Option[ThreadRef])
        extends ThreadCommand
    case class AddMembers(threadId: ThreadId, memberIds: Seq[UserAccountId]) extends ThreadCommand
    case class RemoveMembers(threadId: ThreadId, memberIds: Seq[MemberId])   extends ThreadCommand
  }

}

class ThreadAggregate(threadId: ThreadId) extends PersistentActor with ActorLogging {

  private var thread: Thread = _

  override def persistenceId: String = ThreadAggregate.name(threadId)

  override def receiveCommand: Receive = {
    case m: CreateThread =>
      require(threadId == m.threadId)
      thread = Thread(m.threadId, m.title, m.parentThreadRef)
  }

  override def receiveRecover: Receive = ???

}
