package io.github.domi04151309.home.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.domi04151309.home.api.Tasmota
import io.github.domi04151309.home.api.UnifiedAPI
import io.github.domi04151309.home.data.UnifiedRequestCallback
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.interfaces.HomeRecyclerViewHelperInterface

class ShortcutTasmotaActionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.hasExtra("command") && intent.hasExtra(Devices.INTENT_EXTRA_DEVICE)) {
            Tasmota(
                this,
                intent.getStringExtra(Devices.INTENT_EXTRA_DEVICE) ?: error("Impossible state."),
                null,
            ).execute(
                intent.getStringExtra("command") ?: error("Impossible state."),
                object : UnifiedAPI.CallbackInterface {
                    override fun onItemsLoaded(
                        holder: UnifiedRequestCallback,
                        recyclerViewInterface: HomeRecyclerViewHelperInterface?,
                    ) {
                        // Do nothing.
                    }

                    override fun onExecuted(
                        result: String,
                        shouldRefresh: Boolean,
                    ) {
                        Toast.makeText(
                            this@ShortcutTasmotaActionActivity,
                            result,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
            )
        }
        finish()
    }
}
