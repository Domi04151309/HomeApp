package io.github.domi04151309.home

object Helpers {
    fun getFileContents(path: String): String =
        javaClass.getResource(path)
            ?.readText()
            ?: error("Cannot get file contents.")
}
