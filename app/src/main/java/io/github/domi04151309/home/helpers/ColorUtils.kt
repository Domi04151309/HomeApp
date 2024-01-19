package io.github.domi04151309.home.helpers

import android.graphics.Color
import kotlin.math.ln
import kotlin.math.pow

@Suppress("MagicNumber")
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
                if (temp <= 19) {
                    MIN
                } else {
                    138.5177312231 * ln(temp - 10) - 305.0447927307
                }
        } else {
            red = 329.698727446 * (temp - 60).pow(-0.1332047592)
            green = 288.1221695283 * (temp - 60).pow(-0.0755148492)
            blue = MAX
        }

        return Color.rgb(clamp(red), clamp(green), clamp(blue))
    }

    fun xyToRGB(
        x: Double,
        y: Double,
    ): Int {
        val cieY = 1.0
        val cieX = cieY * x / y
        val cieZ = (1 - x - y) * cieY / y

        val r = +3.2404542 * cieX - 1.5371385 * cieY - 0.4985314 * cieZ
        val g = -0.9692660 * cieX + 1.8760108 * cieY + 0.0415560 * cieZ
        val b = +0.0556434 * cieX - 0.2040259 * cieY + 1.0572252 * cieZ

        return Color.rgb(formatXyzValue(r), formatXyzValue(g), formatXyzValue(b))
    }

    private fun formatXyzValue(v: Double): Int =
        clamp(
            (if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055) * MAX,
        )

    private fun clamp(value: Double): Int =
        when {
            value < MIN -> MIN.toInt()
            value > MAX -> MAX.toInt()
            else -> value.toInt()
        }
}
