# minecraft-physical

MinecraftのレッドストーンをM5StackデバイスのGPIOと双方向にブリッジするFabric MODプロジェクト。
電子工作入門教材として設計されており、UIFlowのビジュアルブロックからMicroPythonへ段階的に学習できる。

## プロジェクト構成

```
minecraft-physical/
├── mod/       # Fabric MOD (Minecraft 1.21.4, Java 21)
│              # MQTTクライアント内蔵。チャンネルIN/OUTブロックを追加する。
└── m5flow/    # UIFlow v2 カスタムブロック拡張 + MicroPythonサンプル
               # UIFlow対応M5Stackデバイス全般で動作（機種非依存）
               # ユーザーは物理I/Oを自由に組み合わせる
```

## アーキテクチャ

```
[PC/Mac]
  ├── Minecraft Java Edition
  │     └── Fabric MOD (MQTTクライアント: Eclipse Paho)
  └── Mosquitto MQTTブローカー (localhost:1883)
              ↕ MQTT over WiFi
        [M5Stackデバイス]
          └── UIFlow v2 / MicroPython
              ↕ GPIO / I2C / UART
        [物理デバイス: LED, スイッチ, etc.]
```

## Minecraftカスタムブロック仕様

Minecraft内のブロックは「チャンネル番号」で識別される抽象的な信号の端点。
物理的に何に繋がるかはM5Stack側（UIFlow）のユーザーが自由に決める。

### チャンネルINブロック (M5Stack → Minecraft)
- M5StackからMQTTでパブリッシュされた値を受信し、レッドストーン信号として出力
- 右クリックでチャンネル番号（1〜99）を設定するGUIを開く
- ONのとき光る（ルミナンス 15）

### チャンネルOUTブロック (Minecraft → M5Stack)
- レッドストーン信号が入力されたとき、MQTTでパブリッシュ
- 右クリックでチャンネル番号（1〜99）を設定するGUIを開く
- ONのとき光る（ルミナンス 15）

### チャンネルの独立性
- INとOUTは独立しており、同じチャンネル番号をINとOUTの両方に設定してよい
- `minecraft/ch/{n}/input` と `minecraft/ch/{n}/state` は別トピックなので衝突しない

## MQTTトピック設計

プレフィックス: `mp/bridge`（mp = minecraft-physical）

```
mp/bridge/m/sig/{id}          # Minecraft OUTブロック → デバイス    (OUTブロックが発行)
mp/bridge/p/sig/{id}          # デバイス → Minecraft INブロック     (INブロックが購読)
mp/bridge/m/cam/{id}/frame    # Minecraftカメラ映像 → デバイス      (JPEG バイナリ)
mp/bridge/m/cam/{id}/ctrl     # デバイス → Minecraftカメラ制御      (JSON)
```

- `m` = Minecraft 世界、`p` = Physical（実世界）
- 将来的に `mp/bridge/{instance}/...` でマルチサーバー対応可能

### カメラ ctrl ペイロード (JSON)

```json
{ "width": 240, "height": 135, "fps": 1.0, "pan": 0.0, "tilt": 0.0, "fov": 70.0 }
```

サブスクライバ（デバイス）が起動時に希望解像度・FPSをパブリッシュし、
以降 MOD はその解像度でフレームを送出する。

例: チャンネル1のOUTブロックがONになると `mp/bridge/m/sig/1` に `"1"` をパブリッシュ。
デバイスがこれをSubscribeしてLEDを光らせる。

## 設定ファイル

`config/gpio_bridge.json` (Minecraftのconfigディレクトリ):
```json
{
  "brokerHost": "localhost",
  "brokerPort": 1883,
  "clientId": "minecraft-mod",
  "reconnectDelayMs": 5000
}
```

## 開発環境・バージョン

| コンポーネント | バージョン |
|---|---|
| Minecraft Java Edition | 1.21.4 |
| Fabric Loader | 0.16.9 |
| Fabric API | 0.115.0+1.21.4 |
| Java | 21 |
| Eclipse Paho MQTT (Java) | 1.2.5 |
| Mosquitto | 2.x |
| M5Stackデバイス (UIFlow v2対応機種) | UIFlow v2 / MicroPython (MicroPython 1.21+) |

## ハードウェア

### 対応M5Stackデバイス（UIFlow v2対応機種全般）
- M5Stack Core / Core2 / CoreS3
- M5StickC / M5StickC Plus / M5StickC Plus2
- M5ATOM Lite / Matrix
- M5Cardputer
- その他UIFlow v2対応デバイス

物理I/Oはユーザーが自由に選択（M5Stack Unitシリーズ、直接GPIO配線、I2C/UARTデバイスなど）

## 学習ステップ（教材設計）

1. UIFlowのMQTTブロックでMinecraftと接続（ビジュアルプログラミング）
2. ボタン入力 → Minecraft信号、Minecraft信号 → LED制御
3. UIFlowのPythonモードでMicroPythonコードを確認・編集
4. センサー(温度・加速度)の値をMinecraftに送り込む応用

## セットアップ手順（概要）

### PC側
1. Mosquittoをインストール・起動 (`brew install mosquitto`)
2. Minecraftに本MODを導入
3. `config/gpio_bridge.json` でブローカーアドレスを設定

### M5Stackデバイス側
1. UIFlow v2でWiFi設定とブローカーアドレスを設定
2. `m5flow/` 内のサンプルを読み込む
