package net.matthewrease.netdot.coms

import java.net.ServerSocket
import java.net.Socket

abstract class ClientCollector(port: Int): Thread() {
    private val connectionManager: ServerSocket =
        ServerSocket(port) // Manages all client connections

    /**
     * Close communications.
     */
    fun close() {
        try {
            connectionManager.close()
        }
        catch (e: Exception) {
            println("Unable to close listener...")
            println(e)
        }
    }

    /**
     * Fires when a connection is established with a client.
     * <br>
     * Should be overridden by something more useful.
     * @param sock Socket between the server and the client.
     */
    abstract fun connected(sock: Socket)

    /**
     * Begin the thread.
     */
    override fun run() {
        println("Listening for connections on port " + connectionManager.localPort + "...")
        // Wait for connections.
        while (true) {
            try {
                val sock: Socket = connectionManager.accept()
                println("Connection established with " + sock.inetAddress)
                connected(sock)
            }
            catch (e: Exception) {
                if (!connectionManager.isBound) {
                    println("Could not accept a connection, will no longer wait for new clients.") // TODO: Show pop-up message for user
                    println(e)
                }
                break
            }
        }
        close()
    }

    /**
     * Open communications and wait for connections from clients.
     */
    /*init {
        // Open Listener
        try {
        }
        catch (Exception e) {
            println("Unable to bind to port " + port + "!");
            println(e);
        }
    }*/
}
