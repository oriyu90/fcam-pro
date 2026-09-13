# Standard portrait preview design evaluation — v2.5.2

## Goal

Use the space released by absent manual controls in standard portrait capture, while retaining Pro's exact-aspect workspace and the existing single-surface crash protection.

## Findings and resolution

| Finding | Resolution | Result |
|---|---|---|
| Standard portrait inherited Pro's short landscape-shaped viewfinder | Give PHOTO, VIDEO and SLOWMO the remaining height above the compact controls | The viewfinder becomes the dominant surface, matching the Basic-mode reference |
| Lens choices occupied a separate flexible region | Overlay them at the viewfinder's bottom edge | No dead space remains between preview and controls |
| Utility modes need room for actions | Keep OTHERS and PANORAMA on the fixed-aspect path | Their menus and progress actions remain reachable |
| Pro depends on a range-accurate workspace | Leave Pro's exact 4:3 / 16:9 frame unchanged | Manual composition and control density do not regress |
| Reparenting native camera surfaces can freeze or crash | Resize only the Compose container around the remembered `PreviewView` | Camera ownership and bind lifecycle stay unchanged |

## Safety and compatibility

- Package ID, minSdk, settings, storage formats, Room schema and capture intents are unchanged.
- The selected aspect ratio still controls capture resolution; only the standard portrait viewfinder is center-cropped.
- Pro, tablet portrait and landscape layout policies are unchanged.
- The native preview remains a single remembered instance and is never duplicated.
- Existing 48dp-or-larger controls, non-color selection outlines and English/Japanese resources are unchanged.

## Verification

- 29 Robolectric tests, Android Lint, debug compilation and upload-key-signed release build.
- Xperia 1: standard portrait preview expands continuously from the top inset to the toolbar; lens controls remain visible at its lower edge.
- Repeated standard/Pro transitions, Photo/Others transitions and portrait/landscape changes retained the same Activity and a live preview.
- No fatal exception, duplicate-parent error, Camera bind failure or ANR was logged.
