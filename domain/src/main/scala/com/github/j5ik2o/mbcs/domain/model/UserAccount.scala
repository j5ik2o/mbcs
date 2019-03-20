package com.github.j5ik2o.mbcs.domain.model

import java.time.Instant

case class UserAccount(id: UserAccountId, name: UserName, createdAt: Instant, updatedAt: Instant)
