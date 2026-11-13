---
name: ios-inspired-android-ui
description: Premium rounded surfaces, glass effects, typography, spacing and icon consistency for Vennela 365
---

# iOS-Inspired Android UI Design System

This skill enforces the ultra-premium iOS-inspired glassmorphic visual aesthetic adapted cleanly for native Android Jetpack Compose in Vennela 365.

## Visual Language Principles
1. **Translucent Frosted Glass**: Use multi-layered surfaces with semi-transparent dark backgrounds (e.g., `0x30111F3F` to `0xD90A1633`), subtle light borders with gradients (`0x356EA8FF` / `0x4D5C8CE6`), and outer glow/shadows.
2. **Curvature & Radii**:
   - Primary cards & containers: `24.dp` to `32.dp` rounded corners.
   - Inner buttons & pills: `16.dp` to `20.dp`, or pill shapes (`50%` / `RoundedCornerShape(50)`).
   - Floating navigation bar dock: `36.dp` pill shape with neon accent highlights.
3. **Typography**:
   - Clean, expressive hierarchy. Bold titles (`24.sp` to `28.sp`, font weight Bold / SemiBold).
   - Secondary text in subtle slate/lavender tints (`TextSecondary = Color(0xFF94A3B8)`).
   - Crisp tracking and line spacing.
4. **Vibrant Glowing Badges**:
   - Icon tiles (Documents, Spreadsheets, Media, Code) use rich color gradients with glowing borders matching their category tint.
5. **Android Native Harmony**:
   - Always respect Android system bars, gestures, edge-to-edge insets (`WindowInsets`), back navigation handlers, and native file pickers.
