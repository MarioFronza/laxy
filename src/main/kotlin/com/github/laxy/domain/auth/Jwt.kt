package com.github.laxy.domain.auth

import com.github.laxy.persistence.UserId

@JvmInline
value class JwtToken(val value: String)

data class JwtContext(val token: JwtToken, val userId: UserId)