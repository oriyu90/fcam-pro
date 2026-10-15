# Fcam pro v2.5.5

Tablet portrait rebuilt: full-width viewfinder, bottom toolbar, floating side shooting rail.

## What changed (no breaking changes)

- Tablet portrait no longer uses a side-by-side panel that the preview surface could cover. It now uses a vertical composition: immersive full-width preview on top, toolbar (controls, lens, modes, gallery, battery) at the bottom.
- New floating icon-type shooting rail on the viewfinder's right edge with shutter + front/back. It dispatches exactly like the phone shutter bar, so PHOTO / VIDEO / OTHERS / PANORAMA behavior is identical on every layout.
- Collapsed tablet portrait shows the immersive preview with the movable control cluster, same contract as before.
- Phone, landscape rail, Pro layouts, capture output, profiles, and the single-preview safety are unchanged.

## Compatibility

- Android 7.0 (API 24)+, existing profiles and mode-bar order preserved.
- Japanese / English UI complete, 48dp+ targets, crash-safe on unsupported HALs.

## Verify

- `Fcam-pro-v2.5.5.apk` + `SHA256SUMS.txt` from GitHub Release. Certificate SHA-256: `99:0C:CC:90:2C:33:11:8E:C1:02:9B:4F:58:2B:39:E3:ED:A9:32:72:E4:6F:E2:76:A0:91:92:62:BE:CD:C5:B2`.

---

# Fcam pro v2.5.5（日本語）

タブレット縦画面を作り直しました。全幅プレビュー＋下ツールバー＋横の撮影レール構成です。

## 変更点（破壊的変更なし）

- タブレット縦は左右分割を廃止し、縦積み構成へ。上に没入の全幅プレビュー、下にツールバー（操作・レンズ・モード・ギャラリー・バッテリー）を配置。プレビューに覆われて操作部が欠ける問題を解消。
- プレビュー右端にアイコン型の撮影レール（シャッター＋前後切替）を追加。スマホのシャッターバーと同一振り分けのため、写真／動画／その他／パノラマの動作は全配置で同一。
- 折り畳み時は全面プレビュー＋移動式コントロール群（従来契約どおり）。
- スマホ・横レール・Pro配置・撮影出力・プロファイル・単一プレビュー安全設計は無変更。

## 互換性

- Android 7.0（API 24）以上。既存プロファイルとモードバー順序を維持。
- 日英完全対応、48dp以上ターゲット、非対応HALでもクラッシュなし。

## 検証

- GitHub Releaseの `Fcam-pro-v2.5.5.apk` と `SHA256SUMS.txt` で照合。証明書SHA-256は上記の通り。
- 実機（Lenovoタブレット縦画面）で写真・動画・その他・折畳み・撮影保存を確認。
