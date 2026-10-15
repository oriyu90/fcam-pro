# Fcam pro v2.5.4

Tablet portrait controls no longer disappear, and phone/tablet rails no longer clip.

## What changed (no breaking changes)

- Tablet portrait now uses a fixed 300–380dp left deck with an immersive preview filling the remainder. The previous height-first exact-aspect preview could demand 1700dp+ width and push the controls to 0px.
- Collapsed preview is now immersive full-screen on both tablet portrait and landscape.
- Narrow phones keep all 7 top controls reachable via sideways scroll instead of clipping Settings.
- Landscape rail mode tabs (up to 5) and the one-row Others menu (up to 4 Japanese labels) scroll sideways instead of clipping.
- Lens pills scroll sideways when 5+ lenses are present.
- Side decks respect gesture navigation and cutouts. Pro layouts, capture output, and the single-preview safety are unchanged.

## Compatibility

- Android 7.0 (API 24)+, existing profiles and mode-bar order preserved (Room v3, SharedPreferences).
- Japanese / English UI complete (172 keys matched), 48dp+ targets, crash-safe on unsupported HALs.

## Verify

- `Fcam-pro-v2.5.4.apk` + `SHA256SUMS.txt` from GitHub Release. Certificate SHA-256: `99:0C:CC:90:2C:33:11:8E:C1:02:9B:4F:58:2B:39:E3:ED:A9:32:72:E4:6F:E2:76:A0:91:92:62:BE:CD:C5:B2`.

---

# Fcam pro v2.5.4（日本語）

タブレット縦向きで操作部が消える問題と、スマホ／タブレットのレール欠けを修正しました。

## 変更点（破壊的変更なし）

- タブレット縦は左に300〜380dpの固定デッキ、残り全幅を没入プレビューに。高さ基準の固定アスペクトが1700dp超を要求して操作部を0pxに潰していた問題を解消。
- 折り畳み時のプレビューはタブレット縦・横とも全面表示。
- 幅の狭いスマホでも上部7操作が横スクロールで全て到達可能（設定の欠けを解消）。
- 横レールのモードタブ（最大5件）と1行その他メニュー（日本語4件）が欠けずに横スクロール。
- レンズピルが5件以上でも横スクロールで欠けない。
- ジェスチャーナビ／切り欠き対応。Pro配置・撮影出力・単一プレビュー安全設計は無変更。

## 互換性

- Android 7.0（API 24）以上。既存プロファイルとモードバー順序を維持。
- 日英完全対応（172キー一致）、48dp以上ターゲット、非対応HALでもクラッシュなし。

## 検証

- GitHub Releaseの `Fcam-pro-v2.5.4.apk` と `SHA256SUMS.txt` で照合。証明書SHA-256は上記の通り。
