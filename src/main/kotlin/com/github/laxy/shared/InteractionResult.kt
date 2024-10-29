package com.github.laxy.shared

sealed class InteractionResult<E, D> {
    fun bind(): D = when (this) {
        is Failure -> throw InteractionException()
        is Success -> this.data
    }
}

data class Failure<E, D>(val error: E) : InteractionResult<E, D>()

data class Success<E, D>(val data: D) : InteractionResult<E, D>()

class InteractionException : Throwable()

inline fun <E, D> either(block: () -> InteractionResult<E, D>, onError: E): InteractionResult<E, D> {
    return try {
        block()
    } catch (e: InteractionException) {
        Failure(onError)
    }
}