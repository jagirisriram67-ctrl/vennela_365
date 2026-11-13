---
name: android-file-viewers
description: File detection, rendering architecture, large-file handling and safe fallbacks
---

# Android File Viewers Architecture

Guidelines for parsing, decoding, and rendering 365+ file formats securely and efficiently on Android.

## Core Rules
1. **Universal Format Recognition**:
   - Detect format using magic numbers/MIME types first, extension second (`FormatRegistry.kt`).
   - Categorize into Document, Spreadsheet, Code, Image, Audio, Video, Archive, Ebook, Diagram, or Unknown.
2. **Safe Memory & Streaming**:
   - Stream large files (PDF, archives, media, spreadsheets) instead of loading full byte arrays into memory.
   - Employ paginated or windowed virtualized lists (`LazyColumn`, `LazyRow`) for tabular data, code lines, and hex dumps.
3. **Graceful Fallbacks**:
   - If a binary file cannot be parsed natively, route seamlessly to `UnknownFileViewerScreen` (Hex viewer + Raw text inspector) or `UnsupportedFileScreen` (with metadata inspection and "Open with Other App" intent).
   - Display informative, helpful recovery actions in `ErrorScreen` if a file is corrupt or password-protected.
