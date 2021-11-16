package io.github.domi04151309.home.helpers

import androidx.core.graphics.ColorUtils

object HueUtils {

    fun ctToRGB(ct: Int): Int {
        require(!(ct < 153 || ct > 500)) { "Argument out of range" }
        return cyanogenmod.util.ColorUtils.temperatureToRGB((6500 - 12.968299711 * (ct - 153)).toInt())
    }

    fun ctToKelvin(ct: Int): String {
        require(!(ct < 153 || ct > 500)) { "Argument out of range" }
        return "${(6500 - 12.968299711 * (ct - 153)).toInt()} K"
    }

    fun hueSatToRGB(hue: Int, sat: Int): Int {
        require(!(hue > 65535 || sat > 254)) { "Argument out of range" }
        val hsl: Array<Float> = arrayOf(hue * 0.0054932478F, 1F, 1F - sat / 254F * .5F)
        return ColorUtils.HSLToColor(hsl.toFloatArray())
    }

    fun hueToDegree(hue: Int): String {
        require(hue <= 65535) { "Argument out of range" }
        return "${(hue * 0.0054932478F).toInt()}°"
    }

    fun satToPercent(sat: Int): String {
        require(sat <= 254) { "Argument out of range" }
        return "${(sat / 254F * 100).toInt()}%"
    }

    fun briToPercent(bri: Int): String {
        require(!(bri < 1 || bri > 254)) { "Argument out of range" }
        val value: Int = (bri / 254F * 100).toInt()
        return "$value%"
    }
}