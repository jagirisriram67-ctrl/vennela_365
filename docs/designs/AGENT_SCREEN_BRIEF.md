# Screen upgrade brief (for delegated agents)

You are upgrading individual screens of **Vennela 365**, an Android Jetpack
Compose universal file viewer at `C:\Users\srira\Desktop\Vennela`.

## Read first

1. `docs/designs/LIQUID_GLASS_SYSTEM.md` — the authoritative design contract.
2. `app/src/main/java/com/vennela365/viewer/ui/components/LiquidComponents.kt`
   — the component library you must use.
3. `app/src/main/java/com/vennela365/viewer/theme/VennelaTokens.kt`,
   `LiquidMotion.kt`, `LiquidGlass.kt`, `ThemePreferences.kt`.
4. `app/src/main/java/com/vennela365/viewer/ui/components/LiquidDock.kt`
   — how a finished, polished screen component looks.

## Hard rules

- **Do not modify** anything under `theme/`, `ui/components/LiquidComponents.kt`,
  `ui/components/LiquidDock.kt`, `parser/`, `registry/`, `viewmodel/MainViewModel.kt`
  or another agent's screen file. Only edit the files assigned to you.
- **Do not change** `MainViewModel`'s public API. It is large and shared. Read the
  properties/functions you need; if something is genuinely missing, note it in
  your final message instead of adding it.
- Keep every screen's **existing constructor signature**
  (`@Composable fun XScreen(viewModel: MainViewModel)`) so `MainActivity` keeps
  compiling.
- **Preserve all existing functionality**: every button, callback and state that
  works today must still work.
- Never hardcode colours. Use `LocalVennelaPalette.current`,
  `LocalVennelaAccent.current` and the tokens.
- Use `LiquidPage` for the frame so insets and dock clearance are correct.
- No dead space, no clipped rows (use `LazyRow` + `Row` with
  `Arrangement.spacedBy`), no mock data.

## What "done" means

1. `.\gradlew.bat :app:compileDebugKotlin --console=plain` succeeds.
2. The screen matches its reference(s) in `Screens/` in hierarchy and mood.
3. Loading / empty / error states are all handled.
4. Your final message lists: files changed, what you improved, and anything you
   could not do.

## Reference screens by name

`Screens/1.splash_screen.png`, `2.home.png`, `3.file_explorer.png`,
`4.universal_search.png`, `5.recentfiles.png`, `6.favourites.png`,
`7.file_type_categories.png`, `8.document_viewer(docx).png`, `9.pdf_viewer.png`,
`10.image_viewer.png`, `11.code_viewer.png`, `12.markdown_viewer.png`,
`13.mermaid_viewer.png`, `14.audio_player.png`, `15.video_player.png`,
`16.archieve_browser.png`, `17.file_meta_data.png`, `18.unknown_fileview.png`,
`19.unsupported_file.png`, `20.settings.png`, `21.permissions.png`,
`22.error_screen.png`, `23.theme_selection.png`, `24.share_screen.png`,
`25.xl.png`.

## Visual target

Dark moonlit-navy atmosphere, frosted glass cards with a bright top rim, neon
accent glows, generous rounded pills, iOS-grade spacing, and liquid spring
motion on everything the finger touches.
