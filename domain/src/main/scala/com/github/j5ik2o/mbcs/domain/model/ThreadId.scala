package com.github.j5ik2o.mbcs.domain.model

case class ThreadId(value: Long) {
  def asString: String = value.toString
}
