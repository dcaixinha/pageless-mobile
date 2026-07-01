# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class live.pageless.mobile.data.remote.** {
    kotlinx.serialization.KSerializer serializer(...);
}
