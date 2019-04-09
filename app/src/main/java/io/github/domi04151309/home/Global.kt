package io.github.domi04151309.home

object Global {

    const val LOG_TAG = "HomeApp"

    const val DEFAULT_JSON = "{\"devices\":{}}"

    fun getIconId(string: String): Int {
        return when (string) {
            "Lamp" -> R.drawable.ic_device_lamp
            "Laptop" -> R.drawable.ic_device_laptop
            "Phone" -> R.drawable.ic_device_phone
            "Raspberry Pi" -> R.drawable.ic_device_raspberry_pi
            "Speaker" -> R.drawable.ic_device_speaker
            "Stack" -> R.drawable.ic_device_stack
            "Tablet" -> R.drawable.ic_device_tablet
            "TV" -> R.drawable.ic_device_tv
            else -> {
                R.drawable.ic_warning
            }
        }
    }
}
