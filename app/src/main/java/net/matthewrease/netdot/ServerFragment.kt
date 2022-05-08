package net.matthewrease.netdot

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import java.net.ServerSocket
import java.net.Socket
import kotlin.math.max

/**
 * A simple [Fragment] subclass.
 * Use the [ServerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ServerFragment : Fragment() {
    private val gameState: GameViewModel by activityViewModels()

    private fun numberEditorActionListener(text: TextView, actionID: Int, keyEvent: KeyEvent?): Boolean {
        /*println("ACTION")
        println("ID: $actionID")
        println("KEYEVENT: ${keyEvent?.action}")*/
        // Soft touch keyboard, done
        return ( if (keyEvent == null) {
            when (actionID) {
                EditorInfo.IME_ACTION_SEND, EditorInfo.IME_ACTION_DONE -> {
                    (text.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(text.windowToken, 0)
                    true
                }
                else -> false
            }
        }
        // Hardware (physical) enter
        else actionID == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN )
            // If number is below 2, force it to 2
            .also {
                // Validity check
                if (it && text.text.toString().toInt() < 2)
                    text.text = "2"
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_server, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val limit = view.findViewById<CheckBox>(R.id.limitPlayers)
        val players = view.findViewById<EditText>(R.id.maxPlayerBox)
        val width = view.findViewById<EditText>(R.id.gridDimsWidthBox)
        val height = view.findViewById<EditText>(R.id.gridDimsHeightBox)
        val advertise = view.findViewById<CheckBox>(R.id.advertiseServer)
        val name = view.findViewById<EditText>(R.id.serverNameBox)
        val start = view.findViewById<Button>(R.id.startServerButton)
        limit.setOnCheckedChangeListener { _, checked -> players.isEnabled = checked }
        players.setOnEditorActionListener(this::numberEditorActionListener)
        width.setOnEditorActionListener(this::numberEditorActionListener)
        height.setOnEditorActionListener(this::numberEditorActionListener)
        advertise.setOnCheckedChangeListener { _, checked ->
            name.isEnabled = checked
            gameState.advertise.value = checked
        }
        start.setOnClickListener {
            gameState.dimX.value = max(width.text.toString().toInt(), 2)
            gameState.dimY.value = max(height.text.toString().toInt(), 2)
            gameState.name.value = name.text.toString()
            // Init server
            gameState.startServer(if (limit.isChecked) max(players.text.toString().toInt(), 2) else 0)
            // Go to game screen
            findNavController().navigate(R.id.gameFragment)
        }
    }
}
