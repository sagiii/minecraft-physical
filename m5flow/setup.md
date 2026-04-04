# M5Stack セットアップ手順

## 必要なもの

- UIFlow v2 対応の M5Stack デバイス
- PC (Minecraft + Mosquitto MQTT ブローカーが動くもの)
- 同じ WiFi に接続できる環境

## Step 1: Mosquitto をインストール (PC側)

```bash
# macOS
brew install mosquitto
brew services start mosquitto

# Ubuntu/Debian
sudo apt install mosquitto mosquitto-clients
sudo systemctl start mosquitto
```

インストール後、PC の IP アドレスを確認しておく:

```bash
# macOS
ipconfig getifaddr en0

# Linux
hostname -I | awk '{print $1}'
```

## Step 2: ライブラリを M5Stack にアップロード

1. UIFlow v2 (flow2.m5stack.com) を開く
2. デバイスを接続する
3. 左パネルの「Files」タブを開く
4. `/flash/libs/` フォルダを作成
5. `m5flow/libs/minecraft_mqtt.py` をアップロード

## Step 3: サンプルプログラムを書き込む

1. UIFlow v2 で「Python」タブを選ぶ
2. `m5flow/examples/` の好きなサンプルをコピー
3. CONFIG セクションの値を自分の環境に書き換える:
   - `WIFI_SSID` / `WIFI_PASSWORD`
   - `BROKER_IP`: PC の IP アドレス
4. 「Run」または「Flash」で書き込む

## Step 4: Minecraft MOD を導入

1. `mod/build/libs/gpio-bridge-1.0.0.jar` を Minecraft の `mods/` フォルダにコピー
2. Minecraft を起動
3. `config/gpio_bridge.json` を確認 (初回起動時に自動生成)
   ```json
   {
     "brokerHost": "localhost",
     "brokerPort": 1883
   }
   ```

## Step 5: 動作確認

1. Minecraft でクリエイティブモードに入る
2. チャンネルOUTブロックをサバイバルインベントリから取り出す
   (コマンド: `/give @p gpio_bridge:channel_out`)
3. 右クリック → チャンネル番号「1」を入力 → Done
4. OUTブロックの隣にレッドストーントーチを置く
5. M5Stack の LED が光れば成功！

## トラブルシューティング

| 症状 | 確認ポイント |
|------|------------|
| WiFi に繋がらない | SSID/パスワードを確認。2.4GHz 帯のみ対応 |
| MQTT に繋がらない | PC の IP アドレス確認、Mosquitto 起動確認 |
| LED が光らない | チャンネル番号が一致しているか確認 |
| Minecraft でブロックが見つからない | JAR が mods/ にあるか、Fabricが入っているか確認 |
