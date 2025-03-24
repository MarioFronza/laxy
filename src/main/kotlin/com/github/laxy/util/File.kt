package com.github.laxy.util

fun loadTemplate(path: String): String {
    return object {}.javaClass.classLoader.getResource(path)?.readText()
        ?: throw IllegalArgumentException("Template file not found: $path")
}