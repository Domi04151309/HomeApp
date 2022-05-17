package io.github.domi04151309.home.helpers

import android.animation.ObjectAnimator
import android.content.res.Resources
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.PaintDrawable
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.google.android.material.slider.Slider

object SliderUtils {

    private fun dpToPx(resources: Resources, dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    fun setSliderGradientNow(view: View, colors: IntArray) {
        val gradient = PaintDrawable()
        gradient.setCornerRadius(dpToPx(view.resources, 16).toFloat())
        gradient.paint.shader = LinearGradient(
            0f, 0f,
            view.width.toFloat(), 0f,
            colors,
            null,
            Shader.TileMode.CLAMP
        )

        val layers = LayerDrawable(arrayOf(gradient))
        layers.setLayerInset(
            0,
            dpToPx(view.resources, 14),
            dpToPx(view.resources, 22),
            dpToPx(view.resources, 14),
            dpToPx(view.resources, 22)
        )
        view.background = layers
    }

    fun setSliderGradient(view: View, colors: IntArray) {
        view.post {
            setSliderGradientNow(view, colors)
        }
    }

    fun setProgress(slider: Slider, value: Int) {
        val animation = ObjectAnimator.ofFloat(slider, "value", value.toFloat())
        animation.duration = 300
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }
}