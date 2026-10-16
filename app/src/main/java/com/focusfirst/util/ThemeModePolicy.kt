package com.focusfirst.util

object ThemeModePolicy {
    const val LIGHT = "Light"
    const val DARK = "Dark"

    /** Only an explicit Light value opts out of the app's existing dark default. */
    fun normalize(value: String?): String = if (value == LIGHT) LIGHT else DARK

    fun isDark(value: String?): Boolean = normalize(value) == DARK
}
