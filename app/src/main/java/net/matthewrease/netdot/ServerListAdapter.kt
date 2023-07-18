package net.matthewrease.netdot

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.matthewrease.netdot.data.ServerListEntry

class ServerListAdapter(private val dataSet: MutableList<ServerListEntry>) : RecyclerView.Adapter<ServerListAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.serverName)
        val volume: TextView = view.findViewById(R.id.serverVolume)
        val button: Button = view.findViewById(R.id.joinButton)
    }

    var onJoin: (String) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.server_list_entry, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with (holder) {
            name.text = dataSet[position].name
            volume.text = "${with (dataSet[position].current) { if (this == 0) '?' else this }}/${with (dataSet[position].max) { if (this == 0) '∞' else this }}"
            button.setOnClickListener { onJoin(dataSet[position].address) }
        }
    }

    override fun getItemCount(): Int = dataSet.size
}
