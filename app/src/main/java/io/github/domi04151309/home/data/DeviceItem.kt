package io.github.domi04151309.home.data

import io.github.domi04151309.home.Global

data class DeviceItem(val id: String) {
    var name: String = "Device"
    var address: String = "http://127.0.0.1/"
        set(value) {
            var url = value
            if (!(url.startsWith("https://") || url.startsWith("http://")))
                url = "http://$url"
            if (!url.endsWith("/"))
                url += "/"
            field = url
        }
    var mode: String = "Default"
    var iconName: String = "Lamp"
    val iconId: Int get() = Global.getIcon(iconName)
}