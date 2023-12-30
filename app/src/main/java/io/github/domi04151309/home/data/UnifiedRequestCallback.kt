package io.github.domi04151309.home.data

data class UnifiedRequestCallback(
    val response: ArrayList<ListViewItem>?,
    val deviceId: String,
    val errorMessage: String = "",
)
