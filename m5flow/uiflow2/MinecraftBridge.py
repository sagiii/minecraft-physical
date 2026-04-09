"""
UIFlow v2 Custom Block Definition
===================================
How to register in UIFlow v2:
1. Open UIFlow v2 (flow2.m5stack.com)
2. Left panel "Custom" tab -> "+ New Class"
3. Paste this file content and save
4. "MinecraftBridge" category appears in the block panel

Requirements:
- libs/minecraft_mqtt.py must be uploaded to M5Stack
  (UIFlow v2 "Files" tab -> place in /flash/libs/ folder)
"""

import sys
sys.path.append('/flash')
from libs.minecraft_mqtt import MqttBridge as _Bridge


class MinecraftBridge:
    """Bridge between Minecraft and M5Stack via MQTT"""

    def __init__(self):
        """%1 init"""
        self._bridge = None

    def connect(self, broker_ip, ssid=None, password=None, broker_port=1883):
        """%1 connect to broker %2 wifi ssid %3 password %4"""
        import network
        self._bridge = _Bridge(broker_ip, broker_port)
        if ssid and password:
            self._bridge.connect_wifi(ssid, password)
        elif not network.WLAN(network.STA_IF).isconnected():
            raise RuntimeError('[mp] WiFi not connected. Provide ssid and password.')
        self._bridge.connect_mqtt()

    # ---- 信号 (boolean) ----

    def send(self, sig_id, value):
        """%1 send signal %2 value %3"""
        if self._bridge:
            self._bridge.send_sig(int(sig_id), bool(value))

    def get_value(self, sig_id):
        """%1 get value of signal %2"""
        if self._bridge:
            return self._bridge.get_sig(int(sig_id))
        return False

    def on_signal(self, sig_id, callback):
        """%1 when signal %2 received call %3"""
        if self._bridge:
            self._bridge.on_sig(int(sig_id), callback)

    def on_signal_value(self, sig_id, value, callback):
        """%1 when signal %2 becomes %3 call %4"""
        if self._bridge:
            expected = bool(value)
            self._bridge.on_sig(int(sig_id), lambda v: callback() if v == expected else None)

    # ---- カメラ ----

    def cam_start(self, cam_id, width=240, height=135, fps=1.0):
        """%1 start camera %2 width %3 height %4 fps %5"""
        if self._bridge:
            self._bridge.cam_start(int(cam_id), width=int(width),
                                   height=int(height), fps=float(fps))

    def cam_ctrl(self, cam_id, pan=None, tilt=None, fov=None, fps=None):
        """%1 camera %2 pan %3 tilt %4 fov %5 fps %6"""
        if self._bridge:
            self._bridge.cam_ctrl(int(cam_id), pan=pan, tilt=tilt, fov=fov, fps=fps)

    def cam_display(self, cam_id, x=0, y=0):
        """%1 display camera %2 at x %3 y %4"""
        if self._bridge:
            self._bridge.cam_display(int(cam_id), x=int(x), y=int(y))

    def cam_frame(self, cam_id):
        """%1 get frame bytes of camera %2"""
        if self._bridge:
            return self._bridge.cam_frame(int(cam_id))
        return None

    # ---- ループ ----

    def check(self):
        """%1 check messages"""
        if self._bridge:
            self._bridge.check()
