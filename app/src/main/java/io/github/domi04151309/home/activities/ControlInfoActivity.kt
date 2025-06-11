package io.github.domi04151309.home.activities

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.elevation.SurfaceColors
import io.github.domi04151309.home.R
import io.github.domi04151309.home.helpers.Devices
import io.github.domi04151309.home.helpers.Global

class ControlInfoActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_info)

        window.statusBarColor = SurfaceColors.SURFACE_0.getColor(this)

        val id = intent.getStringExtra(EXTRA_ID)
        if (id === null) {
            return
        }

        val device = Devices(this).getDeviceById(id.substring(0, id.indexOf('@')))

        findViewById<ImageView>(R.id.deviceIcon).setImageResource(Global.getIcon(device.iconName))
        findViewById<TextView>(R.id.titleText).text = intent.getStringExtra(EXTRA_TITLE)
        findViewById<TextView>(R.id.subTitleText).text = device.name
    }

    companion object {
        const val EXTRA_ID: String = "EXTRA_ID"
        const val EXTRA_TITLE: String = "EXTRA_TITLE"
    }
}
