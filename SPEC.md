# SPEC.md — プロトコル・API仕様

## 1. MQTTトピック設計

### 命名規則

```
mp/bridge/{world}/{type}/{id}[/{subtopic}]
```

| セグメント | 値 | 意味 |
|---|---|---|
| `mp` | 固定 | minecraft-physical プロジェクト識別子 |
| `bridge` | 固定 | ブリッジコンポーネント（将来の拡張余地） |
| `{world}` | `m` / `p` | `m`=Minecraftワールド起点、`p`=Physical(実世界)起点 |
| `{type}` | `sig` `cam` `mob` `env` … | データ種別 |
| `{id}` | 整数または文字列 | インスタンス識別子 |
| `{subtopic}` | `frame` `ctrl` … | 双方向通信の方向を区別するサブトピック |

将来のマルチサーバー対応: `mp/bridge/{instance}/{world}/...` に拡張可能。

---

### 2. データ種別一覧

#### 2-1. sig — boolean信号 ✅ 実装済み

MinecraftのレッドストーンON/OFFと実世界のデジタル信号を双方向に接続する。

| トピック | 発行者 | ペイロード | 説明 |
|---|---|---|---|
| `mp/bridge/m/sig/{id}` | MOD (OUTブロック) | `"1"` / `"0"` | Minecraft → Physical |
| `mp/bridge/p/sig/{id}` | デバイス (INブロック) | `"1"` / `"0"` | Physical → Minecraft |

- `id` = 1〜99（現在のMOD制約。将来変更可能）
- INとOUTは独立 — 同一 `id` をIN/OUTの両方に設定してよい

---

#### 2-2. cam — カメラ映像 🚧 設計済み・MOD未実装

Minecraftワールド内に設置したカメラエンティティの映像をデバイスに配信する。
デバイス側から解像度・向き・FPSを指定できる双方向プロトコル。

| トピック | 発行者 | ペイロード | 説明 |
|---|---|---|---|
| `mp/bridge/m/cam/{id}/frame` | MOD (カメラエンティティ) | JPEG バイナリ | Minecraft → デバイス |
| `mp/bridge/m/cam/{id}/ctrl` | デバイス | JSON (下記) | デバイス → Minecraft |

**ctrl ペイロード:**
```json
{
  "width":  240,
  "height": 135,
  "fps":    1.0,
  "pan":    0.0,
  "tilt":   0.0,
  "fov":    70.0
}
```

- `width` / `height`: 希望解像度 (px)。MODはこのサイズでJPEGを生成する
- `fps`: 希望フレームレート。MODは最大この頻度でframeをパブリッシュする
- `pan` / `tilt`: カメラ水平・垂直角度 (度)
- `fov`: 視野角 (度)。小さいほどズームイン

**フロー:**
1. デバイスが `ctrl` をパブリッシュ（起動時 + 操作時）
2. MODが `ctrl` を受信し、解像度・角度を更新
3. MODが指定FPSで `frame` をパブリッシュし続ける

**対象デバイス (初期):** M5StickC Plus — 横向き 240×135px

---

#### 2-3. mob — モブセンサー 📋 計画中

Minecraftワールド内のエンティティ情報をデバイスに通知する。

| トピック | 発行者 | ペイロード | 説明 |
|---|---|---|---|
| `mp/bridge/m/mob/{id}` | MOD | JSON | 近傍モブ情報 |

**想定ペイロード:**
```json
{
  "type":     "zombie",
  "distance": 8.2,
  "hp":       20,
  "x": 128, "y": 64, "z": -30
}
```

---

#### 2-4. env — 環境情報 📋 計画中

Minecraftワールドの環境状態をデバイスに通知する。

| トピック | 発行者 | ペイロード | 説明 |
|---|---|---|---|
| `mp/bridge/m/env/{id}` | MOD | JSON | 天候・時刻・バイオーム等 |

**想定ペイロード:**
```json
{
  "weather": "rain",
  "time":    14000,
  "biome":   "forest",
  "light":   7
}
```

---

#### 2-5. sensor — 実世界アナログセンサー 📋 計画中

実世界のセンサー値をMinecraftワールドに伝える。

| トピック | 発行者 | ペイロード | 説明 |
|---|---|---|---|
| `mp/bridge/p/sensor/{id}` | デバイス | JSON | センサー値 → Minecraft |

---

## 3. ライブラリAPI (`m5flow/libs/minecraft_mqtt.py`)

### セットアップ

```python
mc_setup(broker_ip, ssid=None, password=None, broker_port=1883)
```

### 信号 (sig)

```python
mc_on(sig_id, callback)          # Minecraft → デバイス: コールバック登録
mc_send(sig_id, value)           # デバイス → Minecraft: 信号送信
mc_check()                       # メインループ内で必ず呼ぶ
```

### カメラ (cam)

```python
mc_cam_start(cam_id, width=240, height=135, fps=1.0, callback=None)
# Minecraftカメラにサブスクライブ + 解像度をMODに通知

mc_cam_ctrl(cam_id, pan=None, tilt=None, fov=None, fps=None)
# カメラ操作（指定しないパラメータは現在値を維持）

mc_cam_display(cam_id, x=0, y=0)
# 最後のフレームをLCDに表示（mc_check()の後に呼ぶ）

mc_cam_frame(cam_id) -> bytes | None
# 最後のフレームのJPEGバイト列を返す（独自表示処理用）
```

### クラスAPI (高度な使い方)

```python
from libs.minecraft_mqtt import MqttBridge
mc = MqttBridge(broker_ip, broker_port=1883, client_id='m5stack')
mc.connect_wifi(ssid, password)
mc.connect_mqtt()
mc.on_sig(id, cb) / mc.send_sig(id, val) / mc.get_sig(id)
mc.cam_start(...) / mc.cam_ctrl(...) / mc.cam_display(...) / mc.cam_frame(...)
mc.check()
```

---

## 4. MODブロック仕様 (Fabric)

### チャンネルINブロック (Physical → Minecraft)

- `mp/bridge/p/sig/{id}` をSubscribe
- 受信値が `"1"` のときレッドストーン信号を出力 (パワー15)
- 右クリックでID設定GUI (1〜99)
- ONのとき発光 (ルミナンス 15)

### チャンネルOUTブロック (Minecraft → Physical)

- レッドストーン信号入力時に `mp/bridge/m/sig/{id}` をPublish
- 右クリックでID設定GUI (1〜99)
- ONのとき発光 (ルミナンス 15)

### カメラエンティティ (未実装)

- Minecraftワールド内に設置可能なエンティティ
- `mp/bridge/m/cam/{id}/ctrl` をSubscribeし、解像度・向き・FPSを更新
- 指定FPSでオフスクリーンレンダリング → JPEG圧縮 → `mp/bridge/m/cam/{id}/frame` をPublish
- エンティティのため移動・視点変更可能

---

## 5. ツール

### tools/mqtt-monitor.html

- ブラウザベースのMQTTデバッグ・学習ツール
- `mp/bridge/p/sig/+` (INPUTグリッド・青) と `mp/bridge/m/sig/+` (OUTputグリッド・赤) を可視化
- 各セルをクリックして手動publish可能
- WebSocket経由でMosquittoに接続 (ポート9001)
- 横長レイアウト: 左右並び / 縦長: 上下並び
