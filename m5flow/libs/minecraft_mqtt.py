"""
minecraft_mqtt.py  —  Minecraft-Physical MQTT ブリッジライブラリ
======================================================================
MicroPython (ESP32) 向けライブラリ。
umqtt は MicroPython 標準内蔵なので追加インストール不要。

トピック設計 (プレフィックス: mp/bridge):
  mp/bridge/m/sig/{id}          Minecraft OUTブロック → デバイス  (受信)
  mp/bridge/p/sig/{id}          デバイス → Minecraft INブロック   (送信)
  mp/bridge/m/cam/{id}/frame    Minecraftカメラ映像 → デバイス    (JPEG バイナリ)
  mp/bridge/m/cam/{id}/ctrl     デバイス → Minecraftカメラ制御   (JSON)

  m = Minecraft 世界、p = Physical（実世界）

カメラ ctrl ペイロード (JSON):
  {"width": 240, "height": 135, "fps": 1.0, "pan": 0.0, "tilt": 0.0, "fov": 70.0}

使い方 (クラス版):
    from libs.minecraft_mqtt import MqttBridge
    mc = MqttBridge('192.168.1.10')
    mc.connect_wifi('MySSID', 'password')
    mc.connect_mqtt()
    mc.on_sig(1, lambda v: print('sig1:', v))
    mc.cam_start(1, width=240, height=135, fps=1)
    while True:
        mc.check()
        mc.cam_display(1)

使い方 (関数版 / UIFlow 向け):
    from libs.minecraft_mqtt import mc_setup, mc_send, mc_on, mc_check
    from libs.minecraft_mqtt import mc_cam_start, mc_cam_ctrl, mc_cam_display
    mc_setup('192.168.1.10', ssid='MySSID', password='pass')
    mc_on(1, lambda v: print('sig1:', v))
    mc_cam_start(1, width=240, height=135, fps=1)
    while True:
        mc_check()
        mc_cam_display(1)
"""

import network
import time
import json

try:
    from umqtt.robust import MQTTClient
except ImportError:
    from umqtt.simple import MQTTClient


# ======================================================================
# JPEG 表示ヘルパー
# ======================================================================

_TMP_JPEG = '/flash/.mc_cam.jpg'


def _show_jpeg(data: bytes, x: int = 0, y: int = 0):
    """
    JPEG バイト列を LCD に表示する。
    一時ファイル経由で表示するため、UIFlow v2 / 素の MicroPython どちらでも動く。
    """
    with open(_TMP_JPEG, 'wb') as f:
        f.write(data)
    # UIFlow v2 では lcd がグローバルに存在する
    try:
        import builtins
        _lcd = getattr(builtins, 'lcd', None)
        if _lcd is not None:
            _lcd.image(x, y, _TMP_JPEG)
            return
    except Exception:
        pass
    # フォールバック: lcd モジュールを直接 import
    try:
        import lcd as _lcd  # type: ignore
        _lcd.image(x, y, _TMP_JPEG)
    except ImportError:
        print('[mp] lcd が見つかりません。cam_frame() でバイト列を取得して手動表示してください。')


# ======================================================================
# MqttBridge クラス
# ======================================================================

