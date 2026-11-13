---
name: reference-screen-matching
description: Compare emulator screenshots against supplied reference images and fix visual differences
---

# Reference Screen Matching Workflow

This skill guides visual regression verification comparing running Android UI against the design source of truth in `Screens/`.

## Loop Execution
1. **Inspect Target Reference**:
   - Open corresponding reference image from `Screens/` (e.g. `2.home.png`, `8.document_viewer(docx).png`).
   - Note header layout, button styling, colors, padding, list layouts, and bottom dock state.
2. **Launch & Screenshot**:
   - Run the app or target preview/screen.
   - Capture screenshot using ADB or Compose test capture at 1080x2400 (or device scale).
3. **Compare & Catalog Differences**:
   - Check margins, padding, alignment, gradient stops, badge sizes, icon choices, and typography.
4. **Refine**:
   - Update Compose modifiers, colors, layouts, and component configurations to match pixel-for-pixel.
