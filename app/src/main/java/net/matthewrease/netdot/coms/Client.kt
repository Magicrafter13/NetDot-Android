package net.matthewrease.netdot.coms

import java.io.PrintWriter
import java.net.Socket
import java.util.Scanner
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * Bi-directional connection between a client over a TCP Socket.
 * @param sock Established socket between the server and client
 */
abstract class Client(// Initial socket for connection to the client
    private val sock: Socket
): Thread() {
    private val outbox = LinkedBlockingQueue<String>() // Queue for outbound messages

    private var validated: Boolean = false // Whether or not this client has been validated

    var disconnect: String = "" // Command to receive when disconnecting
    var clientID: Int = -1      // Unique ID for this player

    val isValidated: Boolean
        get() = validated

    /**
     * Disconnect from client.
     */
    fun close() {
        try {
            sock.close()
        }
        catch (e: Exception) {
            println("Unable to close client socket...")
            e.printStackTrace()
        }
    }

    /**
     * Fires when the client sends a message.
     * @param message A single line (without the newline terminator)
     */
    abstract fun receive(message: String)

    /**
     * Queues a message to send to the client.
     * @param message Outgoing message
     */
    fun send(message: String) = outbox.add(message)

    fun validate(): Boolean {
        if (validated)
            return false
        validated = true
        return true
    }

    /**
     * Begin the thread.
     */
    override fun run() {
        val clientOut: PrintWriter
        try {
            clientOut = PrintWriter(sock.getOutputStream()) // Connection to send commands to the client
        }
        catch (e: Exception) {
            println("Could not create PrintWriter for client!")
            e.printStackTrace()
            return sock.close()
        }
        thread {
            while (true) {
                try {
                    clientOut.println(outbox.take())
                    clientOut.flush()
                }
                catch (e: InterruptedException) {
                    e.printStackTrace()
                    break
                }
                if (!this.isAlive)
                    break
            }
        }

        val clientIn: Scanner
        try {
            clientIn = Scanner(sock.getInputStream()) // Connection to receive commands from the client
        }
        catch (e: Exception) {
            println("Could not create Scanner for client!")
            e.printStackTrace()
            clientOut.close()
            return sock.close()
        }

        while (clientIn.hasNextLine())
            receive(clientIn.nextLine())
        clientIn.close()
        clientOut.close()
        close()
        receive(disconnect)
    }
}
