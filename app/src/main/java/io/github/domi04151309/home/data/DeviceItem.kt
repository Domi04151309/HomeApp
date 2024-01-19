package io.github.domi04151309.home.data

import io.github.domi04151309.home.helpers.Global

class DeviceItem(
    val id: String,
    val name: String = "Device",
    val mode: String = "Default",
    val iconName: String = "Lamp",
    val hide: Boolean = false,
    val directView: Boolean = false,
) {
    var address: String = "http://127.0.0.1/"
        set(value) {
            field = formatAddress(value)
        }
    val iconId: Int get() = Global.getIcon(iconName)

    companion object {
        fun formatAddress(address: String): String {
            var url = address
            if (!(url.startsWith("https://") || url.startsWith("http://"))) {
                url = "http://$url"
            }
            if (!url.endsWith("/")) {
                url += "/"
            }
            return url
        }
    }
}
