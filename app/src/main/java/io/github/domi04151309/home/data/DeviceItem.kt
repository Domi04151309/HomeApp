package io.github.domi04151309.home.data

import io.github.domi04151309.home.helpers.Global

data class DeviceItem(val id: String) {
    var name: String = "Device"
    var address: String = "http://127.0.0.1/"
        set(value) {
            field = formatAddress(value)
        }
    var mode: String = "Default"
    var iconName: String = "Lamp"
    val iconId: Int get() = Global.getIcon(iconName)

    var hide: Boolean = false
    var directView: Boolean = false

    companion object {
        fun formatAddress(address: String): String {
            var url = address
            if (!(url.startsWith("https://") || url.startsWith("http://")))
                url = "http://$url"
            if (!url.endsWith("/"))
                url += "/"
            return url
        }
    }
}