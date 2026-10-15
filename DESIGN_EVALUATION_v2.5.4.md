# Tablet/phone stability design evaluation — v2.5.4

## Goal

Fix disappearing tablet-portrait controls and unstable phone/tablet rails without
changing capture behavior, Pro layouts, or the JA/EN contract. Apply
https://github.com/android/skills (adaptive, camerax, edge-to-edge,
jetpack-compose-performance) as the review baseline.

## Skills applied (imported to `/Users/yuki/適当フォルダ/android-skills`)

- `jetpack-compose/adaptive`: window-size classes, bounded side rails, immersive
  preview takes the remainder, no fixed-aspect sibling that can overflow a Row.
- `system/edge-to-edge`: single Scaffold padding + `windowInsetsPadding(safeDrawing)`
  on side decks only, no double insets, scroll containers keep content tappable.
- `camera/camerax`: single `PreviewView` + `movableContentOf` owner untouched,
  `FILL_CENTER` kept, output aspect stays in `ResolutionSelector` only.
- `jetpack-compose-performance`: no new `BoxWithConstraints` in hot paths, scroll
  instead of clipping (cheap), `LazyRow` for mode tabs (same drag contract),
  pure-function widths covered by Robolectric.

## Findings and resolutions

| # | Finding | Resolution | Result |
|---|---|---|---|
| 1 | Tablet portrait (`LARGE_PORTRAIT`) preview used `fillMaxHeight + aspectRatio(matchHeightConstraintsFirst=true)`. On an 800x1280 tablet this demands 1706dp (4:3) / 2275dp (16:9) width, leaving negative width for the `weight(1f)` deck, so controls disappeared. Collapsed tablet used the same overflow frame. | Deck is now fixed `normalTabletPortraitPanelWidth` (40% width, 300–380dp). Preview is always `weight(1f)` immersive `fillMaxSize`. Collapsed is always immersive `fillMaxSize` on both tablet and landscape. | Deck never 0px, preview never overflows. 600dp tablet keeps >=280dp preview remainder, 800dp keeps >=400dp. |
| 2 | Phone portrait `ControlIcons(columns=1)` Row with 7 tiles needs 360dp + padding, clipping Settings on 320–360dp phones. | Single row now `horizontalScroll`, centered, 48dp targets kept. | No clipping, no wrapping change. |
| 3 | `ModeTabsRow` fixed Row in 272–292dp rail overflows with 5 modes (5*64=320dp+). | Changed to `LazyRow` with same `DraggableModeTab` contract as phone. | 3 tabs fit, 5 tabs scroll, drag reorder/remove unchanged. |
| 4 | `OthersMenu(compactRow=true)` fixed Row SpaceEvenly overflows with 4 Japanese labels in 272dp rail. `LensZoomPills` Row overflows with 5 lenses. | Both now `horizontalScroll` single rows, 48dp+ targets kept, 1-row spec kept. | No clipping, no second row. |
| 5 | Side decks lacked `safeDrawing` insets while phone relied on Scaffold padding only. | Added `windowInsetsPadding(safeDrawing)` to both side decks. Outer Scaffold padding unchanged, no double padding. | Gesture nav / cutout safe. |

## Safety and compatibility

- Capture output still follows 4:3 / 16:9 via `ResolutionSelector`; only normal live view is center-cropped immersive.
- Single remembered `PreviewView`, CameraX bind, debounce/retry/fallback, Room v3, mode-bar persistence, drag gestures, profile storage, capture intents unchanged.
- Pro (`ProScreen`, `proArrangement`, Pro preview sizes) untouched.
- Every interactive tile stays >=48dp, icon-only controls keep localized contentDescriptions, no hardcoded UI text, `values` / `values-ja` remain 172 keys matched.
- Finite aspect, `coerceAtLeast(0.1f)` / `coerceIn`, weight-based layout only — no divide-by-zero, no negative remainder, no unbounded measure, no new allocations in draw path.
- Degenerate HAL ranges still render static rows, panorama/timelapse/BG guards unchanged.

## Verification

- `testDebugUnitTest` 31件 (existing 30 + new tablet remainder test) / `lintDebug` (0 CameraOverlay issues) / `assembleDebug` / upload-key `assembleRelease`.
- `apksigner` fingerprint matches `99:0C:CC:90:2C:33:11:8E:C1:02:9B:4F:58:2B:39:E3:ED:A9:32:72:E4:6F:E2:76:A0:91:92:62:BE:CD:C5:B2`.
- Static width math verified for 600/800/1200dp; phone 320–411dp single-row scroll verified by code inspection.
- No FATAL / parent collision / bind failure / ANR introduced; real-device tablet QA remains recommended (foldables/tablets were already untested per spec).
