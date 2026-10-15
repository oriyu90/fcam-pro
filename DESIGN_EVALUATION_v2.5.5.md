# Tablet portrait redesign evaluation — v2.5.5

## Goal

Fix the remaining tablet-portrait breakage found on-device (Lenovo TB710FU,
2000x3200, 340dpi, sw941dp): the v2.5.4 side-by-side Row let the preview surface
cover the left deck, and even without the overlap the 376dp deck would squeeze
the viewfinder into an unusable tall narrow strip. Implement the requested
bottom-toolbar + side icon-popup shooting panel without touching existing
phone / landscape / Pro / capture paths.

## Skills applied (imported to `/Users/yuki/適当フォルダ/android-skills`)

- `jetpack-compose/adaptive`: portrait tablets use a vertical composition
  (full-width preview + bottom toolbar), side rails only where the remainder is
  wide (landscape). No fixed-aspect sibling inside a Row.
- `system/edge-to-edge`: single Scaffold padding + `safeDrawing` on the tablet
  deck and floating rail only.
- `camera/camerax`: single `PreviewView` + `movableContentOf` owner untouched,
  `FILL_CENTER` kept, output aspect stays in `ResolutionSelector`.
- `jetpack-compose-performance`: new rail is stateless (no remembered offsets),
  existing components reused, dead side-panel code removed instead of left over.

## Findings and resolutions

| # | Finding (real device) | Resolution | Result |
|---|---|---|---|
| 1 | Tablet portrait deck (left, 376dp fixed) was covered by the preview surface; controls at x 264–786 only peeked out at the left edge, close button unreachable | New `TabletPortrait`: phone-like Column (full-width preview on top, toolbar bottom) + floating `TabletSideRail` (shutter + front/back) at `CenterEnd` of the preview | Full-width viewfinder, all controls reachable, verified by screenshot + uiautomator bounds on TB710FU |
| 2 | Side-by-side would still give a ~565dp × 1506dp (0.38 aspect) viewfinder strip in portrait | Vertical composition: preview gets full 941dp width; utility modes keep the phone's fixed-aspect frame + menu contract | Viewfinder matches phone UX, only wider |
| 3 | Dead code after rerouting: `PanelBody` (+ `LensRow`, `ModeTabsColumn`, `lensLabel`) and `normalTabletPortraitPanelWidth` lost all callers | Removed. `NormalSideLayout` simplified to landscape-only `NormalLandscapeLayout` | No private-unused warnings, `lintDebug` shows 0 CameraOverlay issues |
| 4 | Requested layout: bottom toolbar + side popup shooting ops | Bottom holds controls + lens + modes + thumbnail/collapse/battery; side rail holds shutter + front/back dispatching via `SharedActions.onShutter()` exactly like `ShutterBar` | PHOTO/VIDEO/OTHERS/PANORAMA shutter behavior identical on every layout |

## Safety and compatibility (review)

- Untouched paths: `PhonePortrait`, landscape rail, all `ProScreen` arrangements,
  CameraX bind/debounce/retry/fallback, Room v3, mode-bar persistence, BG service,
  timelapse, panorama, QR, external capture intents.
- No new numeric layout math, no new state, no new allocations in draw path.
- No hardcoded UI text; new rail reuses existing tiles/buttons and their
  localized contentDescriptions. `values` / `values-ja` still 172 keys matched.
- All touch targets stay ≥48dp (shutter 72dp, tiles 48dp).
- Known trade-off: the floating rail covers ~100dp of the viewfinder's right
  edge (~11% of 941dp). Requested popup placement; acceptable.
- Removed items are file-private composables or an `internal` pure function
  introduced in v2.5.4 only (never a public API, APK interface unchanged).

## Verification (real device + CI gates)

- Lenovo TB710FU portrait (sw941dp): PHOTO expanded, VIDEO (red rail shutter),
  OTHERS (fixed frame + 4-item menu), collapsed immersive + cluster, expand,
  PHOTO→VIDEO→OTHERS switching, side-rail photo capture saved to MediaStore
  (`Fcam-photo-*.jpg`), thumbnail updated. Landscape rail unchanged and correct.
- `logcat`: no FATAL / bind-failure lines during the session.
- `testDebugUnitTest` 30件 / `lintDebug` (0 CameraOverlay issues) /
  upload-key `assembleRelease` / GitHub Actionsグリーン.
- `apksigner` fingerprint matches `99:0C:CC:90:2C:33:11:8E:C1:02:9B:4F:58:2B:39:E3:ED:A9:32:72:E4:6F:E2:76:A0:91:92:62:BE:CD:C5:B2`.
