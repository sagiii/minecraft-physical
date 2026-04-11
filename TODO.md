# TODO.md

## Phase 0 — 基盤 ✅

- [x] Fabric MOD 基盤 (MQTTクライアント、設定ファイル)
- [x] チャンネルIN/OUTブロック (boolean sig、ID 1〜99)
- [x] MicroPythonライブラリ (`minecraft_mqtt.py`) — sig + cam API
- [x] UIFlow v2 カスタムブロック (`MinecraftBridge.py`)
- [x] MicroPythonサンプル 3本 (LED, ボタン, 双方向)
- [x] MQTT監視ツール (`mqtt-monitor.html`) — 横長/縦長レイアウト切り替え
- [x] トピック命名を `mp/bridge/...` 体系に移行
- [x] `mc_setup` の引数順バグ修正 (broker_ip 第1引数に統一)
- [x] SPEC.md / PLAN.md / TODO.md / KNOWLEDGE.md 作成
- [x] Minecraftバージョンを最新Fabricサポート版に更新

## Phase 1 — カメラストリーミング ✅

- [x] Fabric Rendering API でオフスクリーンフレームバッファを取得する方法を調査
- [x] カメラエンティティ (`CameraEntity.java`) の骨格実装
  - [x] エンティティ登録 (`ModEntities.java`)
  - [x] `ctrl` トピック購読 → 解像度・FOV・pan/tilt パラメータ保持 (SynchedEntityData)
  - [x] サーバーTickでパラメータ更新、クライアントTickでレンダリングトリガー
- [x] JPEG エンコード (Java: `javax.imageio`)
- [x] `frame` トピックへのパブリッシュ (LevelRenderEvents.END_MAIN フック)
- [x] M5StickC Plus デモスクリプト (`m5flow/examples/04_camera.py`)
- [x] `mqtt-monitor.html` にカメラプレビューセクション追加

> **Phase 1 制限事項**: 現在のフレームキャプチャはプレイヤーの視点 (メインフレームバッファ)
> を使用。Phase 1b でカメラエンティティの座標からのオフスクリーンレンダリングに対応予定。

## Phase 1b — エンティティ視点レンダリング 📋 (保留中)

> **調査結果メモ (2026-04-11)**:
> MC 26.x は描画パイプラインが「状態抽出→描画」分離モデルになっており、
> 任意の視点でのオフスクリーン描画は難易度が高い。
> 主な選択肢と評価:
> - **A. 同一GLコンテキスト内で二重描画**: 描画コスト2倍、MC 26.x パイプラインで複雑
> - **B. 共有リソースの別GLコンテキスト**: ちらつきなし、LWJGLコンテキスト共有が難しい
> - **C. FPS制限で現状妥協**: 1fps以下なら実用上問題ない（現在の実装）
> - **D. Mineflayer + prismarine-viewer**: offline-modeならアカウント不要、
>   Three.js近似描画、シングルプレイではLANサーバー開放が必要
> 現状はCで運用し、リアルタイム映像が必要になった時点で再検討する。

- [ ] オフスクリーン描画方式の選定・実装（上記調査結果を参照）

## Phase 2 — 環境・モブセンサー 📋

- [ ] モブセンサーブロック実装 (`mp/bridge/m/mob/{id}`)
- [ ] 環境センサー実装 (`mp/bridge/m/env/{id}`) — 天候・時刻・バイオーム
- [ ] M5Stack向けサンプル (モブ接近でバイブレーション、天気同期LEDなど)

## Phase 3 — 双方向リッチデータ 📋

- [ ] アナログ値チャンネル設計・実装 (`mp/bridge/p/sensor/{id}`)
- [ ] 実世界カメラ → Minecraftマップアート投影 (Python スクリプト)
- [ ] テキスト/チャット連携
- [ ] M5Dial カメラコントローラーデモ (ロータリーエンコーダー → pan/zoom)

## Phase 4 — スケールアップ 📋

- [ ] リモートMQTTブローカー対応ドキュメント (HiveMQ Cloud等)
- [ ] マルチインスタンス対応 (`mp/bridge/{instance}/...`)
- [ ] Web UI 強化 (カメラ表示、センサーグラフ、テラリウムビュー)

## バックログ (フェーズ未定)

- [ ] チャンネル数の上限 (現在99) を設定ファイルで変更可能にする
- [ ] `minecraft_mqtt.py`: 再接続ロジック強化
- [ ] README: カメラ機能のセクション追加 (Phase 1完了後)
