# Vennela 365 — UI Upgrade Brief (Aurora Lumen)

You are upgrading the **visuals** of an Android Jetpack Compose app: "Vennela 365", an
ultra-premium universal file viewer. Theme identity: lunar night / moonlit lake,
glassmorphic, Material 3 Expressive, Apple HIG inspired.

**This upgrade is called "Aurora Lumen": luminous, bright, neon-accented — never murky.**

---

## 1. HARD RULES (violating these breaks the build)

1. **Never** change a function's name or its parameter list/signature.
2. **Never** change, remove, or reorder any ViewModel call (`viewModel.xxx(...)`), any
   `Screen.X` navigation, any `collectAsState()` state read, any `remember`, or any
   business logic. **Visuals and motion only.**
3. **Never** delete a composable, parameter, or feature. Only restyle and add motion.
4. The theme layer (`com/vennela365/viewer/theme/*.kt`) and the shared components
   (`ui/components/LiquidComponents.kt`, `ViewerChrome.kt`, `LiquidDock.kt`,
   `ThumbArcDock.kt`, `GlassComponents.kt`) are **already upgraded centrally**.
   **Do not edit them** — consume them.
5. Do not rename or move files.
6. Add Kotlin imports at the top when you use a new API.
7. **Do not run Gradle.** The coordinator compiles centrally; concurrent builds conflict.

---

## 2. Color system (already brightened)

```kotlin
val palette = LocalVennelaPalette.current
val accent  = LocalVennelaAccent.current
```

- `palette`: `.isDark`, `.backgroundTop`, `.backgroundMid`, `.backgroundBottom`,
  `.textPrimary`, `.textSecondary`, `.textTertiary`, `.cardSurface`,
  `.cardSurfaceStrong`, `.cardBorder`, `.dockSurfaceTop`, `.dockSurfaceBottom`,
  `.dockText`, `.dockTextMuted`
- `accent`: `.primary`, `.secondary`

**Hero fills** use `Brush.linearGradient(listOf(accent.primary, accent.secondary))`.

Neon tokens (`com.vennela365.viewer.theme`):
`NeonBlue, NeonCyan, NeonViolet, NeonMagenta, NeonAmber, NeonGreen, NeonCoral`
Category colors: `CatBlue, CatEmerald, CatViolet, CatAmber, CatRose, CatCyan, CatOrange, CatSlate`

---

## 3. Layout tokens — `VennelaTokens`

| Group | Values |
|---|---|
| Radii | `radiusChip` 12, `radiusBadge` 14, `radiusControl` 16, `radiusTile` 18, `radiusCard` 20, `radiusCardLarge` 24, `radiusSheet` 26, `radiusDock` 30, `radiusPill` 40 |
| Spacing | `spaceXxs, spaceXs, spaceSm, spaceMd, spaceLg, spaceXl, spaceXxl, spaceHuge, gutter` |
| Badges | `badgeXs` 26, `badgeSm` 32, `badgeMd` 38, `badgeLg` 44 |
| Icons | `iconSm, iconMd, iconLg, iconXl` |
| Elevation | `elevationFlat, elevationRaised, elevationCard, elevationBar, elevationDock, elevationOverlay` |
| Frost | `frostLight` 12, `frostRegular` 18, `frostHeavy` 28 |
| Type | `titleHero, titleDisplay, titleLarge, titleMedium, titleSmall, body, bodySmall, caption, micro, nano` |

---

## 4. Glass surfaces

```kotlin
val spec = liquidGlassSpec(
    alpha = 0.42f,          // higher = more solid/brighter body
    frost = VennelaTokens.frostRegular,
    elevation = VennelaTokens.elevationCard,
    sheen = 0.22f,
    highlight = 0.16f,
    strong = false          // true = denser body
)
Modifier.liquidGlass(spec, RoundedCornerShape(VennelaTokens.radiusCard))
```

Ready-made components (signatures are stable):

