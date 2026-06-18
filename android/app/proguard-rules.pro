# Protobuf
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# Retrofit / OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.ktmb.crowdtrend.data.remote.** { *; }
-keep class com.ktmb.crowdtrend.domain.model.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod
