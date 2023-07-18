package net.matthewrease.netdot.shapes

import net.matthewrease.netdot.data.User

/**
 * A line belonging to a [Dot].<br></br>
 * Player's can take ownership of the line, but ownership cannot be transferred.
 * @author Matthew Rease
 */
class Line(
    vertical: Boolean
) {
    private var _owner // Owner of this line
            : User? = null
    private val vertical // Line orientation
            : Boolean

    /**
     * Owner of this line. Can only be changed if there is no owner.
     * @see reset
     */
    var owner: User?
        get() = _owner
        set(value) {
            if (_owner == null)
                _owner = value
        }

    /**
     * Reset line ownership and color.
     */
    fun reset() {
        _owner = null
    }

    override fun toString(): String = if (vertical) "ver" else "hor"

    /*companion object {
        // Drawing Constants
        const val brightness = 0.90f
        const val saturation = 0.85f // Default color saturation and brightness (HSB)
    }*/

    /**
     * A new line - should only be created by a [Dot].
     * @param vertical `true` if this is a vertical line, `false` if horizontal
     */
    init {
        // Set Line Direction
        this.vertical = vertical
        reset()
    }
}
