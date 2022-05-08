package net.matthewrease.netdot

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.matthewrease.netdot.ui.PlayerSquare

class PlayersFragment : Fragment() {
    private val gameState: GameViewModel by activityViewModels()

    private fun nameEditorActionListener(view: TextView, actionID: Int, keyEvent: KeyEvent?): Boolean {
        /*println("ACTION")
        println("ID: $actionID")
        println("KEYEVENT: ${keyEvent?.action}")*/
        // Soft touch keyboard, done
        return (if (keyEvent == null) {
            when (actionID) {
                EditorInfo.IME_ACTION_SEND, EditorInfo.IME_ACTION_DONE -> {
                    (view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
                    true
                }
                else -> false
            }
        }
        // Hardware (physical) enter
        else actionID == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN).also {
            if (it)
                gameState.manager.setName(view.text.toString())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_players, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup UI
        val name = view.findViewById<EditText>(R.id.playerNameBox)
        val color = view.findViewById<PlayerSquare>(R.id.playerSquare)
        val info = view.findViewById<PlayersView>(R.id.playersView)
        val id = gameState.clientID
        name.setText(when {
            id < -2 -> "Unknown client ID error!"
            id == -2 -> "Spectator..."
            id == -1 -> "Error, still in queue!"
            else -> gameState.players.value[id]!!.name
        })
        name.setOnEditorActionListener(this::nameEditorActionListener)
        // Color picker
        color.setOnClickListener {
            if (gameState.clientID >= 0)
                findNavController().navigate(R.id.colorFragment)
        }

        // Update grid when player data changes
        var playerCount: Int = gameState.game.players.size
        gameState.players.observe(viewLifecycleOwner) { players ->
            // Get our color (if we have one)
            val curId = gameState.clientID
            if (curId >= 0)
                color.setBackgroundColor(players[curId]?.color ?: Color.BLACK)
            // Update player name/box grid
            info.updatePlayers(players)
            // Update master server is player count has changed
            if (players.size != playerCount) {
                playerCount = players.size
                gameState.updatePlayers(playerCount)
            }
        }
    }
}
