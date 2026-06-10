package com.github.laxy.util

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes

val meter = GlobalOpenTelemetry.getMeter("laxy")

val quizCreatedCounter =
    meter.counterBuilder("laxy.quiz.created.total").setDescription("Total quizzes created").build()

val quizAttemptCounter =
    meter
        .counterBuilder("laxy.quiz.attempt.total")
        .setDescription("Total quiz attempts by result")
        .build()

val quizDeletedCounter =
    meter.counterBuilder("laxy.quiz.deleted.total").setDescription("Total quizzes deleted").build()

val userRegisteredCounter =
    meter
        .counterBuilder("laxy.user.registered.total")
        .setDescription("Total user registrations by result")
        .build()

val userLoginCounter =
    meter
        .counterBuilder("laxy.user.login.total")
        .setDescription("Total login attempts by result")
        .build()

val gptCompletionCounter =
    meter
        .counterBuilder("laxy.gpt.completion.total")
        .setDescription("Total GPT completion requests by result")
        .build()

fun resultAttributes(success: Boolean): Attributes =
    Attributes.of(AttributeKey.stringKey("result"), if (success) "success" else "failure")
