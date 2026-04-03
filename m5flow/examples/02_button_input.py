"""
例02: ボタン → Minecraft
=========================
M5Stack のボタンを押すと、Minecraft の INブロックにON信号を送る。
ボタンを離すと OFF 信号を送る。

【Minecraft側の準備】
1. チャンネルINブロックを設置
2. 右クリック → チャンネル番号を「1」に設定
3. M5Stack のボタンを押すと、INブロックがレッドストーン信号を出力する

【M5Stack側の配線】
- M5StickC Plus の BtnA (GPIO37) を使用 (変更可能)
- 内蔵ボタンはプルアップ済みなので配線不要

【設定方法】
下記 CONFIG の値を自分の環境に合わせて変更してください。
"""

# ============================================================
# CONFIG — ここを変更する
# ============================================================
WIFI_SSID     = 'YOUR_WIFI_SSID'
WIFI_PASSWORD = 'YOUR_WIFI_PASSWORD'
BROKER_IP     = '192.168.1.xxx'
CHANNEL       = 1     # 対応する Minecraft チャンネル番号
BTN_PIN       = 37    # ボタンの GPIO ピン番号 (M5StickC Plus: BtnA=37)
# ============================================================

from machine import Pin
import time
import sys
sys.path.append('/flash')
from lib.minecraft_mqtt import mc_setup, mc_send, mc_check

# ボタンの初期化 (内蔵プルアップ、押すとLOW)
btn = Pin(BTN_PIN, Pin.IN, Pin.PULL_UP)

last_state = True  # 前回のボタン状態 (True=離している)

# セットアップ
mc_setup(WIFI_SSID, WIFI_PASSWORD, BROKER_IP)

print('準備完了。ボタンを押すと Minecraft にON信号を送ります。')

# メインループ
while True:
    mc_check()

    current = btn.value()  # 押している=0(False), 離している=1(True)

    if current != last_state:
        pressed = (current == 0)
        mc_send(CHANNEL, pressed)  # 押した=ON, 離した=OFF
        print('送信:', 'ON' if pressed else 'OFF')
        last_state = current

    time.sleep_ms(20)  # チャタリング防止
