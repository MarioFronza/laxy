package com.github.laxy.domain.auth

import com.github.laxy.domain.user.Login
import com.github.laxy.domain.user.validPassword
import com.github.laxy.domain.user.validateEmail
import com.github.laxy.domain.validation.accumulateErrors
import com.github.laxy.shared.ApplicationError
import com.github.laxy.shared.InteractionResult

fun Login.validate(): InteractionResult<ApplicationError, Login> =
    listOf(email.validateEmail(), password.validPassword()).accumulateErrors(this)