class MqttBridge:
    """Minecraft-Physical MQTT ブリッジのコアクラス。"""

    # トピックテンプレート
    _T_M_SIG       = 'mp/bridge/m/sig/{}'        # Minecraft → デバイス (信号受信)
    _T_P_SIG       = 'mp/bridge/p/sig/{}'        # デバイス → Minecraft (信号送信)
    _T_M_CAM_FRAME = 'mp/bridge/m/cam/{}/frame'  # カメラ映像受信
    _T_M_CAM_CTRL  = 'mp/bridge/m/cam/{}/ctrl'   # カメラ制御送信

    # ワイルドカード購読
    _SUB_M_SIG = b'mp/bridge/m/sig/+'

    def __init__(self, broker_ip: str, broker_port: int = 1883, client_id: str = 'm5stack'):
        self._broker_ip   = broker_ip
        self._broker_port = broker_port
        self._client_id   = client_id
        self._client      = None
        self._connected   = False
        self._last_ping   = 0
        # 信号
        self._sig_callbacks = {}  # sig_id(int) -> callable(bool)
        self._sig_values    = {}  # sig_id(int) -> bool
        # カメラ
        self._cam_callbacks = {}  # cam_id(int) -> callable(bytes) | None
        self._cam_frames    = {}  # cam_id(int) -> bytes  (最新フレーム)
        self._cam_params    = {}  # cam_id(int) -> dict   (width/height/fps/pan/tilt/fov)

    # ------------------------------------------------------------------
    # 接続
    # ------------------------------------------------------------------

    def connect_wifi(self, ssid: str, password: str, timeout: int = 20) -> bool:
        """WiFi に接続する。成功すれば True を返す。"""
        wlan = network.WLAN(network.STA_IF)
        wlan.active(True)
        if wlan.isconnected():
            return True
        print('[mp] WiFi 接続中:', ssid)
        wlan.connect(ssid, password)
        for _ in range(timeout):
            if wlan.isconnected():
                print('[mp] WiFi 接続成功:', wlan.ifconfig()[0])
                return True
            time.sleep(1)
        print('[mp] WiFi 接続タイムアウト')
        return False

    def connect_mqtt(self):
        """MQTT ブローカーへ接続し、信号トピックを一括購読する。"""
        self._client = MQTTClient(
            self._client_id,
            self._broker_ip,
            port=self._broker_port,
            keepalive=60,
        )
        self._client.set_callback(self._on_message)
        self._client.connect()
        self._client.subscribe(self._SUB_M_SIG)
        self._connected = True
        print('[mp] MQTT 接続成功:', self._broker_ip)

    # ------------------------------------------------------------------
    # 信号 (boolean)
    # ------------------------------------------------------------------

    def on_sig(self, sig_id: int, callback):
        """
        信号受信コールバックを登録する。

        Args:
            sig_id   (int):           信号 ID (1-99)
            callback (callable(bool)): ON=True / OFF=False で呼ばれる
        """
        self._sig_callbacks[int(sig_id)] = callback

    def get_sig(self, sig_id: int) -> bool:
        """最後に受信した信号値を返す。未受信の場合は False。"""
        return self._sig_values.get(int(sig_id), False)

    def send_sig(self, sig_id: int, value: bool):
        """
        Minecraft INブロックに信号を送る。

        Args:
            sig_id (int):  信号 ID (1-99)
            value  (bool): True=ON / False=OFF
        """
        if not self._client:
            return
        topic   = self._T_P_SIG.format(int(sig_id)).encode()
        payload = b'1' if value else b'0'
        try:
            self._client.publish(topic, payload)
        except Exception as e:
            print('[mp] 送信エラー:', e)

    # ------------------------------------------------------------------
    # カメラ
    # ------------------------------------------------------------------

    def cam_start(self, cam_id: int, width: int = 240, height: int = 135,
                  fps: float = 1.0, pan: float = 0.0, tilt: float = 0.0,
                  fov: float = 70.0, callback=None):
        """
        Minecraftカメラにサブスクライブ開始。
        解像度・FPS・向きを ctrl トピック経由で MOD に通知する。

        Args:
            cam_id   (int):   カメラ ID
            width    (int):   希望解像度 横 (px)  M5StickC Plus 横向き: 240
            height   (int):   希望解像度 縦 (px)  M5StickC Plus 横向き: 135
            fps      (float): 希望フレームレート
            pan      (float): 水平角度 (度)
            tilt     (float): 垂直角度 (度)
            fov      (float): 視野角 (度)
            callback:         フレーム受信時に呼ばれる callable(jpeg_bytes: bytes)。
                              省略時は cam_display() で手動表示。
        """
        cam_id = int(cam_id)
        self._cam_params[cam_id] = {
            'width': width, 'height': height, 'fps': fps,
            'pan': pan, 'tilt': tilt, 'fov': fov,
        }
        self._cam_callbacks[cam_id] = callback
        # フレームトピックを購読
        frame_topic = self._T_M_CAM_FRAME.format(cam_id).encode()
        self._client.subscribe(frame_topic)
        # ctrl で解像度・角度を MOD に通知
        self._send_cam_ctrl(cam_id)
        print('[mp] カメラ%d サブスクライブ開始 (%dx%d @%.1ffps)' % (
            cam_id, width, height, fps))

    def cam_ctrl(self, cam_id: int, pan: float = None, tilt: float = None,
                 fov: float = None, fps: float = None):
        """
        カメラの向き・FOV・FPS を変更する。
        指定しないパラメータは現在値を維持する。

        Args:
            pan  (float): 水平角度 (度)
            tilt (float): 垂直角度 (度)
            fov  (float): 視野角 (度)
            fps  (float): フレームレート
        """
        cam_id = int(cam_id)
        params = self._cam_params.setdefault(cam_id, {
            'width': 240, 'height': 135, 'fps': 1.0,
            'pan': 0.0, 'tilt': 0.0, 'fov': 70.0,
        })
        if pan  is not None: params['pan']  = pan
        if tilt is not None: params['tilt'] = tilt
        if fov  is not None: params['fov']  = fov
        if fps  is not None: params['fps']  = fps
        self._send_cam_ctrl(cam_id)

    def cam_display(self, cam_id: int, x: int = 0, y: int = 0):
        """最後に受信したフレームをLCDに表示する。"""
        data = self._cam_frames.get(int(cam_id))
        if data:
            _show_jpeg(data, x, y)

    def cam_frame(self, cam_id: int):
        """最後に受信したフレームの JPEG バイト列を返す。独自表示処理に使う。"""
        return self._cam_frames.get(int(cam_id))

    def _send_cam_ctrl(self, cam_id: int):
        if not self._client:
            return
        params = self._cam_params.get(cam_id, {})
        topic  = self._T_M_CAM_CTRL.format(cam_id).encode()
        try:
            self._client.publish(topic, json.dumps(params).encode())
        except Exception as e:
            print('[mp] カメラ ctrl 送信エラー:', e)

    # ------------------------------------------------------------------
    # ループ
    # ------------------------------------------------------------------

    def check(self):
        """受信メッセージを処理する。メインループ内で毎回呼ぶこと。"""
        if not self._client:
            return
        try:
            self._client.check_msg()
            now = time.ticks_ms()
            if time.ticks_diff(now, self._last_ping) > 20000:
                self._client.ping()
                self._last_ping = now
        except Exception as e:
            print('[mp] 受信エラー:', e)

    # ------------------------------------------------------------------
    # 内部処理
    # ------------------------------------------------------------------

    def _on_message(self, topic: bytes, msg: bytes):
        """MQTT メッセージ受信ハンドラ (内部)。"""
        t     = topic.decode()
        parts = t.split('/')
        try:
            # mp/bridge/m/sig/{id}  →  parts = ['mp','bridge','m','sig', id]
            if len(parts) == 5 and parts[3] == 'sig':
                sig_id = int(parts[4])
                value  = (msg == b'1')
                self._sig_values[sig_id] = value
                cb = self._sig_callbacks.get(sig_id)
                if cb:
                    cb(value)

            # mp/bridge/m/cam/{id}/frame  →  parts = ['mp','bridge','m','cam', id,'frame']
            elif len(parts) == 6 and parts[3] == 'cam' and parts[5] == 'frame':
                cam_id = int(parts[4])
                self._cam_frames[cam_id] = bytes(msg)
                cb = self._cam_callbacks.get(cam_id)
                if cb:
                    cb(bytes(msg))

        except Exception as e:
            print('[mp] メッセージ解析エラー:', e)


