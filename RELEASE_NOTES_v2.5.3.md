# Fcam pro v2.5.3

## 日本語

通常モードの横画面を、プレビュー優先のコンパクトな操作レールへ再構成したメンテナンスリリースです。Proモードは変更していません。

- 通常横画面の設定パネルを272〜292dpへ制限し、画面幅を取りすぎる問題を修正
- 操作を4列ツール、レンズ、撮影、モードの各行へ整理し、縦スクロールを撤廃
- 「その他」のスロー、パノラマ、タイムラプス、BG録画を1行へ集約
- プレビューを操作レール以外の全領域へ拡張し、折り畳み時は横画面全体へ表示
- 48dp以上の操作領域、日英UI、ドラッグ可能なモードバー、単一`PreviewView`構成を維持
- Proモードのプレビュー、設定パネル、プロファイル、シャッター配置は変更なし

Xperia 1で通常写真、「その他」、パネル展開／折り畳み、通常／Pro切替、縦横回転を確認しました。30件の単体テスト、Android Lint、正式鍵署名Releaseビルドも成功しています。

## English

This maintenance release rebuilds standard landscape capture around a compact, preview-first control rail. Pro mode is unchanged.

- Bounds the standard landscape settings rail to 272–292dp instead of consuming all remaining width
- Organizes controls into four-column tools, lens, capture and mode rows with no vertical scrolling
- Places Slow motion, Panorama, Time-lapse and BG recording on one Others row
- Expands the preview into all space outside the rail and makes it full-screen when the rail is collapsed
- Preserves 48dp-or-larger targets, English/Japanese UI, the draggable mode bar and the single `PreviewView`
- Leaves Pro viewfinder, settings, profiles and shutter placement unchanged

Verified on Xperia 1 across standard Photo, Others, expanded/collapsed panels, standard/Pro transitions and orientation changes. All 30 unit tests, Android Lint and the upload-key-signed release build pass.
