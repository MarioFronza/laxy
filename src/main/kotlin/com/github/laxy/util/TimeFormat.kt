package com.github.laxy.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


fun LocalDateTime.toBrazilianFormat(): String {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
    return this.format(formatter)
}