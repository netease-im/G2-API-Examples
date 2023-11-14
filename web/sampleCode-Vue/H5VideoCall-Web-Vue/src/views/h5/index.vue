<template>
  <div class="wrapper">
    <!--画面div-->
    <div class="main-window" ref="large">
      <div class="statistics-box">
        <div>
          网络质量：上行
          <span :class="uplinkNetworkQuality"></span> 下行
          <span :class="downlinkNetworkQuality"></span>
        </div>
        <div>音频码率：{{ audioSendBitrate }} Kbps</div>
        <div>视频码率：{{ videoSendBitrate }} Kbps</div>
        <div>视频帧率：{{ sendFrameRate }} fps</div>
        <div>
          视频分辨率：{{ sendResolutionHeight }} * {{ sendResolutionWidth }}
        </div>
      </div>
      <div class="sub-window-wrapper">
        <!--小画面div-->
        <template v-if="remoteStreams.length">
          <div
            v-for="item in remoteStreams"
            :key="item.getId()"
            class="sub-window"
            ref="small"
            :data-uid="item.getId()"
          ></div>
        </template>
        <div v-else class="sub-window" ref="small">
          <span class="loading-text">等待对方加入…</span>
        </div>
      </div>
    </div>
    <!--底层栏-->
    <ul class="tab-bar">
      <li
        :class="{ silence: true, isSilence }"
        @click="setOrRelieveSilence"
      ></li>
      <li class="set-wrapper" @click="switchCamera">
        <a href="javascript:;" class="set">切换摄像头</a>
      </li>
      <li class="over" @click="handleOver"></li>
      <li :class="{ stop: true, isStop }" @click="stopOrOpenVideo"></li>
    </ul>
  </div>
