# Add project specific ProGuard rules here.

# Keep all chess domain entities for serialization
-keep class com.chesspredictor.domain.entities.** { *; }

# Keep WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Compose related classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin multiplatform shared module
-keep class com.chesspredictor.shared.** { *; }

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Remove debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep kotlinx coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep material design components
-keep class com.google.android.material.** { *; }

# WebView and JavaScript optimizations
-keepattributes JavascriptInterface
-keepattributes *Annotation*
-dontwarn android.webkit.WebView
-dontwarn android.webkit.WebViewClient