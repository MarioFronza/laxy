package com.github.laxy.domain.validation

import com.github.laxy.shared.ApplicationError

data class IncorrectBehavior(
    val errors: List<ApplicationError>
): ApplicationError {
    constructor(head: ApplicationError) : this(listOf(head))
}
