package com.github.laxy.shared

sealed class InteractionResult<out E> {
    fun bind(): E =
        when (this) {
            is Failure -> throw InteractionException(this.applicationError)
            is Success -> this.data
        }
}

data class Failure<E>(val applicationError: ApplicationError) : InteractionResult<E>() {
    val errors
        get() = applicationError.errors
}

data class Success<E>(val data: E) : InteractionResult<E>()

class InteractionException(val failure: ApplicationError) : Throwable()

inline fun <E> interaction(block: () -> InteractionResult<E>): InteractionResult<E> {
    return try {
        block()
    } catch (e: InteractionException) {
        Failure(e.failure)
    }
}
