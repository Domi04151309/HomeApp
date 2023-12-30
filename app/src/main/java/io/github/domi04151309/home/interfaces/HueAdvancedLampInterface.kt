package io.github.domi04151309.home.interfaces

interface HueAdvancedLampInterface : HueLampInterface {
    fun onBrightnessChanged(brightness: Int)

    fun onHueSatChanged(
        hue: Int,
        sat: Int,
    )

    fun onCtChanged(ct: Int)
}
