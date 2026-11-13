---
name: dexcount-apk-guard
description: DEX limit monitoring, method signature budgeting, and APK/AAB size optimization. Tracks method counts per package, prevents DEX bloat, tunes R8 Proguard rules, and guards release binary footprint.
---

# Dexcount & APK Size Optimization Guard

This skill guides method signature tracking, DEX budgeting, and binary size optimization for large-scale enterprise Android applications.

---

## 1. Gradle Configuration

### Root `build.gradle.kts`:
```kotlin
plugins {
    id("com.getkeepsafe.dexcount") version "4.0.0" apply false
}
```

### Module `app/build.gradle.kts`:
```kotlin
plugins {
    id("com.getkeepsafe.dexcount")
}

dexcount {
    format = "list"
    includeFieldCount = true
    includeTotalMethodCount = true
    orderByMethodCount = true
    verbose = false
    maxMethodCount = 64000 // Guard against un-indexed single-dex bloat
    teamCitySlug = null
    runOnEachAssemble = true
}
```

---

## 2. Analyzing Method Count Output

When building the APK:
```bash
./gradlew assembleRelease
```

Dexcount outputs detailed breakdown charts in `app/build/outputs/dexcount/release.txt`:
```
Total methods in release.apk: 42,180 (64.3% of 64k limit)
Total fields in release.apk:  21,405

Top Consumers:
1. androidx.compose.ui:       8,420 methods
2. io.ktor.client:            4,110 methods
3. androidx.room:             3,850 methods
4. com.vennela365.viewer:     2,940 methods
```

---

## 3. R8 Proguard Optimization & Shrinking Strategy

Configure aggressive R8 optimization in `app/build.gradle.kts`:

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### Best Practices for `proguard-rules.pro`:
1. **Never use blanket keep rules**:
   - BAD: `-keep class com.vennela365.** { *; }`
   - GOOD: Keep only `@Serializable` or `@Keep` annotated models:
   ```proguard
   -keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
   -keepclassmembers class * {
       @androidx.annotation.Keep <fields>;
       @androidx.annotation.Keep <methods>;
   }
   ```
2. **Strip logging & assertions in release builds**:
   ```proguard
   -assumenosideeffects class android.util.Log {
       public static boolean isLoggable(java.lang.String, int);
       public static int v(...);
       public static int d(...);
       public static int i(...);
   }
   ```

