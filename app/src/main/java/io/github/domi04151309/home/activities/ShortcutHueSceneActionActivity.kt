package io.github.domi04151309.home.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.domi04151309.home.api.HueAPI

class ShortcutHueSceneActionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra("scene") && intent.hasExtra("group") && intent.hasExtra("device")) {
            HueAPI(
                this,
                intent.getStringExtra("device") ?: error("Impossible state."),
            ).activateSceneOfGroup(
                intent.getStringExtra("group") ?: error("Impossible state."),
                intent.getStringExtra("scene") ?: error("Impossible state."),
            )
        }
        finish()
    }
}
