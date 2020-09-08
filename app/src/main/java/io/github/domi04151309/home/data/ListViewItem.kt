package io.github.domi04151309.home.data

import android.widget.CompoundButton

data class ListViewItem(
        val title: String = "",
        var summary: String = "",
        var hidden: String = "",
        var icon: Int = 0,
        var state: Boolean? = null,
        var stateListener: CompoundButton.OnCheckedChangeListener? = null
)