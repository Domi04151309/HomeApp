package io.github.domi04151309.home.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.github.domi04151309.home.R
import io.github.domi04151309.home.data.DeviceItem
import io.github.domi04151309.home.helpers.Global

class ControlInfoFragment(
    private val device: DeviceItem,
    private val title: String,
) : Fragment(R.layout.fragment_control_info) {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view =
            super.onCreateView(inflater, container, savedInstanceState)
                ?: error("View does not exist yet.")

        view.findViewById<ImageView>(R.id.deviceIcon).setImageResource(Global.getIcon(device.iconName))
        view.findViewById<TextView>(R.id.titleText).text = title
        view.findViewById<TextView>(R.id.subTitleText).text = device.name

        return view
    }
}
