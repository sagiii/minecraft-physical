"""
UIFlow v2 カスタムブロック定義
================================
このファイルを UIFlow v2 の Custom ブロックエディタに貼り付けることで、
Minecraft チャンネルを操作するブロックが使えるようになります。

【UIFlow v2 への登録手順】
1. UIFlow v2 (flow2.m5stack.com) を開く
2. 左パネル下部の「Custom」タブをクリック
3. 「+ New Block」→「Python」を選択
4. このファイルの内容を貼り付ける
5. 保存するとブロックが Custom カテゴリに追加される

【前提条件】
- lib/minecraft_mqtt.py を M5Stack にアップロード済みであること
  (UIFlow v2 の「Files」タブ → lib/ フォルダに配置)
"""

# ライブラリを読み込む
import sys
sys.path.append('/flash')
from lib.minecraft_mqtt import mc_setup, mc_on, mc_send, mc_check


# ============================================================
# UIFlow v2 カスタムブロック関数
# 各関数が1つのブロックに対応する
# ============================================================

def Minecraft接続(ssid, password, broker_ip):
    """WiFiとMinecraftブローカーに接続する"""
    mc_setup(ssid, password, broker_ip)


def Minecraftに送信(channel, value):
    """
    Minecraftのチャンネルにon/offを送る
    channel: チャンネル番号(1-99)
    value: True=ON / False=OFF
    """
    mc_send(int(channel), bool(value))


def Minecraftチャンネル受信登録(channel, callback):
    """
    チャンネルの状態変化を受け取るコールバックを登録する
    callbackはTrue(ON)またはFalse(OFF)を引数に取る
    """
    mc_on(int(channel), callback)


def Minecraftメッセージ確認():
    """
    受信メッセージを処理する。
    必ずループブロックの中に入れること。
    """
    mc_check()
