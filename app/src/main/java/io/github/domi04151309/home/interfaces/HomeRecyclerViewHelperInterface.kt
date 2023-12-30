package io.github.domi04151309.home.interfaces

import android.view.View
import io.github.domi04151309.home.data.ListViewItem

interface HomeRecyclerViewHelperInterface {
    fun onItemClicked(
        view: View,
        data: ListViewItem,
    )

    fun onStateChanged(
        view: View,
        data: ListViewItem,
        state: Boolean,
    )
}
