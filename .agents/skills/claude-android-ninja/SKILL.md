---
name: claude-android-ninja
description: Elite modern Kotlin paradigms, Jetpack Navigation 3 type-safe backstack architecture, and high-performance Gradle build scripts for native Android.
---

# Claude Android Ninja Standards

This skill instructs the agent to apply cutting-edge Kotlin paradigms, modern Jetpack Navigation 3 patterns, and Gradle build script optimizations.

---

## 1. Modern Kotlin 2.0+ Idioms

### A. Value Classes for Type-Safe Primitives
Prevent primitive obsession and zero-cost allocations at runtime:

```kotlin
@JvmInline
value class FileId(val value: String) {
    init {
        require(value.isNotBlank()) { "FileId cannot be blank" }
    }
}

@JvmInline
value class FileSize(val bytes: Long) {
    val formatted: String
        get() = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
        }
}
```

### B. Kotlin Contracts
Inform the compiler about smart casts and function behavior:

```kotlin
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun CharSequence?.isNotNullOrEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNotNullOrEmpty != null)
    }
    return this != null && this.isNotEmpty()
}
```

### C. Context Parameters / Receivers
For dependency injection and scoped utilities without bloated argument lists:

```kotlin
interface ScopeSnackbar {
    fun showSnackbar(message: String)
}

context(ScopeSnackbar)
fun performActionWithFeedback(actionName: String) {
    // perform action
    showSnackbar("$actionName executed successfully")
}
```

---

## 2. Jetpack Navigation 3 (Nav3) Type-Safe Architecture

### Route Serialization & Typed Destinations
Define all navigation routes as `@Serializable` Kotlin objects or data classes:

```kotlin
import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data class Viewer(
        val fileUri: String,
        val mimeType: String,
        val title: String
    ) : Screen

    @Serializable
    data class Settings(val initialSection: String = "general") : Screen
}
```

### Type-Safe NavHost Declaration
```kotlin
val navController = rememberNavController()

NavHost(
    navController = navController,
    startDestination = Screen.Home
) {
    composable<Screen.Home> {
        HomeScreen(
            onNavigateToViewer = { uri, mime, title ->
                navController.navigate(Screen.Viewer(uri, mime, title))
            }
        )
    }

    composable<Screen.Viewer> { backStackEntry ->
        val viewerArgs = backStackEntry.toRoute<Screen.Viewer>()
        UniversalViewerScreen(
            uri = viewerArgs.fileUri,
            mimeType = viewerArgs.mimeType,
            title = viewerArgs.title,
            onBack = { navController.popBackStack() }
        )
    }
}
```

### Predictive Back & Custom Animated Transitions
```kotlin
composable<Screen.Viewer>(
    enterTransition = {
        slideInHorizontally(initialOffsetX = { it }) + fadeIn()
    },
    exitTransition = {
        slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    },
    popEnterTransition = {
        slideInHorizontally(initialOffsetX = { -it }) + fadeIn()
    },
    popExitTransition = {
        slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    }
) { ... }
```

---

## 3. Optimized Gradle Build Scripts & Convention Plugins

### A. Version Catalog (`gradle/libs.versions.toml`) Standard
Ensure dependencies are declared in `libs.versions.toml`:

```toml
[versions]
agp = "8.7.2"
kotlin = "2.0.21"
composeBom = "2024.10.01"
navigation = "2.8.3"
hilt = "2.52"
detekt = "1.23.6"
paparazzi = "1.3.4"

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### B. Configuration Cache & Parallel Build Optimizations (`gradle.properties`)
```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g
kotlin.incremental=true
kotlin.incremental.useClasspathSnapshot=true
```

### C. Build-Logic Convention Plugins
For multi-module architectures, eliminate duplicate gradle scripts by creating convention plugins inside `build-logic/` (e.g. `android-feature-convention`, `android-library-convention`, `android-compose-convention`).

