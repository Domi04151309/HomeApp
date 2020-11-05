package io.github.domi04151309.home.helpers

import androidx.core.graphics.ColorUtils

object HueUtils {

    fun ctToRGB(ct: Int): Int {
        require(!(ct < 153 || ct > 500)) { "Argument out of range" }
        return cyanogenmod.util.ColorUtils.temperatureToRGB((6500 - 12.968299711 * (ct - 153)).toInt())
    }

    fun hueSatToRGB(hue: Int, sat: Int): Int {
        require(!(hue > 65535 || sat > 254)) { "Argument out of range" }
        val hsl: Array<Float> = arrayOf(hue * 0.0054932478F, sat / 254F, 0.5F)
        return ColorUtils.HSLToColor(hsl.toFloatArray())
    }

    fun briToPercent(bri: Int): String {
        require(!(bri < 1 || bri > 254)) { "Argument out of range" }
        val value: Int = (bri / 254F * 100).toInt()
        return "$value%"
    }
}