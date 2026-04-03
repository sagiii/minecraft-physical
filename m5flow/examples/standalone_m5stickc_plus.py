"""
M5StickC Plus 用スタンドアロンサンプル
=======================================
UIFlow・外部ライブラリ不要。MicroPython エディタに貼り付けて実行できる。

動作:
  - ボタンA 短押し → チャンネル1 の IN ブロックを ON/OFF トグル
  - チャンネル2 の OUT ブロックが ON → 赤LED点灯 / OFF → 消灯
  - 接続が切れたら自動再接続する

接続構成:
  M5StickC Plus --WiFi--> Mosquitto (PC) <---> Minecraft MOD
"""

import network
import time
from machine import Pin
from umqtt.simple import MQTTClient

# ============================================================
# ★ ここだけ書き換えてください
# ============================================================
WIFI_SSID     = 'YOUR_SSID'
WIFI_PASSWORD = 'YOUR_PASSWORD'
BROKER_IP     = '192.168.1.10'   # PC の IP アドレス (ifconfig/ipconfig で確認)
BROKER_PORT   = 1883

CH_SEND = 1   # このデバイスが送信するチャンネル (Minecraft の IN ブロック)
CH_RECV = 2   # このデバイスが受信するチャンネル (Minecraft の OUT ブロック)
# ============================================================

# M5StickC Plus ピン定義
LED_PIN  = 10   # 赤LED (LOW=点灯)
BUTTON_A = 37   # 前面ボタン (LOW=押下)

led   = Pin(LED_PIN,  Pin.OUT, value=1)
btn_a = Pin(BUTTON_A, Pin.IN)

send_state = False
client = None


def show(line1, line2=''):
    print('[MC]', line1, line2)


# ------------------------------------------------------------------
# WiFi 接続（切れていたら繋ぎ直す）
# ------------------------------------------------------------------
def ensure_wifi():
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    if wlan.isconnected():
        return True
    show('WiFi...', WIFI_SSID)
    wlan.connect(WIFI_SSID, WIFI_PASSWORD)
    for _ in range(20):
        if wlan.isconnected():
            show('WiFi OK', wlan.ifconfig()[0])
            return True
        time.sleep(1)
    show('WiFi FAIL')
    return False


# ------------------------------------------------------------------
# MQTT 接続（再接続にも使う）
# ------------------------------------------------------------------
def on_message(topic, msg):
    parts = topic.decode().split('/')
    if len(parts) == 4 and parts[3] == 'state':
        ch  = int(parts[2])
        val = (msg == b'1')
        if ch == CH_RECV:
            led.value(0 if val else 1)   # LOW=点灯
            show('recv ch%d' % ch, 'ON' if val else 'OFF')


def connect_mqtt():
    global client
    show('MQTT...', BROKER_IP)
    try:
        if client:
            try:
                client.disconnect()
            except Exception:
                pass
        client = MQTTClient('m5stickc', BROKER_IP, port=BROKER_PORT, keepalive=30)
        client.set_callback(on_message)
        client.connect()
        client.subscribe(('minecraft/ch/%d/state' % CH_RECV).encode())
        show('MQTT OK', 'BtnA=toggle')
        return True
    except Exception as e:
        show('MQTT FAIL', str(e))
        client = None
        return False


# ------------------------------------------------------------------
# メイン
# ------------------------------------------------------------------
ensure_wifi()
connect_mqtt()

prev_a    = btn_a.value()
ping_tick = time.ticks_ms()
PING_MS   = 20_000   # 20秒ごとに ping してキープアライブを維持

while True:
    # WiFi が切れていたら再接続
    if not network.WLAN(network.STA_IF).isconnected():
        show('WiFi lost')
        ensure_wifi()
        connect_mqtt()
        prev_a = btn_a.value()
        continue

    # ボタンA: 押した瞬間（立ち下がり）にトグル送信
    cur_a = btn_a.value()
    if prev_a == 1 and cur_a == 0:
        send_state = not send_state
        payload = b'1' if send_state else b'0'
        topic   = ('minecraft/ch/%d/input' % CH_SEND).encode()
        if client:
            try:
                client.publish(topic, payload)
                show('send ch%d' % CH_SEND, 'ON' if send_state else 'OFF')
            except Exception as e:
                show('send ERR', str(e))
                connect_mqtt()
        time.sleep_ms(50)   # チャタリング防止
    prev_a = cur_a

    # 受信チェック + 定期 ping でキープアライブ
    if client:
        try:
            client.check_msg()
            if time.ticks_diff(time.ticks_ms(), ping_tick) > PING_MS:
                client.ping()
                ping_tick = time.ticks_ms()
        except Exception as e:
            show('conn lost', str(e))
            time.sleep(2)
            connect_mqtt()
            ping_tick = time.ticks_ms()

    time.sleep_ms(20)
