---
name: baseline-profiles-optimizer
description: Macrobenchmark and Baseline Profile Gradle Plugin integration. Generates startup profiles and ahead-of-time (AOT) compilation rules, reducing cold app startup latency by up to 30% and eliminating frame drops on critical user journeys.
---

# Android Baseline Profiles & Startup Optimization

This skill guides the setup, generation, and benchmarking of Android Baseline Profiles to optimize runtime execution and prevent initial cold-start lag.

---

## 1. Why Baseline Profiles?

Without a baseline profile, Android compiles code just-in-time (JIT) or when the device is idle. Baseline Profiles provide ahead-of-time (AOT) compilation rules directly inside the APK/AAB, accelerating:
- App cold launch time by up to 30-40%.
- First-time screen navigation and transition smoothness.
- First-scroll frame drops and jank.

---

## 2. Gradle Setup

### Root `build.gradle.kts`:
```kotlin
plugins {
    id("androidx.baselineprofile") version "1.3.3" apply false
}
```

### Module `app/build.gradle.kts`:
```kotlin
plugins {
    id("androidx.baselineprofile")
}

dependencies {
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    baselineProfile(project(":benchmark"))
}
```

---

## 3. Writing Baseline Profile Generator Tests

Inside the `:benchmark` test module:

```kotlin
package com.vennela365.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun generateBaselineProfile() = baselineRule.collect(
        packageName = "com.vennela365.viewer",
        maxIterations = 5
    ) {
        // Critical Journey 1: Cold App Startup
        pressHome()
        startActivityAndWait()

        // Critical Journey 2: Home Screen List Scroll
        device.wait(Until.hasObject(By.res("file_list")), 5_000)
        val fileList = device.findObject(By.res("file_list"))
        fileList?.fling(Direction.DOWN)
        device.waitForIdle()

        // Critical Journey 3: Opening File Viewer
        val firstFile = device.findObject(By.res("file_item_0"))
        firstFile?.click()
        device.waitForIdle()
    }
}
```

---

## 4. Profile Generation Command

Run on a connected physical device or rooted emulator:
```bash
./gradlew :app:generateBaselineProfile
```

This compiles and embeds `baseline-prof.txt` directly into `app/src/main/baselineProfiles/` which is automatically included in release builds.

