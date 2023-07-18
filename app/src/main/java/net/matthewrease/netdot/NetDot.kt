package net.matthewrease.netdot

import net.matthewrease.netdot.data.User
import net.matthewrease.netdot.grid.Grid
import net.matthewrease.netdot.grid.GridPoint
import net.matthewrease.netdot.live.*
import net.matthewrease.netdot.shapes.Dot
import java.util.concurrent.ConcurrentHashMap

class NetDot(
    val grid: Grid,
) {
    companion object {
        const val PORT: Int = 1234
        const val MASTER_PORT: Int = 4321
        const val VMAJOR: Int = 2
        const val VMINOR: Int = 0
    }

    private val chat = mutableListOf<Pair<Int, String>>()

    val currentPlayer = LiveInt(-1)
    val dots = ConcurrentHashMap<GridPoint, Dot>() // Dot, Line, and Box Data
    val users = ConcurrentHashMap<Int, User>() // Player data
    val uiChat = LiveString("")
    val uiDots = LiveDotMap()
    val uiUsers = LiveUsers()

    var finished: Boolean = false
    var started: Boolean = false

    private fun updateChat() {
        // TODO notify user of new message
        uiChat.postValue(
            chat
                .filter { (id, _) -> id != -1 && id >= -3 }
                .joinToString("\n") { pair ->
                    when (pair.first) {
                        -3 -> pair.second
                        -2 -> "Spectator: ${pair.second}"
                        else -> "${playerName(pair.first)}: ${pair.second}"
                    }
                }
        )
    }

    /**
     * Get the current highest scoring player.
     * @return The {@link Player} with the highest score, <code>null</code> if there is a tie
     * between two or more players.
     */
    fun mostBoxes(): Array<User> { // TODO utilize the fact that this tells us more than one player (say *who* the tie is between!)
        // Find biggest box count
        val highscore: Int = users.values.filter { user -> user.playing && !user.disconnected }.maxOf { player -> player.score }
        // Get all players with that box count
        return users.values.filter { user -> user.score == highscore }.toTypedArray()
    }

    fun playerName(userID: Int): String {
        return if (userID == -2)
            "Spectator"
        else
            if (userID == -1 || !users.containsKey(userID))
                "Queued Client"
            else
                users[userID].toString()
    }

    /**
     * Changes a player's name.
     * @param player The player being renamed
     * @param newName The player's New Name
     * @see NetDot
     * @see User
     */
    fun playerRename(player: User, newName: String) {
        player.name = newName
        updatePlayers()
    }

    fun receiveChat(id: Int, message: String) {
        chat.add(Pair(id, message))
        //TODO("Notify user of new message if in game fragment")
        /*if (!chat.isVisible())
            text.chat.setText("New Msg")*/
        updateChat()
    }

    fun reset() {
        grid.forEach { point -> dots[point]?.reset() }
        updateDots()
    }

    fun resize(size: Grid.Dimension) {
        grid.resize(size)
        // Remove old dots (if board shrunk)
        dots.forEach { (point, _) -> if (point !in grid) dots.remove(point) }
        // Add new dots (if board expanded)
        grid.forEach { point -> dots[point] = Dot() }
    }

    fun stop() {
        chat.clear()
        updateChat()
    }

    fun updateDots() = uiDots.postValue(HashMap(dots))

    /**
     * Refresh player information
     */
    fun updatePlayers() {
        /*// Adjust color based on player index (if they haven't set one).
        players
            .toSortedMap()
            .values
            .filterNot { it.disconnected }
            .withIndex()
            .filterNot { (_, player) -> player.customColor }
            .forEach { (index, player) -> player.color = Color.HSVToColor(floatArrayOf(index * 360.0f / players.size, Line.saturation, Line.brightness)) }*/
        uiUsers.postValue(HashMap(users))
        updateDots()
    }
}
