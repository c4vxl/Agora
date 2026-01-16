package de.c4vxl.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Global utilities for logging
 */
object LoggerUtils {
    /**
     * Create a slf4j-logger instance for a specific class
     */
    inline fun <reified T> createLogger(): Logger =
        createLogger(T::class.java)

    /**
     * Create a slf4j-logger instance for a specific class
     * @param clazz The class to create the logger for
     */
    fun createLogger(clazz: Class<*>): Logger =
        LoggerFactory.getLogger(clazz)

    /**
     * Create a slf4j-logger instance with a specific name
     * @param name The name of the logger
     */
    fun createLogger(name: String): Logger =
        LoggerFactory.getLogger(name)
}