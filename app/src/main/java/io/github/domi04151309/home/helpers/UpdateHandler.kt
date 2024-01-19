package io.github.domi04151309.home.helpers

import android.os.Handler
import android.os.Looper

class UpdateHandler : Handler(Looper.getMainLooper()) {
    var running: Boolean = false
        private set

    fun setUpdateFunction(function: () -> Unit) {
        removeCallbacksAndMessages(null)
        postDelayed(
            object : Runnable {
                override fun run() {
                    function()
                    postDelayed(this, UPDATE_DELAY)
                }
            },
            0,
        )
        running = true
    }

    fun stop() {
        running = false
        removeCallbacksAndMessages(null)
    }

    companion object {
        private const val UPDATE_DELAY = 1000L
    }
}
