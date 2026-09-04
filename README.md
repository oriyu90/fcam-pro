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
| Capture | Photo & video, 4:3 / 16:9, self-timer (0/3/10 s), flash auto/on/off, rule-of-thirds grid, tap to lock / unlock focus, pinch-to-zoom, one-tap jump to the system gallery |
| Manual controls | ISO, shutter speed, focus distance and white balance — every slider is clamped to the ranges the selected physical camera actually reports, and each one (or all at once) can be set back to Auto |
| Lenses | Ultra-wide / wide / tele / macro / front — the app binds the real `cameraId`, not just a lens-facing flag; a compact pop-up switcher is available in photo and video mode |
| Profiles | Save, rename, delete and re-apply manual setups (Room database, survives reinstall-safe destructive migration) |
| OTHERS | Time-lapse (configurable 1–10 s interval, auto-stop on repeated errors), background video recording via a foreground service with an elapsed-time notification, QR detection with open / copy |
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

## Known limitations (v1.0.2)

- Background recording continues while the process is alive (screen off / app
  backgrounded). Fully detached indefinite recording is out of scope.
- Slow-motion and panorama expose UI entry points but are not implemented in v1.
- Physical sub-cameras hidden behind a logical multi-camera cannot always be
  selected individually; the app falls back to the default camera for that facing.
- Real-device camera behaviour has been validated by compilation, unit tests,
  lint and signing checks only — no on-device QA was performed for this release.

## License

MIT — see [LICENSE](LICENSE). Author: **Yuki_Orita** (折田悠希 / おりたゆうき).

---

## 日本語

**Fcam pro** は Kotlin・Jetpack Compose・CameraX で作られた Android 向けの
本格的なカメラアプリです。マニュアル露出、物理レンズの実切り替え、撮影プロファイル、
バックグラウンド録画、日本語／英語の完全対応 UI（アプリ内で切り替え可能、初期値は端末の言語）
を備えています。

### 主な機能

- 写真／動画、4:3・16:9、セルフタイマー（0/3/10 秒）、フラッシュ、三分割グリッド、タップでフォーカス固定／解除、ピンチズーム、標準ギャラリーへのワンタップ遷移
- ISO・シャッター速度・フォーカス距離・ホワイトバランスのマニュアル制御
  （各スライダーは選択中の物理カメラが報告する範囲に自動でクランプ。各項目・一括でオートに戻せる）
- 超広角／広角／望遠／マクロ／前面レンズを実 `cameraId` で切り替え（写真・動画モードでは小さなポップアップで選択）
- マニュアル設定のプロファイル保存・改名・削除・再適用（Room）
- タイムラプス（間隔 1〜10 秒、連続エラー時に自動停止）、バックグラウンド録画、QR 検出
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
