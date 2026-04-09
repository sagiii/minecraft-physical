# KNOWLEDGE.md — 設計判断・技術メモ

## アーキテクチャ判断

### なぜ MQTT か

- MQTTはPub/Subモデルのため、送信者と受信者が疎結合になる
- Minecraftゲーム内オブジェクトとリアルデバイスが「対等なMQTTノード」として並ぶ構造になる
- 本物のIoTプロジェクト (Home Assistant, Node-RED, AWS IoT等) と直接接続できる
- WebSocket直接接続やHTTPポーリングと比べて、リアルタイム性・スケーラビリティが高い
- **Raspberry Pi や中間サーバーは不要** — PC上のMosquittoがブローカーを兼ねる

### なぜ Fabric MOD か

- Minecraft Java Editionのサーバー・クライアント両方にアクセスできる
- カスタムブロック・エンティティを自由に追加できる
- 将来のカメラエンティティ実装にはクライアント側レンダリングAPIが必要で、Fabricが最適

### ブローカーはローカルで良い理由

- 教材用途では同一LAN内で完結するのが最もシンプルで安全
- 将来リモートブローカー (HiveMQ Cloud等) に切り替えても、トピック設計は変えなくて良い

---

## トピック命名の判断

### なぜ `mp/bridge/` プレフィックスか

- MQTTトピックはブローカー内でグローバル — Home Assistant等との衝突を避けるため固有プレフィックスが必要
- `mp` = minecraft-physical (プロジェクト識別子)
- `bridge` = ブリッジコンポーネント (将来 `rcon` 等の別コンポーネントとの区別)
- 将来 `mp/bridge/{instance}/...` でマルチサーバーに拡張できる

### なぜ `m` / `p` (Minecraft/Physical) か

- `r` (real) より `p` (physical) の方がプロジェクト名 "minecraft-**p**hysical" と一致する
- `m/sig` と `p/sig` で方向が一目でわかる

### なぜ `sig` という名前か

- redstone **sig**nal の略
- `ch` (channel) より型を表現できる（将来 `cam`, `mob`, `env` 等と並ぶ）

---

## 実装上の注意点

### `mc_setup` の引数順 (2025年4月 修正)

以前の実装: `mc_setup(broker_ip, broker_port, ssid, password)` — 関数シグネチャとドキュメント・サンプルが不一致だった。  
現在の実装: `mc_setup(broker_ip, ssid=None, password=None, broker_port=1883)` に統一。

### M5Stack での JPEG 表示

- UIFlow v2 MicroPython では `lcd.image(x, y, filepath)` が最も互換性が高い
- バイト列を直接 `lcd.image()` に渡せるかはファームウェアバージョン依存
- **現在の実装**: 一時ファイル `/flash/.mc_cam.jpg` に書き出してから表示 (確実に動く)

### Fabric カメラエンティティの設計上の制約 (調査中)

- オフスクリーンレンダリングはクライアントサイドのみ可能
- サーバーサイドからは直接ピクセルデータを取得できない
- `ctrl` トピック購読 → クライアントスレッドでレンダリング → パブリッシュ という流れになる
- 複数デバイスが同じカメラをSubscribeする場合、MODは最後の `ctrl` の解像度で1種類のframeを配信する (デバイス側でリサイズ)

### umqtt のメッセージサイズ

- ESP32の `umqtt.simple/robust` は大きなペイロードも扱えるが、デフォルト設定では受信バッファが小さい場合がある
- 240×135 JPEG (quality=25) は約 3〜8KB — 通常問題なし
- 大きな解像度を要求する場合は注意

---

## 将来の拡張に向けた設計の意図

- トピック階層の `{type}` セグメントを追加するだけで新しいデータ種別を導入できる
- ライブラリの `MqttBridge` クラスは `_on_message` 内でトピックをルーティングする構造のため、新型別の追加は `elif` を足すだけ
- `mqtt-monitor.html` はチャンネルグリッドとは独立したUIセクションを追加できる構造にすること (カメラ追加時に対応)
