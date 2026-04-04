"""
UIFlow v2 Custom Block Definition
===================================
How to register in UIFlow v2:
1. Open UIFlow v2 (flow2.m5stack.com)
2. Left panel "Custom" tab -> "+ New Class"
3. Paste this file content and save
4. "MinecraftBridge" category appears in the block panel

Requirements:
- lib/minecraft_mqtt.py must be uploaded to M5Stack
  (UIFlow v2 "Files" tab -> place in /flash/lib/ folder)
"""

import sys
sys.path.append('/flash')
from lib.minecraft_mqtt import MinecraftBridge as _Bridge


class MinecraftBridge:
    """Bridge between Minecraft and M5Stack via MQTT"""

    def __init__(self):
        """%1 init"""
        self._bridge = None

    def connect(self, broker_ip, broker_port=1883, ssid=None, password=None):
        """%1 connect to broker %2 port %3 wifi ssid %4 password %5"""
        import network
        self._bridge = _Bridge(broker_ip, broker_port)
        if ssid and password:
            self._bridge.connect_wifi(ssid, password)
        elif not network.WLAN(network.STA_IF).isconnected():
            raise RuntimeError('[Minecraft] WiFi not connected. Provide ssid and password.')
        self._bridge.connect_mqtt()

    def send(self, channel, value):
        """%1 send channel %2 value %3"""
        if self._bridge:
            self._bridge.send(int(channel), bool(value))

    def get_value(self, channel):
        """%1 get value of channel %2"""
        if self._bridge:
            return self._bridge.get_value(int(channel))
        return False

    def on_channel(self, channel, callback):
        """%1 when channel %2 received call %3"""
        if self._bridge:
            self._bridge.on_channel(int(channel), callback)

    def on_channel_value(self, channel, value, callback):
        """%1 when channel %2 becomes %3 call %4"""
        if self._bridge:
            expected = bool(value)
            self._bridge.on_channel(int(channel), lambda v: callback() if v == expected else None)

    def check(self):
        """%1 check messages"""
        if self._bridge:
            self._bridge.check()
