"""
例04: カメラストリーミング
==========================
Minecraftワールド内のカメラエンティティから映像を受信し、
M5StickC Plus のLCDに表示する。

【Minecraft側の準備】
1. コマンドでカメラエンティティを召喚:
     /summon gpio_bridge:camera ~ ~1 ~ {camId:1}
2. MQTTブローカー (Mosquitto) が localhost:1883 で動いていること

【動作説明】
- 起動時に ctrl メッセージを送信してカメラ設定を要求
- MODが 1 FPS でJPEGフレームを mp/bridge/m/cam/1/frame に配信
- M5StickC Plus が受信してLCDに表示

【学習ポイント】
- バイナリMQTTペイロード (JPEG画像)
- カメラ制御プロトコル (ctrl/frame)
- リアルタイム映像ストリーミングの仕組み

【設定方法】
下記 CONFIG を自分の環境に合わせて変更してください。
"""

# ============================================================
# CONFIG — ここを変更する
# ============================================================
WIFI_SSID    = 'YOUR_WIFI_SSID'
WIFI_PASSWORD= 'YOUR_WIFI_PASSWORD'
BROKER_IP    = '192.168.1.xxx'

CAM_ID       = 1      # /summon で指定した camId と一致させる
CAM_WIDTH    = 240    # M5StickC Plus 横幅
CAM_HEIGHT   = 135    # M5StickC Plus 縦幅 (16:9)
CAM_FPS      = 1.0    # 1 フレーム/秒 (MQTT帯域の目安: ~5-10KB/s)
# ============================================================

import sys
sys.path.append('/flash')
from libs.minecraft_mqtt import mc_setup, mc_cam_start, mc_cam_display, mc_check

# セットアップ
mc_setup(BROKER_IP, ssid=WIFI_SSID, password=WIFI_PASSWORD)

# カメラ受信開始 — MOD に解像度と FPS を通知
mc_cam_start(CAM_ID, width=CAM_WIDTH, height=CAM_HEIGHT, fps=CAM_FPS)

print('=== Minecraft カメラ受信開始 ===')
print('カメラID: {}'.format(CAM_ID))
print('解像度: {}x{} @ {} fps'.format(CAM_WIDTH, CAM_HEIGHT, CAM_FPS))
print('トピック: mp/bridge/m/cam/{}/frame'.format(CAM_ID))

# メインループ
while True:
    mc_check()              # MQTT メッセージを受信・処理
    mc_cam_display(CAM_ID)  # 最新フレームを LCD に表示
