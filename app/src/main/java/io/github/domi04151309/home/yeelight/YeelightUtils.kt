package io.github.domi04151309.home.yeelight

import androidx.core.graphics.ColorUtils

object YeelightUtils {

    const val CT_SHIFT = 1700

    fun hueSatToRGB(hue: Int, sat: Int): Int {
        require(!(hue > 359 || sat > 100)) { "Argument out of range" }
        val newSat: Float = sat / 100F
        val hsl: Array<Float> = arrayOf(hue.toFloat(), newSat, 0.5F)
        return ColorUtils.HSLToColor(hsl.toFloatArray())
    }
}