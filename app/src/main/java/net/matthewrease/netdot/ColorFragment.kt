package net.matthewrease.netdot

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController

/**
 * Simple fragment for changing your player color
 */
class ColorFragment : Fragment() {
    private val gameState: GameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_color, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val color = view.findViewById<com.skydoves.colorpickerview.ColorPickerView>(R.id.colorPickerView)
        val button = view.findViewById<Button>(R.id.setColorButton)
        button.setOnClickListener {
            gameState.manager.setColor(color.color)
            findNavController().popBackStack()
        }

        // Update master server if player count changes
        var playerCount: Int = gameState.game.players.size
        gameState.players.observe(viewLifecycleOwner) { players ->
            if (players.size != playerCount) {
                playerCount = players.size
                gameState.updatePlayers(playerCount)
            }
        }
    }
}
