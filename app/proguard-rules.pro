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
