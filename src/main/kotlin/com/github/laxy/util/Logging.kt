package com.github.laxy.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

fun Logger.debug(msg: String, vararg args: Any?) = this.debug(msg, *args)
fun Logger.info(msg: String, vararg args: Any?) = this.info(msg, *args)
fun Logger.warn(msg: String, vararg args: Any?) = this.warn(msg, *args)
fun Logger.error(msg: String, vararg args: Any?) = this.error(msg, *args)
fun Logger.error(msg: String, throwable: Throwable) = this.error(msg, throwable)