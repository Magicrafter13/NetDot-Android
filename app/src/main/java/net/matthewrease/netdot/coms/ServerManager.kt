package net.matthewrease.netdot.coms

import androidx.lifecycle.LiveData
import net.matthewrease.netdot.Manager
import net.matthewrease.netdot.shapes.Dot
import net.matthewrease.netdot.NetDot
import net.matthewrease.netdot.Player
import net.matthewrease.netdot.grid.Grid
import net.matthewrease.netdot.grid.GridPoint
import java.net.Socket

class ServerManager: Manager() {
    private val queue: ArrayList<Client> = ArrayList()
    private val spectators: ArrayList<Client> = ArrayList()

    private var listener: ClientCollector? = null
    private var maxPlayers: Int = 0
    private var randomizeStart: Boolean = false

    class LiveServer: LiveData<ServerManager>(ServerManager()) {
        override fun getValue(): ServerManager = super.getValue()!!
    }

    private fun assign(client: Client, playerID: Int) {
        client.clientID = playerID
        broadcast(client, "network-assign $playerID")
        // TODO(updateScore())
    }

    private fun broadcast(client: Client, messages: String) {
        val prefix: String = "--> " + /*playerName(client.clientID) +*/ ": "
        messages.split('\n').forEach { message ->
            client.send(message)
            println(prefix + message)
        }
        // TODO: check user options messages.split('\n').forEach { msg -> window.network(prefix + client.send(msg))}
    }

    private fun moveSpectators() {
        //println("Moving spectators into free slots...");
        while ((maxPlayers == 0 || game.players.size < maxPlayers) && spectators.isNotEmpty()) {
            val client: Client = spectators[0]
            spectators.remove(client)
            playerAdd(client)
        }
    }

    private fun playerAdd(client: Client) {
        var playerID: Int
        do playerID = game.nextID++
        while (game.players.containsKey(playerID))
        assign(client, playerID)
        val player: Player = game.playerAdd(playerID, "Client $playerID", client)
        broadcast("player-add $playerID $player")
        // TODO: Update Master server on server occupancy
        //window.setCurrent(players.size)
    }

    // Update UI!
    private fun update() {
        game.updatePlayers()
    }

    fun broadcast(messages: String) {
        game.players.forEach { (playerID, player) ->
            if (playerID > 0)
                broadcast(player.client!!, messages)
        }
        spectators.forEach { client -> broadcast(client, messages) }
    }

    fun clientMessage(messages: String) {
        val prefix = "<-- self: "
        for (message in messages.split('\n')) {
            // TODO get user options
            println(prefix.plus(message))
            val id = 0

            // Convenient for Message Parsing
            val words: List<String> = message.split(' ')
            if (words[0] == "network-chat") {
                val msg: String = message.substring(words[0].length)
                broadcast("network-chat $id$msg")
            }
        }
    }

