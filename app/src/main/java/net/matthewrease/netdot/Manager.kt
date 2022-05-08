package net.matthewrease.netdot

import net.matthewrease.netdot.grid.Grid

abstract class Manager {
    val game = NetDot(Grid(Grid.Dimension(8, 8)))

    abstract fun sendChat(message: String)

    abstract fun setColor(color: Int)

    abstract fun setName(name: String)
}
