package com.github.j5ik2o.mbcs.adaptor.idworker

import java.net.URI

import akka.cluster.Cluster
import akka.persistence.Persistence
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorkerIdController.Protocol.{BorrowId, BorrowIdSucceeded, GetBorrowIds, GetBorrowIdsSucceeded}
import com.github.j5ik2o.mbcs.adaptor.utils.DynamoDBSpecSupport
import com.github.j5ik2o.mbcs.domain.model.ULID
import com.github.j5ik2o.reactive.aws.dynamodb.DynamoDBAsyncClientV2
import org.scalatest.BeforeAndAfterAll
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import scala.concurrent.duration._

class IdWorkerIdControllerSingletonSpecNode1
    extends IdWorkerIdControllerSingletonSpec
    with BeforeAndAfterAll
    with DynamoDBSpecSupport {
  implicit val pc: PatienceConfig = PatienceConfig(20 seconds, 1 seconds)

  override protected lazy val dynamoDBPort: Int = 8000

  val underlying: DynamoDbAsyncClient = DynamoDbAsyncClient
    .builder()
    .credentialsProvider(
      StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey))
    )
    .endpointOverride(URI.create(dynamoDBEndpoint))
    .build()

  import scala.concurrent.ExecutionContext.Implicits.global

  override def asyncClient: DynamoDBAsyncClientV2 = DynamoDBAsyncClientV2(underlying)

  override def beforeAll: Unit = {
    super.beforeAll()
    createTable()
  }
  override def afterAll: Unit  = {
    // deleteTable()
    super.afterAll()
  }
}

class IdWorkerIdControllerSingletonSpecNode2 extends IdWorkerIdControllerSingletonSpec

class IdWorkerIdControllerSingletonSpecNode3 extends IdWorkerIdControllerSingletonSpec


//object MultiNodeSampleConfig extends AkkaMultiNodeConfig {
//  val controller = role("controller")
//  val node1      = role("node1")
//  val node2      = role("node2")
//
//  commonConfig(
//    ConfigFactory.parseString(
//      """
//        |akka {
//        |  stdout-loglevel = off // defaults to WARNING can be disabled with off. The stdout-loglevel is only in effect during system startup and shutdown
//        |  log-dead-letters-during-shutdown = on
//        |  loglevel = debug
//        |  log-dead-letters = on
//        |  log-config-on-start = off // Log the complete configuration at INFO level when the actor system is started
//        |
//        |  loggers = ["akka.event.slf4j.Slf4jLogger"]
//        |  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
//        |
//        |  actor {
//        |    debug {
//        |      receive = on // log all messages sent to an actor if that actors receive method is a LoggingReceive
//        |      autoreceive = off // log all special messages like Kill, PoisoffPill etc sent to all actors
//        |      lifecycle = off // log all actor lifecycle events of all actors
//        |      fsm = off // enable logging of all events, transitioffs and timers of FSM Actors that extend LoggingFSM
//        |      event-stream = off // enable logging of subscriptions (subscribe/unsubscribe) on the ActorSystem.eventStream
//        |    }
//        |  }
//        |}
//        |
//        |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
//        |
//        |akka.persistence.journal.plugin = "dynamo-db-journal"
//        |akka.persistence.snapshot-store.plugin = "dynamo-db-snapshot"
//        |
//        |dynamo-db-journal {
//        |  class = "com.github.j5ik2o.akka.persistence.dynamodb.journal.DynamoDBJournal"
//        |  plugin-dispatcher = "akka.actor.default-dispatcher"
//        |  dynamodb-client {
//        |    access-key-id = "x"
//        |    secret-access-key = "x"
//        |    endpoint = "http://127.0.0.1:8000/"
//        |  }
//        |}
//        |
//        |dynamo-db-snapshot {
//        |  class = "com.github.j5ik2o.akka.persistence.dynamodb.snapshot.DynamoDBSnapshotStore"
//        |  plugin-dispatcher = "akka.actor.default-dispatcher"
//        |  dynamodb-client {
//        |    access-key-id = "x"
//        |    secret-access-key = "x"
//        |    endpoint = "http://127.0.0.1:8000/"
//        |  }
//        |}
//        |
//        |dynamo-db-read-journal {
//        |  class = "com.github.j5ik2o.akka.persistence.dynamodb.query.DynamoDBReadJournalProvider"
//        |  write-plugin = "dynamo-db-journal"
//        |  dynamodb-client {
//        |    access-key-id = "x"
//        |    secret-access-key = "x"
//        |    endpoint = "http://127.0.0.1:8000/"
//        |  }
//        |}
//        |
//        |akka.cluster.metrics.enabled=off
//      """.stripMargin
//    )
//  )
//
//}

class IdWorkerIdControllerSingletonSpec
    extends MultiNodeSpec(MultiNodeSampleConfig)
    with STMultiNodeSpecSupport
    with ImplicitSender {
  import MultiNodeSampleConfig._
  override def initialParticipants: Int = roles.size

  def join(from: RoleName, to: RoleName): Unit = {
    runOn(from) {
      Cluster(system) join node(to).address
    }
    enterBarrier(from.name + "-joined")
  }

  "IdWorkerIdControllerSingleton" - {
    "join-cluster" in {
      Persistence(system)
      within(15 seconds) {
        join(node1, node1)
        join(node2, node1)
        enterBarrier("cluster joined")
        IdWorkerIdControllerSingleton.singletonManager(1, 5)
        enterBarrier("singletonManager start")
        runOn(node1) {
          val actorRef = system.actorOf(IdWorkerIdControllerProxy.props(ULID.generate))
          actorRef ! GetBorrowIds
          val result = expectMsgClass(classOf[GetBorrowIdsSucceeded])
          log.debug(result.toString)
        }
        enterBarrier("GetBorrowIds-1")
        runOn(node2) {
          val actorRef = system.actorOf(IdWorkerIdControllerProxy.props(ULID.generate))
          actorRef ! BorrowId
          val result = expectMsgClass(classOf[BorrowIdSucceeded])
          log.debug(result.toString)
        }
        enterBarrier("BorrowId")
        runOn(node1) {
          val actorRef = system.actorOf(IdWorkerIdControllerProxy.props(ULID.generate))
          actorRef ! GetBorrowIds
          val result = expectMsgClass(classOf[GetBorrowIdsSucceeded])
          log.debug(result.toString)
        }
        enterBarrier("GetBorrowIds-2")
      }
    }
  }
}
