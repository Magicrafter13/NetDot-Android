package net.matthewrease.netdot

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton

class GameFragment : Fragment() {
    private val gameState: GameViewModel by activityViewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val view = inflater.inflate(if (portrait) R.layout.game_fragment else R.layout.game_fragment_landscape, container, false)

        // Setup UI
        val text = view.findViewById<TextView>(R.id.statusText)
        val chat = view.findViewById<FloatingActionButton>(R.id.openChat)
        val grid = view.findViewById<DotGrid>(R.id.dotGrid)
        val lobby = view.findViewById<Button>(R.id.lobbyButton)
        val info = view.findViewById<FloatingActionButton>(R.id.playerInfoButton)
        val start = view.findViewById<Button>(R.id.startButton)
        val quit = view.findViewById<Button>(R.id.quitButton)
        chat.setOnClickListener { findNavController().navigate(R.id.chatFragment) }
        // Detect taps on grid
        grid.setOnClickListener {
            if (gameState.started) {
                if (!gameState.finished) {
                    gameState.tryMove(grid.moveX, grid.moveY, grid.moveVertical)
                }
                else println("The game is over!") // TODO: Inform user
            }
            else println("Game hasn't started yet.") // TODO: Inform user
        }
        // Stop game
        lobby.visibility = if (gameState.started) View.VISIBLE else View.INVISIBLE
        lobby.setOnClickListener {
            gameState.stopGame()
            lobby.visibility = if (gameState.started) View.VISIBLE else View.INVISIBLE
            start.text = if (gameState.started) "Restart Game" else "Start Game"
        }
        // View or change player info
        info.setOnClickListener { findNavController().navigate(R.id.playersFragment) }
        // Start game
        start.text = if (gameState.started) "Restart Game" else "Start Game"
        start.setOnClickListener {
            gameState.startGame()
            lobby.visibility = if (gameState.started) View.VISIBLE else View.INVISIBLE
            start.text = if (gameState.started) "Restart Game" else "Start Game"
        }
        // Quit game
        quit.setOnClickListener {
            // Wrap up connections
            gameState.quit()
            // Return to menu
            findNavController().popBackStack()
        }

        // Observers
        var playerCount: Int = gameState.game.players.size
        gameState.dots.observe(viewLifecycleOwner) { dots -> grid.updateDots(dots) }
        gameState.players.observe(viewLifecycleOwner) { players ->
            // Update grid
            grid.updatePlayers(players)
            // Update text
            if (gameState.started) {
                if (gameState.finished)
                    gameState.game.mostBoxes().also { winners -> text.text = if (winners.size > 1) "Tie!" else "${winners[0].name} wins!" }
                else
                    text.text = "Your move, ${players[gameState.game.currentPlayer.value]?.name}"
            }
            else text.text = "Waiting for players... press start when ready."
            println("OLD: $playerCount - NEW: ${players.size}")
            if (players.size != playerCount) {
                playerCount = players.size
                gameState.updatePlayers(playerCount)
            }
        }
        gameState.game.currentPlayer.observe(viewLifecycleOwner) { player ->
            println("CURRENT IS NOW $player")
            if (gameState.started) {
                if (gameState.finished)
                    gameState.game.mostBoxes().also { winners -> text.text = if (winners.size > 1) "Tie!" else "${winners[0].name} wins!" }
                else
                    text.text = "Your move, ${gameState.players.value[player]?.name}"
            }
            else text.text = "Waiting for players... press start when ready."
        }
        if (!gameState.isServer.value) {
            gameState.gridSize.observe(viewLifecycleOwner) { grid.reset(it.width, it.height) }
            gameState.connected.observe(viewLifecycleOwner) { connected ->
                if (!connected)
                    findNavController().popBackStack()
            }
        }

        // Initialize game
        if (gameState.isServer.value)
            grid.reset(gameState.dimX.value!!, gameState.dimY.value!!)
        else
            with (gameState.gridSize.value) { grid.reset(width, height) }
        gameState.fragmentInit()
        grid.invalidate()

        return view
    }
}
