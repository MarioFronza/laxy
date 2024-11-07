package com.github.laxy.shared

sealed class InteractionResult<out E, D> {
    fun bind(): D =
        when (this) {
            is Failure -> throw InteractionException(this.error)
            is Success -> this.data
        }
}

data class Failure<E, D>(val error: ApplicationError) : InteractionResult<E, D>()

data class Success<E, D>(val data: D) : InteractionResult<E, D>()

class InteractionException(val failure: ApplicationError) : Throwable()

inline fun <E, D> interaction(block: () -> InteractionResult<E, D>): InteractionResult<E, D> {
    return try {
        block()
    } catch (e: InteractionException) {
        Failure(e.failure)
    }
}
