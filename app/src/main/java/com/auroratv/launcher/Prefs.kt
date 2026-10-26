package com.auroratv.launcher

import android.content.Context

class Option(val key: String, val title: String, val values: List<String>, val default: Int)

object Options {
    val PALETTE = Option("palette", "Scenery", listOf("Auto (by time of day)", "Sunrise", "Sunset", "Twilight"), 0)
    val CLOCK_FORMAT = Option("clock_format", "Clock format", listOf("System", "12-hour", "24-hour"), 0)
    val CLOCK_HEIGHT = Option("clock_height", "Clock height (mountain overlap)", listOf("Lower", "Default", "Higher"), 1)
    val SHOW_DATE = Option("show_date", "Show date", listOf("On", "Off"), 0)
    val PARALLAX = Option("parallax", "Parallax when navigating", listOf("On", "Off"), 0)
    val AMBIENT = Option("ambient", "Ambient mode after", listOf("Never", "15 seconds", "30 seconds", "1 minute", "2 minutes"), 2)
    val DRIFT = Option("drift", "Slow scenery motion in ambient mode", listOf("On", "Off"), 0)
    val ALL_APPS = Option("all_apps", "Apps in dock", listOf("TV apps only", "All apps"), 0)
    val HOME_ALIAS = Option("home_alias", "Use AuroraTV as Home screen", listOf("Off (original TV home)", "On"), 1)

    val ALL = listOf(PALETTE, CLOCK_FORMAT, CLOCK_HEIGHT, SHOW_DATE, PARALLAX, AMBIENT, DRIFT, ALL_APPS, HOME_ALIAS)

    fun ambientMillis(index: Int): Long = longArrayOf(0L, 15000L, 30000L, 60000L, 120000L).getOrElse(index) { 0L }
}

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE)

    fun get(c: Context, o: Option): Int = sp(c).getInt(o.key, o.default).coerceIn(0, o.values.size - 1)

    fun set(c: Context, o: Option, value: Int) {
        sp(c).edit().putInt(o.key, value).apply()
    }
}
