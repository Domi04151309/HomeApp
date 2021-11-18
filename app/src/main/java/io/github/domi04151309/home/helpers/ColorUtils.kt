package io.github.domi04151309.home.helpers

import android.graphics.Color
import kotlin.math.ln
import kotlin.math.pow

object ColorUtils {

    private const val MAX: Double = 255.0
    private const val MIN: Double = 0.0

    fun temperatureToRGB(kelvin: Int): Int {
        val temp = kelvin / 100.0
        val red: Double
        val green: Double
        val blue: Double

        if (temp <= 66) {
            red = MAX
            green = 99.4708025861 * ln(temp) - 161.1195681661
            blue =
                if (temp <= 19) MIN
                else 138.5177312231 * ln(temp - 10) - 305.0447927307
        } else {
            red = 329.698727446 * (temp - 60).pow(-0.1332047592)
            green = 288.1221695283 * (temp - 60).pow(-0.0755148492)
            blue = MAX
        }

        return Color.rgb(clamp(red), clamp(green), clamp(blue))
    }

    private fun clamp(value: Double): Int {
        return when {
            value < MIN -> MIN.toInt()
            value > MAX -> MAX.toInt()
            else -> value.toInt()
        }
    }
}