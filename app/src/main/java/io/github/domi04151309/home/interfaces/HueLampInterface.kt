package io.github.domi04151309.home.interfaces

import io.github.domi04151309.home.data.DeviceItem

interface HueLampInterface {
    var id: String
    var device: DeviceItem
    var addressPrefix: String
    var canReceiveRequest: Boolean

    fun onColorChanged(color: Int)
}
