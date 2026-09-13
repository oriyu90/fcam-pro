# Preview and recording design evaluation — v2.5.1

## Goal

Correct the normal camera's displaced preview, remove unusable gaps from both orientations and make background recording finalize reliably without regressing the stable Pro workspace.

## Findings and resolution

| Finding | Resolution | Result |
|---|---|---|
| The normal preview and full-screen overlay were independent trees | Give `CameraOverlay` ownership of a single preview content slot | Preview and controls now share deterministic bounds |
| The landscape panel floated over a full-screen preview, leaving a dead strip | Compose the exact-aspect preview and control deck as siblings | The deck begins directly at the viewfinder edge |
| A rotation-aware `FIT_CENTER` surface became a narrow portrait image inside the preserved landscape frame | Use `FILL_CENTER` inside the explicit 4:3 / 16:9 frame | The visible viewfinder keeps the requested shape in portrait and landscape |
| BG start remained callable while CameraX was still binding | Keep a start-pending guard until `VideoRecordEvent.Start` | Repeated taps cannot create overlapping recordings |
| Service teardown released the camera before recorder finalization | Stop the recording, wait for `VideoRecordEvent.Finalize`, then unbind and stop foreground state | MediaStore output closes cleanly before the UI camera reconnects |
| `onTaskRemoved` stopped an otherwise valid foreground recording | Keep the foreground service active and use intent redelivery | Home, screen-off and Recents removal no longer truncate the clip |

## Safety and compatibility

- Package ID, minSdk, storage destinations, database schema and capture-intent contracts are unchanged.
- One remembered `PreviewView` remains the only native preview owner; layout changes move Compose content rather than creating a competing surface.
- Main StateFlows are collected with lifecycle awareness, avoiding off-screen UI work.
- BG service state remains true until finalization and camera release complete, so the Activity cannot race the service for the same Sony camera.
- A five-second teardown fallback prevents the foreground service from hanging if a vendor encoder never reports Finalize.
- Foreground-start failures abort cleanly instead of continuing camera initialization without a valid service state.
- Square controls retain at least a 48dp target. Selection uses an outline as well as orange color, preserving non-color recognition.
- English and Japanese string-key sets remain matched.

## Verification

- 28 Robolectric tests, Android Lint, debug compilation and upload-key-signed release build.
- Xperia 1: portrait 4:3 viewfinder measured 1096×822; portrait 16:9 measured 1096×617; landscape 16:9 measured 1854×1043.
- Twelve Pro/standard transitions plus portrait/landscape transitions retained one process and a live preview; no fatal exception, duplicate-parent error or bind failure was logged.
- BG clips of 14m16s and 20.4s finalized successfully. The long run survived screen-off and Recents removal; the final run survived Home and rotation. Normal preview reconnected after each stop.
