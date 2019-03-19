package com.github.j5ik2o.mbcs.domain.model

case class ThreadId(value: Long) {
  def asString: String = value.toString
}

case class Thread(id: ThreadId, title: ThreadTitle, parentThreadRef: Option[ThreadRef], messageCount: Long = 0L)
