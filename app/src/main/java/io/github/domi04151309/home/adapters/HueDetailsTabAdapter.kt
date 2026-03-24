package io.github.domi04151309.home.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.domi04151309.home.fragments.HueColorFragment
import io.github.domi04151309.home.fragments.HueLampsFragment
import io.github.domi04151309.home.fragments.HueScenesFragment

class HueDetailsTabAdapter(
    activity: FragmentActivity,
) : FragmentStateAdapter(activity) {
    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> HueColorFragment()
            1 -> HueScenesFragment()
            2 -> HueLampsFragment()
            else -> Fragment()
        }

    override fun getItemCount(): Int = 3
}
