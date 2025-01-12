package com.github.laxy.validation

import arrow.core.EitherNel
import arrow.core.leftNel
import arrow.core.right

fun String.notBlank(): EitherNel<String, String> =
    if (isNotBlank()) right() else "Cannot be blank".leftNel()

fun String.minSize(size: Int): EitherNel<String, String> =
    if (length >= size) right() else "is too short (minimum is $size characters)".leftNel()

fun String.maxSize(size: Int): EitherNel<String, String> =
    if (length <= size) right() else "is too long (maximum is $size characters)".leftNel()
