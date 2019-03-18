package com.github.j5ik2o.mbcs.adaptor.idworker
import akka.remote.testkit.MultiNodeSpecCallbacks
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike, Matchers}

trait STMultiNodeSpecSupport extends MultiNodeSpecCallbacks with FreeSpecLike with Matchers with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()

}
