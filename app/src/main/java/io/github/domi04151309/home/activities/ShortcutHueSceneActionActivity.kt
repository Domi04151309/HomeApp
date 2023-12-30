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
                intent.getStringExtra("device") ?: throw IllegalStateException(),
                null,
            ).activateSceneOfGroup(
                intent.getStringExtra("group") ?: throw IllegalStateException(),
                intent.getStringExtra("scene") ?: throw IllegalStateException(),
            )
        }
        finish()
    }
}
