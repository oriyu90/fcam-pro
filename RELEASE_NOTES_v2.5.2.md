# Fcam pro v2.5.2

## 日本語

通常モードの縦画面プレビューを、Sony Photography ProのBasic画面に近い没入型レイアウトへ修正したメンテナンスリリースです。

- 通常の写真／動画／スローモーションで、プレビューを画面上端からコンパクト操作デッキ直前まで拡張
- レンズ選択をプレビュー下端へ重ね、マニュアル項目がない通常モードに残っていた大きな空白を解消
- 出力設定の4:3／16:9は維持し、通常プレビューは利用可能領域へ中央クロップ
- Proモードは従来の正確な4:3／16:9固定枠と設定デッキを維持
- その他／パノラマでは専用操作を隠さないよう、固定比率プレビューを維持
- 単一の`PreviewView`を再利用するため、モード切替や回転でCameraXを再bindせず、既存の停止・クラッシュ対策を維持

Xperia 1で通常／Proの反復切替、写真／その他の切替、縦横回転とプレビュー継続を確認しました。29件の単体テスト、Android Lint、正式鍵署名Releaseビルドも成功しています。

## English

This maintenance release gives standard portrait capture an immersive viewfinder inspired by Sony Photography Pro's Basic screen.

- Expands the standard photo, video and slow-motion preview from the top edge to the compact control deck
- Overlays lens choices at the preview's lower edge and removes the large gap left by absent manual controls
- Keeps the selected 4:3 / 16:9 output setting while center-cropping the standard preview into the available space
- Preserves Pro mode's exact 4:3 / 16:9 frame and manual control deck
- Retains a fixed-ratio preview in Others and Panorama so mode-specific actions stay visible
- Reuses the single `PreviewView`, preserving the existing no-rebind safety across mode and orientation changes

Verified on Xperia 1 with repeated standard/Pro transitions, Photo/Others transitions, portrait/landscape changes and a continuously live preview. All 29 unit tests, Android Lint and the upload-key-signed release build pass.