- `GlassCard(modifier, shape, frost, elevation, alpha, onClick) { /* ColumnScope */ }`
- `GlassPanel(modifier, shape, frost, elevation, alpha) { /* BoxScope */ }`
- `GlassListRow(title, subtitle, leading, trailing, showChevron, onClick)`
- `GlassDivider()`
- `IconBadge(icon, colors: List<Color>, size, radius, contentDescription, onClick)`
- `LiquidChip(text, icon, isActive, onClick)`
- `LiquidButton(label, onClick, icon, enabled, fillWidth)`
- `SectionHeader(title, actionLabel, onAction, trailing)`
- `LiquidTopBar(title, subtitle, onBack, actions)`
- `LiquidIconButton(icon, contentDescription, onClick, tint, active)`
- `AnimatedDisclosure(expanded, modifier)`
- `FileBrandBadge(extension, size)`
- `VennelaEmptyState(...)`, `VennelaErrorState(...)`, `VennelaLoadingState(...)`
- `FloatingDockNav(activeTab, onTabSelected)`
- `LiquidPage(title, subtitle, onBack, dock, topBarActions) { pagePadding -> }`

Viewer chrome:

- `ViewerTopBar(title, subtitle, onBack, progress, actions)`
- `ViewerIconAction(icon, contentDescription, active, enabled, onClick)`
- `ViewerDock { ViewerDockAction(icon, contentDescription, active, enabled, onClick) }`
- `ViewerSheet(visible, icon, title, trailing) { }`
- `ViewerHint(text)`, `ViewerDot(color, size)`

---

## 5. Motion — `LiquidMotion`

Springs: `.press()` (touch) · `.liquid()` (surface settle) · `.shared()` (screen/dock) ·
`.hero()` (bigger, bouncy) · `.bounce()` (celebratory) · `.playful()`

Tweens: `.glide(duration = durationBase, delay = 0)` · `.exit(duration = durationFast)` ·
`.emphasis(duration = durationSlow)` · `.shimmer(duration = durationHero)` ·
`.cascade(index, stepMillis = 46, duration = durationBase)`

> Parameter names are `duration` / `delay` / `stepMillis` — **not** `durationMs`.
Durations: `durationInstant` 90 · `durationFast` 180 · `durationBase` 280 ·
`durationSlow` 420 · `durationHero` 620

Easings: `glideEasing`, `exitEasing`, `emphasisEasing`

Modifiers:

- `Modifier.pressScale(pressedScale = 0.96f, onClick = { ... })`
- `Modifier.staggeredEntrance(index)` — fade + rise + scale-in cascade for list items

> `animateColorAsState` lives in `androidx.compose.animation.animateColorAsState`
> (**not** `animation.core`).

Entrance pattern:

```kotlin
val a = remember { Animatable(0f) }
LaunchedEffect(Unit) { a.animateTo(1f, LiquidMotion.glide(LiquidMotion.durationSlow)) }
// then
.graphicsLayer { translationY = (1f - a.value) * 20f; alpha = a.value }
```

---

## 6. The upgrade recipe (apply to every screen/section you own)

1. **Brighten** — raise alpha on frosted fills; replace flat/dim colors with accent
   gradients; add soft neon glow on hero cards, badges and primary buttons:
   ```kotlin
   .shadow(
       elevation = 16.dp, shape = shape, clip = false,
       ambientColor = accent.secondary.copy(alpha = 0.50f),
       spotColor = accent.primary.copy(alpha = 0.50f)
   )
   ```
2. **Deepen hierarchy** — hero headers become gradient-tinted glass panels; primary
   actions get accent-gradient fills with a white hairline border
   (`Color.White.copy(alpha = 0.30f)`).
3. **Animate** — list items get `Modifier.staggeredEntrance(index)`; taps get
   `pressScale`; chips/selected states animate via `animateColorAsState` +
   `animateFloatAsState(...)`; progress/stat bars animate from 0 to target; headers
   scale-in subtly.
4. **Polish** — larger corner radii per `VennelaTokens`; brighter borders
   (`accent.secondary.copy(alpha = 0.40f)`); gradient icon badges; bold title weights.
5. **Preserve** — every `key = "..."` in lazy lists, every click handler, every icon,
   and every piece of real data still renders.

Keep it tasteful and consistent. Text must stay readable: white on accent fills;
`palette.textPrimary` / `palette.textSecondary` on glass.

---

## 7. Before you finish

- Re-read every file you changed.
- Confirm: all original functions still exist with identical signatures; all ViewModel
  calls and `Screen.X` navigations still present; braces balance; new APIs imported.
- Report back concisely: which screens/sections you restyled, and any API you were
  unsure about.
