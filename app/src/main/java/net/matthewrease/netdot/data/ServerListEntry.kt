package net.matthewrease.netdot.data

data class ServerListEntry(
    val name: String = "Unknown Server",
    val current: Int = 0,
    val max: Int = 0,
    val address: String
)
