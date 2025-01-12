package com.github.laxy

import io.kotest.common.runBlocking
import io.kotest.core.spec.style.FreeSpec

@Suppress("UnnecessaryAbstractClass")
abstract class SuspendFun(body: suspend FreeSpec.() -> Unit) : FreeSpec() {
    init {
        runBlocking { body() }
    }
}
