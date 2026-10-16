package com.focusfirst.util

/** Validation rules for externally supplied focus-session lengths. */
object DeepLinkDuration {
    const val DEFAULT_MINUTES = 25
    const val MIN_MINUTES = 10
    const val MAX_MINUTES = 60

    fun parse(value: String?): Int? {
        if (value == null) return DEFAULT_MINUTES
        return value.toIntOrNull()?.takeIf(::isSupported)
    }

    fun isSupported(minutes: Int): Boolean = minutes in MIN_MINUTES..MAX_MINUTES
}
