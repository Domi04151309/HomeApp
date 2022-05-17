package io.github.domi04151309.home.data

import io.github.domi04151309.home.R

data class SceneListItem(
        var title: String = "",
        var hidden: String = "",
        var icon: Int = R.drawable.ic_circle,
        var state: Boolean = false,
        var brightness: String = "",
        var color: Int = 0
)