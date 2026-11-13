# Vennela 365 — Liquid Glass Design System (authoritative)

This is the contract every screen must follow. If a screen disagrees with this
document, the screen is wrong.

## 1. The look

Origin-OS-style liquid glass on a moonlit lake. Concretely, every surface is:

1. a **real frosted backdrop** — the pixels behind the surface, down-scaled and
   blurred, not a flat translucent rectangle;
2. a **tinted body** washed over that frost;
3. a **bright specular rim**, strongest along the top edge;
4. an **interior sheen** falling away from the top;
5. a **drop shadow** tinted with the accent glow.

## 2. Foundations (never hardcode)

| Concern | Source |
| --- | --- |
| Colours | `LocalVennelaPalette.current`, `LocalVennelaAccent.current` |
| Radii / spacing / type / elevation | `VennelaTokens` |
| Motion | `LiquidMotion` |
| Glass surface | `Modifier.liquidGlass(spec, shape)` + `liquidGlassSpec(...)` |

**Hardcoding a hex colour in a screen is a defect.** Use palette tokens.

### Radii
`radiusChip 14 · radiusBadge 16 · radiusControl 20 · radiusTile 22 ·
radiusCard 26 · radiusCardLarge 32 · radiusSheet 34 · radiusDock 36 ·
radiusPill 50`

### Spacing
`spaceXxs 2 · spaceXs 4 · spaceSm 8 · spaceMd 12 · spaceLg 16 · spaceXl 20 ·
spaceXxl 28 · spaceHuge 40 · gutter 16`

### Motion
- Finger-driven → `LiquidMotion.press()` / `.liquid()` (springs, slight overshoot)
- Content entering → `LiquidMotion.glide()` / `.emphasis()` (tweens, no overshoot)
- Durations: `instant 90 · fast 180 · base 280 · slow 420 · hero 620`

## 3. Components (use these, do not re-invent)

All in `com.vennela365.viewer.ui.components`:

| Component | Use for |
| --- | --- |
| `LiquidPage` | Every screen frame: atmosphere, insets, top bar, dock clearance |
| `LiquidTopBar` | Frosted title bar with optional back button and actions |
| `GlassCard` / `GlassPanel` | Frosted card (padded / unpadded) |
| `SectionHeader` | Section title with optional "See all" action |
| `IconBadge` | Gradient rounded-square icon tile |
| `GlassListRow` | File / folder / setting row with leading + trailing slots |
| `GlassDivider` | Hairline divider that fades at both ends |
| `LiquidChip` | Filter / quick-action pill |
| `LiquidButton` | Primary filled action button |
| `FloatingDockNav` | Bottom dock (already wired) |
| `VennelaBackground` | Atmosphere + backdrop host (used by `LiquidPage`) |
| `VennelaEmptyState`, `VennelaLoadingState`, `VennelaErrorState` | State surfaces |

Motion helpers: `Modifier.pressScale(onClick)`, `Modifier.staggeredEntrance(index)`.

## 4. Screen rules

1. **One frame.** Wrap the screen in `LiquidPage`. Never hand-roll
   `Scaffold` + background.
2. **No dead space.** Content starts directly under the top bar. Vertical gaps
   larger than `spaceXl` between unrelated blocks are defects.
3. **No clipped rows.** Any horizontal row of chips or buttons must be
   `LazyRow`/`Row + horizontalScroll`, never a fixed `Row` that overflows.
4. **Bottom clearance.** Any scrolling content must clear the dock — inside
   `LiquidPage`, add the `PaddingValues` you receive to your list's
   `contentPadding`.
5. **Real data only.** No mock/placeholder file names, sizes or timestamps.
   Empty data shows `VennelaEmptyState`.
6. **Every state.** Loading, empty, error and success must all be designed.
7. **Touch targets.** Interactive elements are at least 48×48dp.
8. **Content descriptions.** Every icon that conveys meaning needs one.
9. **Low-end safe.** Avoid nested vertical scrollables, avoid large
   `Modifier.blur`, keep composed node counts low, prefer `LazyColumn`/`LazyRow`
   with stable keys.

## 5. Reference screens

`Screens/*.png` are the source of truth for layout and hierarchy. Match the
structure: header → search capsule → content blocks → dock. Exact pixel
positions may adapt for smaller devices, but the hierarchy must not.

## 6. Verification

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Install and eyeball on the running emulator before claiming a screen is done.
