package net.matthewrease.netdot

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * A simple [Fragment] subclass.
 * Use the [OptionsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OptionsFragment : Fragment() {
    private val gameState: GameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_options, container, false)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Randomize Starting Player
        with (view.findViewById<SwitchMaterial>(R.id.randomStart)) {
            isChecked = gameState.randomizeStart.value
            setOnClickListener { gameState.randomizeStart.value = isChecked }
        }
        /*// Fast Clients
        with (view.findViewById<SwitchMaterial>(R.id.fastClients)) {
            isChecked = gameState.fastClients.value
            setOnClickListener { gameState.fastClients.value = isChecked }
        }*/
        // Version Label
        view.findViewById<TextView>(R.id.versionLabel)
            .text = "Net-code Version ${NetDot.VMAJOR}.${NetDot.VMINOR}"
    }
}
