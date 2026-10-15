# Tablet portrait Pro redesign evaluation — v2.5.6

## Goal

Fix the broken tablet-portrait manual (Pro) arrangement found on-device (Lenovo
TB710FU, sw941dp): the side-by-side Row left a ~1500px dead void under the
left preview/shutter column while the right deck clipped items (FORMAT/MIC cut
off at the screen edge) and stranded the profile dock / mode tabs / thumbnail
at the very bottom, disconnected from the deck.

## Skills applied (imported to `/Users/yuki/適当フォルダ/android-skills`)

- `jetpack-compose/adaptive`: portrait (any width) uses the vertical composition;
  side-by-side panes only where the remainder stays wide (landscape).
- `system/edge-to-edge`: unchanged single Scaffold padding + `safeDrawing` blocks.
- `camera/camerax`: single `PreviewView` + `movableContentOf` owner untouched,
  `FILL_CENTER` kept, output aspect stays in `ResolutionSelector`.
- `jetpack-compose-performance`: no new state, no new measuring code; the fix
  deletes a branch instead of adding one.

## Findings and resolutions

| # | Finding (real device) | Resolution | Result |
|---|---|---|---|
| 1 | Pro tablet portrait Row: left column (preview + shutter + `Spacer(weight 1f)`) left two-thirds of the 3200px height empty black | Removed the tablet-only Row branch; portrait Pro (phone + tablet) shares the proven vertical composition: toolbar → full-width aspect preview → summary → deck → control area → profile dock → mode tabs → shutter bar | No dead void; verified by screenshot + uiautomator bounds on TB710FU |
| 2 | Right deck `LazyRow` clipped FORMAT/MIC off-screen; dock/tabs/thumbnail stranded at bottom | 941dp width fits all 6–7 deck cells (74dp) in one horizontal row with room to spare | All of ISO/SS/AF/WB/EV/FMT visible, no scroll needed |
| 3 | Change risk | One condition edit (`proMode && !proLandscape`, `compact` requirement dropped) + branch deletion; phone portrait pixels identical (same branch as before) | `lintDebug` shows no new issues (1 pre-existing UseKtx warning, also present on baseline) |

## Safety and compatibility (review)

- Untouched: phone portrait Pro pixels, landscape Pro, all normal layouts, CameraX
  bind/debounce/retry/fallback, Room v3, mode-bar persistence, BG service,
  timelapse, panorama, QR, external capture intents.
- No new numeric layout math, no new remembered state, no new allocations.
- No hardcoded UI text; deck/dock/tabs/shutter are the existing components with
  their localized labels. `values` / `values-ja` still 172 keys matched.
- All touch targets stay ≥48dp (deck cells 58dp tall, shutter 78dp).
- Trade-off: tablet portrait preview is large (full 909dp width, 682dp tall at
  4:3) and the control area gets ~290dp + internal scroll — the same contract
  as phone Pro, appropriate for a viewfinder-first camera UI.

## Verification (real device + CI gates)

- TB710FU portrait Pro: toolbar (7 tiles), status, full-width 4:3 preview,
  SS/F/EV/ISO summary, all 6 deck cells, ISO tap → preset chips scroll,
  profile dock, mode tabs, shutter bar; Pro photo capture saved to MediaStore.
- TB710FU landscape Pro: 2-column icon strip + center preview + right panel,
  unchanged and correct. Normal tablet portrait re-checked, no regression.
- `logcat`: no FATAL / bind-failure lines during the session.
- `testDebugUnitTest` 30件 / `lintDebug` / upload-key `assembleRelease` /
  GitHub Actionsグリーン.
- `apksigner` fingerprint matches `99:0C:CC:90:2C:33:11:8E:C1:02:9B:4F:58:2B:39:E3:ED:A9:32:72:E4:6F:E2:76:A0:91:92:62:BE:CD:C5:B2`.
