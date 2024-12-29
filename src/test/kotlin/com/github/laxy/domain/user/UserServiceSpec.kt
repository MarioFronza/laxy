package com.github.laxy.domain.user

import com.github.laxy.KotestProject
import com.github.laxy.SuspendFun
import com.github.laxy.auth.JwtToken
import com.github.laxy.shared.Failure
import com.github.laxy.shared.IllegalStateError.Companion.illegalStates
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class UserServiceSpec : SuspendFun({

    val userService = KotestProject.dependencies.get().userService

    val validUsername = "username"
    val validEmail = "valid@domain.com"
    val validPassword = "123456789"

    "given an empty username, should return Failure when calls register" {
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

    "given a too long username, should return Failure when calls register" {
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

    "given an empty email, should return Failure when calls register" {
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

    "given a too long email, should return Failure when calls register" {
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

    "given an invalid email, should return Failure when calls register" {
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

    "given an empty password, should return Failure when calls register" {
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

    "given a too long password, should return Failure when calls register" {
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

    "given a valid input, should return Success when calls register" {
        val res = userService.register(
            RegisterUser(
                username = validUsername,
                email = validEmail,
                password = validPassword
            )
        )
        res.bind()::class.java shouldBeSameInstanceAs JwtToken::class.java
    }

    "given a valid input, should return Failure when call register two times" {
        val firstRes = userService.register(
            RegisterUser(
                username = validUsername,
                email = validEmail,
                password = validPassword
            )
        )
        firstRes.bind()::class.java shouldBeSameInstanceAs JwtToken::class.java
        val secondRest = userService.register(
            RegisterUser(
                username = validUsername,
                email = validEmail,
                password = validPassword
            )
        )
        val errors = listOf("$validUsername already exists")
        val expected = Failure<JwtToken>(illegalStates(errors))
        expected shouldBe secondRest
    }

    "given an empty email, should return Failure when calls login" {
        val res = userService.login(
            Login(
                email = "",
                password = validPassword
            )
        )
        val errors = listOf("cannot be blank", "'' is invalid email")
        val expected = Failure<Pair<JwtToken, UserInfo>>(illegalStates(errors))
        expected shouldBe res
    }

    "given a too long email, should return Failure when calls login" {
        val email = "${(0..340).joinToString("") { "A" }}@domain.com"
        val res = userService.login(
            Login(
                email = email,
                password = validPassword
            )
        )
        val errors = listOf("is too long (maximum is 350 characters)")
        val expected = Failure<Pair<JwtToken, UserInfo>>(illegalStates(errors))
        expected shouldBe res
    }

    "given an invalid email, should return Failure when calls login" {
        val email = "AAAA"
        val res = userService.login(
            Login(
                email = email,
                password = validPassword
            )
        )
        val errors = listOf("'AAAA' is invalid email")
        val expected = Failure<Pair<JwtToken, UserInfo>>(illegalStates(errors))
        expected shouldBe res
    }

    "given an empty password, should return Failure when calls login" {
        val res = userService.login(
            Login(
                email = validEmail,
                password = ""
            )
        )
        val errors = listOf("cannot be blank", "is too short (minimum is 8 characters)")
        val expected = Failure<Pair<JwtToken, UserInfo>>(illegalStates(errors))
        expected shouldBe res
    }

    "given a too long password, should return Failure when calls login" {
        val password = (0..100).joinToString("") { "A" }
        val res = userService.login(
            Login(
                email = validEmail,
                password = password
            )
        )
        val errors = listOf("is too long (maximum is 100 characters)")
        val expected = Failure<Pair<JwtToken, UserInfo>>(illegalStates(errors))
        expected shouldBe res
    }

    "given a valid input, should return Success when call login" {
        userService.register(
            RegisterUser(
                username = validUsername,
                email = validEmail,
                password = validPassword
            )
        )
        val res = userService.login(
            Login(
                email = validEmail,
                password = validPassword
            )
        )
        val expectedUser = UserInfo(
            username = "username",
            email = "valid@domain.com"
        )
        val (token, user) = res.bind()
        token::class.java shouldBeSameInstanceAs JwtToken::class.java
        user::class.java shouldBeSameInstanceAs UserInfo::class.java
        user shouldBe expectedUser
    }
})
