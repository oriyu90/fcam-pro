# Fcam pro

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/oriyu90/fcam-pro.svg)](https://github.com/oriyu90/fcam-pro/releases)
[![Build](https://github.com/oriyu90/fcam-pro/actions/workflows/build.yml/badge.svg)](https://github.com/oriyu90/fcam-pro/actions/workflows/build.yml)

**Fcam pro** is a professional camera application for Android, built with Kotlin,
Jetpack Compose and CameraX. It offers manual exposure controls, real multi-lens
selection, saved capture profiles, background video recording and a fully
localized Japanese / English interface.

日本語の説明は [下のセクション](#日本語) にあります。

## Features

| Area | What you get |
|---|---|
| Capture | Photo, video, slow-motion & panorama, 4:3 / 16:9, self-timer (0/3/10 s), flash auto/on/off, rule-of-thirds grid, tap to lock / unlock focus, pinch-to-zoom with an on-screen ratio pill (tap to reset to 1.0x; camera and UI always start at 1.0x), one-tap jump to the system gallery |
| Manual controls | A Photography Pro-inspired control deck for ISO, shutter speed, focus, white balance, EV, format and microphone, followed by a dedicated profile dock. Each item shows its live value and offers photographic presets limited to the selected lens's reported range. Exposure stays atomic: setting ISO or speed fills the other with a safe, clamped default. RAW-capable lenses expose JPEG / JPEG+RAW / RAW (DNG) choices |
| Lenses | Ultra-wide / wide / tele / macro / front — binds the real `cameraId`, including sub-cameras hidden behind a logical multi-camera (e.g. Galaxy telephoto) via `setPhysicalCameraId`; depth-only cameras are excluded, same-focal logical/physical twins are deduped; a zoom-factor pill row (×0.5 / ×1 / ×2 …) in phone mode, switcher row on larger screens. Aperture (f-number) is shown when the HAL reports it |
| Adaptive UI | The preview keeps the selected capture aspect in portrait and landscape while controls rotate around it. Pro mode uses a Photography Pro-inspired portrait deck and a two-column landscape rail. The default mode bar is Photo / Video / Others; Slow motion and Panorama can be dragged out of Others and any optional bar mode can be dragged back |
| Profiles | The permanent Pro dock has an icon-only save button plus color-coded profile buttons. Save as new or overwrite, choose one of six colors, apply with a tap, long-drag to reorder, or keep holding for confirmed deletion. Room migrations preserve existing profiles across updates |
| OTHERS | Time-lapse (configurable 1–10 s interval, auto-stop on repeated errors), background video recording via a foreground service with an elapsed-time notification (continues the active lens, disabled shutter/lens UI while running), QR detection with open / copy |
| System integration | Registers for `IMAGE_CAPTURE` / `VIDEO_CAPTURE` / `STILL_IMAGE_CAMERA`, so it can be set as the OS default camera app and returns results to the caller |
| Localization | English (default) and 日本語, switchable in-app; initial value follows the device locale |
| Safety | Fixed dark theme, edge-to-edge insets, every camera path wrapped with error reporting, no crash on unsupported hardware |

## Requirements

- Android 7.0 (API 24) or newer
- Camera permission (required); microphone and notification permissions are
  requested only when you record video or start background recording

## Install

Download the latest signed APK from the
[Releases page](https://github.com/oriyu90/fcam-pro/releases) and verify it
against `SHA256SUMS.txt`.

## Build from source

```bash
git clone https://github.com/oriyu90/fcam-pro.git
cd fcam-pro
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```

- JDK 21 (Robolectric unit tests against `compileSdk 36` require it)
- Android SDK Platform 36 and Build-Tools 36.0.0
- A `debug.keystore` is generated automatically by the Android Gradle plugin

Release builds read the signing keystore from environment variables
(`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`); without them
`assembleRelease` falls back to the debug key so the build never breaks for
contributors.

## Tech

- Kotlin 2.2, Jetpack Compose (Material 3), Navigation-Compose
- CameraX 1.5 (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`, `camera-video`)
- Camera2 interop for manual capture-request options
- Room 2.7 for capture profiles
- ML Kit barcode scanning for QR detection
- AndroidX AppCompat per-app locales for the in-app language switch
- `LifecycleService` + foreground service for background recording

## Project layout

```
app/src/main/java/com/oriyu90/fcampro/
├── MainActivity.kt          # permission gate, nav host, OS capture-intent handling
├── FcamProApp.kt            # applies the stored locale on cold start
├── core/                    # AppSettings (SharedPreferences), LocaleController
├── data/                    # Room: CameraProfile, Dao, Database, Repository
├── services/                # BackgroundCameraService (headless recording)
└── ui/
    ├── CameraScreen.kt      # CameraX binding, capture, time-lapse, QR
    ├── CameraOverlay.kt     # responsive control layout (portrait / wide rail)
    ├── CameraViewModel.kt   # lens detection + capability clamping + state
    ├── SettingsScreen.kt    # language, capture defaults, about
    └── theme/               # fixed dark Material 3 theme
```

## Known limitations (v2.5.1)

- Background recording continues with the screen off, in the background and
  after the app is removed from Recents. Android may still stop it under severe
  system pressure; a restored service redelivers the pending start request.
- Slow-motion needs a camera with constrained high-speed video (≥60 fps);
  otherwise the shutter reports that the device is unsupported. If the HAL
  rejects the high-fps bind, the app falls back to a normal recording bind
  so the preview never dies. Slow-mo clips are saved silent (audio is
  dropped during the time-stretch).
- RAW (DNG) stills ride on CameraX 1.5 `OUTPUT_FORMAT_RAW` / `OUTPUT_FORMAT_RAW_JPEG`,
  so they need a lens reporting `REQUEST_AVAILABLE_CAPABILITIES_RAW` plus
  `RAW_SENSOR` stream sizes; JPEG-only lenses hide the format selector.
  RAW output applies to plain PHOTO mode only (panorama, time-lapse and OS
  capture requests stay JPEG). RAW + flash depends on the HAL; a failure is
  reported as a capture error.
- Panorama stitching is translation-only with feather blending — scenes with
  strong parallax may show seams. Frames never flash (fixed off for exposure
  consistency); devices without a rotation sensor capture on a timer instead.
- Physical sub-cameras hidden behind a logical multi-camera cannot always be
  selected individually; the app falls back to the default camera for that facing.
- This release was exercised on an Xperia 1 (Android, Japanese and English UI)
  in portrait and landscape, including 12 rapid Pro/normal transitions, nine
  forced rotations without process or preview-surface replacement, 4:3 / 16:9,
  mode-bar persistence, still capture, video, profile workflows and background
  recording across Home / rotation / Recents removal. A 14-minute clip and a
  final 20-second regression clip both finalized without an encoder error.
  Camera HAL behaviour can still vary on other device families.

## License

MIT — see [LICENSE](LICENSE). Author: **Yuki_Orita** (折田悠希 / おりたゆうき).

---

## 日本語

**Fcam pro** は Kotlin・Jetpack Compose・CameraX で作られた Android 向けの
本格的なカメラアプリです。マニュアル露出、物理レンズの実切り替え、撮影プロファイル、
バックグラウンド録画、日本語／英語の完全対応 UI（アプリ内で切り替え可能、初期値は端末の言語）
を備えています。

### 主な機能

- 写真／動画／スローモーション／パノラマ、4:3・16:9、セルフタイマー（0/3/10 秒）、フラッシュ、三分割グリッド、タップでフォーカス固定／解除、ピンチズーム（画面に倍率表示、タップで1倍にリセット。起動時はUI・カメラとも必ず1倍）、標準ギャラリーへのワンタップ遷移
- Photography Pro を参考にした操作デッキから ISO・シャッター速度・フォーカス距離・ホワイトバランス・露出補正・保存形式・マイクを選択し、その下に専用プロファイルドックを配置。現在値を常時表示し、選択中の物理カメラが報告する範囲内の実用的なプリセットだけを提示。露出の手動化は不可分で、ISO・速度の片方だけ設定するともう片方を安全な既定値で補完。センサーRAW対応レンズでは JPEG／JPEG+RAW／RAW（DNG）を選択可能
- 超広角／広角／望遠／マクロ／前面レンズを実 `cameraId` で切り替え（論理マルチカメラ背後のサブカメラ＝Galaxy の望遠等も `setPhysicalCameraId` で対応）。HAL が報告する絞り値（F値）を表示
- プロモード時は専用レイアウト：選択した4:3／16:9のプレビュー形状を縦横で変えず、上部状態帯（バッテリー／モード／空き容量／焦点距離）、SS・F値・EV・ISO サマリー、現在値付きの設定デッキ、設定集中パネル＋シャッターを配置。縦画面は Photography Pro を縦向きに再構成した上下配置、横画面は全項目が欠けない2列サイドデッキ
- 画面に応じた操作パネル: スマホ縦は従来の縦積み、≥600dp 縦は左に浮くパネル（アイコン 2 列・縦タブ・上/中央/下寄せ）、横向きは右のサイドバー。パネルは開閉でき、閉じるとシャッター＋バッテリー＋展開ボタンだけの移動可能なクラスタになる
- プロ画面に常設したフロッピーアイコンから、色を選んで新規プロファイルを作成、または既存を上書き。色付きボタンをタップして適用、長押しドラッグで並び替え、さらに保持すると削除確認を表示。Room の既存データはアップデート時も保持
- 初期モードバーは写真／動画／その他。スロー／パノラマは「その他」に入り、下方向へのドラッグでバーへ追加できる。バー上の任意モードは並び替え・上方向ドラッグで「その他」へ戻せ、構成は再起動後も保持
- タイムラプス（間隔 1〜10 秒、連続エラー時に自動停止）、バックグラウンド録画（使用中のレンズを引き継ぎ、録画中はシャッター・レンズ切替を無効化）、QR 検出
- `IMAGE_CAPTURE` / `VIDEO_CAPTURE` に対応し、OS の標準カメラアプリに設定可能
- 固定ダークテーマ、エッジトゥエッジ対応、カメラ処理は全て例外安全

### 動作環境

- Android 7.0（API 24）以上
- カメラ権限（必須）。マイクと通知は動画録画・バックグラウンド録画の利用時のみ要求

### ビルド

JDK 21 / Android SDK 36 / Build-Tools 36.0.0 が必要です。

```bash
./gradlew assembleDebug
```

### ライセンス

MIT。著作者: **Yuki_Orita**（折田悠希 / おりたゆうき）。
