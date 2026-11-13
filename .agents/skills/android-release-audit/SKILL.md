---
name: android-release-audit
description: Navigation tests, crash checks, accessibility, performance and actual APK/AAB size
---

# Android Release Audit

Standard audit checklist for production readiness in Vennela 365.

## Audit Criteria
1. **Compilation & ProGuard/R8**:
   - Clean compilation with `-Werror` or zero critical warnings.
   - R8 shrinking and resource optimization enabled for release builds.
2. **Edge-to-Edge & System Bars**:
   - Verify status bars, gesture bars, cutouts, and IME insets on Android 14/15/16.
3. **Navigation & Crash Freedom**:
   - Verify all 25+ screens can be navigated to and from without memory leaks or uncaught exceptions.
   - Handle Android system back gestures smoothly at every level.
4. **App Size Budget**:
   - Measured initial APK size under target download budget (e.g., <80MB target).
