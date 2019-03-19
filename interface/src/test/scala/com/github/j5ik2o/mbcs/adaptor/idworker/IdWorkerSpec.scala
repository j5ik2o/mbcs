package com.github.j5ik2o.mbcs.adaptor.idworker
import java.net.URI

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorker.{ GenerateId, IdGenerated }
import com.github.j5ik2o.mbcs.adaptor.utils.DynamoDBSpecSupport
import com.github.j5ik2o.reactive.aws.dynamodb.DynamoDBAsyncClientV2
import org.scalatest.{ FreeSpecLike, Matchers }
import software.amazon.awssdk.auth.credentials.{ AwsBasicCredentials, StaticCredentialsProvider }
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient

import scala.concurrent.duration._

class IdWorkerSpec
    extends TestKit(ActorSystem("IdWorkerSpec"))
    with FreeSpecLike
    with Matchers
    with ImplicitSender
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

  "IdWorkerSpec" - {
    "generateId" in {
      val idWorkerRef = system.actorOf(IdWorker.props(IdWorkerConfig(dataCenterId = 1L)))
      idWorkerRef ! GenerateId()
      expectMsgClass(classOf[IdGenerated])
      Thread.sleep(1000 * 5)
      idWorkerRef ! GenerateId()
      expectMsgClass(classOf[IdGenerated])
      Thread.sleep(1000 * 5)
      idWorkerRef ! GenerateId()
      expectMsgClass(classOf[IdGenerated])

    }
  }

}
