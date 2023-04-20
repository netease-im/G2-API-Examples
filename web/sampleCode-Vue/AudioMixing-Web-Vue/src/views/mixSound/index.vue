<template>
  <div class="wrapper">
    <div class="content">
      <!--画面div-->
      <div class="main-window" ref="large"></div>
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
      <li class="set-wrapper" @click="drawer=true">
        <a href="javascript:;" class="set">设置混音</a>
      </li>
      <li class="over" @click="handleOver"></li>
    </ul>
    <el-drawer
      title="设置混音"
      :visible.sync="drawer"
      direction="ltr"
    >
        <div class="pl20 pr20 mb20 flex jcb">
            <span class="mr10">背景音乐</span>
            <el-radio-group  v-model="primaryRadio">
                <el-radio-button label="1">音乐1</el-radio-button>
                <el-radio-button label="2">音乐2</el-radio-button>
            </el-radio-group>
        </div>
        <div class="pl20 pr20 mb20 flex jcb">
            <span class="mr10">背景音量</span>
            <div class="f1">
                <el-slider @input="changeVolume" :max="255" v-model="primaryVolume"></el-slider>
            </div>
        </div>
        <div class="t-center">
            <el-button type="success" :disabled="!primaryRadio" round @click="startAudio">播放</el-button>
            <el-button type="warning" :disabled="!primaryRadio" round @click="pauseAudio">暂停</el-button>
            <el-button type="success" :disabled="!primaryRadio" round @click="resumeAudio">恢复</el-button>
            <el-button type="danger" :disabled="!primaryRadio" round @click="stopAudio">停止</el-button>
        </div>
    </el-drawer>
  </div>
</template>
<script>
    import { message } from '../../components/message';
    import NERTC from 'nertc-web-sdk'
    import config from '../../../config';
    import { getToken } from '../../common';
    import music1 from '../../assets/music/music1.mp3';
    import music2 from '../../assets/music/music2.mp3';

    export default {
        name: 'mixSound',
        data() {
            return {
                drawer: false,
                primaryRadio: null,
                // secondaryRadio: '1',
                primaryVolume: 50,
                // secondaryVolume: 50,
                client: null,
                localUid: Math.ceil(Math.random() * 1e5),
                localStream: null,
                remoteStreams: [],
                max: 4,
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

            this.client.on('stream-subscribed', (evt) => {
                const userId = evt.stream.getId();
                console.warn(`收到订阅 ${userId} 的 ${evt.mediaType} 成功的通知`) // mediaType为：'audio' | 'video' | 'screen'
                //这里可以根据mediaType的类型决定播放策略
                const remoteStream = evt.stream;
                //用于播放对方视频画面的div节点
                const div = [...this.$refs.small].find((item) => {
                    return Number(item.dataset.uid) === Number(remoteStream.getId());
                });
                //这里可以控制是否播放某一类媒体，这里设置的是用户主观意愿
                //比如这里都是设置为true，本次通知的mediaType为audio，则本次调用的play会播放音频，如果video、screen内部已经订阅成功，则也会同时播放video、screen，反之不播放
                const playOptions = {
                    audio: true,
                    video: true,
                    screen: true
                }
                remoteStream.play(div, playOptions).then(() => {
                    console.log('播放对端的流成功: ', playOptions);
                    remoteStream.setRemoteRenderMode({
                        // 设置视频窗口大小
                        width: 160,
                        height: 90,
                        cut: false, // 是否裁剪
                    });
                }).catch((err) => {
                    console.warn('播放对方视频失败了: ', err);
                });
                //这里监听一下音频自动播放会被浏览器限制的问题（https://doc.yunxin.163.com/nertc/docs/jM3NDE0NTI?platform=web）
                remoteStream.on('notAllowedError', (err) => {
                    const errorCode = err.getCode();
                    const id = remoteStream.getId();
                    console.log('remoteStream notAllowedError: ', id);
                    if (errorCode === 41030) {
                        //页面弹筐加一个按钮，通过交互完成浏览器自动播放策略限制的接触
                        const userGestureUI = document.createElement('div')
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
                            }
                            userGestureUI.style.display = 'block';
                            userGestureUI.innerHTML = '自动播放受到浏览器限制，需手势触发。<br/>点击此处手动播放'
                            document.body.appendChild(userGestureUI)
                        }
                    }
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

            this.getToken().then(token => {
                this.joinChannel(token)
            }).catch(e => {
                message(e)
                console.error(e)
            })
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
            getToken() {
                return getToken({
                    uid: this.localUid,
                    appkey: config.appkey,
                    appSecret: config.appSecret,
                    channelName: this.$route.query.channelName
                }).then(token => {
                    return token
                }, (e) => {
                    throw e;
                });
            },
            returnJoin(time = 2000) {
                setTimeout(() => {
                    this.$router.push({
                        path: '/',
                        query: {
                            path: 'mixSound',
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
                remoteStream.setSubscribeConfig({
                    audio: true,
                    video: true,
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
            changeVolume() {
                if (!this.localStream) {
                    throw Error('内部错误，请重新加入房间')
                }
                if (!this.primaryRadio){
                    return
                }
                this.localStream.adjustAudioMixingVolume(this.primaryVolume).then(() => {
                    console.warn('设置伴音的音量成功');
                }).catch(err => {
                    console.error('设置伴音的音量失败: ', err);
                })
            },
            startAudio(){
                if (!this.localStream) {
                    throw Error('内部错误，请重新加入房间')
                }
                const map = {
                    1: music1,
                    2: music2,
                }
                this.localStream.startAudioMixing({
                    audioFilePath: map[this.primaryRadio],
                    loopback: false,
                    replace: false,
                    cycle: 0,
                    playStartTime: 0,
                    volume: this.primaryVolume,
                    auidoMixingEnd: () => { console.warn('伴音结束') }
                }).then(() => {
                    console.warn('开始伴音成功');
                }).catch(err => {
                    message('开始伴音失败');
                    console.error('开始伴音失败: ', err);
                })
            },
            resumeAudio() {
                if (!this.localStream) {
                    throw Error('内部错误，请重新加入房间')
                }

                this.localStream.resumeAudioMixing().then(res => {
                    console.warn('播放伴音成功')
                }).catch(err => {
                    message('播放伴音失败')
                    console.error('播放伴音失败: ', err)
                })
            },
            pauseAudio() {
                if (!this.localStream) {
                    throw Error('内部错误，请重新加入房间')
                }
                this.localStream.pauseAudioMixing().then(res => {
                    console.warn('暂停伴音成功')
                }).catch(err => {
                    message('暂停伴音失败')
                    console.error('暂停伴音失败: ', err)
                })
            },
            stopAudio() {
                if (!this.localStream) {
                    throw Error('内部错误，请重新加入房间')
                }
                this.localStream.stopAudioMixing().then(res => {
                    console.warn('停止伴音成功')
                }).catch(err => {
                    message('停止伴音失败')
                    console.error('停止伴音失败: ', err)
                })
            },
        },
    };
</script>

<style scoped lang="less">
.wrapper {
  height: 100vh;
  background-image: linear-gradient(179deg, #141417 0%, #181824 100%);
  display: flex;
  flex-direction: column;

  .content {
    flex: 1;
    display: flex;
    position: relative;

    .main-window {
      height: 100%;
      width: 67vh;
      //width: 37vw;
      //width: 427px;
      margin: 0 auto;
      background: #25252d;
    }

    .sub-window-wrapper {
        position: absolute;
        top: 16px;
        right: 16px;
        z-index: 9;
        width: 165px;
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
            background-color: #2A6AF2;
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
