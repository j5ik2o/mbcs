package com.github.j5ik2o.mbcs.adaptor.aggregate

import java.time.Instant

import akka.actor.{ Actor, ActorLogging, Props, SupervisorStrategy }
import akka.persistence.PersistentActor
import com.github.j5ik2o.mbcs.adaptor.aggregate.UserAccountAggregate.Protocol._
import com.github.j5ik2o.mbcs.adaptor.aggregate.UserAccountAggregate.UserAccountCreated
import com.github.j5ik2o.mbcs.adaptor.utils.SupervisorActor
import com.github.j5ik2o.mbcs.domain.model.{ UserAccount, UserAccountId, UserName }

object UserAccountAggregate {

  def props(threadId: UserAccountId): Props = propWithSupervisor(Props(new PersistentUserAccountAggregate(threadId)))

  def propsWithoutPersist(userAccountId: UserAccountId): Props =
    Props(new UserAccountAggregate(userAccountId))

  def propWithSupervisor(p: => Props) =
    Props(new SupervisorActor(p, SupervisorStrategy.defaultStrategy))

  def name(userAccountId: UserAccountId): String = s"thread-${userAccountId.asString}"

  sealed trait Event
  case class UserAccountCreated(userAccountId: UserAccountId, userName: UserName, createdAt: Instant) extends Event

  object Protocol {

    sealed trait CommandRequest {
      val userAccountId: UserAccountId
    }
    trait CommandResponse

    case class CreateUserAccount(userAccountId: UserAccountId, userName: UserName)    extends CommandRequest
    sealed trait CreateUserResponse                                                   extends CommandResponse
    case class CreateUserAccountSucceeded(userAccountId: UserAccountId)               extends CreateUserResponse
    case class CreateUserAccountFailed(userAccountId: UserAccountId, message: String) extends CreateUserResponse

    case class GetUserAccount(userAccountId: UserAccountId)                        extends CommandRequest
    sealed trait GetUserAccountResponse                                            extends CommandResponse
    case class GetUserAccountSucceeded(userAccount: UserAccount)                   extends GetUserAccountResponse
    case class GetUserAccountFailed(userAccountId: UserAccountId, message: String) extends GetUserAccountResponse

  }

}

class PersistentUserAccountAggregate(userAccountId: UserAccountId) extends PersistentActor with ActorLogging {

  private val childRef = context.actorOf(UserAccountAggregate.propsWithoutPersist(userAccountId))

  override def persistenceId: String = UserAccountAggregate.name(userAccountId)

  override def receiveRecover: Receive = ???

  override def receiveCommand: Receive = {
    case m: CreateUserAccount =>
      persist(UserAccountCreated(m.userAccountId, m.userName, Instant.now())) { _ =>
        childRef forward m
      }
  }
}

class UserAccountAggregate(userAccountId: UserAccountId) extends Actor with ActorLogging {

  override def receive: Receive = onMessage(None)

  private def onMessage(state: Option[UserAccount]): Receive = {
    case m: GetUserAccount =>
      require(userAccountId == m.userAccountId)
      state match {
        case None =>
          sender() ! GetUserAccountFailed(userAccountId, "Failed to get status, because it has not been initialized")
        case Some(s) =>
          sender() ! GetUserAccountSucceeded(s)
      }
    case m: CreateUserAccount =>
      require(userAccountId == m.userAccountId)
      context.become(onMessage(Some(UserAccount(m.userAccountId, m.userName, Instant.now, Instant.now))))
      sender() ! CreateUserAccountSucceeded(m.userAccountId)
  }
}
