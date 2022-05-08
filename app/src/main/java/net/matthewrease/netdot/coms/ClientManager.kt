package net.matthewrease.netdot.coms

import androidx.lifecycle.LiveData
import net.matthewrease.netdot.Manager
import net.matthewrease.netdot.NetDot
import net.matthewrease.netdot.grid.Grid
import net.matthewrease.netdot.grid.GridPoint
import net.matthewrease.netdot.live.LiveGridDimension

class ClientManager: Manager() {
    private var clientID: Int = -1
    private var server: Server? = null

    class LiveClient: LiveData<ClientManager>(ClientManager()) {
        override fun getValue(): ClientManager = super.getValue()!!
    }

    val uiDims = LiveGridDimension(8, 8)

    private fun serverMessage(message: String) {
        val prefix = "<-- server: "
        println(prefix + message) // TODO: Check user options

        val words: List<String> = message.split(' ')
        val command: List<String> = words[0].split('-')

        val vertical: Boolean

        when (command[0]) {
            "player" -> {
                val playerID: Int
                try {
                    playerID = words[1].toInt()
                }
                catch (e: Exception) {
                    return broadcast("info-malformed Could not parse playerID")
                }
                val player = game.players[playerID]
                when (command[1]) {
                    "add" -> {
                        game.playerAdd(playerID, message.substring(words[0].length + words[1].length + 2), null)
                        if (playerID >= game.nextID)
                            game.nextID = playerID + 1
                    }
                    "rename" -> {
                        // Don't rename ourselves (that already should have happened)
                        if (player != null)
                            game.playerRename(player, message.substring(words[0].length + words[1].length + 2))
                    }
                    "color" -> {
                        val RGB: Int
                        try {
                            RGB = words[2].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse RGB color!")
                        }
                        player?.color = RGB
                        player?.customColor = true
                        game.updatePlayers()
                    }
                    "remove" -> {
                        // If the server has disconnected us (or itself!) return to the menu
                        if (playerID == clientID || playerID == 0)
                            server?.disconnected()
                        game.playerRemove(playerID)
                        update()
                    }
                    "line" -> {
                        vertical = words[2] == "ver"
                        if (!vertical && words[2] != "hor")
                            return broadcast("info-malformed Could not parse line direction!")
                        val point: GridPoint
                        try {
                            point = GridPoint.parsePoint(words[3])
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse GridPoint!")
                        }
                        val dot = game.dots[point]
                        (if (vertical) dot?.down else dot?.right)?.setOwner(playerID)
                        //updatePlayers()
                        game.updateDots()
                    }
                    "box" -> {
                        val point: GridPoint
                        try {
                            point = GridPoint.parsePoint(words[2])
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse GridPoint!")
                        }
                        player?.add()
                        game.dots[point]?.box?.setOwner(playerID)
                        game.updatePlayers()
                    }
                    else -> broadcast("unknown-player")
                }
            }
            "grid" -> {
                when (command[1]) {
                    "size" -> {
                        val size: Grid.Dimension
                        try {
                            size = Grid.parseDimension(words[1])
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse grid dimensions!")
                        }
                        game.resize(size)
                        uiDims.postValue(Grid.Dimension(size.width, size.height))
                    }
                    "reset" -> game.reset()
                    else -> broadcast("unknown-grid")
                }
            }
            "network" -> {
                when (command[1]) {
                    "assign" -> {
                        val playerID: Int
                        try {
                            playerID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse clientID!")
                        }
                        clientID = playerID
                        when (clientID) {
                            -2 -> println("Assigned to spectator mode.")
                            -1 -> println("Placed in queue, waiting for further instructions from the server...")
                            else -> println("Joining game with player ID $clientID")
                        }
                    }
                    "busy" -> {
                        println("The server is already in the middle of a game. Asking to spectate.")
                        broadcast("request-spectate")
                    }
                    "chat" -> {
                        val playerID: Int
                        try {
                            playerID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse playerID!")
                        }
                        game.receiveChat(playerID, message.substring(words[0].length + words[1].length + 2))
                    }
                    "disconnect" -> close()
                    "full" -> {
                        println("The server is full! Asking to spectate.")
                        broadcast("request-spectate")
                    }
                    else -> broadcast("unknown-network")
                }
            }
            "game" -> {
                when (command[1]) {
                    "start", "restart" -> gameRestart()
                    "play" -> {
                        val playerID: Int
                        try {
                            playerID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse playerID!")
                        }
                        val point: GridPoint
                        try {
                            point = GridPoint.parsePoint(words[2])
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse GridPoint!")
                        }
                        vertical = words[3] == "ver"
                        if (!vertical && words[3] != "hor")
                            return broadcast("info-malformed Could not parse line direction!")
                        game.makeMove(playerID, point, vertical)
                    }
                    "stop" -> gameStop()
                    "current" -> {
                        val playerID: Int
                        try {
                            playerID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse current player!")
                        }
                        game.currentPlayer.postValue(playerID)
                        //updateText()
                    }
                    else -> broadcast("unknown-game")
                }
            }
            "info" -> {
                when (command[1]) {
                    "warn" -> println("Received warning: " + message.substring(words[0].length))
                    "malformed" -> println("Whatever you just did sent a pretty bad request to the server, please report this error!")
                    "version" -> {
                        val version: Array<Int> = arrayOf(0, 0)
                        try {
                            version[0] = words[1].toInt()
                            version[1] = words[2].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse version numbers!")
                        }
                        println("Server is running version " + version[0] + "." + version[1])
                        if (version[0] != NetDot.VMAJOR) {
                            println("Server version is incompatible with client version (${NetDot.VMAJOR}.${NetDot.VMINOR})!")
                            //serverOut.close();
                        }
                    }
                    else -> broadcast("unknown-info")
                }
            }
            "request" -> {
                when (command[1]) {
                    "deny" -> println("Server denied request with reason: " + message.substring(words[0].length + 1))
                    "info" -> broadcast("info-version ${NetDot.VMAJOR} ${NetDot.VMINOR}")
                }
            }
            "unknown" -> {
                when (command[1]) {
                    "" -> println("Server did not recognize command group!")
                    else -> println("Server did not recognize ${command[1]} directive!")
                }
            }
            else -> broadcast("unknown-")
        }
    }

