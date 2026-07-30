-keep class com.nilam.iptv.** { *; }
-keepclassmembers class * extends android.webkit.WebChromeClient { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep public class * extends android.app.Activity
-dontwarn org.jetbrains.annotations.**
