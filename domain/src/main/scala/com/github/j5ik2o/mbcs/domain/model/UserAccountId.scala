package com.github.j5ik2o.mbcs.domain.model

case class UserAccountId(value: Long) {
  def asString: String = value.toString
}
