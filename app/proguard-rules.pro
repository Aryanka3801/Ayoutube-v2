# NewPipe Extractor — keep all classes
-keep class org.schabi.newpipe.extractor.** { *; }
-dontwarn org.schabi.newpipe.extractor.**

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Gson
-keepattributes Signature,*Annotation*
-keep class com.google.gson.** { *; }

# RxJava3
-dontwarn io.reactivex.rxjava3.**
-keep class io.reactivex.rxjava3.** { *; }

# VideoItem must survive serialization across activities
-keepnames class com.example.myapp.model.VideoItem
-keep class com.example.myapp.model.VideoItem { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# Leanback
-keep class androidx.leanback.** { *; }
-dontwarn androidx.leanback.**
