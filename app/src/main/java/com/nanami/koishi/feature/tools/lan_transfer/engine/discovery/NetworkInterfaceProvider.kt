package com.nanami.koishi.feature.tools.lan_transfer.engine.discovery

import java.net.Inet4Address
import java.net.NetworkInterface

data class LocalNetworkInterface(
    val networkInterface: NetworkInterface,
    val address: Inet4Address,
    val prefixLength: Int
)

class NetworkInterfaceProvider {
    fun activeIpv4Interfaces(): List<LocalNetworkInterface> =
        NetworkInterface.getNetworkInterfaces()
            ?.toList()
            ?.asSequence()
            ?.filter { it.isUp && !it.isLoopback && !it.isVirtual }
            ?.flatMap { networkInterface ->
                networkInterface.inetAddresses.toList()
                    .asSequence()
                    .filterIsInstance<Inet4Address>()
                    .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                    .map { address ->
                        val prefixLength = networkInterface.interfaceAddresses
                            .firstOrNull { it.address == address }
                            ?.networkPrefixLength
                            ?.toInt()
                            ?: 24
                        LocalNetworkInterface(networkInterface, address, prefixLength)
                    }
            }
            ?.toList()
            .orEmpty()
}
