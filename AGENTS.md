# Vennela 365 - Agent Instructions & Project Rules

## Project Identity & Visual Soul
Vennela 365 is an ultra-premium universal file viewer for Android inspired by Apple Human Interface Guidelines and Material 3 Expressive.
- **Theme**: Lunar Night / Moonlit Lake ("Vennela" meaning moonlight).
- **Surfaces**: Frosted translucent glassmorphic cards, glowing neon accents, elegant typography, rounded pills, and floating docks.
- **Source of Truth**: The 26 design references in `Screens/`. Every screen must match the reference pixel-for-pixel while functioning with real data and interactive native Android behavior.

## Core Rules
1. **Pixel-Perfect Screen Fidelity**:
   - Follow the reference screens in `Screens/` strictly for layouts, color gradients, pill buttons, badges, top bars, and docks.
2. **Edge-to-Edge Design**:
   - Full immersion with translucent status bars, gesture navigation insets, and proper padding.
3. **Real Functionality**:
   - Screens are never dummy static pictures. Connect files, search, categorization, media playback, syntax highlighting, hex viewing, zip browsing, spreadsheets, diagrams, themes, and settings to working state and ViewModels.
4. **Resilient Architecture**:
   - Offline-first privacy.
   - Robust error handling and fallbacks for unhandled formats.
