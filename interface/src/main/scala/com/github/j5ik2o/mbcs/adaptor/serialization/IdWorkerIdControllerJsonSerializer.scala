//package com.github.j5ik2o.mbcs.adaptor.serialization
//import java.nio.charset.StandardCharsets
//
//import akka.serialization.SerializerWithStringManifest
//import io.circe.generic.auto._
//import io.circe.java8.time.{JavaTimeDecoders, JavaTimeEncoders}
//import io.circe.parser._
//import io.circe.syntax._
//
//class IdWorkerIdControllerJsonSerializer
//    extends SerializerWithStringManifest
//    with JavaTimeDecoders
//    with JavaTimeEncoders {
//
//  final val IdBorrowedManifest = classOf[IdBorrowed].getName
//  final val IdReturnedManifest = classOf[IdReturned].getName
//
//  override def identifier: Int = 10001
//
//  override def manifest(o: AnyRef): String = o.getClass.getName
//
//  override def toBinary(o: AnyRef): Array[Byte] = o match {
//    case e: IdBorrowed => e.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)
//    case e: IdReturned => e.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)
//    case x             => throw new NotImplementedError("Can not serialize '" + x + "'")
//  }
//
//  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {
//    case IdBorrowedManifest => decode[IdBorrowed](new String(bytes, StandardCharsets.UTF_8)).right.get
//    case IdReturnedManifest => decode[IdReturned](new String(bytes, StandardCharsets.UTF_8)).right.get
//    case x                  => throw new NotImplementedError("Can not deserialize '" + x + "'")
//  }
//
//}
