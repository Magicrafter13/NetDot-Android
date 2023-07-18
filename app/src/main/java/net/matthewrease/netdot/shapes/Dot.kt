package net.matthewrease.netdot.shapes

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
 * @see Line
 * @see Box
 */
class Dot {
    companion object {
        // Drawing Constants
        const val DIAMETER: Int = 10 // Diameter of the dot (circle)
    }

    val box: Box = Box()          // The box
    val down: Line = Line(true)   // The vertical line
    val right: Line = Line(false) // The horizontal line

    fun forEachLine(action: (Line) -> Unit) {
        action(right)
        action(down)
    }

    /**
     * Resets this dot, as well as its lines and box. (Ownership info.)
     */
    fun reset() {
        right.reset()
        down.reset()
        box.reset()
    }
}
