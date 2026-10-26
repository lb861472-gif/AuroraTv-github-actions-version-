package com.auroratv.launcher

import android.graphics.Color

class Palette(
    val skyTop: Int,
    val skyMid: Int,
    val skyHorizon: Int,
    val sun: Int,
    val sunGlow: Int,
    val far: Int,
    val mid: Int,
    val midRim: Int,
    val fg: Int,
    val fgBottom: Int,
    val stars: Boolean
)

object Palettes {
    private fun c(s: String): Int = Color.parseColor(s)

    val SUNRISE = Palette(
        c("#4E6FB0"), c("#F0A3B0"), c("#FFD7A0"),
        c("#FFF4D0"), c("#FFD08A"),
        c("#A99BBB"), c("#5C5679"), c("#EFC0AC"),
        c("#1C2131"), c("#090B11"), false
    )

    val SUNSET = Palette(
        c("#2B2A5E"), c("#B8456F"), c("#FFB059"),
        c("#FFE7A8"), c("#FF9A4D"),
        c("#7B5A8E"), c("#3C3459"), c("#C98AA0"),
        c("#14161F"), c("#07080C"), false
    )

    val TWILIGHT = Palette(
        c("#0B1033"), c("#2A2C6B"), c("#7A5AA6"),
        c("#E8ECFF"), c("#7F8CFF"),
        c("#3A3F7A"), c("#20244F"), c("#6C74C0"),
        c("#0A0C1C"), c("#040510"), true
    )

    fun forIndex(pref: Int, hour: Int): Palette = when (pref) {
        1 -> SUNRISE
        2 -> SUNSET
        3 -> TWILIGHT
        else -> if (hour in 5..11) SUNRISE else if (hour in 12..18) SUNSET else TWILIGHT
    }
}
