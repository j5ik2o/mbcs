package com.github.j5ik2o.mbcs.adaptor.idworker

import java.net.URI

import akka.cluster.Cluster
import akka.persistence.Persistence
import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeSpec
import akka.testkit.ImplicitSender
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorkerIdController.Protocol.{
  BorrowId,
  BorrowIdSucceeded,
  GetBorrowIds,
  GetBorrowIdsSucceeded
}
import com.github.j5ik2o.mbcs.adaptor.utils.DynamoDBSpecSupport
import com.github.j5ik2o.mbcs.domain.model.ULID
import com.github.j5ik2o.reactive.aws.dynamodb.DynamoDBAsyncClientV2
import org.scalatest.BeforeAndAfterAll
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import scala.concurrent.duration._

class IdWorkerIdControllerSingletonMultiJvmNode1
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
  override def afterAll: Unit = {
    // deleteTable()
    super.afterAll()
  }
}

class IdWorkerIdControllerSingletonMultiJvmNode2 extends IdWorkerIdControllerSingletonSpec

class IdWorkerIdControllerSingletonMultiJvmNode3 extends IdWorkerIdControllerSingletonSpec

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
