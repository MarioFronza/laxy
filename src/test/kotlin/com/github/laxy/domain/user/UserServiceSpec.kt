package com.github.laxy.domain.user

import com.github.laxy.KotestProject
import com.github.laxy.persistence.UserId
import com.github.laxy.shared.Failure
import com.github.laxy.shared.IllegalStateError.Companion.illegalStates
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class UserServiceSpec : FreeSpec({

    val userService = KotestProject.dependencies.userService

    val validUsername = "username"
    val validEmail = "valid@domain.com"
    val validPassword = "123456789"

    "username cannot be empty" {
        val res = userService.register(
            RegisterUser(
                username = "",
                email = validEmail,
                password = validPassword
            )
        )
        val errors = listOf("Cannot be blank", "is too short (minimum is 1 characters)")
        val expected = Failure<UserId>(illegalStates(errors))
        res shouldBe expected
    }

})
