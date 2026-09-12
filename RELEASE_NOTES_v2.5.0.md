# Fcam pro v2.5.0

## 日本語

プレビューのライフサイクルを組み直し、プロ／通常と縦／横を連続で切り替えても映像面が止まらない構成へ更新しました。Photography Pro を縦画面へ展開した操作感を保ちつつ、Fcam pro 独自の編集可能モードバーとカラープロファイルを追加しています。

- 単一 `PreviewView` と単一表示ホストを全レイアウトで共有し、切替時の Surface 再親子付け・不要な camera rebind を解消
- 4:3／16:9プレビューを利用可能領域へ正確に内接。縦横で矩形の形を変えず、UIと撮影方向だけを変更
- 初期モードバーを写真／動画／その他へ整理。スロー／パノラマをドラッグで追加し、並び替えや「その他」への格納が可能
- フロッピーアイコンから新規保存／上書きを選ぶプロファイルドックを常設。6色、タップ適用、長押し並び替え、追加保持で削除確認
- Room v2→v3 migration で既存プロファイルを維持しながら色と並び順を追加
- 日本語／英語UIを171キーずつ完全一致

Xperia 1 SOV40 で Pro／通常12回、縦横9回の連続切替、4:3／16:9、モードバー永続化、プロファイル操作、静止画、動画を実機確認しました。テスト中にプロセス再起動、FATAL、PreviewView親競合、camera bind失敗はありませんでした。

## English

The preview lifecycle has been rebuilt so repeated Pro/standard and portrait/landscape transitions no longer stall or corrupt the live view. The Photography Pro-inspired workspace now includes Fcam pro's own customizable mode bar and color-coded capture profiles.

- One `PreviewView` and one movable host across every layout, eliminating surface reparenting and unnecessary camera rebinds
- Exact 4:3 / 16:9 fit inside available space; the preview rectangle keeps its shape while only UI placement and capture rotation change
- Photo / Video / Others by default, with drag-to-add Slow motion and Panorama, bar reordering and drag-back removal
- A permanent profile dock with icon-only save, New / Overwrite choice, six colors, tap to apply, long-drag reorder and extended-hold deletion confirmation
- Explicit Room v2→v3 migration preserves existing profiles while adding color and order
- Complete English/Japanese resource parity at 171 keys each

Tested on Xperia 1 SOV40 with 12 Pro/standard transitions, nine forced rotations, both aspect ratios, persistent mode customization, profile workflows, still capture and video. No process restart, fatal exception, PreviewView parent conflict or camera bind failure occurred.
