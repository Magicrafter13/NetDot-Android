package net.matthewrease.netdot

import android.graphics.Color
import net.matthewrease.netdot.coms.Client

/**
 * A player in the game.
 * @param name The player's display name
 * @param client The player's communication client
 * @author Matthew Rease
 * @see NetDot
 */
class Player (
    var name: String,
    var client: Client?       // The client object for this player (shouldn't be used by clients...)
) {
    private var _disconnected: Boolean = false // Whether or not this player has disconnected (shouldn't be used by clients...)
    private var boxes: Int = 0                 // How many boxes this player owns

    val clientID: Int
        get() = client?.clientID.let { 0 }

    /**
     * Whether or not this player has been disconnected from the game.
     * @see Player#disconnect()
     * @return
     */
    val disconnected: Boolean
        get() = _disconnected

    /**
     * The player's current score.
     * @return How many boxes this player has claimed
     * @see NetDot
     * @see ScoreBoard
     */
    val score: Int
        get() = boxes

    var color: Int = 0               // This player's display color
    var customColor: Boolean = false // Whether or not the user has set their own color

    /**
     * Increment the player's score by one. (Meaning they claimed one box.)
     */
    inline fun add() = add(1)

    /**
     * Add <code>num</code> boxes to the player's score.
     * @param num How many boxes they have just earned
     */
    fun add(num: Int) {
        boxes += num
    }

    /**
     * Consider this player to be disconnected. (Changes their name too.)
     */
    fun disconnect() {
        _disconnected = true
        //reset()
        name = "Disconnected"
        color = Color.BLACK
    }

    /**
     * Reset this player's score.
     */
    fun reset() {
        boxes = 0
    }

    /**
     * Send a message to this player's client. (Unless they have disconnected.)
     * @param message The message to send
     */
    fun send(message: String): String =
        if (client != null && !_disconnected) client?.send(message).let { "" } else ""

    override fun toString(): String = name
}
