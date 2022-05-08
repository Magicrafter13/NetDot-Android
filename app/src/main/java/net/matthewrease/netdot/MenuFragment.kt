package net.matthewrease.netdot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MenuFragment : Fragment() {
    private val tabNames: Array<String> = arrayOf("Server", "Client", "Options")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_menu, container, false)

        val pager = view.findViewById<ViewPager2>(R.id.menuTabPager)
        val layout = view.findViewById<TabLayout>(R.id.menuTabs)

        pager.adapter = MenuAdapter(this)
        TabLayoutMediator(layout, pager) { tab, position ->
            tab.text = tabNames[position]
        }.attach()

        return view
    }
}