    fun clientMessage(client: Client, messages: String) {
        val prefix = "<-- ${game.playerName(client.clientID)}: "
        for (message in messages.split('\n')) {
            // TODO get user options
            println(prefix.plus(message))
            /*
             * ID meanings
             *  0: server
             * -1: queued client
             * -2: spectator
             * >0: player
             */
            val id: Int = client.clientID

            val player: Player? = game.players[id]

            // Convenient for Message Parsing
            val words: List<String> = message.split(' ')
            val command: List<String> = words[0].split('-')

            if (command.size < 2) {
                broadcast(client, "info-malformed " + command[0] + " was not followed by a hyphen!")
                return
            }

            when (command[0]) {
                // player- commands
                "player" -> {
                    if (client.isValidated) {
                        if (id >= 0) { // player!! safe due to (id >= 0) check
                            when (command[1]) {
                                "rename" -> {
                                    val name: String = message.substring(words[0].length + 1)
                                    broadcast("player-rename $id $name")
                                    game.playerRename(player!!, name)
                                }
                                "color" -> {
                                    val RGB: Int
                                    try {
                                        RGB = words[1].toInt()
                                    }
                                    catch (e: NumberFormatException) {
                                        return broadcast(client, "info-malformed Could not parse RGB integer!")
                                    }
                                    broadcast("player-color $id ${words[1]}")
                                    player?.color = RGB
                                    player?.customColor = true
                                    game.updatePlayers()
                                }
                                else -> broadcast(client, "unknown-player")
                            }
                        }
                        else broadcast(client, "info-warn You aren't a player yet!")
                    }
                    else broadcast(client, "info-warn Server has not validated you yet!\nrequest-info")
                }
                // network- commands
                "network" -> {
                    when (command[1]) {
                        "disconnect" -> {
                            broadcast(client, "network-disconnect")
                            client.close()
                            when {
                                id == -2 -> spectators.remove(client)
                                id == -1 -> queue.remove(client)
                                id > 0 -> {
                                    // Inform Other Clients and Update Server (us)
                                    broadcast("player-remove $id")
                                    game.playerRemove(id)
                                    //moveSpectators()
                                }
                            }
                        }
                        "chat" -> {
                            val msg: String = message.substring(words[0].length)
                            broadcast("network-chat $id$msg")
                            game.receiveChat(id, msg)
                        }
                        else -> broadcast(client, "unknown-network")
                    }
                }
                // game- commands
                "game" -> {
                    if (client.isValidated) {
                        when (command[1]) {
                            "play" -> {
                                if (id == game.currentPlayer.value) {
                                    val point: GridPoint
                                    try {
                                        point = GridPoint.parsePoint(words[1])
                                    }
                                    catch (e: Exception) {
                                        return broadcast(client, "info-malformed Could not parse GridPoint!")
                                    }
                                    val vertical: Boolean = words[2] == "ver"
                                    if (!vertical && words[2] != "hor") {
                                        return broadcast(client, "info-malformed Could not parse line direction!")
                                    }
                                    if (game.makeMove(id, point, vertical))
                                        broadcast("game-play $id $point ${if (vertical) "ver" else "hor"}")
                                    else broadcast(client, "info-warn Invalid move!")
                                }
                                else broadcast(client, "info-warn ${if (id >= 0) "Not your turn!" else "You aren't part of this game!"}")
                            }
                            else -> broadcast(client, "unknown-game")
                        }
                    }
                    else broadcast(client, "info-warn Server has not validated you yet!\nrequest-info")
                }
                // request- commands
                "request" -> {
                    if (client.isValidated) {
                        when (command[1]) {
                            "start", "restart" -> {
                                val chat = "${game.playerName(id)} wants to ${command[1]} the game."
                                broadcast("network-chat -3 $chat")
                                game.receiveChat(id, chat)
                            }
                            "stop" -> {
                                val chat = "${game.playerName(id)} wants to return to the lobby."
                                broadcast("network-chat -3 $chat")
                                game.receiveChat(id, chat)
                            }
                            "join" -> {
                                // If the client isn't in the queue, they must already be a player
                                if (client in queue) {
                                    // If we're in the lobby, they can join, otherwise they can spectate or leave
                                    if (!game.started) {
                                        if (maxPlayers == 0 || game.players.size != maxPlayers) {
                                            // Send full game data
                                            game.players.forEach { (playerID, plyr) ->
                                                broadcast(client, "player-add $playerID $plyr")
                                                if (plyr.customColor)
                                                    broadcast(client, "player-color " + playerID + " " + plyr.color)
                                            }
                                            broadcast(client, "grid-size ${game.grid}\ngrid-reset")
                                            playerAdd(client)
                                            queue.remove(client)
                                        }
                                        else broadcast(client, "request-deny Server full! (${game.players.size}/$maxPlayers players) - feel free to spectate\nnetwork-full")
                                    }
                                    else broadcast(client, "request-deny Server is in the middle of a game, feel free to spectate.\nnetwork-busy")
                                }
                                else broadcast(client, "request-deny Already joined.")
                            }
                            "spectate" -> {
                                if (queue.contains(client)) {
                                    if (game.started || maxPlayers == game.players.size) {
                                        assign(client, -2)
                                        // Send full game data
                                        game.players.forEach { (playerID, plyr) ->
                                            broadcast(client, "player-add $playerID $plyr")
                                            if (plyr.customColor)
                                                broadcast(client, "player-color $playerID ${plyr.color}")
                                        }
                                        broadcast(client, "grid-size ${game.grid}\ngrid-reset")
                                        spectators.add(client)
                                        queue.remove(client)
                                        if (game.started)
                                            broadcast(client, "game-start")
                                        game.players
                                            .filter { (_, player) -> player.disconnected }
                                            .forEach{ (playerID, _) -> broadcast(client, "player-remove $playerID") }
                                        broadcast(client, "game-current ${game.currentPlayer.value}")
                                        game.dots.forEach { (point, dot) ->
                                            dot.forEach { line ->
                                                if (line != null && line.getOwner() >= 0)
                                                    broadcast(client, "player-line ${line.getOwner()} $line $point")
                                            }
                                            if (dot.box != null && dot.box.getOwner() >= 0)
                                                broadcast(client, "player-box ${dot.box.getOwner()} $point")
                                        }
                                    }
                                    else broadcast(client, "request-deny There isn't a game running right now, feel free to join the lobby!")
                                }
                                else broadcast(client, "request-deny Already joined.")
                            }
                            else -> broadcast(client, "unknown-request")
                        }
                    }
                    else broadcast(client, "request-deny Server has not validated you yet!\nrequest-info")
                }
                // info- commands
                "info" -> {
                    when (command[1]) {
                        "version" -> {
                            if (!client.isValidated) {
                                val version: Array<Int> = arrayOf(0, 0)
                                try {
                                    version[0] = Integer.parseInt(words[1])
                                    version[1] = Integer.parseInt(words[2])
                                }
                                catch (e: Exception) {
                                    return broadcast(client, "bad-syntax Could not parse version numbers!")
                                }
                                println("Client is running version ${version[0]}.${version[1]}")
                                if (version[0] != NetDot.VMAJOR) {
                                    println("Client version is incompatible with server version (${NetDot.VMAJOR}.${NetDot.VMINOR})!")
                                    queue.remove(client)
                                    client.close()
                                }
                                else client.validate()
                            }
                            else broadcast(client, "info-warn Server has already received your version info.")
                        }
                        "malformed" -> println("The client reported a malformed command...")
                        else -> broadcast(client, "unknown-info")
                    }
                }
                // unknown- commands
                "unknown" -> {
                    when (command[1]) {
                        "" -> println("Client did not recognize command group!")
                        else -> println("Client did not recognize ${command[1]} directive!")
                    }
                }
                else -> broadcast(client, "unknown-")
            }
        }
    }

    fun close() {
        game.stop()
        val listener = this.listener
        if (listener != null) {
            with (queue) {
                println("Disconnecting $size queued clients...")
                if (isNotEmpty())
                    forEach { client ->
                        client.send(client.disconnect)
                        client.close()
                    }
            }
            with (game.players) {
                println("Disconnecting $size player clients...")
                if (isNotEmpty())
                    forEach { (_, player) ->
                        val client = player.client
                        if (client != null)
                            with (client) {
                                this.send(this.disconnect)
                                this.close()
                            }
                    }
            }
            with (spectators) {
                println("Disconnecting $size spectating clients...")
                if (isNotEmpty())
                    forEach { client ->
                        client.send(client.disconnect)
                        client.close()
                    }
            }
            listener.close()
            this.listener = null
        }
    }

    fun gameRestart() {
        // Stop Game
        gameStop()

        // Start Game
        with (game) {
            started = true
            if (randomizeStart) {
                currentPlayer.value = (0 until players.size).random()
                // TODO: This doesn't violate revision 2.0 of the net-code, but mainly because I
                //  never set hard rules for when commands are allowed... If and when I create 3.0,
                //  I should solidify things to disallow things like this. Because if a client was,
                //  originally rightly, created without supporting this command when not spectating,
                //  this feature won't work with them.
                broadcast("game-current ${currentPlayer.value}")
            }
            else currentPlayer.value = 0
        }
    }

    fun gameStop() {
        with (game) {
            started = false
            finished = false
            currentPlayer.value = -1
        }

        game.players.forEach { (playerID, player) ->
            if (player.disconnected)
                game.players.remove(playerID)
            else
                player.reset()
        }

        moveSpectators()

        game.dots.forEach { (_, dot) -> dot.reset() }

        update()
    }

    // Start server
    fun open(size: Grid.Dimension, max: Int, randomizeStart: Boolean) {
        gameStop()

        queue.clear()
        spectators.clear()
        game.players.clear()
        game.grid.resize(size)
        maxPlayers = max
        this.randomizeStart = randomizeStart

        listener = object: ClientCollector(NetDot.PORT) {
            override fun connected(sock: Socket) {
                val client: Client = object: Client(sock) {
                    override fun receive(message: String) {
                        clientMessage(this, message)
                    }
                }
                queue.add(client)
                assign(client, -1)
                client.disconnect = "network-disconnect"
                client.start()
            }
        }
        listener?.start()

        // grid-size
        println("<-- server: grid-size ${game.grid}")
        game.grid.forEach { point -> game.dots[point] = Dot(point, game.grid, game.players) }
        game.playerAdd(0, "Server", null)

        //updateDots()
    }

    override fun sendChat(message: String) {
        broadcast("network-chat 0 $message")
        game.receiveChat(0, message)
    }

    override fun setColor(color: Int) {
        broadcast("player-color 0 $color")
        with (game.players[0]!!) {
            this.color = color
            customColor = true
        }
    }

    override fun setName(name: String) {
        broadcast("player-rename 0 $name")
        game.playerRename(game.players[0]!!, name)
    }

    fun tryMove(column: Int, row: Int, vertical: Boolean) {
        if (0 == game.currentPlayer.value) {
            val point = GridPoint(column, row)
            println("<-- self: game-play $column,$row ${if (vertical) "ver" else "hor"}")
            if (game.makeMove(0, point, vertical))
                broadcast("game-play 0 $point ${if (vertical) "ver" else "hor"}")
            else
                println("Could not make move!")
        }
        else println("It's not your turn.")
    }
}
