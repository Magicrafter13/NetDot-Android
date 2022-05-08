package net.matthewrease.netdot.shapes

import android.graphics.Point
import net.matthewrease.netdot.Player
import net.matthewrease.netdot.grid.Grid
import net.matthewrease.netdot.grid.GridPoint
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a dot to be used in a grid of dots.
 * <p>
 * Each dot tracks 2 lines, a horizontal and a vertical line. These represent
 * the line directly to the right, and directly below, the dot.<br>
 * Each dot also tracks a single box.<br>
 * Dots on the edge of the game grid never have a box, will not track one or
 * more lines.
 * </p>
 * @author Matthew Rease
 * @see GameField
 * @see Line
 * @see Box
 */
class Dot {
    companion object {
        // Drawing Constants
        const val OFFSET: Int = 5

        const val DIAMETER: Int = 10 // Diameter of the dot (circle)
    }

    private val grid: Grid         // Grid Data
    private val gridPos: GridPoint // Coordinate position of this dot on the grid

    val box: Box?    // The box
    val down: Line?  // The vertical line
    val right: Line? // The horizontal line

    /**
     * Checks if the user clicked in (or near) a {@link Line}.
     * @param vertical <code>true</code> if the vertical line is being checked, <code>false</code>
     * to check the horizontal line
     * @param point The point clicked
     * @return <code>true</code> if the line was clicked, <code>false</code> if not
     */
    /*fun click(vertical: Boolean, point: Point): Boolean =
        with (if (vertical) down else right) { this?.click(point) ?: false }*/

    fun forEach(action: (Line?) -> Unit) {
        action(right)
        action(down)
    }

    /**
     * Informs this dot's lines, that the mouse has moved.
     * @param point The mouse pointer's x,y location
     */
    /*fun hover(point: Point) {
        down?.hover(point);
        right?.hover(point);
    }*/

    /**
     * Resets this dot, as well as its lines and box. (Ownership info.)
     */
    fun reset() {
        right?.reset();
        down?.reset();
        box?.reset();
    }

    /**
     * Create a new dot.<br>
     * The dot will also create lines and a box if necessary.
     * @param gridPos Grid position of the dot
     * @param grid Grid information
     * @param players The players in the game
     * @see Line
     * @see Box
     * @see GameField
     * @see Player
     */
    constructor(gridPos: GridPoint, grid: Grid, players: ConcurrentHashMap<Int, Player>) {
        // Set Relative Location
        this.gridPos = gridPos

        // Game Grid Information
        this.grid = grid

        // Create Lines
        right = if (gridPos.x < grid.width - 1) Line(gridPos, grid, players, false) else null
        down = if (gridPos.y < grid.height - 1) Line(gridPos, grid, players, true) else null

        box = if (right != null && down != null) Box(gridPos, grid, players) else null
    }
}
