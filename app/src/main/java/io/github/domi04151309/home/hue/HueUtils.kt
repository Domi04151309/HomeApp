package io.github.domi04151309.home.hue

import androidx.core.graphics.ColorUtils

object HueUtils {

    fun ctToRGB(ct: Int): Int {
        require(!(ct < 153 || ct > 500)) { "Argument out of range" }
        val newCt = ct - 153
        val temp: Int = (6500 - 12.968299711 * newCt).toInt()
        return cyanogenmod.util.ColorUtils.temperatureToRGB(temp)
    }

    fun hueSatToRGB(hue: Int, sat: Int): Int {
        require(!(hue > 65535 || sat > 254)) { "Argument out of range" }
        val newHue: Float = hue * 0.0054932478F
        val newSat: Float = sat / 254F
        val hsl: Array<Float> = arrayOf(newHue, newSat, 0.5F)
        return ColorUtils.HSLToColor(hsl.toFloatArray())
    }
}