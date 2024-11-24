package com.github.laxy.shared

interface ApplicationError

data class IllegalStateError(val message: String) : ApplicationError {
    companion object {
        fun illegalState(message: String) = IllegalStateError(message)
    }
}