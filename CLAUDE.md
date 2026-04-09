# minecraft-physical

MinecraftワールドとM5Stack等の物理デバイスをMQTTで接続する、汎用ブリッジツールボックス。
ゲーム内の出来事を実世界に伝え、実世界のセンサーや映像をゲーム内に持ち込む。

詳細は各ドキュメントを参照:
- アーキテクチャ・プロトコル仕様 → `SPEC.md`
- 実装計画・ロードマップ → `PLAN.md`
- タスク管理 → `TODO.md`
- 設計判断・技術メモ → `KNOWLEDGE.md`

## ディレクトリ構成

```
minecraft-physical/
├── mod/       Fabric MOD (Minecraft 1.21.4 / Java 21)
│              MQTTクライアント内蔵。カスタムブロックを追加する。
├── m5flow/    M5Stack向けMicroPythonライブラリ・サンプル・UIFlowカスタムブロック
├── tools/     ブラウザ用デバッグ・学習支援ツール (mqtt-monitor.html 等)
├── SPEC.md    プロトコル・API仕様
├── PLAN.md    ロードマップ
├── TODO.md    タスク一覧
└── KNOWLEDGE.md 設計判断・技術メモ
```

## アーキテクチャ概要

```
[PC/Mac]
  ├── Minecraft Java Edition
  │     └── Fabric MOD  ←→  Eclipse Paho MQTT
  └── Mosquitto MQTTブローカー (localhost:1883 / ws:9001)
              ↕ MQTT over WiFi
        [M5Stackデバイス]
          └── UIFlow v2 / MicroPython  (libs/minecraft_mqtt.py)
              ↕ GPIO / I2C / UART / カメラ / ディスプレイ
        [物理デバイス・センサー・アクチュエータ]
```

MQTTブローカーを共有することで、Minecraft世界のオブジェクトと
実世界のIoTデバイスが対等な「MQTTノード」として並ぶ構造。

## 開発環境

| コンポーネント | バージョン |
|---|---|
| Minecraft Java Edition | 1.21.4 |
| Fabric Loader | 0.16.9 |
| Fabric API | 0.115.0+1.21.4 |
| Java | 21 |
| Eclipse Paho MQTT (Java) | 1.2.5 |
| Mosquitto | 2.x |
| MicroPython (M5Stack) | 1.21+ |

## MOD設定ファイル

`config/gpio_bridge.json` (初回起動時に自動生成):
```json
{
  "brokerHost": "localhost",
  "brokerPort": 1883,
  "clientId":   "minecraft-mod",
  "reconnectDelayMs": 5000
}
```

## MQTTトピック (概要)

プレフィックス `mp/bridge` — 詳細は `SPEC.md` 参照。

```
mp/bridge/m/{type}/{id}[/subtopic]   # Minecraft起点
mp/bridge/p/{type}/{id}[/subtopic]   # Physical(実世界)起点
```

現在実装済みの type: `sig`（boolean信号）
設計済み: `cam`（カメラ映像）
