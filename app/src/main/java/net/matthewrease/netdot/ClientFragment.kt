package net.matthewrease.netdot

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import net.matthewrease.netdot.data.ServerListEntry

/**
 * A simple [Fragment] subclass.
 * Use the [ClientFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ClientFragment : Fragment() {
    private val gameState: GameViewModel by activityViewModels()

    // Check local network
    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) = println("Service discovery started")

        override fun onServiceFound(service: NsdServiceInfo) {
            when {
                service.serviceType != "_netdot._tcp" -> {}
                service.serviceName.contains("NetDot") -> knownServices[service.host.toString()] = service
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            knownServices.remove(service.host.toString())
        }

        override fun onDiscoveryStopped(serviceType: String) {}

        override fun onStartDiscoveryFailed(p0: String?, p1: Int) {}

        override fun onStopDiscoveryFailed(p0: String?, p1: Int) {}
    }

    //private val nsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager)

    private var knownServices: HashMap<String, NsdServiceInfo> = HashMap()

    private fun joinGame(url: String) {
        // Init client
        gameState.startClient(url)
        // Go to game screen
        findNavController().navigate(R.id.gameFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_client, container, false)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Views
        val name = view.findViewById<EditText>(R.id.remoteAddressBox)
        val refresh = view.findViewById<Button>(R.id.refreshButton)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        //val list = view.findViewById<LinearLayout>(R.id.serverList)
        val refreshLan = view.findViewById<Button>(R.id.refreshLANButton)
        val recyclerLan = view.findViewById<RecyclerView>(R.id.recyclerLocal)
        val connect = view.findViewById<Button>(R.id.connectButton)
        // Data Adapter
        val serverList = mutableListOf<ServerListEntry>()
        val listAdapter = ServerListAdapter(serverList).also { it.onJoin = this::joinGame }
        val localList = mutableListOf<ServerListEntry>()
        val localAdapter = ServerListAdapter(localList).also { it.onJoin = this::joinGame }


        connect.setOnClickListener {
            // Init client
            gameState.startClient(name.text.toString())
            // Go to game screen
            findNavController().navigate(R.id.gameFragment)
        }
        refresh.setOnClickListener {
            println("REFRESH SERVER LIST")
            //list.removeAllViewsInLayout()
            serverList.clear()
            gameState.queryMasterServer { message ->
                println("MASTER SERVER SAID: $message")
                val segments = message.split(' ')
                val url: String = segments[0]
                val current: Int = segments[1].toInt()
                val max: Int = segments[2].toInt()
                val name: String = message.substring(segments[0].length + segments[1].length + segments[2].length + 3)
                serverList.add(0, ServerListEntry(name, current, max, url))
                activity?.runOnUiThread { listAdapter.notifyDataSetChanged() }
                /*val button = Button(context).also {
                    with (it) {
                        text = "$name | ${if (current >= 0) current else '?'}/${if (max > 0) max else '∞'}"
                        setOnClickListener { joinGame(url) }
                    }
                }
                val text = TextView(context).also {
                    with (it) {
                        text = "${if (current >= 0) current else '?'}/${if (max > 0) max else '∞'}"
                    }
                }
                activity?.runOnUiThread {
                    list.addView(button)
                    list.addView(text)
                }*/
            }
        }
        recycler.adapter = listAdapter
        refreshLan.setOnClickListener {
            println("Checking found LAN games.")
            localList.clear()
            knownServices.forEach { (address, _) ->
                println("Adding to list")
                localList.add(0, ServerListEntry(address, -1, -1, address))
                activity?.runOnUiThread { localAdapter.notifyDataSetChanged() }
            }
        }
        recyclerLan.adapter = localAdapter
    }
}
