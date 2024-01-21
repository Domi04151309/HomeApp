package io.github.domi04151309.home.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.domi04151309.home.api.HueAPI
import io.github.domi04151309.home.helpers.Devices

class ShortcutHueSceneActionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra("scene") && intent.hasExtra("group") && intent.hasExtra(Devices.INTENT_EXTRA_DEVICE)) {
            HueAPI(
                this,
                intent.getStringExtra(Devices.INTENT_EXTRA_DEVICE) ?: error(IMPOSSIBLE_STATE),
            ).activateSceneOfGroup(
                intent.getStringExtra("group") ?: error(IMPOSSIBLE_STATE),
                intent.getStringExtra("scene") ?: error(IMPOSSIBLE_STATE),
            )
        }
        finish()
    }

    companion object {
        private const val IMPOSSIBLE_STATE = "Impossible state."
    }
}
