package net.matthewrease.netdot.coms

import androidx.lifecycle.LiveData
import net.matthewrease.netdot.Manager
import net.matthewrease.netdot.shapes.Dot
import net.matthewrease.netdot.NetDot
import net.matthewrease.netdot.data.User
import net.matthewrease.netdot.grid.Grid
import net.matthewrease.netdot.grid.GridPoint
import net.matthewrease.netdot.shapes.Line
import java.lang.System.currentTimeMillis
import java.net.Socket
import kotlin.random.Random

class ServerManager: Manager() {
    private enum class ServerState {
        LOBBY,
        GAME
    }

    private val queue: ArrayList<Client> = ArrayList()
    private val rng: Random = Random(currentTimeMillis())

    // Server state
    private var running: Boolean = false // Indicates a running server
    private var state: ServerState = ServerState.LOBBY // The current operational state of the server

    private var listener: ClientCollector? = null
    private var nextID: Int = 0
    private var maxPlayers: Int = 0
    private var randomizeStart: Boolean = false

    class LiveServer: LiveData<ServerManager>(ServerManager()) {
        override fun getValue(): ServerManager = super.getValue()!!
    }

    private fun assign(user: User, userID: Int) {
        user.id = userID
        user.client?.also { broadcast(it, "network-assign $userID") }
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
        // TODO: only add spectators that are ready
        //println("Moving spectators into free slots...");
        val freeSpace: Int = maxPlayers - game.users.filterValues { user -> !user.disconnected && user.playing }.size
        val spectators: List<User> = game.users.filterValues { user -> !user.disconnected && !user.playing }.map { (_, user) -> user }
        spectators.subList(0, if (maxPlayers == 0) spectators.size else freeSpace).forEach { user ->
            user.playing = true
            broadcast("game-join ${user.id}")
        }
    }

    private fun nextPlayer(startPlayer: User) {
        val playerIDs: List<Int> = game.users
            .filterValues { user -> !user.disconnected && user.playing && user.id >= 0 }.values
            .map { user -> user.id }
            .sorted()
        val index: Int = playerIDs.indexOf(startPlayer.id)
        val nextPlayer: Int = if (index == playerIDs.size - 1) playerIDs[0] else playerIDs[index + 1]
        game.currentPlayer.postValue(nextPlayer)
        broadcast("game-current $nextPlayer")
    }

    // Update UI!
    private fun update() {
        game.updatePlayers()
    }

    private fun userAdd(user: User) {
        val userID = nextID++
        broadcast("network-add $userID ${user.color} ${user.name}")
        assign(user, userID)
        game.users[userID] = user
        game.updatePlayers()
        // TODO: Update Master server on server occupancy
        //window.setCurrent(players.size)
    }

    fun broadcast(messages: String) =
        game.users.forEach { (_, user) -> user.client?.also { broadcast(it, messages) } }

