# Proguard rules for Koishi
-keepattributes *Annotation*
-dontwarn javax.annotation.**

# ML Kit Barcode Scanning & Firebase Components
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
    public *;
}
-keep class com.google.android.gms.common.annotation.KeepName { *; }
-keep @com.google.android.gms.common.annotation.KeepName class * { *; }
-keepclassmembers class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# CameraX
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }
-dontwarn androidx.camera.**

# Kotlinx Serialization：保证通用表内的 JSON 载荷在混淆后仍可正确往返
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.nanami.koishi.**$$serializer { *; }
-keepclassmembers class com.nanami.koishi.** {
    *** Companion;
}
-keepclasseswithmembers class com.nanami.koishi.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Ktor server (CIO) ships JVM-only debug helpers that reference java.lang.management
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn java.util.logging.**
-dontwarn io.ktor.util.debug.**
-dontwarn kotlinx.coroutines.debug.**

# BouncyCastle references desktop-only JDK APIs
-dontwarn javax.naming.**
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.security.auth.x500.**


# Netty server optional transports and TLS providers
-dontwarn io.netty.channel.epoll.**
-dontwarn io.netty.channel.kqueue.**
-dontwarn io.netty.internal.tcnative.**
-dontwarn jdk.jfr.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**


# Netty creates NIO channels through ReflectiveChannelFactory
-keep class io.netty.channel.socket.nio.NioServerSocketChannel { public <init>(); }
-keep class io.netty.channel.socket.nio.NioSocketChannel { public <init>(); }
-keep class io.netty.channel.socket.nio.NioDatagramChannel { public <init>(); }


# Netty relies on runtime type information, reflection and internal class hierarchies
-keep class io.netty.** { *; }
-keep interface io.netty.** { *; }
-keep class io.ktor.server.netty.** { *; }
-keep class io.ktor.server.engine.** { *; }


# Optional Netty codecs and integrations absent from the Android build
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.google.protobuf.**
-dontwarn com.jcraft.jzlib.**
-dontwarn com.ning.compress.**
-dontwarn com.oracle.svm.**
-dontwarn io.netty.pkitesting.**
-dontwarn lzma.**
-dontwarn net.jpountz.**
-dontwarn org.jboss.marshalling.**
-dontwarn org.osgi.annotation.bundle.**
-dontwarn reactor.blockhound.**
