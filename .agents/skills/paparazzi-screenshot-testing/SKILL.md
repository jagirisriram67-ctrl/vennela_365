---
name: paparazzi-screenshot-testing
description: Fast, headless JVM snapshot testing for Jetpack Compose using Cash App Paparazzi. Verifies pixel-perfect UI rendering, theme variants, and prevents visual regressions without booting an emulator.
---

# Paparazzi Snapshot Testing for Jetpack Compose

This skill guides the implementation of headless, ultra-fast visual regression and screenshot testing using Cash App Paparazzi.

---

## 1. Gradle Configuration

### Root `build.gradle.kts`:
```kotlin
plugins {
    id("app.cash.paparazzi") version "1.3.4" apply false
}
```

### Module `app/build.gradle.kts`:
```kotlin
plugins {
    id("app.cash.paparazzi")
}
```

---

## 2. Writing Paparazzi Snapshot Tests

Paparazzi runs on the standard JVM without requiring an Android emulator or device:

```kotlin
package com.vennela365.viewer.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.vennela365.viewer.ui.components.GlassCard
import com.vennela365.viewer.ui.theme.VennelaTheme
import org.junit.Rule
import org.junit.Test

class FileCardScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO,
        theme = "android:Theme.Material.NoActionBar"
    )

    @Test
    fun snapFileCard_lunarNightTheme() {
        paparazzi.snapshot {
            VennelaTheme(darkTheme = true) {
                FileCard(
                    item = FileUiItem(
                        name = "quarterly_financials_2026.xlsx",
                        sizeFormatted = "3.4 MB",
                        extension = "xlsx"
                    ),
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun snapFileCard_accessibilityLargeFont() {
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(fontScale = 1.5f)
        )
        paparazzi.snapshot {
            VennelaTheme {
                FileCard(
                    item = FileUiItem(
                        name = "accessibility_spec.pdf",
                        sizeFormatted = "512 KB",
                        extension = "pdf"
                    ),
                    onClick = {}
                )
            }
        }
    }
}
```

---

## 3. Snapshot Commands & Verification

### Record New Golden Images:
Generates baseline images in `src/test/snapshots/images/`:
```bash
./gradlew recordPaparazziDebug
```

### Verify Changes Against Golden Images:
Compares rendered output against existing snapshots; fails if any visual pixel diff occurs:
```bash
./gradlew verifyPaparazziDebug
```

If a test fails, Paparazzi outputs visual diff delta images in `build/reports/paparazzi/` highlighting exact pixel shifts in red.