</template>
<script>
    import { message } from '../../components/message';
    import NERTC from 'nertc-web-sdk';
    import config from '../../../config';
    import { getToken } from '../../common';
    import { Toast, Dialog } from 'vant';
    import 'vant/es/dialog/style';
    import 'vant/es/toast/style';
    import Vconsole from '../../lib/vconsole.min.js'

    export default {
        name: 'h5',
        data() {
            return {
                isSilence: false,
                isStop: false,
                client: null,
                localUid: Math.ceil(Math.random() * 1e5),
                localStream: null,
                remoteStreams: [],
                max: 20,
                devicesId: [], //0前置相机 1后置相机
                currentDevice: 0,
                showStatistics: false,
                uplinkNetworkQuality: 'grey',
                downlinkNetworkQuality: 'grey',
                networkQuality: ['grey', 'green', 'green', 'yellow', 'red', 'red'],
                audioSendBitrate: 0,
                videoSendBitrate: 0,
                sendFrameRate: 0,
                sendResolutionHeight: 0,
                sendResolutionWidth: 0,
                vConsole: null
            };
        },
        mounted() {
            // 初始化音视频实例
            console.warn('初始化音视频sdk');
            // this.vConsole = new Vconsole();
            window.self = this;
            this.client = NERTC.createClient({
                appkey: config.appkey,
                debug: true,
            });
            //监听事件
            this.client.on('peer-online', (evt) => {
                console.warn(`${evt.uid} 加入房间`);
            });

            this.client.on('peer-leave', (evt) => {
                console.warn(`${evt.uid} 离开房间`);
                this.remoteStreams = this.remoteStreams.filter(
                    (item) => !!item.getId() && item.getId() !== evt.uid
                );
            });

            this.client.on('stream-added', async (evt) => {
                //收到房间中其他成员发布自己的媒体的通知，对端同一个人同时开启了麦克风、摄像头、屏幕贡献，这里会通知多次
                const stream = evt.stream;
                const userId = stream.getId();
                console.warn(`收到 ${userId} 的发布 ${evt.mediaType} 的通知`) // mediaType为：'audio' | 'video' | 'screen'
                if (this.remoteStreams.some((item) => item.getId() === userId)) {
                    console.warn('收到已订阅的远端发布，需要更新', stream);
                    this.remoteStreams = this.remoteStreams.map((item) =>
                        item.getId() === userId ? stream : item
                    );
                    //订阅其发布的媒体，可以渲染播放
                    await this.subscribe(stream);
                } else if (this.remoteStreams.length < this.max - 1) {
                    console.warn('收到新的远端发布消息', stream);
                    this.remoteStreams = this.remoteStreams.concat(stream);
                    //订阅其发布的媒体，可以渲染播放
                    await this.subscribe(stream);
                } else {
                    console.warn('房间人数已满');
                }
            });

            this.client.on('connection-state-change', evt => {
                if (evt.curState === 'DISCONNECTED') {Toast('连接断开');}
            })
            this.client.on('exception', evt => {
                if (evt.uid === this.localUid){
                    switch (evt.msg){
                    case 'FRAMERATE_SENT_TOO_LOW':
                        return Toast('视频发送帧率过低');
                    case 'FRAMERATE_VIDEO_BITRATE_TOO_LOW':
                        return Toast('视频发送码率过低');
                    case 'AUDIO_INPUT_LEVEL_TOO_LOW':
                        return Toast('发送音量过低');
                    case 'SEND_AUDIO_BITRATE_TOO_LOW':
                        return Toast('音频发送码率过低');
                    }
                }
            })

            this.client.on('stream-removed', (evt) => {
                const stream = evt.stream;
                const userId = stream.getId();
                console.warn(`收到 ${userId} 的停止发布 ${evt.mediaType} 的通知`) // mediaType为：'audio' | 'video' | 'screen'
                stream.stop(evt.mediaType);
                this.remoteStreams = this.remoteStreams.map((item) =>
                    item.getId() === userId ? stream : item
                );
                console.warn('远端流停止订阅，需要更新', userId, stream);
            });

            this.client.on('network-quality', (stats) => {
                stats.forEach((item) => {
                    if (item.uid === this.localUid) {
                        this.uplinkNetworkQuality =
                            this.networkQuality[Number(item.uplinkNetworkQuality)];
                        this.downlinkNetworkQuality =
                            this.networkQuality[Number(item.downlinkNetworkQuality)];
                    }
                });
            });

            this.client.on('stream-subscribed', (evt) => {
                console.warn('收到了对端的流，准备播放');
                const remoteStream = evt.stream;
                //用于播放对方视频画面的div节点
                const div = [...this.$refs.small].find(
                    (item) => Number(item.dataset.uid) === Number(remoteStream.getId())
                );
                remoteStream
                    .play(div)
                    .then(() => {
                        console.warn('播放视频');
                        remoteStream.setRemoteRenderMode({
                            // 设置视频窗口大小
                            width: 160,
                            height: 90,
                            cut: false, // 是否裁剪
                        });

                        remoteStream.on('notAllowedError', (evt) => {
                            // 获取错误码
                            const errorCode = evt.getCode();
                            // 判断为自动播放受限
                            if (errorCode === 41030) {
                                // 手势操作恢复
                                Dialog.confirm({
                                    title: '提示',
                                    message: '是否播放远端流？',
                                })
                                    .then(async () => {
                                        // 大多数浏览器限制停用的是音频
                                        await remoteStream.resume();
                                    })
                                    .catch(() => {
                                        Toast('取消播放远端流');
                                    });
                            }
                        });
                    })
                    .catch((err) => {
                        console.warn('播放对方视频失败了: ', err);
                    });
            });
            this.client.on('uid-duplicate', () => {
                console.warn('==== uid重复，你被踢出');
            });

            this.client.on('error', (type) => {
                console.error('===== 发生错误事件：', type);
                if (type === 'SOCKET_ERROR') {
                    console.warn('==== 网络异常，已经退出房间');
                }
            });

            this.client.on('accessDenied', (type) => {
                console.warn(`==== ${type}设备开启的权限被禁止`);
            });

            this.client.on('connection-state-change', (evt) => {
                console.warn(
                    `网络状态变更: ${evt.prevState} => ${evt.curState}, 当前是否在重连：${evt.reconnect}`
                );
            });
            this.getToken()
                .then((token) => {
                    this.joinChannel(token);
                })
                .catch((e) => {
                    message(e);
                    console.error(e);
                });
        },
        async destroyed() {
            try {
                await this.client.leave()
                this.localStream.destroy()
                this.client.destroy()
            } catch (e) {
                // 为了兼容低版本，用try catch包裹一下
            }
        },
        methods: {
            toggleShowStatistics() {
                this.showStatistics = !this.showStatistics;
            },
            getCameras() {
                NERTC.getCameras()
                    .then((data) => {
                        data.forEach((item) => {
                            this.devicesId.push(item.deviceId);
                        });
                    })
                    .catch((error) => {
                        console.warn.log(error);
                    });
            },
            getLocalAudioStats() {
                setInterval(async () => {
                    try {
                        const localAudioStats = await this.client.getLocalAudioStats();
                        if (!localAudioStats[0]) {
                            return;
                        }
                        this.audioSendBitrate = localAudioStats[0].SendBitrate;
                    } catch (error) {
                        console.error(error);
                    }
                }, 1000);
            },
            getLocalVideoStats() {
                setInterval(async () => {
                    try {
                        const localVideoStats = await this.client.getLocalVideoStats('video');
                        if (!localVideoStats[0]) {
                            return;
                        }
                        this.videoSendBitrate = localVideoStats[0].SendBitrate;
                        this.sendFrameRate = localVideoStats[0].SendFrameRate;
                        this.sendResolutionHeight = localVideoStats[0].SendResolutionHeight;
                        this.sendResolutionWidth = localVideoStats[0].SendResolutionWidth;
                    } catch (error) {
                        console.error(error);
                    }
                }, 1000);
            },
            switchCamera() {
                if (this.devicesId.length <= 0) {
                    return;
                }
                this.currentDevice = ++this.currentDevice % this.devicesId.length;
                this.setCamera(this.devicesId[this.currentDevice]);
            },
            async setCamera(deviceId) {
                try {
                    if (this.localStream.hasVideo()) {
                        await this.localStream.close({ type: 'video' })
                    }
                    await this.localStream.open({
                        type: 'video',
                        deviceId,
                    });
                    const div = this.$refs.large;
                    this.localStream.play(div);
                    this.localStream.setLocalRenderMode({
                        width: div.clientWidth,
                        height: div.clientHeight,
                        cut: false,
                    });
                } catch (error) {
                    console.log(error);
                }
            },
            getToken() {
                return getToken({
                    uid: this.localUid,
                    appkey: config.appkey,
                    appSecret: config.appSecret,
                    channelName: this.$route.query.channelName,
                }).then(
                    (token) => {
                        return token;
                    },
                    (e) => {
                        throw e;
                    }
                );
            },
            returnJoin(time = 2000) {
                setTimeout(() => {
                    this.$router.push({
                        path: '/',
                        query: {
                            path: 'h5',
                        },
                    });
                }, time);
            },
            joinChannel(token) {
                if (!this.client) {
                    message('内部错误，请重新加入房间');
                    return;
                }
                console.info('开始加入房间: ', this.$route.query.channelName);
                this.client
                    .join({
                        channelName: this.$route.query.channelName,
                        uid: this.localUid,
                        token,
                    })
                    .then((data) => {
                        console.info('加入房间成功，开始初始化本地音视频流');
                        this.initLocalStream();
                    })
                    .catch((error) => {
                        console.error('加入房间失败：', error);
                        message(`${error}: 请检查appkey或者token是否正确`);
                        this.returnJoin();
                    });
            },
            initLocalStream() {
                //初始化本地的Stream实例，用于管理本端的音视频流
                this.localStream = NERTC.createStream({
                    uid: this.localUid,
                    audio: true, //是否启动mic
                    video: true, //是否启动camera
                    screen: false, //是否启动屏幕共享
                });

                //设置本地视频质量
                this.localStream.setVideoProfile({
                    resolution: NERTC.VIDEO_QUALITY_720p, //设置视频分辨率
                    frameRate: NERTC.CHAT_VIDEO_FRAME_RATE_15, //设置视频帧率
                });
                //设置本地音频质量
                this.localStream.setAudioProfile('speech_low_quality');
                //启动媒体，打开实例对象中设置的媒体设备
                this.localStream
                    .init()
                    .then(() => {
                        console.warn('音视频开启完成，可以播放了');
                        const div = self.$refs.large;
                        this.localStream.play(div);
                        this.localStream.setLocalRenderMode({
                            // 设置视频窗口大小
                            width: div.clientWidth,
                            height: div.clientHeight,
                            cut: true, // 是否裁剪
                        });
                        // 发布
                        this.publish();
                        // 获取摄像头设备id
                        this.getCameras();
                        // 获取本地流音频统计信息
                        this.getLocalAudioStats();
                        // 获取本地流视频统计信息
                        this.getLocalVideoStats();
                    })
                    .catch((err) => {
                        console.warn('音视频初始化失败: ', err);
                        message('音视频初始化失败');
                        this.localStream = null;
                    });
            },
            publish() {
                console.warn('开始发布视频流');
                //发布本地媒体给房间对端
                this.client
                    .publish(this.localStream)
                    .then(() => {
                        console.warn('本地 publish 成功');
                    })
                    .catch((err) => {
                        console.error('本地 publish 失败: ', err);
                        message('本地 publish 失败');
                    });
            },
            subscribe(remoteStream) {
                //这里可以控制是否订阅某一类媒体，这里设置的是用户主观意愿
                //比如这里都是设置为true，如果stream-added事件中通知了是audio发布了，则本次调用会订阅音频，如果video、screen之前已经有stream-added通知，则也会同时订阅video、screen，反之会忽略
                remoteStream.setSubscribeConfig({
                    audio: true,
                    video: true,
                    screen: true
                });
                this.client
                    .subscribe(remoteStream)
                    .then(() => {
                        console.warn('本地 subscribe 成功');
                    })
                    .catch((err) => {
                        console.warn('本地 subscribe 失败: ', err);
                        message('订阅对方的流失败');
                    });
            },
            setOrRelieveSilence() {
                const { isSilence } = this;
                this.isSilence = !isSilence;
                if (this.isSilence) {
                    console.warn('关闭mic');
                    this.localStream
                        .close({
                            type: 'audio',
                        })
                        .then(() => {
                            console.warn('关闭 mic sucess');
                        })
                        .catch((err) => {
                            console.warn('关闭 mic 失败: ', err);
                            message('关闭 mic 失败');
                        });
                } else {
                    console.warn('打开mic');
                    if (!this.localStream) {
                        message('当前不能打开mic');
                        return;
                    }
                    this.localStream
                        .open({
                            type: 'audio',
                        })
                        .then(() => {
                            console.warn('打开mic sucess');
                        })
                        .catch((err) => {
                            console.warn('打开mic失败: ', err);
                            message('打开mic失败');
                        });
                }
            },
            stopOrOpenVideo() {
                const { isStop } = this;
                this.isStop = !isStop;
                if (this.isStop) {
                    console.warn('关闭摄像头');
                    this.localStream
                        .close({
                            type: 'video',
                        })
                        .then(() => {
                            console.warn('关闭摄像头 sucess');
                        })
                        .catch((err) => {
                            console.warn('关闭摄像头失败: ', err);
                            message('关闭摄像头失败');
                        });
                } else {
                    console.warn('打开摄像头');
                    if (!this.localStream) {
                        message('当前不能打开camera');
                        return;
                    }
                    this.localStream
                        .open({
                            type: 'video',
                        })
                        .then(() => {
                            console.warn('打开摄像头 sucess');
                            const div = self.$refs.large;
                            this.localStream.play(div);
                            this.localStream.setLocalRenderMode({
                                // 设置视频窗口大小
                                width: div.clientWidth,
                                height: div.clientHeight,
                                cut: true, // 是否裁剪
                            });
                        })
                        .catch((err) => {
                            console.warn('打开摄像头失败: ', err);
                            message('打开摄像头失败');
                        });
                }
            },
            handleOver() {
                console.warn('离开房间');
                this.returnJoin(1);
            },
        },
    };
