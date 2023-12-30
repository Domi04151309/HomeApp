package io.github.domi04151309.home.interfaces

import android.view.View
import io.github.domi04151309.home.data.SceneListItem

interface SceneRecyclerViewHelperInterface {
    fun onItemClicked(
        view: View,
        data: SceneListItem,
    )

    fun onStateChanged(
        view: View,
        data: SceneListItem,
        state: Boolean,
    )
}
