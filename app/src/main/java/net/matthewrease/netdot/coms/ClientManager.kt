package net.matthewrease.netdot.coms

import androidx.lifecycle.LiveData
import net.matthewrease.netdot.Manager
import net.matthewrease.netdot.NetDot
import net.matthewrease.netdot.data.User
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
            "request" -> {
                when (command[1]) {
                    "info" -> {
                        broadcast("info-version ${NetDot.VMAJOR} ${NetDot.VMINOR}\nrequest-info")
                        //TODO("Send features")
                    }
                    "deny" -> {
                        //println("Server denied request with reason: " + message.substring(words[0].length + 1))
                        TODO("Inform user and quit to menu")
                    }
                }
            }
            "info" -> {
                when (command[1]) {
                    "version" -> {
                        val version: Array<Int> = arrayOf(0, 0)
                        try {
                            version[0] = words[1].toInt()
                            version[1] = words[2].toInt()
                        }
                        catch (e: Exception) {
                            TODO("Quit to menu")
                            return broadcast("info-malformed Could not parse version numbers!")
                        }
                        println("Server is running version " + version[0] + "." + version[1])
                        if (version[0] != NetDot.VMAJOR) {
                            println("Server version is incompatible with client version (${NetDot.VMAJOR}.${NetDot.VMINOR})!")
                            //serverOut.close();
                        }
                        else broadcast("request-join")
                    }
                    "motd" -> {} // TODO: use this somewhere in the UI?
                    "features" -> {}
                }
            }
            "feature" -> {
                when (command[1]) {
                    "enable" -> {}
                    "disable" -> {}
                }
            }
            "vote" -> {
                when (command[1]) {
                    "start" -> {}
                    "end" -> {}
                    "enable" -> {}
                    "disable" -> {}
                }
            }
            "network" -> {
                when (command[1]) {
                    "assign" -> {
                        val userID: Int
                        try {
                            userID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            TODO("Quit to menu.")
                            return broadcast("info-malformed Could not parse clientID!")
                        }
                        clientID = userID
                        // TODO: show a toast to the user perhaps?
                    }
                    "announce" -> { game.receiveChat(-3, message.substring(words[0].length + 1)) } // TODO: change to an announcement function
                    "add" -> {
                        val userID: Int
                        try {
                            userID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse clientID!")
                        }
                        val color: Int
                        try {
                            color = words[2].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse color!")
                        }
                        game.users[userID] = User().also {
                            it.id = userID
                            it.color = color
                            it.name = message.substring(words[0].length + words[1].length + words[2].length + 3)
                        }
                        game.updatePlayers()
                    }
                    "remove" -> {
                        val userID: Int
                        try {
                            userID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse clientID!")
                        }
                        game.users.remove(userID)
                        update()
                    }
                    "chat" -> {
                        val userID: Int
                        try {
                            userID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse playerID!")
                        }
                        game.receiveChat(userID, message.substring(words[0].length + words[1].length + 2))
                    }
                    "ping" -> { return broadcast("network-pong") }
                    "pong" -> {} // TODO: something
                }
            }
            "user" -> {
                val userID: Int
                try {
                    userID = words[1].toInt()
                }
                catch (e: Exception) {
                    return broadcast("info-malformed Could not parse clientID!")
                }
                val user = game.users[userID] ?: return // TODO: bug in server or bug in us? inform them?
                when (command[1]) {
                    "name" -> {
                        user.name = message.substring(words[0].length + words[1].length + 2)
                        game.updatePlayers()
                    }
                    "color" -> {
                        val color: Int
                        try {
                            color = words[2].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse RGB color!")
                        }
                        user.color = color
                        game.updatePlayers()
                    }
                }
            }
            "game" -> {
                when (command[1]) {
                    "ready" -> {
                        val userID: Int
                        try {
                            userID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse playerID!")
                        }
                        game.users[userID]?.also { user -> user.ready = true }
                    }
                    "notready" -> {
                        val userID: Int
                        try {
                            userID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse playerID!")
                        }
                        game.users[userID]?.also { user -> user.ready = false }
                    }
                    "start" -> gameRestart()
                    "stop" -> gameStop()
                    "leave" -> {
                        val userID: Int
                        try {
                            userID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse playerID!")
                        }
                        game.users[userID]?.also { user -> user.playing = false }
                    }
                    "join" -> {
                        val userID: Int
                        try {
                            userID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse playerID!")
                        }
                        game.users[userID]?.also { user -> user.playing = true }
                    }
                    "size" -> {
                        val width: Int
                        try {
                            width = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse grid dimensions!")
                        }
                        val height: Int
                        try {
                            height = words[2].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse grid dimensions!")
                        }
                        val size = Grid.Dimension(width, height)
                        game.resize(size)
                        uiDims.postValue(size)
                    }
                    "line" -> {
                        val userID: Int
                        try {
                            userID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse playerID!")
                        }
                        val x: Int
                        try {
                            x = words[2].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse GridPoint!")
                        }
                        val y: Int
                        try {
                            y = words[3].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse GridPoint!")
                        }
                        val point = GridPoint(x, y)
                        vertical = words[4] == "ver"
                        if (!vertical && words[4] != "hor")
                            return broadcast("info-malformed Could not parse line direction!")
                        if (point in game.grid) {
                            println("In grid")
                            println("There are ${game.dots.size} dots")
                            game.dots[point]?.also { dot ->
                                println("Dot exists")
                                game.users[userID]?.also { user ->
                                    println("User exists")
                                    println("dot.${if (vertical) "down" else "right"}.owner = ${(if (vertical) dot.down else dot.right).owner}")
                                    (if (vertical) dot.down else dot.right).owner = user
                                    println("dot.${if (vertical) "down" else "right"}.owner = ${(if (vertical) dot.down else dot.right).owner}")
                                    game.updateDots()
                                }
                            }
                        }
                    }
                    "box" -> {
                        val userID: Int
                        try {
                            userID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse playerID!")
                        }
                        val x: Int
                        try {
                            x = words[2].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse GridPoint!")
                        }
                        val y: Int
                        try {
                            y = words[3].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse GridPoint!")
                        }
                        val point = GridPoint(x, y)
                        if (point in game.grid)
                            game.dots[point]?.also { dot ->
                                game.users[userID]?.also { user ->
                                    dot.box.owner = user
                                    ++user.score
                                    // Check if the game is over
                                    if (game.users.values.sumOf { _user -> _user.score } == game.grid.maxSpaces) {
                                        game.finished = true
                                        game.currentPlayer.postValue(-1)
                                    }
                                    game.updatePlayers()
                                }
                            }
                    }
                    "current" -> {
                        val userID: Int
                        try {
                            userID = words[1].toInt()
                        }
                        catch (e: Exception) {
                            return broadcast("info-malformed Could not parse current player!")
                        }
                        game.currentPlayer.postValue(userID)
                        //updateText()
                    }
                }
            }
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

        // Remove disconnected users
        game.users
            .filterValues { user -> user.disconnected }.keys
            .forEach { id -> game.users.remove(id) }
        // Reset all users game data
        game.users.values.forEach { user ->
            user.playing = false
            user.reset()
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

        game.users.clear()

        server = object: Server(address, NetDot.PORT) {
            override fun connected() {} //broadcast("info-version ${NetDot.VMAJOR} ${NetDot.VMINOR}\nrequest-join")
            override fun disconnected() = quit()
            override fun receive(message: String) = serverMessage(message)
        }
        server?.start()

        clientID = -1

        //updateDots()
    }

    override fun sendChat(message: String) = broadcast("network-chat $message")

    override fun setColor(color: Int) = broadcast("user-color $color")

    override fun setName(name: String) = broadcast("user-name $name")

    fun tryMove(column: Int, row: Int, vertical: Boolean) {
        if (game.started) {
            if (!game.finished) {
                if (clientID == game.currentPlayer.value) {
                    broadcast("game-line $column $row ${if (vertical) "ver" else "hor"}")
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
