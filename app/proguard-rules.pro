# Proguard rules for RouteWise SA
-keep class com.routewise.sa.data.model.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
# Gson reads and writes these classes by field name, so R8 must not rename them in release builds
-keep class com.routewise.sa.data.remote.dto.** { *; }
-keepattributes Signature, *Annotation*
