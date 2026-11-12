package com.musta.iptv.server

import java.net.Inet4Address
import java.net.NetworkInterface

object NetUtil {
    /** The device's address on the local network, or null when it is not connected. */
    fun lanIp(): String? = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    } catch (e: Exception) {
        null
    }
}
