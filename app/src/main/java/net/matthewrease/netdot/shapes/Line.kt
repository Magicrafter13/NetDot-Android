package net.matthewrease.netdot.shapes

import android.graphics.Color
import net.matthewrease.netdot.Player
import net.matthewrease.netdot.grid.Grid
import net.matthewrease.netdot.grid.GridPoint
import java.util.concurrent.ConcurrentHashMap

/**
 * A line belonging to a [Dot].<br></br>
 * Player's can take ownership of the line, but ownership cannot be transferred.
 * @author Matthew Rease
 * @see GameField
 */
class Line(
    gridPos: GridPoint,
    grid: Grid,
    players: ConcurrentHashMap<Int, Player>,
    vertical: Boolean
) {
    private var color // Current line color
            : Int? = null
    private var owner // Owner of this line
            : Int? = null
    private val players // Player Data
            : ConcurrentHashMap<Int, Player>
    /*private var validArea // Bounds for Valid Mouse Clicks/Hovers
            : Polygon? = null*/
    private val grid // Game grid information
            : Grid
    private val gridPos // Location on the grid
            : GridPoint
    private val vertical // Line orientation
            : Boolean

    /**
     * Check if the user clicked in (or near) the line.
     * @param point Where the user clicked
     * @return `true` if the user clicked this line, `false` if not
     */
    /*fun click(point: Point?): Boolean {
        return validArea.contains(point)
    }*/

    /**
     * Get the user that owns this line.
     * @return The owner (0 if unclaimed)
     */
    fun getOwner(): Int {
        return owner!!
    }

    /**
     * Tell the line where the mouse is hovering, for color changing.
     * @param point The location of the mouse cursor
     */
    /*fun hover(point: Point?) {
        if (owner == -1) color =
            if (validArea == null || !validArea.contains(point)) defaultColor else hoverColor
    }*/

    /**
     * Reset line ownership and color.
     */
    fun reset() {
        owner = -1
        color = defaultColor
    }

    /**
     * Adjust elements if window (panel) has been resized.
     * @param panelWidth New width of the game grid panel
     * @param panelHeight New height of the game grid panel
     * @see GameField
     *
     * @see Dot.resize
     */
    fun resize(panelWidth: Int, panelHeight: Int) {
        // Determine JPanel Coordinates and Size
        /*panelRect.setBounds(
            offset + gridPos.x * panelWidth / (grid.width - 1) + if (vertical) close else far,
            offset + gridPos.y * panelHeight / (grid.height - 1) + if (vertical) far else close,
            if (vertical) size else panelWidth / (grid.width - 1) - offset * 4,
            if (vertical) panelHeight / (grid.height - 1) - offset * 4 else size
        )*/

        // Calculate Mouse Diamond
        val horizontalRadius: Int =
            (panelWidth / (grid.width - 1) - offset * 4) / 2 + Dot.DIAMETER / 2 + offset
        val verticalRadius: Int =
            (panelHeight / (grid.height - 1) - offset * 4) / 2 + Dot.DIAMETER / 2 + offset
        /*val center = Point(panelRect.x + panelRect.width / 2, panelRect.y + panelRect.height / 2)
        validArea = Polygon(
            intArrayOf(
                center.x - horizontalRadius,
                center.x,
                center.x + horizontalRadius,
                center.x
            ), intArrayOf(
                center.y,
                center.y - verticalRadius,
                center.y,
                center.y + verticalRadius
            ),
            4
        )*/
    }

    /**
     * Claim ownership of this line.
     * @param player The claimant
     * @return `true` if the line was claimed, `false` if it was already claimed
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

    override fun toString(): String {
        return if (vertical) "ver" else "hor"
    }

    companion object {
        // Drawing Constants
        private const val close = 3
        private val defaultColor: Int = Color.rgb(230, 230, 230) // Default drawing color
        private const val far = 15
        private val hoverColor: Int =
            Color.rgb(180, 180, 180) // Color when hovering over line with mouse
        private const val offset = 5
        private const val showMouseBoundaries = false // Show debug boundary lines
        private const val size = 4 // Line thickness
        const val brightness = 0.90f
        const val saturation = 0.85f // Default color saturation and brightness (HSB)
    }

    /**
     * A new line - should only be created by a [Dot].
     * @param gridPos Grid position of this line's [Dot]
     * @param grid Grid information
     * @param players Player data
     * @param vertical `true` if this is a vertical line, `false` if horizontal
     */
    init {
        // Set Relative Location
        this.gridPos = gridPos

        // Game Grid Information
        this.grid = grid

        // Set Player Count
        this.players = players

        // Set Line Direction
        this.vertical = vertical
        reset()
    }
}
