package io.github.domi04151309.home.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.domi04151309.home.fragments.HueColorFragment
import io.github.domi04151309.home.fragments.HueLampsFragment
import io.github.domi04151309.home.fragments.HueScenesFragment
import io.github.domi04151309.home.interfaces.HueRoomInterface

class HueDetailsTabAdapter(
    activity: FragmentActivity,
    private val lampInterface: HueRoomInterface,
) : FragmentStateAdapter(activity) {
    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> HueColorFragment(lampInterface)
            1 -> HueScenesFragment(lampInterface)
            2 -> HueLampsFragment(lampInterface)
            else -> Fragment()
        }

    override fun getItemCount(): Int = 3
}
