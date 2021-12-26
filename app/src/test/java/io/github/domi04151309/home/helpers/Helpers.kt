package io.github.domi04151309.home.helpers

object Helpers {

    fun getFileContents(path: String): String {
        return javaClass.getResource(path)?.readText() ?: throw IllegalStateException()
    }
}