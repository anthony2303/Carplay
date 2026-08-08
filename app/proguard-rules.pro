# Add project specific ProGuard rules here.
-keep class com.carplay.clone.** { *; }
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Keep WebRTC
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**
