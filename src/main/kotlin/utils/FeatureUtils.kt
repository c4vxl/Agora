package de.c4vxl.utils

/**
 * Collection of utilities used over multiple features
 */
object FeatureUtils {
    /**
     * Returns the name of a feature by its class
     */
    fun featureName(clazz: Class<*>): String =
        clazz.simpleName
}