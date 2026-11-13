package com.vennela365.viewer.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates the AOT baseline profile shipped in `app/src/main/baseline-prof.txt`.
 *
 * Walking the launch path and the five dock destinations covers the hot code on
 * cold start, which is where low-end devices feel the difference most.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "com.vennela365.viewer",
        includeInStartupProfile = true,
        maxIterations = 12
    ) {
        pressHome()
        startActivityAndWait()

        val device = device
        device.wait(Until.hasObject(By.pkg(packageName)), 5_000)

        // Splash → Home.
        device.waitForIdle()
        Thread.sleep(1_200)

        // Walk the dock destinations, then back to Home.
        val labels = listOf("Files", "Search", "Favorites", "Settings", "Home")
        labels.forEach { label ->
            device.findObject(By.text(label))?.click()
            device.waitForIdle()
            Thread.sleep(700)
        }

        // Exercise a scroll on Home so list composition is profiled too.
        device.swipe(
            device.displayWidth / 2,
            (device.displayHeight * 0.75f).toInt(),
            device.displayWidth / 2,
            (device.displayHeight * 0.25f).toInt(),
            12
        )
        device.waitForIdle()
    }
}
