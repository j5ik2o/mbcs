package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorker.{ GenerateId, IdGenerated }
import org.scalatest.{ FreeSpecLike, Matchers }

class IdWorkerSpec extends TestKit(ActorSystem("IdWorkerSpec")) with FreeSpecLike with Matchers with ImplicitSender {

  "IdWorker" - {
    "generateId" in {
      val idWorkerRef = system.actorOf(IdWorker.props(IdWorkerConfig(), dataCenterId = 1, workerId = 1))
      idWorkerRef ! GenerateId(1, 1)
      val result1 = expectMsgClass(classOf[IdGenerated])
      println(result1)
      idWorkerRef ! GenerateId(1, 1)
      val result2 = expectMsgClass(classOf[IdGenerated])
      println(result2)
      idWorkerRef ! GenerateId(1, 1)
      val result3 = expectMsgClass(classOf[IdGenerated])
      println(result3)
    }
  }

}
