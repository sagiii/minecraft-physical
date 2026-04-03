"""
UIFlow v2 カスタムブロック定義 (クラス形式)
=============================================
【UIFlow v2 への登録手順】
1. UIFlow v2 (flow2.m5stack.com) を開く
2. 左パネルの「Custom」タブ → 「+ New Class」
3. このファイルの内容を貼り付けて保存
4. ブロックパネルに「MinecraftBridge」カテゴリが追加される

【前提条件】
- lib/minecraft_mqtt.py を M5Stack にアップロード済みであること
  (UIFlow v2 の「Files」タブ → /flash/lib/ フォルダに配置)

【UIFlow v2 での使い方イメージ】

  セットアップ:
    mc = MinecraftBridge()
    mc.接続する('MySSID', 'password', '192.168.1.10')
    mc.チャンネル受信時(1, lambda v: led.value(1 if v else 0))

  ループ:
    mc.メッセージ確認()

  イベント(ボタン押下など):
    mc.チャンネルに送信(2, True)
"""

import sys
sys.path.append('/flash')
from lib.minecraft_mqtt import MinecraftBridge as _Bridge


class MinecraftBridge:
    """Minecraft ↔ M5Stack MQTT ブリッジ"""

    def __init__(self):
        self._bridge = None

    def 接続する(self, ssid, password, broker_ip, broker_port=1883):
        """
        WiFi と MQTT ブローカーに接続する。
        プログラムの最初に一度だけ呼ぶ。
        ssid: WiFiのSSID
        password: WiFiのパスワード
        broker_ip: PCのIPアドレス
        broker_port: MQTTポート番号(通常1883)
        """
        self._bridge = _Bridge(broker_ip, broker_port)
        self._bridge.connect_wifi(ssid, password)
        self._bridge.connect_mqtt()

    def チャンネルに送信(self, channel, value):
        """
        Minecraft の INブロックに信号を送る。
        channel: チャンネル番号(1-99)
        value: True=ON / False=OFF
        """
        if self._bridge:
            self._bridge.send(int(channel), bool(value))

    def チャンネル受信時(self, channel, callback):
        """
        Minecraft の OUTブロックが変化したときのコールバックを登録する。
        channel: チャンネル番号(1-99)
        callback: ON=True / OFF=False を受け取る関数
        """
        if self._bridge:
            self._bridge.on_channel(int(channel), callback)

    def メッセージ確認(self):
        """
        受信メッセージを処理する。
        必ずループブロックの中に入れること。
        """
        if self._bridge:
            self._bridge.check()
