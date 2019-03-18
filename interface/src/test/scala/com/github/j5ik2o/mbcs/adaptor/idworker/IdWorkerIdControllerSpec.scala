package com.github.j5ik2o.mbcs.adaptor.idworker

import java.net.URI

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorkerIdController.Protocol._
import com.github.j5ik2o.mbcs.adaptor.utils.DynamoDBSpecSupport
import com.github.j5ik2o.reactive.aws.dynamodb.DynamoDBAsyncClientV2
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

class IdWorkerIdControllerSpec
    extends TestKit(ActorSystem("IdWorkerIdControllerSpec"))
    with FreeSpecLike
    with Matchers
    with ImplicitSender
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

  before { createTable }

  after { deleteTable }

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "IdWorkerIdControllerSpec" - {
    "BorrowId" in {
      val result               = ArrayBuffer.empty[BorrowIdSucceeded]
      val idWorkerIdController = system.actorOf(IdWorkerIdController.props(1, 5))
      idWorkerIdController ! BorrowId
      val response1 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response1)
      idWorkerIdController ! BorrowId
      val response2 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response2)
      idWorkerIdController ! BorrowId
      val response3 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response3)
      idWorkerIdController ! BorrowId
      val response4 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response4)
      idWorkerIdController ! BorrowId
      val response5 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response5)
      result.map(_.id).sorted.toVector shouldBe (1 to 5).toVector
    }
    "When error, BorrowId" in {
      val result               = ArrayBuffer.empty[BorrowIdSucceeded]
      val idWorkerIdController = system.actorOf(IdWorkerIdController.props(1, 5))
      idWorkerIdController ! BorrowId
      val response1 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response1)
      idWorkerIdController ! BorrowId
      val response2 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response2)
      idWorkerIdController ! BorrowId
      val response3 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response3)
      idWorkerIdController ! BorrowId
      val response4 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response4)
      idWorkerIdController ! BorrowId
      val response5 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response5)
      result.map(_.id).sorted.toVector shouldBe (1 to 5).toVector
      idWorkerIdController ! BorrowId
      expectMsgClass(classOf[BorrowIdFailed])
    }
    "ReturnId" in {
      val result               = ArrayBuffer.empty[BorrowIdSucceeded]
      val idWorkerIdController = system.actorOf(IdWorkerIdController.props(1, 5))
      idWorkerIdController ! BorrowId
      val response1 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response1)
      idWorkerIdController ! ReturnId(response1.id, noReply = false)
      expectMsg(ReturnIdSucceeded)

      idWorkerIdController ! BorrowId
      val response2 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response2)
      idWorkerIdController ! BorrowId
      val response3 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response3)
      idWorkerIdController ! BorrowId
      val response4 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response4)
      idWorkerIdController ! BorrowId
      val response5 = expectMsgClass(classOf[BorrowIdSucceeded])
      result.append(response5)
      result.map(_.id).toSet.size shouldBe >=(4)
    }
    "GetIds" in {
      val idWorkerIdController = system.actorOf(IdWorkerIdController.props(1, 5))
      idWorkerIdController ! BorrowId
      idWorkerIdController ! BorrowId
      receiveN(2)
      idWorkerIdController ! GetBorrowIds
      val response = expectMsgClass(classOf[GetBorrowIdsSucceeded])
      response.idWorkers.size shouldBe 2
    }

  }
}
