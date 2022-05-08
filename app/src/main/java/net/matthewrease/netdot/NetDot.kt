package net.matthewrease.netdot

import android.graphics.Color
import net.matthewrease.netdot.coms.Client
import net.matthewrease.netdot.grid.Grid
import net.matthewrease.netdot.grid.GridPoint
import net.matthewrease.netdot.live.LiveDotMap
import net.matthewrease.netdot.live.LiveInt
import net.matthewrease.netdot.live.LivePlayers
import net.matthewrease.netdot.live.LiveString
import net.matthewrease.netdot.shapes.Dot
import net.matthewrease.netdot.shapes.Line
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
    val players = ConcurrentHashMap<Int, Player>() // Player data
    val uiChat = LiveString("")
    val uiDots = LiveDotMap()
    val uiPlayers = LivePlayers()

    var finished: Boolean = false
    var nextID: Int = 1 // next available unique ID
    var started: Boolean = false

    private fun playerNext() {
        var next = currentPlayer.value
        do {
            ++next
            if (next >= nextID)
                next = 0
            println("next = $next, ${!players.containsKey(next)}, ${players[next]?.disconnected}")
        } while (!players.containsKey(next) || players[next]?.disconnected == true)
        currentPlayer.postValue(next)
        // TODO: Update UI
        //updateText()
    }

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
     * Attempts a move by the current player.
     * <p>
     * If the move is successful, the player's score is incremented by the
     * number of boxes claimed (if any).<br>
     * If the player did score points, then they are allowed to take another
     * turn, otherwise play goes to the next player in line.
     * </p>
     * @param gridPos The grid position of the dot that owns the clicked line
     * @param verticalLine <code>true</code> if the player clicked a vertical line,
     * <code>false</code> if they clicked a horizontal line
     * @return <code>true</code> if the move was successfully executed, <code>false</code>
     * if it was not (most likely the line was already owned by a player)
     */
    fun makeMove(playerID: Int, gridPos: GridPoint, verticalLine: Boolean): Boolean {
        if (finished || gridPos !in grid || dots[gridPos] == null)
            return false
        val requestDot: Dot = dots[gridPos]!!

        val first: GridPoint = if (verticalLine) gridPos.left else gridPos.up
        val third: GridPoint = if (verticalLine) first.down else first.right

        val uniqueDot: Dot? = if (first in grid) dots[first] else null

        if ((if (verticalLine) requestDot.down else requestDot.right) == null)
            return false
        val request: Line = (if (verticalLine) requestDot.down else requestDot.right)!!
        if (!request.setOwner(playerID)) {
            println("Invalid move! Line already taken.")
            return false
        }

        // Claim boxes if possible
        var scored = false
        if (uniqueDot != null &&
            uniqueDot.right!!.getOwner() >= 0 &&
            uniqueDot.down!!.getOwner() >= 0 &&
            (if (verticalLine)
                dots[third]?.right
            else
                dots[third]?.down)!!.getOwner() >= 0) {
            uniqueDot.box?.setOwner(playerID)
            players[playerID]?.add()
            scored = true
        }
        val adjacentLine: Line? = if (verticalLine) dots[gridPos]?.right else dots[gridPos]?.down
        if (adjacentLine != null && adjacentLine.getOwner() >= 0 &&
            dots[gridPos.right]?.down!!.getOwner() >= 0 &&
            dots[gridPos.down]?.right!!.getOwner() >= 0) {
            dots[gridPos]?.box?.setOwner(playerID)
            players[playerID]?.add()
            scored = true
        }

        // If a box was claimed, check if the game is over, otherwise change players. (Players who made a box get another move.)
        if (scored) {
            if (players.values.sumOf { player -> player.score } == grid.maxSpaces) {
                finished = true
                currentPlayer.postValue(-1)
            }
        }
        else playerNext()
        updateDots()
        return true
    }

    /**
     * Get the current highest scoring player.
     * @return The {@link Player} with the highest score, <code>null</code> if there is a tie
     * between two or more players.
     */
    fun mostBoxes(): Array<Player> { // TODO utilize the fact that this tells us more than one player (say *who* the tie is between!)
        // Find biggest box count
        val highscore: Int = players.values.filter { player -> !player.disconnected }.maxOf { player -> player.score }
        // Get all players with that box count
        return players.values.filter { player -> player.score == highscore }.toTypedArray()
    }

    fun playerAdd(playerID: Int, name: String, client: Client?): Player {
        val player = Player(name, client)
        players[playerID] = player
        updatePlayers()
        return player
    }

    fun playerName(playerID: Int): String {
        return if (playerID == -2)
            "Spectator"
        else
            if (playerID == -1 || !players.containsKey(playerID))
                "Queued Client"
            else
                players[playerID].toString()
    }

    fun playerRemove(playerID: Int) {
        val player = players[playerID]
        if (player != null) {
            if (started) {
                player.disconnect()
                if (playerID == currentPlayer.value)
                    playerNext()
            }
            else players.remove(playerID)
            //updateScore()
        }
        updatePlayers()
    }

    /**
     * Changes a player's name.
     * @param player The player's number (<code>>= 0</code>)
     * @param newName The player's New Name
     * @see NetDot
     * @see Player
     */
    fun playerRename(player: Player, newName: String) {
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
        grid.forEach { point -> dots[point] = Dot(point, grid, players) }
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
        // Adjust color based on player index (if they haven't set one).
        players
            .toSortedMap()
            .values
            .filterNot { it.disconnected }
            .withIndex()
            .filterNot { (_, player) -> player.customColor }
            .forEach { (index, player) -> player.color = Color.HSVToColor(floatArrayOf(index * 360.0f / players.size, Line.saturation, Line.brightness)) }
        uiPlayers.postValue(HashMap(players))
        updateDots()
    }
}
