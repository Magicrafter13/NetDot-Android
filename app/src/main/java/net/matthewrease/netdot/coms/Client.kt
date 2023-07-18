package net.matthewrease.netdot.coms

import java.io.IOException
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
    private val inbox = LinkedBlockingQueue<String>()  // Queue for inbound messages
    private val outbox = LinkedBlockingQueue<String>() // Queue for outbound messages

    private var validated: Boolean = false // Whether or not this client has been validated

    val isValidated: Boolean
        get() = validated

    /**
     * Disconnect from client.
     */
    fun close() {
        try {
            sock.shutdownInput()
            sock.shutdownOutput()
            sock.close()
        }
        catch (e: IOException) {
            println("Unable to close client socket...")
            e.printStackTrace()
        }
    }

    abstract fun connected()

    abstract fun disconnected()

    /**
     * Get the next message in the queue (or wait for a new one).
     */
    fun receive(): String = inbox.take()

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

        connected()
        while (clientIn.hasNextLine())
            inbox.add(clientIn.nextLine())
        disconnected()

        clientIn.close()
        clientOut.close()
        close()
    }
}
