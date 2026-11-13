---
name: compose-stability-performance
description: Compose Compiler Metrics diagnosis, stability auditing, and 120 FPS optimization. Identifies unstable parameters, optimizes un-skippable composables, configures strong skipping mode, and eliminates unnecessary recompositions.
---

# Jetpack Compose Stability & 120 FPS Performance Guide

This skill guides the auditing, measurement, and optimization of Jetpack Compose rendering performance to guarantee smooth 120 FPS frame rates without jank.

---

## 1. Enabling Compose Compiler Metrics in Gradle

Add the compiler metrics flags in `app/build.gradle.kts`:

```kotlin
android {
    // ...
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
    enableStrongSkippingMode = true
}
```

Run report generation:
```bash
./gradlew assembleRelease -PcomposeCompilerReports=true
```

Reports are generated in `app/build/compose_compiler/`:
- `<module>-composables.txt`: Details every composable (restartable, skippable, or unskippable).
- `<module>-classes.txt`: Details class stability (stable, unstable, immutable).

---

## 2. Understanding Stability & The Skippable Contract

A composable is **skippable** if:
1. It is restartable.
2. ALL of its arguments are **stable**.

If a single parameter is marked `unstable`, Compose must re-invoke the composable whenever its parent recomposes—even if the arguments haven't changed!

### Common Unstable Culprits:
1. **Standard Standard Library Collections** (`List<T>`, `Set<T>`, `Map<K, V>`):
   - The Compose compiler treats standard `List` as unstable because it cannot guarantee the underlying implementation isn't mutable (e.g. `ArrayList`).
   - **Fix**: Use `kotlinx.collections.immutable.ImmutableList<T>` or wrap in a `@Immutable` data class.

```kotlin
// Unstable - Causes recomposition on parent changes!
@Composable
fun FileList(files: List<FileUiItem>) { ... }

// Optimized - Fully skippable!
@Composable
fun FileList(files: ImmutableList<FileUiItem>) { ... }
```

2. **External Classes or DTOs** from third-party libraries:
   - Mark domain/UI models with `@Immutable` or `@Stable`:

```kotlin
import androidx.compose.runtime.Immutable

@Immutable
data class FileUiItem(
    val id: String,
    val name: String,
    val size: String
)
```

---

## 3. Lambda Memoization & Unstable Closures

Passing unstabilized lambdas breaks composable skipping.

```kotlin
// Bad: Creates a new lambda instance on every recomposition
ItemRow(onClick = { viewModel.selectItem(item) })

// Good: Method reference or remember with key
val onSelect = remember(item.id) {
    { viewModel.selectItem(item.id) }
}
ItemRow(onClick = onSelect)
```

---

## 4. DerivedStateOf Optimization for High-Frequency State

When observing rapid state updates (e.g., `LazyListState.firstVisibleItemIndex`, scroll offsets):

```kotlin
// Bad: Triggers recomposition on every single pixel scrolled!
val isScrolled = listState.firstVisibleItemIndex > 0

// Good: Recomposes ONLY when the boolean condition transitions from false to true or true to false
val isScrolled by remember {
    derivedStateOf { listState.firstVisibleItemIndex > 0 }
}
```

---

## 5. Deferring State Reads with Lambda Modifiers

Read high-frequency state during the **draw** or **layout** phase, completely skipping the **composition** phase:

```kotlin
// Bad: Recomposes on every scroll tick
Box(modifier = Modifier.offset(y = scrollOffset.dp))

// Good: Executed in Layout phase - Zero recomposition overhead!
Box(modifier = Modifier.offset { IntOffset(0, scrollOffset.roundToInt()) })

// Good: Executed in Draw phase
Box(modifier = Modifier.graphicsLayer { alpha = currentAlpha })
```

