package net.matthewrease.netdot

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.matthewrease.netdot.coms.ClientManager
import net.matthewrease.netdot.coms.Server
import net.matthewrease.netdot.coms.ServerManager
import net.matthewrease.netdot.grid.Grid
import net.matthewrease.netdot.live.*

/**
 * Storage for NetDot game data.
 */
class GameViewModel: ViewModel() {
    private var receiveMaster: (String) -> Unit = {}

    private val masterServer = MutableLiveData<Server>()

    private fun newMaster(): Server {
        //println("Connecting to master server")
        return object: Server("matthewrease.net", NetDot.MASTER_PORT) {
            override fun connected() {}
            override fun disconnected() {}
            override fun receive(message: String) {
                receiveMaster(message)
            }
        }.also { it.start() }
    }

    val advertise = LiveBoolean(false)
    val client = ClientManager.LiveClient()
    val connected = LiveBoolean(false)
    val dimX = MutableLiveData(8)
    val dimY = MutableLiveData(8)
    val isServer = LiveBoolean(false)
    val name = LiveString("")
    val randomizeStart = LiveBoolean(false)
    val server = ServerManager.LiveServer()

    val clientID: Int
        get() = if (this.isServer.value) 0 else client.value.getID()
    val dots: LiveDotMap
        get() = game.uiDots
    val finished: Boolean
        get() = game.finished
    val game: NetDot
        get() = manager.game
    val gridSize: LiveGridDimension
        get() = client.value.uiDims
    val manager: Manager
        get() = (if (this.isServer.value) server.value else client.value)
    val players: LivePlayers
        get() = game.uiPlayers
    val started: Boolean
        get() = game.started

    fun fragmentInit() = game.updatePlayers()

    fun queryMasterServer(callback: (String) -> Unit) {
        receiveMaster = callback
        var master = masterServer.value
        if (master == null) {
            with (newMaster()) {
                master = this
                masterServer.value = this
            }
        }
        master!!.send("list")
    }

    fun quit() {
        if (isServer.value) {
            if (advertise.value)
                masterServer.value?.send("reset")
            server.value.run { close() }
            println("STOPPING SERVER")
        }
        else {
            client.value.run { close() }
            println("DISCONNECTING FROM SERVER")
        }
    }

    fun sendChat(message: String) = (if (isServer.value) server.value else client.value).sendChat(message)

    fun startClient(url: String) {
        isServer.value = false
        connected.value = true
        client.value.run { open({ connected.postValue(false) }, url) }
    }

    fun startGame() {
        if (isServer.value) {
            with (server.value) {
                broadcast("game-${if (game.started) "restart" else "start"}")
                gameRestart()
            }
        }
        else {
            with (client.value) {
                broadcast("request-${if (game.started) "restart" else "start"}")
            }
        }
    }

    fun startServer(max: Int) {
        isServer.value = true
        if (advertise.value) {
            receiveMaster = {}
            var master = masterServer.value
            if (master == null) {
                with (newMaster()) {
                    master = this
                    masterServer.value = this
                }
            }
            master!!.send("name ${name.value}\nmax $max\ncurrent 1")
        }
        server.value.run { open(Grid.Dimension(dimX.value!!, dimY.value!!), max, randomizeStart.value) }
        println("STARTING SERVER")
    }

    fun stopGame() {
        if (isServer.value) {
            with (server.value) {
                broadcast("game-stop")
                gameStop()
            }
        }
        else client.value.broadcast("request-stop")
    }

    fun tryMove(column: Int, row: Int, vertical: Boolean) {
        if (isServer.value)
            server.value.tryMove(column, row, vertical)
        else
            client.value.tryMove(column, row, vertical)
        //println("hash ${dots.hashCode()}")
    }

    fun updatePlayers(count: Int) {
        if (isServer.value) {
            receiveMaster = {}
            var master = masterServer.value
            if (master == null) {
                with (newMaster()) {
                    master = this
                    masterServer.value = this
                    send("name ${this@GameViewModel.name.value}\nmax 0")
                }
            }
            master!!.send("current $count")
        }
    }
}
