# Fcam pro v2.5.1

## 日本語

通常モードのプレビュー配置とバックグラウンド録画を安定化し、Sony Photography Proを基準に操作領域を詰めたメンテナンスリリースです。

- 通常モードでプレビューと操作デッキを同じレイアウト階層へ統合し、横画面の不要な隙間と縦画面の位置ずれを解消
- 縦横とも4:3／16:9の横長プレビュー枠を維持し、回転後の縦長ピラーボックス表示を修正
- 主要操作を48dp以上の角丸を抑えた正方形タイルへ統一し、境界・選択枠・色を併用
- Pro／通常切替や回転でも単一の`PreviewView`を維持し、Surfaceの二重親化と停止を防止
- BG録画の非同期開始中の二重起動を防止し、ホーム、画面オフ、履歴からの削除後も録画を継続
- BG停止時はCameraXのFinalizeを待ってからカメラを解放し、保存破損と通常プレビューとの再接続競合を防止
- 停止保存中の通知を日本語／英語へ追加

Xperia 1で通常／Proを12回連続切替し、縦横回転、4:3／16:9、20秒のBG録画、ホーム移動、14分16秒の長時間BG録画、履歴からの削除、停止後のプレビュー復帰を確認しました。28件の単体テスト、Android Lint、署名Releaseビルドも成功しています。

## English

This maintenance release stabilizes standard-mode preview placement and background recording while tightening the controls around a Sony Photography Pro-inspired layout.

- Places the standard preview and control deck in one layout hierarchy, removing the landscape gap and portrait offset
- Keeps a landscape-shaped 4:3 / 16:9 viewfinder in both device orientations and removes rotated-stream pillarboxing
- Standardizes primary actions on square 48dp-or-larger tiles with explicit boundaries and shape-plus-color selection
- Preserves one `PreviewView` through Pro/standard and orientation transitions, preventing parent collisions and frozen surfaces
- Rejects duplicate asynchronous BG starts and keeps recording through Home, screen-off and Recents removal
- Waits for CameraX Finalize before releasing the service camera, preventing damaged saves and preview-reconnect races
- Adds localized English and Japanese saving-state notifications

Verified on Xperia 1 with 12 repeated standard/Pro transitions, portrait/landscape changes, both aspect ratios, a 20-second BG regression recording, Home, a 14m16s long BG recording, Recents removal and normal-preview recovery. All 28 unit tests, Android Lint and the signed release build pass.
