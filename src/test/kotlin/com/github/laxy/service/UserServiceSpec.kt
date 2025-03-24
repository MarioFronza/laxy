package com.github.laxy.service

import arrow.core.nonEmptyListOf
import com.github.laxy.EmptyUpdate
import com.github.laxy.IncorrectInput
import com.github.laxy.KotestProject
import com.github.laxy.SuspendFun
import com.github.laxy.UsernameAlreadyExists
import com.github.laxy.auth.JwtToken
import com.github.laxy.persistence.UserId
import com.github.laxy.validation.InvalidEmail
import com.github.laxy.validation.InvalidPassword
import com.github.laxy.validation.InvalidUsername
import io.github.nefilim.kjwt.JWSHMAC512Algorithm
import io.github.nefilim.kjwt.JWT
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.matchers.string.shouldNotBeBlank

class UserServiceSpec :
    SuspendFun({
        val userService = KotestProject.dependencies.get().userService

        val validUsername = "username"
        val validEmail = "valid@domain.com"
        val validPassword = "123456789"

        "register" -
                {
                    "given an empty username, should return IncorrectInput when calls register" {
                        val res =
                            userService.register(
                                RegisterUser(
                                    username = "",
                                    email = validEmail,
                                    password = validPassword
                                )
                            )
                        val errors =
                            nonEmptyListOf("Cannot be blank", "is too short (minimum is 1 characters)")
                        val expected = IncorrectInput(InvalidUsername(errors))
                        res shouldBeLeft expected
                    }

                    "given a too long username, should return IncorrectInput when calls register" {
                        val name = "this is a too long username you should change too a shorter one"
                        val res =
                            userService.register(
                                RegisterUser(
                                    username = name,
                                    email = validEmail,
                                    password = validPassword
                                )
                            )
                        val errors = nonEmptyListOf("is too long (maximum is 25 characters)")
                        val expected = IncorrectInput(InvalidUsername(errors))
                        res shouldBeLeft expected
                    }

                    "given an empty email, should return IncorrectInput when calls register" {
                        val res =
                            userService.register(
                                RegisterUser(
                                    username = validUsername,
                                    email = "",
                                    password = validPassword
                                )
                            )
                        val errors = nonEmptyListOf("Cannot be blank", "'' is invalid email")
                        val expected = IncorrectInput(InvalidEmail(errors))
                        res shouldBeLeft expected
                    }

                    "given a too long email, should return IncorrectInput when calls register" {
                        val email = "${(0..340).joinToString("") { "A" }}@domain.com"
                        val res =
                            userService.register(
                                RegisterUser(
                                    username = validUsername,
                                    email = email,
                                    password = validPassword
                                )
                            )
                        val errors = nonEmptyListOf("is too long (maximum is 350 characters)")
                        val expected = IncorrectInput(InvalidEmail(errors))
                        res shouldBeLeft expected
                    }

                    "given an invalid email, should return IncorrectInput when calls register" {
                        val email = "AAA"
                        val res =
                            userService.register(
                                RegisterUser(
                                    username = validUsername,
                                    email = email,
                                    password = validPassword
                                )
                            )
                        val errors = nonEmptyListOf("'AAA' is invalid email")
                        val expected = IncorrectInput(InvalidEmail(errors))
                        res shouldBeLeft expected
                    }

                    "given an empty password, should return IncorrectInput when calls register" {
                        val res =
                            userService.register(
                                RegisterUser(
                                    username = validUsername,
                                    email = validEmail,
                                    password = ""
                                )
                            )
                        val errors =
                            nonEmptyListOf("Cannot be blank", "is too short (minimum is 8 characters)")
                        val expected = IncorrectInput(InvalidPassword(errors))
                        res shouldBeLeft expected
                    }

                    "given a too long password, should return IncorrectInput when calls register" {
                        val password = (0..100).joinToString("") { "A" }
                        val res =
                            userService.register(
                                RegisterUser(
                                    username = validUsername,
                                    email = validEmail,
                                    password = password
                                )
                            )
                        val errors = nonEmptyListOf("is too long (maximum is 100 characters)")
                        val expected = IncorrectInput(InvalidPassword(errors))
                        res shouldBeLeft expected
                    }

                    "given a valid input, should return Success when calls register" {
                        userService
                            .register(
                                RegisterUser(
                                    username = validUsername,
                                    email = validEmail,
                                    password = validPassword
                                )
                            )
                            .shouldBeRight()
                    }

                    "given a valid input, should return IncorrectInput when call register two times" {
                        userService.register(
                            RegisterUser(
                                username = validUsername,
                                email = validEmail,
                                password = validPassword
                            )
                        )
                        val res =
                            userService.register(
                                RegisterUser(
                                    username = validUsername,
                                    email = validEmail,
                                    password = validPassword
                                )
                            )
                        res shouldBeLeft UsernameAlreadyExists(validUsername)
                    }
                }

        "login" -
                {
                    "given an empty email, should return IncorrectInput when calls login" {
                        val res = userService.login(Login(email = "", password = validPassword))
                        val errors = nonEmptyListOf("Cannot be blank", "'' is invalid email")
                        val expected = IncorrectInput(InvalidEmail(errors))
                        res shouldBeLeft expected
                    }

                    "given a too long email, should return IncorrectInput when calls login" {
                        val email = "${(0..340).joinToString("") { "A" }}@domain.com"
                        val res = userService.login(Login(email = email, password = validPassword))
                        val errors = nonEmptyListOf("is too long (maximum is 350 characters)")
                        val expected = IncorrectInput(InvalidEmail(errors))
                        res shouldBeLeft expected
                    }

                    "given an invalid email, should return IncorrectInput when calls login" {
                        val email = "AAAA"
                        val res = userService.login(Login(email = email, password = validPassword))
                        val errors = nonEmptyListOf("'AAAA' is invalid email")
                        val expected = IncorrectInput(InvalidEmail(errors))
                        res shouldBeLeft expected
                    }

                    "given an empty password, should return IncorrectInput when calls login" {
                        val res = userService.login(Login(email = validEmail, password = ""))
                        val errors =
                            nonEmptyListOf("Cannot be blank", "is too short (minimum is 8 characters)")
                        val expected = IncorrectInput(InvalidPassword(errors))
                        res shouldBeLeft expected
                    }

                    "given a too long password, should return IncorrectInput when calls login" {
                        val password = (0..100).joinToString("") { "A" }
                        val res = userService.login(Login(email = validEmail, password = password))
                        val errors = nonEmptyListOf("is too long (maximum is 100 characters)")
                        val expected = IncorrectInput(InvalidPassword(errors))
                        res shouldBeLeft expected
                    }

                    "given a valid input, should return Success when call login" {
                        userService.register(
                            RegisterUser(
                                username = validUsername,
                                email = validEmail,
                                password = validPassword
                            )
                        )
                        val res =
                            userService
                                .login(Login(email = validEmail, password = validPassword))
                                .shouldBeRight()
                        res.first.value.shouldNotBeBlank()
                    }
                }

        "update" -
                {
                    "given a valid input, should update with all null values when calls update" {
                        val token =
                            userService
                                .register(RegisterUser(validUsername, validEmail, validPassword))
                                .shouldBeRight()
                        val res = userService.update(Update(token.id(), null, null, null))
                        res shouldBeLeft
                                EmptyUpdate("Cannot update user with ${token.id()} with only null values")
                    }
                }
    })

private fun JwtToken.id(): UserId =
    JWT.decodeT(value, JWSHMAC512Algorithm)
        .shouldBeRight { "JWToken $value should be valid JWT but found $it" }
        .jwt
        .claimValueAsLong("id")
        .shouldBeSome { "JWTToken $value should have id but found None" }
        .let(::UserId)
