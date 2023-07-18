package net.matthewrease.netdot

import net.matthewrease.netdot.data.User
import net.matthewrease.netdot.grid.Grid

abstract class Manager {
    val game = NetDot(Grid(Grid.Dimension(8, 8)))
    val user = User()

    abstract fun sendChat(message: String)

    abstract fun setColor(color: Int)

    abstract fun setName(name: String)
}
