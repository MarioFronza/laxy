package com.github.laxy.shared

sealed interface ApplicationError {
    val errors: List<String>
}

data class IllegalStateError(override val errors: List<String>) : ApplicationError {
    companion object {
        fun illegalState(message: String) = IllegalStateError(listOf(message))

        fun illegalStates(errors: List<String>) = IllegalStateError(errors)
    }
}
