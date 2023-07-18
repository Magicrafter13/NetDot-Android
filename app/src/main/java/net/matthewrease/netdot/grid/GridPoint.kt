package net.matthewrease.netdot.grid

import android.graphics.Point
import net.matthewrease.netdot.grid.GridPoint

/**
 * Represents a point (coordinate pair) on a grid.
 * @author Matthew Rease
 * @see Grid
 */
class GridPoint(column: Int, row: Int) : Point() {
    /**
     * Get a point to the bottom.
     * @return The [GridPoint] directly below (`y + 1`) of this one
     */
    val down: GridPoint
        get() = GridPoint(x, y + 1)

    /**
     * Get a point to the left.
     * @return The [GridPoint] directly to the left (`x - 1`) of this one
     */
    /**
     * Get a point to the left.
     * @return The [GridPoint] directly to the left (`x - 1`) of this one
     */
    val left: GridPoint
        get() = GridPoint(x - 1, y)

    /**
     * Get a point to the right.
     * @return The [GridPoint] directly to the right (`x + 1`) of this one
     */
    val right: GridPoint
        get() = GridPoint(x + 1, y)

    /**
     * Get a point to the top.
     * @return The [GridPoint] directly above (`y - 1`) of this one
     */
    val up: GridPoint
        get() = GridPoint(x, y - 1)

    /**
     * Indicates whether another point is "equal" to this one.
     * @param point The operand of the test
     * @return `true` if `point` has the same x, y coordinates as this point, `false` if not
     * @see Point.equals
     */
    override fun equals(other: Any?): Boolean
        = (other is GridPoint) && other.x == x && other.y == y

    override fun toString(): String
        = "$x $y"

    companion object {
        fun parsePoint(point: String): GridPoint {
            return GridPoint(
                point.substring(0, point.indexOf(",")).toInt(),
                point.substring(point.indexOf(",") + 1).toInt()
            )
        }
    }

    /**
     * A new point.
     * @param column The point's grid x coordinate
     * @param row The point's grid y coordinate
     */
    init {
        x = column
        y = row
    }
}
