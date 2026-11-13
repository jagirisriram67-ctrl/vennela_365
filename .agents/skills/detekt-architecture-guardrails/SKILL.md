---
name: detekt-architecture-guardrails
description: Static code analysis and architecture guardrails using Detekt and Kotlin Lint. Enforces Clean Architecture boundaries, bans presentation-to-database imports, blocks ViewModel anti-patterns, and flags memory leaks.
---

# Detekt Architecture Guardrails for Android

This skill enforces strict architectural boundaries, linting standards, and code smell prevention using Detekt.

---

## 1. Gradle Configuration

### Root `build.gradle.kts`:
```kotlin
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
}
```

---

## 2. Architecture Boundary Rules

Enforce the following non-negotiable architectural boundaries:

### A. Layer Import Bans
- **Rule**: `presentation` package must NEVER import packages from `data.local`, `data.remote`, or `androidx.room.*`.
- **Enforcement**:
```yaml
# config/detekt/detekt.yml
style:
  ForbiddenImport:
    active: true
    imports:
      - value: 'androidx.room.*'
        reason: 'Room database types must never be exposed or imported into UI/Presentation components.'
      - value: 'com.vennela365.viewer.data.local.dao.*'
        reason: 'UI components cannot access DAOs directly. All access must be routed through Domain UseCases or Repositories.'
```

### B. ViewModel Anti-Patterns
1. **Never store an Android `Context` or `View` in a ViewModel**:
   - Causes severe memory leaks during activity recreation.
   - Use AndroidViewModel ONLY if strictly necessary for application context, otherwise inject application context into repository/datasource.
2. **Never expose `MutableStateFlow` or `MutableSharedFlow`**:
   - Always expose `.asStateFlow()` or `.asSharedFlow()`.

```kotlin
// BAD - Unsafe state modification from outside
val state = MutableStateFlow(UiState())

// GOOD - Encapsulated immutability
private val _state = MutableStateFlow(UiState())
val state: StateFlow<UiState> = _state.asStateFlow()
```

3. **Never hardcode Coroutine Dispatchers**:
   - Always inject `CoroutineDispatcher` (or a `DispatcherProvider`) to allow test overriding with `StandardTestDispatcher` or `UnconfinedTestDispatcher`.

---

## 3. Detekt Verification Workflow

Run static analysis check:
```bash
./gradlew detekt
```

Generate or update baseline for legacy issues:
```bash
./gradlew detektBaseline
```

