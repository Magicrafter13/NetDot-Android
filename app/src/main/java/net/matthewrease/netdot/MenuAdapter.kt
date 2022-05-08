package net.matthewrease.netdot

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MenuAdapter(fragment: Fragment): FragmentStateAdapter(fragment) {
    private val tabCount: Int = 3

    override fun getItemCount(): Int {
        return tabCount
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ServerFragment()
            1 -> ClientFragment()
            2 -> OptionsFragment()
            else -> ServerFragment()
        }
    }
}
