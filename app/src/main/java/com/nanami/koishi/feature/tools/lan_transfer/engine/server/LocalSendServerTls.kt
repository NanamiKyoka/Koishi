package com.nanami.koishi.feature.tools.lan_transfer.engine.server

import com.nanami.koishi.feature.tools.lan_transfer.engine.security.LocalSendIdentityMaterial
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelPipeline
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.SslHandshakeCompletionEvent
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap

internal class LocalSendClientCertificateRegistry {
    private val certificates = ConcurrentHashMap<String, X509Certificate>()

    fun add(channel: Channel, certificate: X509Certificate) {
        channel.remoteAddress()?.let { certificates[key(it)] = certificate }
    }

    fun remove(channel: Channel) {
        channel.remoteAddress()?.let { certificates.remove(key(it)) }
    }

    fun find(host: String, port: Int): X509Certificate? =
        certificates[addressKey(host, port)]
            ?: certificates.entries.firstOrNull { (key, _) ->
                key.substringAfterLast(':').toIntOrNull() == port &&
                    key.substringBeforeLast(':').equals(host, ignoreCase = true)
            }?.value

    private fun key(address: java.net.SocketAddress): String {
        val inet = address as InetSocketAddress
        return addressKey(inet.address?.hostAddress ?: inet.hostString, inet.port)
    }

    private fun addressKey(host: String, port: Int): String = "$host:$port"
}

internal fun createLocalSendSslContext(identity: LocalSendIdentityMaterial): SslContext =
    SslContextBuilder.forServer(identity.privateKey, identity.certificate)
        .trustManager(InsecureTrustManagerFactory.INSTANCE)
        .clientAuth(ClientAuth.REQUIRE)
        .build()

internal fun localSendTlsPipelineConfig(
    identity: LocalSendIdentityMaterial,
    certificates: LocalSendClientCertificateRegistry
): ChannelPipeline.() -> Unit {
    val sslContext = createLocalSendSslContext(identity)
    return {
        val existing = get(SslHandler::class.java)
        if (existing != null) {
            replace(existing, "ssl", SslHandler(sslContext.newEngine(channel().alloc())))
        }
        addLast("localsend-client-certificates", ClientCertificateCaptureHandler(certificates))
    }
}

private class ClientCertificateCaptureHandler(
    private val certificates: LocalSendClientCertificateRegistry
) : ChannelInboundHandlerAdapter() {
    override fun userEventTriggered(ctx: ChannelHandlerContext, event: Any) {
        if (event is SslHandshakeCompletionEvent) {
            if (event.isSuccess) {
                val sslHandler = ctx.pipeline().get(SslHandler::class.java)
                val certificate = sslHandler?.engine()?.session?.peerCertificates?.firstOrNull() as? X509Certificate
                if (certificate != null) {
                    certificates.add(ctx.channel(), certificate)
                } else {
                    certificates.remove(ctx.channel())
                }
            } else {
                certificates.remove(ctx.channel())
            }
        }
        super.userEventTriggered(ctx, event)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        certificates.remove(ctx.channel())
        super.channelInactive(ctx)
    }
}
