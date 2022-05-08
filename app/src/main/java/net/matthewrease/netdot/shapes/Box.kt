package net.matthewrease.netdot.shapes

import net.matthewrease.netdot.Player
import net.matthewrease.netdot.grid.Grid
import net.matthewrease.netdot.grid.GridPoint
import java.util.concurrent.ConcurrentHashMap

/**
 * A box for the game grid. Should be created by a [Dot], special exception may be made for a [ScoreBoard].
 * @author Matthew Rease
 * @see GameField
 */
class Box(gridPos: GridPoint, grid: Grid, players: ConcurrentHashMap<Int, Player>) {
    private var owner // Owner of this box
            : Int? = null
    private val grid // Game grid information
            : Grid
    private val gridPos // Position on game grid
            : GridPoint
    private val players // Player Data
            : ConcurrentHashMap<Int, Player>

    /**
     * Get the box's owner.
     * @return The number of the player that owns the box
     */
    fun getOwner(): Int {
        return owner!!
    }

    /**
     * Reset box ownership.
     */
    fun reset() {
        owner = -1
    }

    /**
     * Claim ownership of the box.
     * @param player Number of the player claiming the box
     * @return `true` if successfully claimed, `false` if box already owned
     */
    fun setOwner(playerID: Int): Boolean {
        if (players.containsKey(playerID)) {
            if (owner == -1) {
                owner = playerID
                return true
            }
        }
        return false
    }

    companion object {
        // Drawing Constants
        private const val offset = 5
    }

    /**
     * A new box, with no owner.
     * @param gridPos Grid position of the box's [Dot]
     * @param grid Grid information
     * @param players Players in the game
     * @see GridPoint
     */
    init {
        // Set Relative Location
        this.gridPos = gridPos

        // Game Grid Information
        this.grid = grid

        // Set Player Count
        this.players = players
        reset()
    }
}
