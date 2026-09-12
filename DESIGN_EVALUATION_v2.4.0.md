# Pro mode design evaluation — v2.4.0

## Goal

Retain Fcam pro's existing CameraX architecture and capture capabilities while making Pro mode read and operate like a portrait-capable Photography Pro workspace. User-created capture profiles remain a first-class Fcam pro feature rather than a clone of the reference app.

## Findings and resolution

| Finding | Resolution | Result |
|---|---|---|
| Portrait placed controls far below the live image and left visually empty space | Moved summary, quick deck and active control directly after the preview | Information hierarchy follows the shooting flow without dead zones |
| Landscape used one narrow vertical list and clipped lower items | Widened the rail and arranged controls in two columns | ISO through PRF/FMT/MIC remain reachable on Xperia 1 |
| Round abbreviations did not expose current values | Replaced them with labeled 58dp cells showing code and live value | Faster scanning and larger touch targets |
| Continuous sliders were slow for common exposure choices | Added photographic presets derived from HAL ranges | Common changes are one tap while unsupported values never appear |
| Profiles lacked clear empty/applied/destructive states | Added count/empty state, current-match styling, name validation and delete confirmation | Profile CRUD is explicit and resistant to accidental loss |
| Rotation could reuse a stale PreviewView surface | Keyed the preview instance to orientation while retaining one AndroidView host | Avoids reparenting failures without duplicating camera surfaces |
| A shutter press could race a just-started camera bind | Added a short camera-ready gate after successful binding | Prevents zero-frame captures during rapid mode changes |

## Compatibility and safety assessment

- No Room schema change; v2 profiles remain readable and persistent.
- No change to package ID, minSdk, MediaStore paths or public capture intents.
- Manual ISO and shutter remain an atomic pair, preventing undefined AE-off states.
- Existing multi-lens selection and CameraX fallback paths remain intact.
- English and Japanese string sets contain matching keys; visible labels and accessibility descriptions are localized.
- Controls meet a 48dp minimum target; principal cells use 58dp, and non-text boundaries meet a 3:1 contrast target on black.

## Verification

- 26 unit tests, Android lint, debug build and signed release build.
- Xperia 1 SOV40: portrait/landscape, five repeated rotations, three physical lenses, still capture, video with and without audio, rapid mode changes and profile creation.
- Japanese and English UI checked on device.
- No camera bind failure, PreviewView parent error, battery receiver permission denial, capture crash or out-of-memory event observed in the final run.