    fun broadcast(messages: String) {
        val prefix = "--> server: "
        messages.split('\n').forEach { message ->
            server?.send(message)
            println(prefix + message) // TODO: Check user options
        }
    }

    fun close() {
        game.stop()
        val server = this.server
        if (server != null) {
            with (server) {
                send("network-disconnect")
                close()
            }
        }
    }

    fun gameRestart() {
        // Stop Game
        gameStop()

        // Start Game
        game.started = true
        game.currentPlayer.postValue(0)

        // TODO: update text
        //update()
    }

    fun gameStop() {
        game.started = false
        game.finished = false
        game.currentPlayer.postValue(-1)

        game.players.forEach { (playerID, player) ->
            if (player.disconnected)
                game.players.remove(playerID)
            else
                player.reset()
        }

        game.dots.forEach { (_, dot) -> dot.reset() }

        update()
    }

    fun getID(): Int {
        return clientID
    }

    // Start server
    fun open(quit: () -> Unit, address: String) {
        gameStop()

        game.players.clear()

        server = object: Server(address, NetDot.PORT) {
            override fun connected() = broadcast("info-version ${NetDot.VMAJOR} ${NetDot.VMINOR}\nrequest-join")
            override fun disconnected() = quit()
            override fun receive(message: String) = serverMessage(message)
        }
        server?.start()

        clientID = -1

        //updateDots()
    }

    override fun sendChat(message: String) = broadcast("network-chat $message")

    override fun setColor(color: Int) = broadcast("player-color $color")

    override fun setName(name: String) = broadcast("player-rename $name")

    fun tryMove(column: Int, row: Int, vertical: Boolean) {
        if (game.started) {
            if (!game.finished) {
                if (clientID == game.currentPlayer.value) {
                    broadcast("game-play $column,$row ${if (vertical) "ver" else "hor"}")
                }
                else println("It's not your turn.")
            }
            else println("The game is over!")
        }
        else println("Game hasn't started yet.")
    }

    // Update UI!
    fun update() {
        game.updatePlayers()
    }
}