# ======================================================================
# UIFlow 向けグローバル関数 API
# UIFlow v2 の Custom ブロックからこれらの関数を呼ぶ。
# ======================================================================

_bridge: MqttBridge = None  # type: ignore


def mc_setup(broker_ip: str, ssid: str = None, password: str = None,
             broker_port: int = 1883):
    """
    [ブロック: セットアップ]
    MQTT ブローカーに接続する。プログラムの最初に一度だけ呼ぶ。
    ssid/password を指定した場合のみ WiFi 接続を行う。
    すでに WiFi に接続済みの場合は省略できる。

    Args:
        broker_ip   (str): Mosquitto を動かしている PC の IP アドレス
        ssid        (str): WiFi の SSID (省略可)
        password    (str): WiFi のパスワード (省略可)
        broker_port (int): MQTT ポート番号 (通常は 1883)
    """
    global _bridge
    _bridge = MqttBridge(broker_ip, broker_port)
    if ssid and password:
        _bridge.connect_wifi(ssid, password)
    elif not network.WLAN(network.STA_IF).isconnected():
        raise RuntimeError('[mp] WiFi未接続。ssid と password を指定してください。')
    _bridge.connect_mqtt()


def mc_on(sig_id: int, callback):
    """
    [ブロック: 信号受信時]
    Minecraft OUTブロックの ON/OFF 変化をコールバックで受け取る。

    Args:
        sig_id   (int):           信号 ID (1-99)
        callback (callable(bool)): ON=True / OFF=False で呼ばれる関数
    """
    if _bridge:
        _bridge.on_sig(sig_id, callback)


