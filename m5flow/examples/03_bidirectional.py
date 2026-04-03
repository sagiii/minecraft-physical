"""
例03: 双方向通信
================
・Minecraft OUTブロック(チャンネル1) → M5Stack LED 点灯
・M5Stack ボタン押下                → Minecraft INブロック(チャンネル2) ON

【Minecraft側の準備】
1. チャンネルOUTブロック: チャンネル番号「1」に設定
2. チャンネルINブロック:  チャンネル番号「2」に設定
3. OUTブロック→レッドストーン→M5Stack LED が光る
4. ボタン→M5Stack→INブロック→レッドストーン信号

【学習ポイント】
- Pub/Sub モデルの双方向性
- コールバック関数の書き方
- IoT デバイスの基本構造 (センサー入力 + アクチュエータ出力)

【設定方法】
下記 CONFIG を自分の環境に合わせて変更してください。
"""

# ============================================================
# CONFIG — ここを変更する
# ============================================================
WIFI_SSID       = 'YOUR_WIFI_SSID'
WIFI_PASSWORD   = 'YOUR_WIFI_PASSWORD'
BROKER_IP       = '192.168.1.xxx'

CH_OUT_TO_LED   = 1   # Minecraft OUTブロックのチャンネル → LED
CH_BTN_TO_MC    = 2   # ボタン → Minecraft INブロックのチャンネル

LED_PIN         = 10  # LED の GPIO ピン番号
BTN_PIN         = 37  # ボタンの GPIO ピン番号
# ============================================================

from machine import Pin
import time
import sys
sys.path.append('/flash')
from lib.minecraft_mqtt import mc_setup, mc_on, mc_send, mc_check

# ハードウェア初期化
led = Pin(LED_PIN, Pin.OUT)
btn = Pin(BTN_PIN, Pin.IN, Pin.PULL_UP)

led.value(0)
last_btn = True


# ----- コールバック関数 -----

def on_minecraft_out(value):
    """Minecraft OUTブロック(ch1) が変化したとき"""
    led.value(1 if value else 0)
    print('[受信] チャンネル{}: {}'.format(CH_OUT_TO_LED, 'ON' if value else 'OFF'))


# ----- セットアップ -----

mc_setup(WIFI_SSID, WIFI_PASSWORD, BROKER_IP)
mc_on(CH_OUT_TO_LED, on_minecraft_out)

print('=== 双方向ブリッジ 起動 ===')
print('Ch{}: Minecraft → LED'.format(CH_OUT_TO_LED))
print('Ch{}: ボタン → Minecraft'.format(CH_BTN_TO_MC))


# ----- メインループ -----

while True:
    mc_check()  # Minecraft からのメッセージを処理

    # ボタン状態を確認
    current = btn.value()
    if current != last_btn:
        pressed = (current == 0)
        mc_send(CH_BTN_TO_MC, pressed)
        print('[送信] チャンネル{}: {}'.format(CH_BTN_TO_MC, 'ON' if pressed else 'OFF'))
        last_btn = current

    time.sleep_ms(20)
