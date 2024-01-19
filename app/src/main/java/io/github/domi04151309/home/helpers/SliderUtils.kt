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
    private const val CORNER_RADIUS = 16
    private const val MARGIN_VERTICAL = 22
    private const val MARGIN_HORIZONTAL = 14
    private const val ANIMATION_DURATION = 300L

    private fun dpToPx(
        resources: Resources,
        dp: Int,
    ): Int = (dp * resources.displayMetrics.density).toInt()

    fun setSliderGradientNow(
        view: View,
        colors: IntArray,
    ) {
        val gradient = PaintDrawable()
        gradient.setCornerRadius(dpToPx(view.resources, CORNER_RADIUS).toFloat())
        gradient.paint.shader =
            LinearGradient(
                0f, 0f,
                view.width.toFloat(), 0f,
                colors,
                null,
                Shader.TileMode.CLAMP,
            )

        val layers = LayerDrawable(arrayOf(gradient))
        layers.setLayerInset(
            0,
            dpToPx(view.resources, MARGIN_HORIZONTAL),
            dpToPx(view.resources, MARGIN_VERTICAL),
            dpToPx(view.resources, MARGIN_HORIZONTAL),
            dpToPx(view.resources, MARGIN_VERTICAL),
        )
        view.background = layers
    }

    fun setSliderGradient(
        view: View,
        colors: IntArray,
    ) {
        view.post {
            setSliderGradientNow(view, colors)
        }
    }

    fun setProgress(
        slider: Slider,
        value: Int,
    ) {
        val animation = ObjectAnimator.ofFloat(slider, "value", value.toFloat())
        animation.duration = ANIMATION_DURATION
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }
}
