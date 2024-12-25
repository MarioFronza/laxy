package com.github.laxy.domain.user

import com.github.laxy.KotestProject
import com.github.laxy.auth.JwtToken
import com.github.laxy.shared.Failure
import com.github.laxy.shared.IllegalStateError.Companion.illegalStates
import com.github.laxy.shared.Success
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class UserServiceSpec : FreeSpec({

    val userService = KotestProject.dependencies.userService

    val validUsername = "username"
    val validEmail = "valid@domain.com"
    val validPassword = "123456789"

    "register" - {
        "username cannot be empty" {
            val res = userService.register(
                RegisterUser(
                    username = "",
                    email = validEmail,
                    password = validPassword
                )
            )
            val errors = listOf("cannot be blank", "is too short (minimum is 1 characters)")
            val expected = Failure<JwtToken>(illegalStates(errors))
            res shouldBe expected
        }

        "username long than 25 characters" {
            val name = "this is a too long username you should change to a shorter one"
            val res = userService.register(
                RegisterUser(
                    username = name,
                    email = validEmail,
                    password = validPassword
                )
            )
            val errors = listOf("is too long (maximum is 25 characters)")
            val expected = Failure<JwtToken>(illegalStates(errors))
            res shouldBe expected
        }

        "email cannot be empty" {
            val res = userService.register(
                RegisterUser(
                    username = validUsername,
                    email = "",
                    password = validPassword
                )
            )
            val errors = listOf("cannot be blank", "'' is invalid email")
            val expected = Failure<JwtToken>(illegalStates(errors))
            res shouldBe expected
        }

        "email too long" {
            val email = "${(0..340).joinToString("") { "A" }}@domain.com"
            val res = userService.register(
                RegisterUser(
                    username = validUsername,
                    email = email,
                    password = validPassword
                )
            )
            val errors = listOf("is too long (maximum is 350 characters)")
            val expected = Failure<JwtToken>(illegalStates(errors))
            res shouldBe expected
        }

        "email is not valid" {
            val email = "AAA"
            val res = userService.register(
                RegisterUser(
                    username = validUsername,
                    email = email,
                    password = validPassword
                )
            )
            val errors = listOf("'AAA' is invalid email")
            val expected = Failure<JwtToken>(illegalStates(errors))
            res shouldBe expected
        }

        "password cannot be empty" {
            val res = userService.register(
                RegisterUser(
                    username = validUsername,
                    email = validEmail,
                    password = ""
                )
            )
            val errors = listOf("cannot be blank", "is too short (minimum is 8 characters)")
            val expected = Failure<JwtToken>(illegalStates(errors))
            res shouldBe expected
        }

        "password can be max 100" {
            val password = (0..100).joinToString("") { "A" }
            val res = userService.register(
                RegisterUser(
                    username = validUsername,
                    email = validEmail,
                    password = password
                )
            )
            val errors = listOf("is too long (maximum is 100 characters)")
            val expected = Failure<JwtToken>(illegalStates(errors))
            res shouldBe expected
        }

        "all valid returns a token" {
            val res = userService.register(
                RegisterUser(
                    username = validUsername,
                    email = validEmail,
                    password = validPassword
                )
            )

            res.bind()::class.java shouldBeSameInstanceAs JwtToken::class.java
        }
    }

})
