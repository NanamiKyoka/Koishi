package com.nanami.koishi.feature.tools.lan_transfer.engine.discovery

import java.net.Inet4Address
import java.net.NetworkInterface

data class LocalNetworkInterface(
    val networkInterface: NetworkInterface,
    val address: Inet4Address
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
                    .map { address -> LocalNetworkInterface(networkInterface, address) }
            }
            ?.toList()
            .orEmpty()
}
