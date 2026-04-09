package io.github.domi04151309.home.data

data class RoomItem(
    val id: String,
    val name: String = "Room",
    val iconName: String = "Lamp",
) {
    val iconId: Int get() = io.github.domi04151309.home.helpers.Global.getIcon(iconName)
}
