# Add project specific ProGuard rules here.
#
# blankTV intentionally ships unminified for now. These rules are the starting point for the day
# R8 is switched on.

# Media3 / ExoPlayer reflectively instantiates several components.
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Room generates implementations of the @Database / @Dao interfaces at compile time; KSP output is
# referenced reflectively by the runtime.
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**

# OkHttp / Okio ship their own consumer rules, but keep the platform TLS hooks visible.
-dontwarn okhttp3.**
-dontwarn okio.**
