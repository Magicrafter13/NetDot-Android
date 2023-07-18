package net.matthewrease.netdot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ChatFragment : Fragment() {
    private val gameState: GameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chat = view.findViewById<TextView>(R.id.chatWindow)
        val message = view.findViewById<TextView>(R.id.chatBox)
        val send = view.findViewById<FloatingActionButton>(R.id.sendChatButton)
        send.setOnClickListener {
            val newMessage: String = message.text.toString()
            if (newMessage.isNotEmpty()) {
                gameState.sendChat(newMessage)
                message.text = ""
            }
        }
        gameState.game.uiChat.observe(viewLifecycleOwner) { string -> chat.text = string }

        // Update master server if player count changes
        var playerCount: Int = gameState.game.users.size
        gameState.users.observe(viewLifecycleOwner) { users ->
            if (users.size != playerCount) {
                playerCount = users.size
                gameState.updatePlayers(playerCount)
            }
        }
    }
}
