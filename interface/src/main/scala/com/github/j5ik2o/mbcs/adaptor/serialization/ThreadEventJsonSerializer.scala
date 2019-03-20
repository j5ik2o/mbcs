package com.github.j5ik2o.mbcs.adaptor.serialization

import java.nio.charset.StandardCharsets

import akka.actor.ExtendedActorSystem
import akka.serialization.SerializerWithStringManifest
import com.github.j5ik2o.mbcs.adaptor.aggregate.ThreadAggregate.ThreadCreated
import org.slf4j.LoggerFactory
import io.circe.syntax._

class ThreadEventJsonSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {

  import io.circe.generic.auto._

  private val logger = LoggerFactory.getLogger(getClass)

  final val ThreadCreatedManifest = classOf[ThreadCreated].getName

  override def identifier: Int             = ???

  override def manifest(o: AnyRef): String = o.getClass.getName

  override def toBinary(o: AnyRef): Array[Byte] = {
    case e: ThreadCreated => CirceJsonSerialization.toBinary(e, false)
    case x             => throw new NotImplementedError("Can not serialize '" + x + "'")
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = ???

}
