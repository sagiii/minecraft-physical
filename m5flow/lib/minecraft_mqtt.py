"""
minecraft_mqtt.py  —  Minecraft ↔ M5Stack MQTT ブリッジライブラリ
======================================================================
MicroPython (ESP32) 向けライブラリ。
umqtt.simple は MicroPython 標準内蔵なので追加インストール不要。

トピック設計:
  minecraft/ch/{n}/state   Minecraft OUTブロック → M5Stack  (受信)
  minecraft/ch/{n}/input   M5Stack → Minecraft INブロック   (送信)

使い方 (クラス版):
    from lib.minecraft_mqtt import MinecraftBridge
    mc = MinecraftBridge('192.168.1.10')
    mc.connect_wifi('MySSID', 'password')
    mc.connect_mqtt()
    mc.on_channel(1, lambda v: print('ch1:', v))
    while True:
        mc.check()

使い方 (関数版 / UIFlow向け):
    from lib.minecraft_mqtt import mc_setup, mc_send, mc_on, mc_check
    mc_setup('MySSID', 'password', '192.168.1.10')
    mc_on(1, lambda v: print('ch1:', v))
    # ループ内で mc_check() を呼ぶ
"""

import network
import time

try:
    from umqtt.simple import MQTTClient
except ImportError:
    from umqtt.robust import MQTTClient


class MinecraftBridge:
    """Minecraft MQTT ブリッジのコアクラス。"""

    TOPIC_STATE = 'minecraft/ch/{}/state'   # 受信用トピック
    TOPIC_INPUT = 'minecraft/ch/{}/input'   # 送信用トピック

    def __init__(self, broker_ip, broker_port=1883, client_id='m5stack'):
        self._broker_ip = broker_ip
        self._broker_port = broker_port
        self._client_id = client_id
        self._client = None
        self._callbacks = {}   # channel(int) -> callable(bool)
        self._values = {}      # channel(int) -> bool  (最後に受信した値)
        self._connected = False

    # ------------------------------------------------------------------
    # 接続
    # ------------------------------------------------------------------

    def connect_wifi(self, ssid, password, timeout=20):
        """WiFi に接続する。成功すれば True を返す。"""
        wlan = network.WLAN(network.STA_IF)
        wlan.active(True)
        if wlan.isconnected():
            return True
        print('[Minecraft] WiFi 接続中:', ssid)
        wlan.connect(ssid, password)
        for _ in range(timeout):
            if wlan.isconnected():
                print('[Minecraft] WiFi 接続成功:', wlan.ifconfig()[0])
                return True
            time.sleep(1)
        print('[Minecraft] WiFi 接続タイムアウト')
        return False

    def connect_mqtt(self):
        """MQTT ブローカーへ接続し、全チャンネルを購読する。"""
        self._client = MQTTClient(
            self._client_id,
            self._broker_ip,
            port=self._broker_port,
            keepalive=60,
        )
        self._client.set_callback(self._on_message)
        self._client.connect()
        # ワイルドカードで全チャンネルを一括購読
        self._client.subscribe(b'minecraft/ch/+/state')
        self._connected = True
        print('[Minecraft] MQTT 接続成功:', self._broker_ip)

    # ------------------------------------------------------------------
    # チャンネル登録 / 送受信
    # ------------------------------------------------------------------

    def get_value(self, channel):
        """
        指定チャンネルの最後に受信した値を返す。

        Args:
            channel (int): チャンネル番号 (1-99)
        Returns:
            bool: ON=True / OFF=False。未受信の場合は False。
        """
        return self._values.get(int(channel), False)

    def on_channel(self, channel, callback):
        """
        指定チャンネルの状態変化コールバックを登録する。

        Args:
            channel  (int):           チャンネル番号 (1-99)
            callback (callable(bool)): ON=True / OFF=False で呼ばれる
        """
        self._callbacks[int(channel)] = callback

    def send(self, channel, value):
        """
        Minecraft の INブロックにチャンネル信号を送る。

        Args:
            channel (int):  チャンネル番号 (1-99)
            value   (bool): True=ON / False=OFF
        """
        if not self._client:
            return
        topic   = self.TOPIC_INPUT.format(int(channel)).encode()
        payload = b'1' if value else b'0'
        try:
            self._client.publish(topic, payload)
        except Exception as e:
            print('[Minecraft] 送信エラー:', e)

    def check(self):
        """
        受信メッセージを処理する。メインループ内で毎回呼ぶこと。
        """
        if not self._client:
            return
        try:
            self._client.check_msg()
        except Exception as e:
            print('[Minecraft] 受信エラー:', e)

    # ------------------------------------------------------------------
    # 内部処理
    # ------------------------------------------------------------------

    def _on_message(self, topic, msg):
        """MQTT メッセージ受信ハンドラ (内部)。"""
        try:
            # topic 例: b'minecraft/ch/1/state'
            parts = topic.decode().split('/')
            if len(parts) == 4 and parts[0] == 'minecraft' and parts[3] == 'state':
                channel = int(parts[2])
                value   = (msg == b'1')
                self._values[channel] = value
                cb = self._callbacks.get(channel)
                if cb:
                    cb(value)
        except Exception as e:
            print('[Minecraft] メッセージ解析エラー:', e)


# ======================================================================
# UIFlow 向けグローバル関数 API
# UIFlow v2 の Custom ブロックからこれらの関数を呼ぶ。
# ======================================================================

_bridge: MinecraftBridge = None  # type: ignore


def mc_setup(broker_ip: str, broker_port: int = 1883, ssid: str = None, password: str = None):
    """
    [ブロック: セットアップ]
    MQTT ブローカーに接続する。プログラムの最初に一度だけ呼ぶ。
    ssid/password を指定した場合のみ WiFi 接続を行う。
    すでに WiFi に接続済みの場合は ssid/password を省略できる。

    Args:
        broker_ip  (str): PC の IP アドレス (Mosquitto が動いている PC)
        broker_port(int): MQTTポート番号 (通常は 1883)
        ssid       (str): WiFi のSSID (省略可)
        password   (str): WiFi のパスワード (省略可)
    """
    global _bridge
    _bridge = MinecraftBridge(broker_ip, broker_port)
    if ssid and password:
        _bridge.connect_wifi(ssid, password)
    elif not network.WLAN(network.STA_IF).isconnected():
        raise RuntimeError('[Minecraft] WiFi未接続です。ssidとpasswordを指定してください。')
    _bridge.connect_mqtt()


def mc_on(channel: int, callback):
    """
    [ブロック: チャンネル受信時]
    Minecraft の OUTブロックが ON/OFF になったときのコールバックを登録する。

    Args:
        channel  (int):           チャンネル番号 (1-99)
        callback (callable(bool)): ON=True / OFF=False で呼ばれる関数
    """
    if _bridge:
        _bridge.on_channel(channel, callback)


def mc_send(channel: int, value: bool):
    """
    [ブロック: チャンネルに送信]
    Minecraft の INブロックに信号を送る。

    Args:
        channel (int):  チャンネル番号 (1-99)
        value   (bool): True=ON / False=OFF
    """
    if _bridge:
        _bridge.send(channel, value)


def mc_check():
    """
    [ブロック: メッセージ確認]
    受信メッセージを処理する。メインループ内で必ず毎回呼ぶこと。
    """
    if _bridge:
        _bridge.check()
