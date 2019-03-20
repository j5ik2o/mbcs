package com.github.j5ik2o.mbcs.adaptor.json

import com.github.j5ik2o.mbcs.adaptor.serialization.ModelToJsonReprIso
import com.github.j5ik2o.mbcs.domain.model.{ Hash, ThreadId, ThreadRef }

case class ThreadRefJson(threadId: Long, hash: String)

object ThreadRefJson {

  implicit object ThreadRefIso extends ModelToJsonReprIso[ThreadRef, ThreadRefJson] {
    override def convertTo(
        model: ThreadRef
    ): ThreadRefJson = ThreadRefJson(model.id.value, model.hash.value)

    override def convertFrom(
        json: ThreadRefJson
    ): ThreadRef = ThreadRef(ThreadId(json.threadId), Hash(json.hash))
  }

}
