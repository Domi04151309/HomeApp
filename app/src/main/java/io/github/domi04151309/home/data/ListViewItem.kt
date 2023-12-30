package io.github.domi04151309.home.data

class ListViewItem(
    title: String = "",
    summary: String = "",
    hidden: String = "",
    icon: Int = 0,
    var state: Boolean? = null,
) : SimpleListItem(title, summary, hidden, icon) {
    override fun toString(): String = "title: $title, summary: $summary, hidden: $hidden, state: $state"
}
