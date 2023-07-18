package net.matthewrease.netdot.data

import android.graphics.Color
import net.matthewrease.netdot.coms.Client

class User {
    var vmajor: Int = -1
    var vminor: Int = -1
    var id: Int = -1
    var color: Int = Color.BLACK
    var name: String = "A Client"
    var playing: Boolean = false
    var score: Int = 0
    var disconnected: Boolean = false
    var ready: Boolean = true

    var client: Client? = null

    fun reset() {
        score = 0
    }
}
