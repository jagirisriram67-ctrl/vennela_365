# Vennela 365 Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-dontwarn java.lang.management.**
