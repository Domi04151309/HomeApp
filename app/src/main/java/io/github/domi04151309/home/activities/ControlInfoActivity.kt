package io.github.domi04151309.home.activities

import android.os.Bundle
import com.google.android.material.elevation.SurfaceColors
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.fragments.ControlInfoFragment
import io.github.domi04151309.home.fragments.HueColorFragment
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global
import io.github.domi04151309.home.interfaces.HueRoomInterface

class ControlInfoActivity : BaseActivity() {
    private var hueRoom: ControlInfoActivityHueRoom? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        window.statusBarColor = SurfaceColors.SURFACE_0.getColor(this)

        val id = intent.getStringExtra(EXTRA_ID)
        if (id === null) {
            return
        }

        val device = Devices(this).getDeviceById(id.substring(0, id.indexOf('@')))

        if (device.mode == Global.HUE_API) {
            showHueFragment(id, device)
            return
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, ControlInfoFragment(device, intent.getStringExtra(EXTRA_TITLE) ?: ""))
            .commit()
    }

    override fun onStart() {
        super.onStart()
        hueRoom?.onStart()
    }

    override fun onStop() {
        super.onStop()
        hueRoom?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        hueRoom?.onDestroy()
    }

    private fun showHueFragment(
        id: String,
        device: DeviceItem,
    ) {
        hueRoom = ControlInfoActivityHueRoom(this, device, id.substring(id.indexOf('@') + 1))
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, HueColorFragment(hueRoom as HueRoomInterface))
            .commit()
    }

    companion object {
        const val EXTRA_ID: String = "EXTRA_ID"
        const val EXTRA_TITLE: String = "EXTRA_TITLE"
    }
}
