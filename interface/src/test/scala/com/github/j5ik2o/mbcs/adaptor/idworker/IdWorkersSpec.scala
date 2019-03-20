package com.github.j5ik2o.mbcs.adaptor.idworker

import akka.actor.ActorSystem
import akka.testkit.{ ImplicitSender, TestKit }
import com.github.j5ik2o.mbcs.adaptor.idworker.IdWorker.{ GenerateId, IdGenerated }
import org.scalatest.{ FreeSpecLike, Matchers }

class IdWorkersSpec extends TestKit(ActorSystem("IdWorkersSpec")) with FreeSpecLike with Matchers with ImplicitSender {

  "IdWorkers" - {
    "generateId" in {
      val idWorkerRef = system.actorOf(IdWorkers.props(IdWorkerConfig()))
      idWorkerRef ! GenerateId(1, 1)
      val result1 = expectMsgClass(classOf[IdGenerated])
      println(result1)
      idWorkerRef ! GenerateId(1, 2)
      val result2 = expectMsgClass(classOf[IdGenerated])
      println(result2)
      idWorkerRef ! GenerateId(1, 3)
      val result3 = expectMsgClass(classOf[IdGenerated])
      println(result3)
    }
  }

}
