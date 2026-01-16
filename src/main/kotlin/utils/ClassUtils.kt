package de.c4vxl.utils

/**
 * Collection of class utilities
 */
object ClassUtils {
    /**
     * Returns the name of a class
     */
    fun className(clazz: Class<*>): String =
        clazz.simpleName
}