</script>

<style scoped lang="less">
.common-light {
  width: 10px;
  height: 10px;
  border-radius: 10px;
  display: inline-block;
}
.wrapper {
  height: 100vh;
  width: 100vw;
  background-image: linear-gradient(179deg, #141417 0%, #181824 100%);
  display: flex;
  flex-direction: column;
  .main-window {
    height: 100%;
    width: 100%;
    margin: 0 auto;
    background: #25252d;
    position: relative;
    .statistics-box {
      position: absolute;
      top: 10px;
      left: 10px;
      background-color: rgba(30, 30, 30, 0.6);
      //width: 200px;
      height: auto;
      color: green;
      font-size: 12px;
      padding: 5px;
      z-index: 9;
      .grey:extend(.common-light) {
        background-color: grey;
      }
      .green:extend(.common-light) {
        background-color: green;
      }
      .red:extend(.common-light) {
        background-color: red;
      }
      .yellow:extend(.common-light) {
        background-color: yellow;
      }
    }

    .sub-window-wrapper {
      position: absolute;
      top: 10px;
      right: 10px;
      z-index: 9;
      width: 165px;
      .sub-window {
        background: #25252d;
        border: 1px solid #ffffff;
        margin-bottom: 20px;
        width: 165px;
        height: 92px;
        text-align: center;
        .loading-text {
          display: block;
          width: 100%;
          text-align: center;
          line-height: 90px;
          font-size: 12px;
          color: #fff;
          font-weight: 400;
        }
      }
    }
  }

  .tab-bar {
    height: 54px;
    width: 100%;
    background-image: linear-gradient(180deg, #292933 7%, #212129 100%);
    box-shadow: 0 0 0 0 rgba(255, 255, 255, 0.3);
    list-style: none;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #fff;

    li {
      height: 54px;
      width: 125px;
      cursor: pointer;

      &.set-wrapper {
        display: flex;
        justify-content: center;
        align-items: center;

        &:hover {
          background-color: #18181d;
        }

        .set {
          background-color: #2a6af2;
          color: #fff;
          display: inline-block;
          width: 68px;
          height: 36px;
          text-align: center;
          line-height: 36px;
          font-size: 12px;
          text-decoration: none;
          font-weight: 500;
          border-radius: 100px;
        }
      }

      //静音
      &.silence {
        background: url("../../assets/img/icon/silence.png") no-repeat center;
        background-size: 60px 54px;

        &:hover {
          background: url("../../assets/img/icon/silence-hover.png") no-repeat
            center;
          background-size: 60px 54px;
        }

        &:active {
          background: url("../../assets/img/icon/silence-click.png") no-repeat
            center;
          background-size: 60px 54px;
        }

        &.isSilence {
          //已经开启静音
          background: url("../../assets/img/icon/relieve-silence.png") no-repeat
            center;
          background-size: 60px 54px;

          &:hover {
            background: url("../../assets/img/icon/relieve-silence-hover.png")
              no-repeat center;
            background-size: 60px 54px;
          }

          &:active {
            background: url("../../assets/img/icon/relieve-silence-click.png")
              no-repeat center;
            background-size: 60px 54px;
          }
        }
      }

      //结束按钮
      &.over {
        background: url("../../assets/img/icon/over.png") no-repeat center;
        background-size: 68px 36px;

        &:hover {
          background: url("../../assets/img/icon/over-hover.png") no-repeat
            center;
          background-size: 68px 36px;
        }

        &:active {
          background: url("../../assets/img/icon/over-click.png") no-repeat
            center;
          background-size: 68px 36px;
        }
      }

      // 停止按钮
      &.stop {
        background: url("../../assets/img/icon/stop.png") no-repeat center;
        background-size: 60px 54px;

        &:hover {
          background: url("../../assets/img/icon/stop-hover.png") no-repeat
            center;
          background-size: 60px 54px;
        }

        &:active {
          background: url("../../assets/img/icon/stop-click.png") no-repeat
            center;
          background-size: 60px 54px;
        }

        //已经是停止状态
        &.isStop {
          background: url("../../assets/img/icon/open.png") no-repeat center;
          background-size: 60px 54px;

          &:hover {
            background: url("../../assets/img/icon/open-hover.png") no-repeat
              center;
            background-size: 60px 54px;
          }

          &:active {
            background: url("../../assets/img/icon/open-click.png") no-repeat
              center;
            background-size: 60px 54px;
          }
        }
      }
    }
  }
}
</style>
