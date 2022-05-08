package net.matthewrease.netdot.coms

import java.io.PrintWriter
import java.net.Socket
import java.util.Scanner
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

/**
 * Bi-directional connection between a central server over a TCP Socket.
 * @param address remote server to connect to
 * @param port port of listener socket
 */
abstract class Server(
    private val address: String,
    private val port: Int
): Thread() {
    private val outbox = LinkedBlockingQueue<String>() // Queue for outbound messages

    private var sock: Socket? = null

    /**
     * Disconnect from the remote host.
     */
    fun close() {
        try {
            sock?.close()
        }
        catch (e: Exception) {
            println("Unable to close server socket...")
            e.printStackTrace()
        }
    }

    /**
     * Fires when a connection is established with a server.
     */
    abstract fun connected()

    /**
     * Fires when a connection is lost or terminated with a server.
     */
    abstract fun disconnected()

    /**
     * Fires when the server sends a message.
     * @param message A single line (without the newline terminator)
     */
    abstract fun receive(message: String)

    /**
     * Queues a message to send to the server.
     * @param message Outgoing message
     */
    fun send(message: String) = outbox.add(message)

    /**
     * Initiate the connection. IO takes place in two threads.
     */
    override fun run() {
        // TODO add timer in GameManager, if unable to connect within certain time, say connection timed out in console, and kill the server thread
        try {
            sock = Socket(address, port)
        }
        catch (e: Exception) {
            println("Unable to connect to $address on port $port\n$e")
            e.printStackTrace()
            return disconnected()
        }

        val serverOut: PrintWriter
        try {
            serverOut = PrintWriter(sock!!.getOutputStream())
        }
        catch (e: Exception) {
            println("Could not create PrintWriter for server!")
            e.printStackTrace()
            close()
            return disconnected()
        }
        thread {
            while (true) {
                try {
                    serverOut.println(outbox.take())
                    serverOut.flush()
                }
                catch (e: InterruptedException) {
                    e.printStackTrace()
                    break
                }
                if (!this.isAlive)
                    break
            }
        }

        val serverIn: Scanner
        try {
            serverIn = Scanner(sock!!.getInputStream())
        }
        catch (e: Exception) {
            println("Could not create Scanner for server!")
            e.printStackTrace()
            serverOut.close()
            close()
            return disconnected()
        }

        connected()
        while (serverIn.hasNextLine())
            receive(serverIn.nextLine())
        serverIn.close()
        serverOut.close()
        close()
        disconnected()
    }
}
