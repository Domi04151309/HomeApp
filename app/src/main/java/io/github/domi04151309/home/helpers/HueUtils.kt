package io.github.domi04151309.home.helpers

import android.graphics.Color

object HueUtils {

    fun ctToRGB(ct: Int): Int {
        require(!(ct < 153 || ct > 500)) { "Argument out of range" }
        return ColorUtils.temperatureToRGB((6500 - 12.968299711 * (ct - 153)).toInt())
    }

    fun ctToKelvin(ct: Int): String {
        require(!(ct < 153 || ct > 500)) { "Argument out of range" }
        return "${(6500 - 12.968299711 * (ct - 153)).toInt()} K"
    }

    fun hueSatToRGB(hue: Int, sat: Int): Int {
        require(!(hue > 65535 || sat > 254)) { "Argument out of range" }
        return Color.HSVToColor(floatArrayOf(hue * 0.0054932478F, sat / 254F, 1F))
    }

    fun hueToRGB(hue: Int): Int {
        require(hue <= 65535) { "Argument out of range" }
        return Color.HSVToColor(floatArrayOf(hue * 0.0054932478F, 1F, 1F))
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
        return when {
            bri < 1 -> "0%"
            bri > 254 -> "100%"
            else -> "${(bri / 254F * 100).toInt()}%"
        }
    }

    fun rgbToHueSat(color: Int): IntArray {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        return intArrayOf(
            (hsv[0] / 0.0054932478).toInt(),
            (hsv[1] * 254).toInt()
        )
    }
}