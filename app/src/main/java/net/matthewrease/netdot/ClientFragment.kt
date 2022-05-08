package net.matthewrease.netdot

import android.annotation.SuppressLint
import android.net.InetAddresses
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import java.net.URL

/**
 * A simple [Fragment] subclass.
 * Use the [ClientFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ClientFragment : Fragment() {
    private val gameState: GameViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_client, container, false)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = view.findViewById<EditText>(R.id.remoteAddressBox)
        val refresh = view.findViewById<Button>(R.id.refreshButton)
        val list = view.findViewById<LinearLayout>(R.id.serverList)
        val connect = view.findViewById<Button>(R.id.connectButton)
        connect.setOnClickListener {
            // Init client
            gameState.startClient(name.text.toString())
            // Go to game screen
            findNavController().navigate(R.id.gameFragment)
        }
        refresh.setOnClickListener {
            println("REFRESH SERVER LIST")
            list.removeAllViewsInLayout()
            gameState.queryMasterServer { message ->
                println("MASTER SERVER SAID: $message")
                val segments = message.split(' ')
                val url: String = segments[0]
                val current: Int = segments[1].toInt()
                val max: Int = segments[2].toInt()
                val name: String = message.substring(segments[0].length + segments[1].length + segments[2].length + 3)
                Button(context).also {
                    with (it) {
                        text = "$name | ${if (current >= 0) current else '?'}/${if (max > 0) max else '∞'}"
                        setOnClickListener {
                            // Init client
                            gameState.startClient(url)
                            // Go to game screen
                            findNavController().navigate(R.id.gameFragment)
                        }
                    }
                    activity?.runOnUiThread { list.addView(it) }
                }
            }
        }
    }
}
