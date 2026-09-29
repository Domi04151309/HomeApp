package io.github.domi04151309.home.activities

import java.io.File

class WebActivityWebViewClientSslErrorTest {

    fun sslErrorsAreCancelledRatherThanProceeded() {
        val methodBody = onReceivedSslErrorBody(readWebViewClientSource())

        check(CANCEL_CALL.containsMatchIn(methodBody)) {
            "onReceivedSslError must reject SSL errors with handler.cancel()"
        }
        check(!PROCEED_CALL.containsMatchIn(methodBody)) {
            "onReceivedSslError must not bypass SSL errors with handler.proceed()"
        }
    }

    private fun readWebViewClientSource(): String {
        val relativePath = "src/main/java/io/github/domi04151309/home/activities/WebActivityWebViewClient.kt"
        val sourceFile = sequenceOf(
            File(relativePath),
            File("app/$relativePath"),
        ).firstOrNull(File::isFile)
            ?: error("Unable to locate WebActivityWebViewClient.kt from the test working directory")

        return sourceFile.readText()
    }

    private fun onReceivedSslErrorBody(source: String): String {
        val functionStart = source.indexOf("override fun onReceivedSslError(")
        check(functionStart >= 0) {
            "WebActivityWebViewClient must override onReceivedSslError"
        }

        val bodyStart = source.indexOf('{', functionStart)
        check(bodyStart >= 0) {
            "Unable to locate the onReceivedSslError function body"
        }

        var braceDepth = 0
        for (index in bodyStart until source.length) {
            when (source[index]) {
                '{' -> braceDepth++
                '}' -> {
                    braceDepth--
                    if (braceDepth == 0) {
                        return source.substring(bodyStart + 1, index)
                    }
                }
            }
        }

        error("Unable to locate the end of the onReceivedSslError function body")
    }

    private companion object {
        val CANCEL_CALL = Regex("\\bhandler\\s*\\.\\s*cancel\\s*\\(\\s*\\)")
        val PROCEED_CALL = Regex("\\bhandler\\s*\\.\\s*proceed\\s*\\(\\s*\\)")
    }
}
