# TODO.md

## 🚧 進行中

- Fabric カメラエンティティ設計・実装 (Phase 1)

---

## 🔜 次にやること

### カメラ実装 (Phase 1)

- [ ] Fabric Rendering API でオフスクリーンフレームバッファを取得する方法を調査
- [ ] カメラエンティティ (`CameraEntity.java`) の骨格実装
  - エンティティ登録 (`ModEntities.java`)
  - `ctrl` トピック購読 → 解像度・FOV・pan/tilt パラメータ保持
  - サーバーTickでパラメータを更新、クライアントTickでレンダリングをトリガー
- [ ] JPEG エンコード (Java: `javax.imageio` or サードパーティライブラリ)
- [ ] `frame` トピックへのパブリッシュ
- [ ] M5StickC Plus デモスクリプト (`m5flow/examples/04_camera.py`)
- [ ] `mqtt-monitor.html` にカメラプレビューセクション追加

---

## 📋 バックログ

### ツール・デバッグ
- [ ] `mqtt-monitor.html`: カメラフレームのサムネイル表示
- [ ] `mqtt-monitor.html`: mob/env 等の将来トピックを表示できる拡張可能なレイアウト

### ライブラリ
- [ ] `minecraft_mqtt.py`: 再接続 (umqtt.robust への自動フォールバック強化)
- [ ] `minecraft_mqtt.py`: mob/env トピックのサブスクライブ API 追加
- [ ] UIFlow v2 カスタムブロック: カメラブロックの動作確認・調整

### MOD
- [ ] チャンネル数の上限 (現在99) を設定ファイルで変更可能にする
- [ ] モブセンサーブロック実装 (Phase 2)
- [ ] 環境センサー実装 (Phase 2)

### ドキュメント・教材
- [ ] README: カメラ機能のセクション追加 (Phase 1完了後)
- [ ] サンプル動画/GIF: カメラストリーミングのデモ

### 将来 (Phase 3+)
- [ ] 実世界カメラ → Minecraftマップアート投影 (Python スクリプト)
- [ ] アナログ値チャンネル設計・実装
- [ ] M5Dial カメラコントローラーデモ
- [ ] リモートブローカー対応ドキュメント

---

## ✅ 完了

- Fabric MOD 基盤 (MQTTクライアント、設定ファイル)
- チャンネルIN/OUTブロック (boolean sig、ID 1〜99)
- MicroPythonライブラリ (`minecraft_mqtt.py`) — sig + cam API
- UIFlow v2 カスタムブロック (`MinecraftBridge.py`)
- MicroPythonサンプル 3本 (LED, ボタン, 双方向)
- MQTT監視ツール (`mqtt-monitor.html`) — 横長/縦長レイアウト切り替え
- トピック命名を `mp/bridge/...` 体系に移行
- `mc_setup` の引数順バグ修正 (broker_ip 第1引数に統一)
- SPEC.md / PLAN.md / TODO.md / KNOWLEDGE.md 作成
