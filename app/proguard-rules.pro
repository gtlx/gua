# GuaBrowser ProGuard 规则

# ===== GeckoView =====
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**

# ===== Kotlin Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===== Compose =====
-dontwarn androidx.compose.**

# ===== DataStore =====
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-keepclassmembers class * implements androidx.datastore.preferences.protobuf.ProtocolStringList {
    <fields>;
}
-dontwarn androidx.datastore.preferences.protobuf.**

# ===== Kotlin Serialization / JSON =====
-keepclassmembers class org.json.** { *; }
-dontwarn org.json.**

# ===== GuaBrowser 模型类（避免混淆导致序列化问题）=====
-keep class com.gua.browser.ui.SearchEngine { *; }
-keep class com.gua.browser.bookmark.Bookmark { *; }
-keep class com.gua.browser.bookmark.HistoryItem { *; }
-keep class com.gua.browser.userscript.UserScript { *; }
