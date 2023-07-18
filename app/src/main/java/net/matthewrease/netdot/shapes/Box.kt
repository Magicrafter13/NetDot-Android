package net.matthewrease.netdot.shapes

import net.matthewrease.netdot.data.User

/**
 * A box for the game grid. Should be created by a [Dot].
 * @author Matthew Rease
 */
class Box {
    private var _owner // Owner of this box
            : User? = null

    /**
     * Owner of this box. Can only be changed if there is no owner.
     * @see reset
     */
    var owner: User?
        get() = _owner
        set(value) {
            if (_owner == null)
                _owner = value
        }

    /**
     * Reset box ownership.
     */
    fun reset() {
        _owner = null
    }
}
