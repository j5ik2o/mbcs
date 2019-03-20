package com.github.j5ik2o.mbcs.adaptor.json

import com.github.j5ik2o.mbcs.adaptor.aggregate.ThreadAggregate.ThreadCreated
import com.github.j5ik2o.mbcs.adaptor.serialization.ModelToJsonReprIso
import com.github.j5ik2o.mbcs.domain.model.{ ThreadId, ThreadTitle }

case class ThreadCreatedJson(id: Long, title: String, parentThreadRef: Option[ThreadRefJson])

object ThreadCreatedJson {
  
  import ThreadRefJson._

  implicit object ThreadCreatedIso extends ModelToJsonReprIso[ThreadCreated, ThreadCreatedJson] {

    override def convertTo(
        event: ThreadCreated
    ): ThreadCreatedJson =
      ThreadCreatedJson(id = event.threadId.value,
                        title = event.title.value,
                        parentThreadRef = event.parentThreadRef.map(ThreadRefIso.convertTo))

    override def convertFrom(
        json: ThreadCreatedJson
    ): ThreadCreated = ThreadCreated(ThreadId(json.id), ThreadTitle(json.title), null, null)

  }

}
