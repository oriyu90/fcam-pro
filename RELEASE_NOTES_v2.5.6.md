# Fcam pro v2.5.6

Tablet portrait Pro rebuilt on the proven vertical composition.

## What changed (no breaking changes)

- Tablet portrait manual mode no longer uses a side-by-side layout that left a giant empty area and clipped settings items off-screen. It now uses the same vertical composition as phone Pro: toolbar, full-width preview, exposure summary, live-value deck, control area, profile dock, mode tabs and shutter bar.
- All deck items (ISO / SS / AF / WB / EV / FMT) fit in one row on tablet widths; the control area scrolls internally like on phones.
- Phone Pro pixels, landscape Pro, standard layouts, capture output, profiles, and the single-preview safety are unchanged.

## Compatibility

- Android 7.0 (API 24)+, existing profiles and mode-bar order preserved.
- Japanese / English UI complete, 48dp+ targets, crash-safe on unsupported HALs.

## Verify

- `Fcam-pro-v2.5.6.apk` + `SHA256SUMS.txt` from GitHub Release. Certificate SHA-256: `99:0C:CC:90:2C:33:11:8E:C1:02:9B:4F:58:2B:39:E3:ED:A9:32:72:E4:6F:E2:76:A0:91:92:62:BE:CD:C5:B2`.

---

# Fcam pro v2.5.6（日本語）

タブレット縦のマニュアル表示を作り直しました。

## 変更点（破壊的変更なし）

- タブレット縦のマニュアル表示は左右分割を廃止し、スマホと同じ縦積み構成へ（ツールバー→全幅プレビュー→サマリー→設定デッキ→操作域→プロファイル→モード→シャッター）。巨大な空白と項目欠けを解消。
- タブレット幅では全設定項目が1行に収まり、操作域は内部スクロール（スマホと同一契約）。
- スマホのマニュアル表示・横Pro・通常配置・撮影出力・プロファイル・単一プレビュー安全設計は無変更。

## 互換性

- Android 7.0（API 24）以上。既存プロファイルとモードバー順序を維持。
- 日英完全対応、48dp以上ターゲット、非対応HALでもクラッシュなし。

## 検証

- GitHub Releaseの `Fcam-pro-v2.5.6.apk` と `SHA256SUMS.txt` で照合。証明書SHA-256は上記の通り。
- 実機（Lenovoタブレット縦）で全項目表示・ISO選択・撮影保存、横Pro、通常モード回帰を確認。
