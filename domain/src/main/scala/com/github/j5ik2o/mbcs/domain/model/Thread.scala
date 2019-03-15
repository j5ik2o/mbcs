package com.github.j5ik2o.mbcs.domain.model

case class ThreadId(value: Long)

case class Thread(id: ThreadId, title: ThreadTitle, hash: Hash, parentThreadRef: Option[ThreadRef], messageCount: Long)
