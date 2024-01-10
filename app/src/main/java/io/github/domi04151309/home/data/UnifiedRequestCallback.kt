package io.github.domi04151309.home.data

data class UnifiedRequestCallback(
    val response: List<ListViewItem>?,
    val deviceId: String,
    val errorMessage: String = "",
)