def mc_send(sig_id: int, value: bool):
    """
    [ブロック: 信号を送信]
    Minecraft INブロックに信号を送る。

    Args:
        sig_id (int):  信号 ID (1-99)
        value  (bool): True=ON / False=OFF
    """
    if _bridge:
        _bridge.send_sig(sig_id, value)


def mc_check():
    """
    [ブロック: メッセージ確認]
    受信メッセージを処理する。メインループ内で必ず毎回呼ぶこと。
    """
    if _bridge:
        _bridge.check()


def mc_cam_start(cam_id: int, width: int = 240, height: int = 135,
                 fps: float = 1.0, callback=None):
    """
    [ブロック: カメラ開始]
    Minecraft カメラにサブスクライブ開始。
    解像度・FPS を MOD に通知し、フレームが届くたびに callback を呼ぶ。

    callback(jpeg_bytes: bytes) は省略可能。
    省略した場合は mc_cam_display() でループ内に表示する。

    Args:
        cam_id   (int):   カメラ ID
        width    (int):   横解像度 px  (M5StickC Plus 横向き: 240)
        height   (int):   縦解像度 px  (M5StickC Plus 横向き: 135)
        fps      (float): フレームレート (1.0 推奨)
    """
    if _bridge:
        _bridge.cam_start(cam_id, width=width, height=height, fps=fps, callback=callback)


def mc_cam_ctrl(cam_id: int, pan: float = None, tilt: float = None,
                fov: float = None, fps: float = None):
    """
    [ブロック: カメラ操作]
    カメラの向き・視野角・FPS を変更する。
    指定しないパラメータは現在値を維持する。

    Args:
        pan  (float): 水平角度 (度)
        tilt (float): 垂直角度 (度)
        fov  (float): 視野角 (度、小さいほどズームイン)
        fps  (float): フレームレート
    """
    if _bridge:
        _bridge.cam_ctrl(cam_id, pan=pan, tilt=tilt, fov=fov, fps=fps)


def mc_cam_display(cam_id: int, x: int = 0, y: int = 0):
    """
    [ブロック: カメラ表示]
    最後に受信したフレームを LCD に表示する。
    mc_check() の後に呼ぶこと。

    Args:
        cam_id (int): カメラ ID
        x, y   (int): 表示位置 (px)
    """
    if _bridge:
        _bridge.cam_display(cam_id, x=x, y=y)


def mc_cam_frame(cam_id: int):
    """
    [ブロック: カメラフレーム取得]
    最後に受信したフレームの JPEG バイト列を返す。
    独自の表示処理を行う場合に使う。

    Returns:
        bytes | None
    """
    if _bridge:
        return _bridge.cam_frame(cam_id)
    return None