    fun clientMessage(client: Client, user: User, messages: String) {
        val prefix = "<-- ${game.playerName(user.id)}: "
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

            // Convenient for Message Parsing
            val words: List<String> = message.split(' ')
            val command: List<String> = words[0].split('-')

            if (command.size < 2) {
                broadcast(client, "info-malformed " + command[0] + " was not followed by a hyphen!")
                return
            }

            when (command[0]) {
                // request- commands
                "request" -> {
                    when (command[1]) {
                        "info" -> {
                            if (user.vmajor < 0)
                                client.send("request-deny Server wants to know your version first!")
                            else {
                                client.send("info-version ${NetDot.VMAJOR} ${NetDot.VMINOR}\ninfo-features chat random-start") // TODO: Add vote and random-order
                                if (randomizeStart)
                                    client.send("feature-enable random-start")
                                //TODO("Send features and voting or whatever.")
                            }
                        }
                        "motd" -> if (!client.isValidated) client.send("info-motd An Android server.")
                        "join" -> {
                            if (user.id == -1) {
                                var color = (Int.MIN_VALUE..Int.MAX_VALUE).random()
                                var name = "User $nextID"
                                if (words.size > 1) {
                                    if (words[1] == "color") {
                                        if (words.size > 2) {
                                            try {
                                                color = words[2].toInt()
                                            }
                                            catch (e: Exception) {
                                                return TODO("Send warning back.")
                                            }
                                            if (words.size > 3 && words[3] == "name")
                                                name = message.substring(words[0].length + words[1].length + words[2].length + words[3].length + 4)
                                        }
                                    }
                                    else if (words[1] == "name")
                                        name = message.substring(words[0].length + words[1].length + 2)
                                }
                                user.color = color
                                user.name = name
                                println("Sending User ID")
                                userAdd(user)
                                println("Sending network data")
                                client.send( game.users
                                    .filterValues { !it.disconnected }
                                    .map { (_, user) -> "network-add ${user.id} ${user.color} ${user.name}" }
                                    .joinToString("\n"))
                                client.send("game-size ${game.grid.width} ${game.grid.height}")
                                if (game.started) {
                                    client.send("game-start")
                                    client.send( game.users
                                        .filterValues { !it.disconnected && it.playing }
                                        .map { (_, user) -> "game-join ${user.id}" }
                                        .joinToString("\n"))
                                    game.dots.forEach { (point, dot) ->
                                        dot.forEachLine { line ->
                                            line.owner?.let { client.send("game-line ${it.id} $point $line") }
                                        }
                                        dot.box.owner?.let { client.send("game-box ${it.id} $point") }
                                    }
                                    if (!game.finished)
                                        client.send("game-current ${game.currentPlayer.value}")
                                    //TODO("Finish this.")
                                }
                                //TODO("Add player to network.")
                            }
                        }
                    }
                    /*if (client.isValidated) {
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
                    else broadcast(client, "request-deny Server has not validated you yet!\nrequest-info")*/
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
                                user.vmajor = version[0]
                                user.vminor = version[1]
                                println("Client is running version ${user.vmajor}.${user.vminor}")
                                if (version[0] != NetDot.VMAJOR) {
                                    println("Client version is incompatible with server version (${NetDot.VMAJOR}.${NetDot.VMINOR})!")
                                    queue.remove(client)
                                    client.close()
                                }
                                else client.validate()
                            }
                            //else broadcast(client, "info-warn Server has already received your version info.")
                        }
                        "features" -> TODO("Something.")
                        /*"malformed" -> println("The client reported a malformed command...")
                        else -> broadcast(client, "unknown-info")*/
                    }
                }
                // vote- commands
                "vote" -> {
                    when (command[1]) {
                        "request" -> {
                            when (words[1]) {
                                // Feature
                                "enable", "disable" -> {
                                    when (words[2]) {
                                        "chat" -> {}
                                        "vote" -> {}
                                        "random-start" -> {}
                                        "random-order" -> {}
                                    }
                                }
                                // Regular votes
                                "restart" -> {}
                                "lobby" -> {}
                                "shuffle" -> {}
                                "kick" -> {}
                            }
                        }
                    }
                }
                // network- commands
                "network" -> {
                    when (command[1]) {
                        "chat" -> {
                            val msg: String = message.substring(words[0].length)
                            broadcast("network-chat ${user.id}$msg")
                            game.receiveChat(user.id, msg)
                        }
                        "ping" -> client.send("network-pong")
                        "pong" -> {}
                        else -> broadcast(client, "unknown-network")
                    }
                }
                // player- commands
                "user" -> {
                    if (client.isValidated) {
                        when (command[1]) {
                            "name" -> {
                                val name: String = message.substring(words[0].length + 1)
                                broadcast("user-name ${user.id} $name")
                                user.name = name
                                game.updatePlayers()
                            }
                            "color" -> {
                                val RGB: Int
                                try {
                                    RGB = words[1].toInt()
                                }
                                catch (e: NumberFormatException) {
                                    return broadcast(client, "info-malformed Could not parse RGB integer!")
                                }
                                broadcast("user-color ${user.id} $RGB")
                                user.color = RGB
                                game.updatePlayers()
                            }
                            else -> broadcast(client, "unknown-player")
                        }
                    }
                    else broadcast(client, "info-warn Server has not validated you yet!\nrequest-info")
                }
                // game- commands
                "game" -> {
                    if (client.isValidated) {
                        when (command[1]) {
                            "ready" -> {}
                            "notready" -> {}
                            "leave" -> {}
                            "join" -> {}
                            "line" -> {
                                if (user.id == game.currentPlayer.value) {
                                    val x: Int
                                    try {
                                        x = words[1].toInt()
                                    }
                                    catch (e: Exception) {
                                        return broadcast("info-malformed Could not parse GridPoint!")
                                    }
                                    val y: Int
                                    try {
                                        y = words[2].toInt()
                                    }
                                    catch (e: Exception) {
                                        return broadcast("info-malformed Could not parse GridPoint!")
                                    }
                                    val point = GridPoint(x, y)
                                    val vertical: Boolean = words[3] == "ver"
                                    if (!vertical && words[3] != "hor") {
                                        return broadcast(client, "info-malformed Could not parse line direction!")
                                    }
                                    if (makeMove(user, point, vertical))
                                        broadcast("game-line ${user.id} ${point.x} ${point.y} ${if (vertical) "ver" else "hor"}")
                                    else broadcast(client, "info-warn Invalid move!")
                                }
                                else broadcast(client, "info-warn ${if (user.id >= 0) "Not your turn!" else "You aren't part of this game!"}")
                            }
                            "box" -> {}
                            else -> broadcast(client, "unknown-game")
                        }
                    }
                    else broadcast(client, "info-warn Server has not validated you yet!\nrequest-info")
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

    fun gameRestart() {
        // Start Game
        with (game) {
            users.forEach { (_, user) -> user.reset() }
            broadcast("game-start")
            started = true
            finished = false
            if (randomizeStart) {
                currentPlayer.value = users.filterValues { user -> user.playing }.keys.random()
                broadcast("game-current ${currentPlayer.value}")
            }
            else currentPlayer.value = 0

            dots.forEach{ (_, dot) -> dot.reset() }
        }

        moveSpectators()

        update()
    }

    fun gameStop() {
        with (game) {
            broadcast("game-stop")
            started = false
            finished = false
            currentPlayer.value = -1
        }

        moveSpectators()

        game.dots.forEach { (_, dot) -> dot.reset() }

        update()
    }

    /**
     * Attempts a move by the current player.
     * <p>
     * If the move is successful, the player's score is incremented by the
     * number of boxes claimed (if any).<br>
     * If the player did score points, then they are allowed to take another
     * turn, otherwise play goes to the next player in line.
     * </p>
     * @param player The player making the move
     * @param gridPos The grid position of the dot that owns the clicked line
     * @param verticalLine <code>true</code> if the player clicked a vertical line,
     * <code>false</code> if they clicked a horizontal line
     * @return <code>true</code> if the move was successfully executed, <code>false</code>
     * if it was not (most likely the line was already owned by a player)
     */
    fun makeMove(player: User, gridPos: GridPoint, verticalLine: Boolean): Boolean {
        //println("game.finished: ${game.finished}\ngridPos !in game.grid: ${gridPos !in game.grid}\ngame.dots[gridPos] == null: ${game.dots[gridPos] == null}")
        if (game.finished || gridPos !in game.grid || game.dots[gridPos] == null)
            return false
        val requestDot: Dot = game.dots[gridPos]!!

        val first: GridPoint = if (verticalLine) gridPos.left else gridPos.up
        val third: GridPoint = if (verticalLine) first.down else first.right

        val uniqueDot: Dot? = if (first in game.grid) game.dots[first] else null

        // Ignore lines outside the grid (outer right/bottom edge)
        /*println("MADE IT")
        println("verticalLine: $verticalLine\ngridPos.x,y: ${gridPos.x},${gridPos.y}\ngame.grid.width,height: ${game.grid.width}x${game.grid.height}")*/
        if (if (verticalLine) gridPos.y >= game.grid.height - 1 else gridPos.x >= game.grid.width - 1)
            return false
        val request: Line = (if (verticalLine) requestDot.down else requestDot.right)
        if (request.owner != null) {
            println("Invalid move! Line already taken.")
            return false
        }
        request.owner = player

        // Claim boxes if possible
        var scored = false
        if (uniqueDot != null &&
            uniqueDot.right.owner != null &&
            uniqueDot.down.owner != null &&
            (if (verticalLine)
                game.dots[third]?.right
            else
                game.dots[third]?.down)!!.owner != null) {
            uniqueDot.box.owner = player
            broadcast("game-box ${player.id} ${first.x} ${first.y}")
            player.score += 1
            scored = true
        }
        val adjacentLine: Line? = if (verticalLine) game.dots[gridPos]?.right else game.dots[gridPos]?.down
        if (adjacentLine?.owner != null &&
            game.dots[gridPos.right]?.down!!.owner != null &&
            game.dots[gridPos.down]?.right!!.owner != null) {
            game.dots[gridPos]?.box?.owner = player
            broadcast("game-box ${player.id} ${gridPos.x} ${gridPos.y}")
            player.score += 1
            scored = true
        }

        // If a box was claimed, check if the game is over, otherwise change players. (Players who made a box get another move.)
        if (scored) {
            if (game.users.values.sumOf { user -> user.score } == game.grid.maxSpaces) {
                game.finished = true
                game.currentPlayer.postValue(-1)
            }
        }
        else
            nextPlayer(player)
        game.updateDots()
        return true
    }

    override fun sendChat(message: String) {
        broadcast("network-chat 0 $message")
        game.receiveChat(0, message)
    }

    override fun setColor(color: Int) {
        broadcast("user-color 0 $color")
        game.users[0]?.color = color
    }

    override fun setName(name: String) {
        broadcast("user-name ${user.id} $name")
        game.playerRename(game.users[0]!!, name)
    }

    /**
     * Initialize and start a server (does nothing if server is already running).
     * @param size Starting dimensions of the grid.
     * @param randomizeStart Picks a random player to start when a new game is started.
     * @see stop
     */
    fun start(size: Grid.Dimension, max: Int, randomizeStart: Boolean) {
        // Do nothing if the server was already started
        if (running)
            return

        // Initialize new game/lobby
        gameStop()

        // TODO: these should be cleared on a call to stop, and if stop isn't called but running is set to false presumably they are already cleared?
        queue.clear()
        game.users.clear()
        game.resize(size)
        maxPlayers = max
        this.randomizeStart = randomizeStart

        listener = object: ClientCollector(NetDot.PORT) {
            override fun connected(sock: Socket) {
                val client: Client = object: Client(sock) {
                    private val monitor = Thread {
                        while (true) {
                            try {
                                val message = receive()
                                clientMessage(this, user, message)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                                break
                            }
                            if (!this.isAlive)
                                break
                        }
                    }
                    private val user = User().also { it.client = this }
                    override fun connected() {
                        monitor.start()
                    }
                    override fun disconnected() {
                        if (isValidated) {
                            if (game.currentPlayer.value == user.id)
                                nextPlayer(user)
                            game.users.remove(user.id)
                            broadcast("network-remove ${user.id}")
                            game.updatePlayers()
                        }
                        //TODO("Not yet implemented")
                    }
                }
                queue.add(client)
                client.start()
                client.send("request-info")
            }
        }
        listener?.start()

        // grid-size
        println("<-- server: grid-size ${game.grid}")
        with (user) {
            vmajor = NetDot.VMAJOR
            vminor = NetDot.VMINOR
            id = 0
            color = rng.nextInt()
            name = "Server"
        }
        game.users[0] = user
        nextID = 1
        game.updatePlayers()
        // TODO: Update Master server on server occupancy
        //window.setCurrent(players.size)

        //updateDots()

        running = true
    }

    /**
     * Cleans up and stops a server (does nothing if server isn't running).
     * @see start(Grid.Dimension, Int, Boolean)
     */
    fun stop() {
        // Server isn't running, do nothing
        if (!running)
            return

        game.stop()
        val listener = this.listener
        if (listener != null) {
            with (queue) {
                println("Disconnecting $size queued clients...")
                if (isNotEmpty())
                    forEach { client -> client.close() }
            }
            with (game.users) {
                println("Disconnecting $size user clients...")
                if (isNotEmpty())
                    forEach { (_, user) ->
                        val client = user.client
                        if (client != null)
                            with (client) {
                                this.send("network-disconnect")
                                this.close()
                            }
                    }
            }
            listener.close()
            this.listener = null
        }

        running = false
    }

    fun tryMove(column: Int, row: Int, vertical: Boolean) {
        if (user.id == game.currentPlayer.value) {
            val point = GridPoint(column, row)
            println("<-- self: game-line $column $row ${if (vertical) "ver" else "hor"}")
            if (makeMove(user, point, vertical)) // TODO: store self (user) in class
                broadcast("game-line ${user.id} ${point.x} ${point.y} ${if (vertical) "ver" else "hor"}")
            else
                println("Could not make move!")
        }
        else println("It's not your turn.")
    }
}
