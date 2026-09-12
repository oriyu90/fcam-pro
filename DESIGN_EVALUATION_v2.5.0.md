# Pro workspace design evaluation — v2.5.0

## Goal

Make the Photography Pro-inspired controls reliable in portrait and landscape without changing the selected preview shape, then give Fcam pro user-owned mode and profile workflows while preserving capture compatibility.

## Findings and resolution

| Finding | Resolution | Result |
|---|---|---|
| Layout transitions could detach and reattach the same native preview view through different Compose hosts | Keep one orientation-independent `PreviewView` inside one `movableContentOf` host | Pro/standard and portrait/landscape transitions retain one live surface |
| Width-only aspect sizing could overflow or compress overlays in short landscape regions | Fit the aspect rectangle against both maximum width and height and use `FIT_CENTER` | 4:3 and 16:9 remain exact; lens and status overlays stay inside the frame |
| Slow motion and Panorama permanently crowded the main mode selector | Default to Photo / Video / Others and make optional modes draggable | The primary bar is calmer while every mode remains discoverable and persistent |
| Profile actions were hidden inside the PRF control and did not use the lower free region | Add a permanent icon-only save control and colored profile dock | Save/apply is always reachable without displacing exposure controls |
| Profile identity, ordering and destructive actions were weak | Six colors, contrast-aware labels, white selected outline, long-drag ordering and extended-hold confirmation | Profiles are scannable, accessible and resistant to accidental deletion |

## Compatibility and safety assessment

- Package ID, minSdk 24, MediaStore paths and external capture intents are unchanged.
- Room v3 uses an explicit v2→v3 migration with non-null defaults; existing rows remain readable.
- The preview, image capture and video capture rotations update together without rebinding.
- `OTHERS` is pinned by sanitization, so users cannot remove the only path back to hidden modes.
- Mode lists are deduplicated and unknown persisted enum names are ignored.
- Profile selection uses shape as well as color, and labels choose contrasting foreground colors.
- English and Japanese sets both contain 171 keys.

## Verification

- 28 Robolectric unit tests, Android lint, debug build and signed release build.
- Xperia 1 SOV40: 12 rapid Pro/standard transitions and nine forced rotations with the same process; no fatal exception, parent conflict or bind failure.
- The 4:3 frame measured 1024×768 and the 16:9 frame 1024×576 in portrait. Landscape retained the selected aspect and all three lens labels.
- Added Slow motion from Others, verified persistence after force-stop/relaunch, then dragged it back and restored the default bar.
- Created a colored profile, overwrote it, opened extended-hold deletion confirmation and removed the test row.
- Still capture and a three-second video completed; final memory was approximately 137 MB PSS after the transition stress.
