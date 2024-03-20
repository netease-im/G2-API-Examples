<template>
  <div class="wrapper">
    <div class="content">
      <!--画面div-->
      <div :class="`main-window ${scale ? 'scale' : ''}`" ref="large"></div>
      <!-- 远端屏幕共享 -->
      <div class="main-window" v-show="scale" ref="largeScreen"></div>
      <div :class="`sub-window-wrapper ${scale ? 'addTop' : ''}`">
        <!-- 本端屏幕共享画面 -->
        <div class="sub-window" v-show="isSharing" ref="localScreen"></div>
        <!--小画面视频流div-->
        <template v-if="remoteStreams.length">
          <div
            v-for="item in remoteStreams"
            :key="item.getId()"
            class="sub-window"
            ref="smallVideo"
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
      <li class="set-wrapper" @click="toggleVideo">
        <a href="javascript:;" class="set">{{
          isOpenVideo ? "开启视频" : "关闭视频"
        }}</a>
      </li>
      <li class="over" @click="handleOver"></li>
      <li class="set-wrapper" @click="toggleShareScreen">
        <a href="javascript:;" class="set">{{
          isSharing ? "停止共享" : "开始共享"
        }}</a>
      </li>
    </ul>
  </div>
</template>
<script>
    import { message } from '../../components/message';
    import NERTC from 'nertc-web-sdk';
    import config from '../../../config';
    import { getToken } from '../../common';

    export default {
        name: 'screenShare',
        data() {
            return {
                scale: false,
                isSharing: false,
                client: null,
                localUid: Math.ceil(Math.random() * 1e5),
                localStream: null,
                remoteStreams: [],
                max: 4,
                isOpenVideo: false,
                maxBitrate: 5000, // 设置最大码率为5M（单位：kbps）
                contentHint: 'detail', //屏幕贡献的编码测试，detail表示清晰度优先、motion表示流畅度优先
                screenId: null
            };
        },
        mounted() {
            // 初始化音视频实例
            console.warn('初始化音视频sdk');
            window.self = this;
            this.client = NERTC.createClient({
                appkey: config.appkey,
                debug: true,
            });
            //监听sdk抛出的事件
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
                console.warn(`收到 ${userId} 的发布 ${evt.mediaType} 的通知`); // mediaType为：'audio' | 'video' | 'screen'
                if (
                    this.remoteStreams.some(
                        (item) => !!item.getId() && item.getId() === userId
                    )
                ) {
                    console.warn('收到已订阅的远端发布，需要更新', stream);
                    this.remoteStreams = this.remoteStreams.map((item) =>
                        item.getId() === userId ? stream : item
                    );
                    //订阅其发布的媒体，可以渲染播放
                    await this.subscribe(stream);
                } else if (this.remoteStreams.length < this.max - 1) {
                    //订阅其发布的媒体，可以渲染播放
                    console.warn('收到新的远端发布消息', stream);
                    this.remoteStreams = this.remoteStreams.concat(stream);
                    await this.subscribe(stream);
                } else {
                    console.warn('房间人数已满');
                    message('房间人数已满');
                }
            });

            this.client.on('stream-removed', (evt) => {
                const stream = evt.stream;
                const userId = stream.getId();
                console.warn(`收到 ${userId} 的停止发布 ${evt.mediaType} 的通知`); // mediaType为：'audio' | 'video' | 'screen'
                stream.stop(evt.mediaType);
                if (evt.mediaType === 'video' && this.remoteStreams.length) {
                    this.remoteStreams = this.remoteStreams.filter((item) => {
                        return item && !!item.getId() && item.getId() !== userId;
                    });
                }
                if (evt.mediaType === 'screen' && stream.getId() === this.screenId) {
                    this.scale = false;
                    this.$nextTick(() => {
                        const div = self.$refs.large;
                        this.localStream.setLocalRenderMode(
                            {
                                // 设置视频窗口大小
                                width: div.clientWidth,
                                height: div.clientHeight,
                                cut: false, // 是否裁剪
                            },
                            'video'
                        );
                    });
                }
            });
            //执行完subscribe()方法后，如果订阅成功，这里会通知用户，此次订阅成功了
            this.client.on('stream-subscribed', (evt) => {
                const userId = evt.stream.getId();
                console.warn(`收到订阅 ${userId} 的 ${evt.mediaType} 成功的通知`); // mediaType为：'audio' | 'video' | 'screen'
                //这里可以根据mediaType的类型决定播放策略
                const remoteStream = evt.stream;
                //用于播放对方视频或者屏幕共享画面的div节点
                const doms = this.$refs.smallVideo; //音频播放不需要div节点，所以这里没有考虑音频的情况；共享屏幕单独处理
                let div = [...doms].find(
                    (item) => Number(item.dataset.uid) === Number(remoteStream.getId())
                );
                // 这里只显示一个屏幕共享画面
                if (evt.mediaType === 'screen' && !this.scale) {
                    this.screenId = remoteStream.getId()
                    this.scale = true;
                    div = this.$refs.largeScreen;

                    this.localStream.setLocalRenderMode(
                        {
                            // 设置视频窗口大小
                            width: 160,
                            height: 90,
                            cut: false, // 是否裁剪
                        },
                        'video'
                    );
                } else if (evt.mediaType === 'screen'){
                    return;
                }
                //这里可以控制是否播放某一类媒体，这里设置的是用户主观意愿
                //比如这里都是设置为true，本次通知的mediaType为audio，则本次调用的play会播放音频，如果video、screen内部已经订阅成功，则也会同时播放video、screen，反之不播放
                const playOptions = {
                    audio: true,
                    video: true,
                    screen: true,
                };
                remoteStream
                    .play(div, playOptions)
                    .then(() => {
                        console.log('播放对端的流成功: ', playOptions);

                        remoteStream.setRemoteRenderMode(
                            {
                                // 设置视频窗口大小
                                width: evt.mediaType === 'screen' ? div.clientWidth : 160,
                                height: evt.mediaType === 'screen' ? div.clientHeight : 90,
                                cut: false, // 是否裁剪
                            },
                            `${evt.mediaType === 'screen' ? 'screen' : 'video'}`
                        );
                    })
                    .catch((err) => {
                        console.warn('播放对方视频失败了: ', err);
                    });
                //这里监听一下音频自动播放会被浏览器限制的问题（https://doc.yunxin.163.com/nertc/docs/jM3NDE0NTI?platform=web）
                remoteStream.on('notAllowedError', (err) => {
                    const errorCode = err.getCode();
                    const id = remoteStream.getId();
                    console.log('remoteStream notAllowedError: ', id);
                    if (errorCode === 41030) {
                        //页面弹筐加一个按钮，通过交互完成浏览器自动播放策略限制的接触
                        const userGestureUI = document.createElement('div');
                        if (userGestureUI && userGestureUI.style) {
                            userGestureUI.style.fontSize = '20px';
                            userGestureUI.style.position = 'fixed';
                            userGestureUI.style.background = 'yellow';
                            userGestureUI.style.margin = 'auto';
                            userGestureUI.style.width = '100%';
                            userGestureUI.style.zIndex = '9999';
                            userGestureUI.style.top = '0';
                            userGestureUI.onclick = () => {
                                if (userGestureUI && userGestureUI.parentNode) {
                                    userGestureUI.parentNode.removeChild(userGestureUI);
                                }
                                remoteStream.resume();
                            };
                            userGestureUI.style.display = 'block';
                            userGestureUI.innerHTML =
                                '自动播放受到浏览器限制，需手势触发。<br/>点击此处手动播放';
                            document.body.appendChild(userGestureUI);
                        }
                    }
                });
            });
            //开启屏幕贡献后，浏览器会在底部弹出一个xxx正在共享您的屏幕 的弹框
            //里面有停止贡献的蓝色按钮，点击这个按钮，会触发这个事件，需要业务层主动处理
            //不处理的话，由于权限已经被收回了，测试屏幕贡献是采集不到数据的，会是黑屏
            this.client.on('stopScreenSharing', (evt) => {
                console.warn(
                    '===== 检测到您手动停止屏幕贡献的权限, 这里业务层关闭屏幕共享'
                );
                this.localStream
                    .close({
                        type: 'screen',
                    })
                    .then(() => {
                        console.log('关闭屏幕共享 sucess');
                        this.isSharing = false;
                    })
                    .catch((err) => {
                        console.log('关闭屏幕共享 失败: ', err);
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
            toggleVideo() {
                this.isOpenVideo = !this.isOpenVideo;
                if (this.isOpenVideo) {
                    console.warn('关闭视频');
                    this.localStream
                        .close({
                            type: 'video',
                        })
                        .then(() => {
                            console.warn('关闭视频');
                        })
                        .catch((err) => {
                            console.warn('关闭视频:', err.message);
                            message(`关闭视频: ${err.message}`);
                        });
                } else {
                    console.warn('打开视频');
                    this.localStream
                        .open({
                            type: 'video',
                        })
                        .then(() => {
                            console.warn('打开视频成功, 本地播放打开的视频');
                            const div = self.$refs.large;
                            this.localStream.play(div);
                            this.localStream.setLocalRenderMode({
                                // 设置视频窗口大小
                                width: this.scale ? 160 : div.clientWidth,
                                height: this.scale ? 90 : div.clientHeight,
                                cut: false, // 是否裁剪
                            });
                        })
                        .catch((err) => {
                            console.warn('关闭视频:', err.message);
                            message(`关闭视频: ${err.message}`);
                        });
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
                            path: 'screenShare',
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

                // 监听分享关闭事件
                this.localStream.on('stopScreenSharing', () => {
                    console.warn('屏幕共享已经停止');
                    this.closeShare();
                });

                //设置本地视频质量
                this.localStream.setVideoProfile({
                    resolution: NERTC.VIDEO_QUALITY_720p, //设置视频分辨率
                    frameRate: NERTC.CHAT_VIDEO_FRAME_RATE_15, //设置视频帧率
                });
                //设置本地音频质量
                this.localStream.setAudioProfile('speech_low_quality');
                //前设置屏幕共享帧率为20帧
                this.localStream.setScreenProfile({
                    frameRate: NERTC.VIDEO_FRAME_RATE.CHAT_VIDEO_FRAME_RATE_25,
                });
                // 设置码率
                this.localStream.setVideoEncoderConfiguration({
                    mediaType: 'screen',
                    maxBitrate: this.maxBitrate,
                    contentHint: this.contentHint,
                });
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
                            cut: false, // 是否裁剪
                        });
                        // 发布
                        this.publish();
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
                    screen: true,
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
            handleOver() {
                console.warn('离开房间');
                this.returnJoin(1);
            },
            openShare() {
                return this.localStream
                    .open({
                        type: 'screen',
                    })
                    .then(
                        () => {
                            console.log('正在打开屏幕共享');
                            const div = this.$refs.localScreen;
                            this.localStream.play(div);
                            this.localStream.setLocalRenderMode(
                                {
                                    // 设置视频窗口大小
                                    width: 160,
                                    height: 90,
                                    cut: false, // 是否裁剪
                                },
                                'screen'
                            );
                            this.isSharing = true;
                            console.warn('打开屏幕共享成功');
                        },
                        (err) => {
                            console.error('打开屏幕共享失败: ', err);
                        }
                    );
            },
            closeShare() {
                return this.localStream
                    .close({
                        type: 'screen',
                    })
                    .then(
                        () => {
                            this.isSharing = false;
                            console.warn('关闭屏幕共享 success');
                        },
                        (err) => {
                            console.error('关闭屏幕共享 error: ', err);
                        }
                    );
            },
            toggleShareScreen() {
                if (!this.localStream) {
                    throw Error('内部错误，请重新加入房间');
                }
                if (this.isSharing) {
                    this.closeShare();
                } else {
                    this.openShare();
                }
            },
            screenWindow(uid) {
                return uid + 'screen';
            },
        },
    };
</script>

<style scoped lang="less">
.wrapper {
  width: 100vw;
  height: 100vh;
  background-image: linear-gradient(179deg, #141417 0%, #181824 100%);
  display: flex;
  flex-direction: column;
  .content {
    flex: 1;
    display: flex;
    position: relative;

    .main-window {
      width: calc(100vw - 181px);
      height: calc(100vh - 54px);
    }
    .scale {
      background: #25252d;
      border: 1px solid #ffffff;
      position: absolute;
      top: 16px;
      right: 8px;
      width: 165px;
      height: 95px;
      z-index: 9;
    }
    .sub-window-wrapper {
      margin-top: 16px;
      margin-right: 8px;
      margin-left: 8px;
      width: 165px;
    }
    .addTop {
      margin-top: 132px;
    }

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

  .tab-bar {
    height: 54px;
    background-image: linear-gradient(180deg, #292933 7%, #212129 100%);
    box-shadow: 0 0 0 0 rgba(255, 255, 255, 0.3);
    list-style: none;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #fff;
    z-index: 9;

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
    }
  }
}
</style>
