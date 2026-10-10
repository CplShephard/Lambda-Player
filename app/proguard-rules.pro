# App package
-keep class dev.shephard.player.** { *; }

# Kotlin metadata / reflection
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Jetpack Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# Navigation Compose - keep sealed destination classes & companions
-keep class dev.shephard.player.ui.navigation.** { *; }
-keepclassmembers class dev.shephard.player.ui.navigation.** {
    public static ** INSTANCE;
}

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# DataStore
-keep class androidx.datastore.*.** { *; }
-dontwarn androidx.datastore.**

# AndroidX core
-dontwarn androidx.**

# JAudioTagger (MP3/M4A tag editing)
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-keep class java.awt.** { *; }
-dontwarn java.awt.**

# Compose 1.12.0 keyboard flicker workaround (see LambdaPlayerApplication): keep the
# Choreographer text-input path so R8 can drop the out-of-frame scheduler.
-assumevalues class androidx.compose.ui.AndroidComposeUiFlags {
    public static boolean isOutOfFrameSchedulerForTextInputEventsEnabled return false;
}
