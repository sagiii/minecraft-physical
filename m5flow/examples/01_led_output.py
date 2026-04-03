"""
例01: Minecraft → LED
======================
Minecraft の OUTブロック (チャンネル1) が ON になったら LED を光らせる。

【Minecraft側の準備】
1. チャンネルOUTブロックを設置
2. 右クリック → チャンネル番号を「1」に設定
3. OUTブロックにレッドストーン信号を入力すると M5Stack の LED が光る

【M5Stack側の配線】
- 内蔵LED (M5StickC Plus なら GPIO10) を使う場合はそのまま動作
- 外部LED: GPIO26 → LED → GND

【設定方法】
下記 CONFIG の値を自分の環境に合わせて変更してください。
"""

# ============================================================
# CONFIG — ここを変更する
# ============================================================
WIFI_SSID     = 'YOUR_WIFI_SSID'
WIFI_PASSWORD = 'YOUR_WIFI_PASSWORD'
BROKER_IP     = '192.168.1.xxx'  # Mosquitto を動かしている PC の IP
CHANNEL       = 1                 # 対応する Minecraft チャンネル番号
LED_PIN       = 10                # LED の GPIO ピン番号
# ============================================================

from machine import Pin
import sys
sys.path.append('/flash')
from lib.minecraft_mqtt import mc_setup, mc_on, mc_check

# LED の初期化
led = Pin(LED_PIN, Pin.OUT)
led.value(0)  # 最初は消灯

def on_channel_change(value):
    """チャンネル1の状態が変わったときに呼ばれる"""
    if value:
        led.value(1)  # ON → LED 点灯
        print('LED ON')
    else:
        led.value(0)  # OFF → LED 消灯
        print('LED OFF')

# セットアップ
mc_setup(WIFI_SSID, WIFI_PASSWORD, BROKER_IP)
mc_on(CHANNEL, on_channel_change)

print('待機中... Minecraft で OUTブロックにレッドストーンを入れてください')

# メインループ
while True:
    mc_check()  # 受信メッセージを処理 (必須)
