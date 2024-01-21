package io.github.domi04151309.home.helpers

import android.graphics.Color

@Suppress("MagicNumber")
object HueUtils {
    const val MIN_COLOR_TEMPERATURE: Int = 153
    const val MAX_BRIGHTNESS: Int = 255

    private const val ARGUMENT_OUT_OF_RANGE = "Argument out of range."

    fun defaultColors(): IntArray =
        IntArray(7) {
                index ->
            Color.HSVToColor(floatArrayOf(index * 60f, 1f, 1f))
        }

    fun ctToRGB(ct: Int): Int {
        require(!(ct < MIN_COLOR_TEMPERATURE || ct > 500)) { ARGUMENT_OUT_OF_RANGE }
        return ColorUtils.temperatureToRGB((6500 - 12.968299711 * (ct - MIN_COLOR_TEMPERATURE)).toInt())
    }

    fun ctToKelvin(ct: Int): String {
        require(!(ct < MIN_COLOR_TEMPERATURE || ct > 500)) { ARGUMENT_OUT_OF_RANGE }
        return "${(6500 - 12.968299711 * (ct - MIN_COLOR_TEMPERATURE)).toInt()} K"
    }

    fun hueSatToRGB(
        hue: Int,
        sat: Int,
    ): Int {
        require(!(hue > 65_535 || sat > 254)) { ARGUMENT_OUT_OF_RANGE }
        return Color.HSVToColor(floatArrayOf(hue * 0.005493248F, sat / 254F, 1F))
    }

    fun hueToRGB(hue: Int): Int {
        require(hue <= 65_535) { ARGUMENT_OUT_OF_RANGE }
        return Color.HSVToColor(floatArrayOf(hue * 0.005493248F, 1F, 1F))
    }

    fun hueToDegree(hue: Int): String {
        require(hue <= 65_535) { ARGUMENT_OUT_OF_RANGE }
        return "${(hue * 0.005493248F).toInt()}°"
    }

    fun satToPercent(sat: Int): String {
        require(sat <= 254) { ARGUMENT_OUT_OF_RANGE }
        return "${(sat / 254F * 100).toInt()}%"
    }

    fun briToPercent(bri: Int): String =
        when {
            bri < 1 -> "0%"
            bri > 254 -> "100%"
            else -> "${(bri / 254F * 100).toInt()}%"
        }

    fun rgbToHueSat(color: Int): IntArray {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        return intArrayOf(
            (hsv[0] / 0.0054932478).toInt(),
            (hsv[1] * 254).toInt(),
        )
    }
}
