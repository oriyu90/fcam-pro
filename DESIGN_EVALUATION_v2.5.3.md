# Standard landscape rail design evaluation — v2.5.3

## Goal

Remove scrolling and oversized empty control space from standard landscape capture, return that space to the live viewfinder, and make no change to Pro mode.

## Findings and resolution

| Finding | Resolution | Result |
|---|---|---|
| An exact-aspect preview consumed height first and donated all remaining width to the rail | Bound the rail to 272–292dp and give the preview the remaining width | Xperia 1 preview width grows from about 1,440px to about 1,845px |
| The generic panel body stacked controls beyond short landscape height | Add a landscape-only four-column hierarchy with no scroll container | Every primary action stays on screen |
| Others used two rows and repeated generous spacing | Show its maximum four actions in one row | Others remains fully visible without scrolling |
| Collapsed standard mode retained a small exact-aspect frame | Let the preview fill the entire content area | Collapsed mode has no side dead space |
| Pro already met the requested design | Keep `ProScreen`, `proArrangement` and Pro sizing untouched | Pro layout and composition remain identical |

## Safety and compatibility

- Capture output still follows the selected 4:3 / 16:9 setting; only the normal live view is center-cropped.
- The remembered native `PreviewView`, CameraX binding and lifecycle are unchanged.
- Existing mode ordering, drag gestures, profile storage, database schema and capture intents are unchanged.
- Every interactive tile remains at least 48dp and icon-only controls retain localized accessibility names.
- Large portrait and all Pro layout paths retain their previous behavior.

## Verification

- 30 Robolectric tests, Android Lint and upload-key-signed release build.
- Xperia 1: standard landscape, maximum-content Others, expanded and collapsed rail, standard/Pro switching and orientation changes.
- Pro screenshot and control bounds remain unchanged.
- No fatal exception, duplicate-parent error, Camera bind failure or ANR was logged.
