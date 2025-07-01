<template>
  <div class="wrapper">
    <div class="content">
      <!--画面div-->
      <el-card class="control-container" body-style="backgroundColor: initial">
        <el-row>
          <el-col :span="8"><span class="demonstration">开关</span></el-col>
          <el-col :span="8">
            <el-button type="primary" @click="registerAIAudioEffects"
              >注册</el-button
            >
          </el-col>
          <el-col :span="8">
            <el-button
              type="success"
              :disabled="!registerEffect"
              @click="unregisterAIAudioEffects"
              >销毁</el-button
            >
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8"><span class="demonstration">音效</span></el-col>
          <el-col :span="8">
            <el-button
              type="success"
              :disabled="!registerEffect"
              @click="enableAudioEffect"
              >打开</el-button
            >
          </el-col>
          <el-col :span="8">
            <el-button
              type="success"
              :disabled="!registerEffect"
              @click="disableAudioEffect"
              >关闭</el-button
            >
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8"><span class="demonstration">降噪</span></el-col>
          <el-col :span="8">
            <el-button
              type="success"
              :disabled="!registerEffect"
              @click="enableAIDenoise"
              >打开</el-button
            >
          </el-col>
          <el-col :span="8">
            <el-button
              type="success"
              :disabled="!registerEffect"
              @click="disableAIDenoise"
              >关闭</el-button
            >
          </el-col>
        </el-row>
      </el-card>
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
      <li
        :class="{ silence: true, isSilence }"
        @click="setOrRelieveSilence"
      ></li>
      <li class="over" @click="handleOver"></li>
      <li :class="{ stop: true, isStop }" @click="stopOrOpenVideo"></li>
    </ul>
  </div>
</template>
<script>
import { message } from "../../components/message";
import NERTC from "nertc-web-sdk";
import AIAudioEffects from "nertc-web-sdk/NERTC_Web_SDK_AIAudioEffects";
import config from "../../../config";
import { getToken } from "../../common";
import wasmFeatureDetect from "../../lib/wasmFeatureDetect";
export default {
  name: "AIAudioEffects",
  data() {
    return {
      isSilence: false,
      isStop: false,
      client: null,
      localUid: Math.ceil(Math.random() * 1e5),
      localStream: null,
      remoteStreams: [],
      max: 20,
      color: "#e7ad3c",
      level: 1,
      AIAudioEffectsPluginConfig: {
        simd: {
          key: "AIAudioEffects",
          pluginObj: AIAudioEffects,
          wasmUrl:
            "https://yx-web-nosdn.netease.im/package/NIM_Web_AIAudioEffects_simd_v5.8.20.wasm" +
            `?download=${new Date().valueOf()}`,
        },
        nosimd: {
          key: "AIAudioEffects",
          pluginObj: AIAudioEffects,
          wasmUrl:
            "https://yx-web-nosdn.netease.im/package/NIM_Web_AIAudioEffects_nosimd_v5.8.20.wasm" +
            `?download=${new Date().valueOf()}`,
        },
      },
      registerEffect: false,
      openEffect: false,
    };
  },
  mounted() {
    // 初始化音视频实例
    console.warn("初始化音视频sdk");
    window.self = this;
    this.client = NERTC.createClient({
      appkey: config.appkey,
      debug: true,
    });
    //监听事件
    this.client.on("peer-online", (evt) => {
      console.warn(`${evt.uid} 加入房间`);
    });

    this.client.on("peer-leave", (evt) => {
      console.warn(`${evt.uid} 离开房间`);
      this.remoteStreams = this.remoteStreams.filter(
        (item) => !!item.getId() && item.getId() !== evt.uid
      );
    });

    this.client.on("stream-added", async (evt) => {
      //收到房间中其他成员发布自己的媒体的通知，对端同一个人同时开启了麦克风、摄像头、屏幕贡献，这里会通知多次
      const stream = evt.stream;
      const userId = stream.getId();
      console.warn(`收到 ${userId} 的发布 ${evt.mediaType} 的通知`); // mediaType为：'audio' | 'video' | 'screen'
      if (this.remoteStreams.some((item) => item.getId() === userId)) {
        console.warn("收到已订阅的远端发布，需要更新", stream);
        this.remoteStreams = this.remoteStreams.map((item) =>
          item.getId() === userId ? stream : item
        );
        //订阅其发布的媒体，可以渲染播放
        await this.subscribe(stream);
      } else if (this.remoteStreams.length < this.max - 1) {
        console.warn("收到新的远端发布消息", stream);
        this.remoteStreams = this.remoteStreams.concat(stream);
        //订阅其发布的媒体，可以渲染播放
        await this.subscribe(stream);
      } else {
        console.warn("房间人数已满");
      }
    });

    this.client.on("stream-removed", (evt) => {
      const stream = evt.stream;
      const userId = stream.getId();
      console.warn(`收到 ${userId} 的停止发布 ${evt.mediaType} 的通知`); // mediaType为：'audio' | 'video' | 'screen'
      stream.stop(evt.mediaType);
      this.remoteStreams = this.remoteStreams.map((item) =>
        item.getId() === userId ? stream : item
      );
      console.warn("远端流停止订阅，需要更新", userId, stream);
    });

    this.client.on("stream-subscribed", (evt) => {
      const userId = evt.stream.getId();
      console.warn(`收到订阅 ${userId} 的 ${evt.mediaType} 成功的通知`); // mediaType为：'audio' | 'video' | 'screen'
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
        screen: true,
      };
      remoteStream
        .play(div, playOptions)
        .then(() => {
          console.log("播放对端的流成功: ", playOptions);
          remoteStream.setRemoteRenderMode({
            // 设置视频窗口大小
            width: 160,
            height: 90,
            cut: false, // 是否裁剪
          });
        })
        .catch((err) => {
          console.warn("播放对方视频失败了: ", err);
        });
      //这里监听一下音频自动播放会被浏览器限制的问题（https://doc.yunxin.163.com/nertc/docs/jM3NDE0NTI?platform=web）
      remoteStream.on("notAllowedError", (err) => {
        const errorCode = err.getCode();
        const id = remoteStream.getId();
        console.log("remoteStream notAllowedError: ", id);
        if (errorCode === 41030) {
          //页面弹筐加一个按钮，通过交互完成浏览器自动播放策略限制的接触
          const userGestureUI = document.createElement("div");
          if (userGestureUI && userGestureUI.style) {
            userGestureUI.style.fontSize = "20px";
            userGestureUI.style.position = "fixed";
            userGestureUI.style.background = "yellow";
            userGestureUI.style.margin = "auto";
            userGestureUI.style.width = "100%";
            userGestureUI.style.zIndex = "9999";
            userGestureUI.style.top = "0";
            userGestureUI.onclick = () => {
              if (userGestureUI && userGestureUI.parentNode) {
                userGestureUI.parentNode.removeChild(userGestureUI);
              }
              remoteStream.resume();
            };
            userGestureUI.style.display = "block";
            userGestureUI.innerHTML =
              "自动播放受到浏览器限制，需手势触发。<br/>点击此处手动播放";
            document.body.appendChild(userGestureUI);
          }
        }
      });
    });

    this.client.on("uid-duplicate", () => {
      console.warn("==== uid重复，你被踢出");
    });

    this.client.on("error", (type) => {
      console.error("===== 发生错误事件：", type);
      if (type === "SOCKET_ERROR") {
        console.warn("==== 网络异常，已经退出房间");
      }
    });

    this.client.on("accessDenied", (type) => {
      console.warn(`==== ${type}设备开启的权限被禁止`);
    });

    this.client.on("connection-state-change", (evt) => {
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
      await this.client.leave();
      this.localStream.destroy();
      this.client.destroy();
    } catch (e) {
      // 为了兼容低版本，用try catch包裹一下
    }
  },
  methods: {
    async registerAIAudioEffects() {
      if (!this.localStream) {
        return;
      }
      const type = (await wasmFeatureDetect.simd()) ? "simd" : "nosimd";
      const AIAudioEffectsConfig = this.AIAudioEffectsPluginConfig[type];
      this.localStream.registerPlugin(AIAudioEffectsConfig);
    },
    enableAudioEffect() {
      this.localStream.enableAudioEffect();
      this.openEffect = true;
    },
    disableAudioEffect() {
      this.localStream.disableAudioEffect();
      this.openEffect = false;
    },
    unregisterAIAudioEffects() {
      this.localStream.unregisterPlugin("AIAudioEffects");
      this.registerEffect = false;
    },
    enableAIDenoise() {
      this.localStream.enableAIDenoise();
    },
    disableAIDenoise() {
      this.localStream.disableAIDenoise();
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
          path: "/",
          query: {
            path: "AIAudioEffects",
          },
        });
      }, time);
    },
    joinChannel(token) {
      if (!this.client) {
        message("内部错误，请重新加入房间");
        return;
      }
      console.info("开始加入房间: ", this.$route.query.channelName);
      this.client
        .join({
          channelName: this.$route.query.channelName,
          uid: this.localUid,
          token,
        })
        .then((data) => {
          console.info("加入房间成功，开始初始化本地音视频流");
          this.initLocalStream();
        })
        .catch((error) => {
          console.error("加入房间失败：", error);
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

      //注册plugin-load事件
      this.localStream.on("plugin-load", (name) => {
        if (name === "AIAudioEffects") {
          console.log("register AIAudioEffects success");
          this.registerEffect = true;
        }
      });

      this.localStream.on("audio-effect-enabled", () => {
        console.warn("AI音效已开启");
        this.localStream.setAudioEffect(0, 2);
      });

      //插件注册失败时触发
      this.localStream.on("plugin-load-error", (error) => {
        console.error("plugin-load-error", error);
      });

      //背景人声消除，默认设置15
      this.localStream?.on("ai-denoise-enabled", () => {
        this.localStream?.setVoiceGate(15);
      });

      //设置本地视频质量
      this.localStream.setVideoProfile({
        resolution: NERTC.VIDEO_QUALITY_720p, //设置视频分辨率
        frameRate: NERTC.CHAT_VIDEO_FRAME_RATE_15, //设置视频帧率
      });
      //设置本地音频质量
      this.localStream.setAudioProfile("speech_low_quality");
      //启动媒体，打开实例对象中设置的媒体设备
      this.localStream
        .init()
        .then(async () => {
          console.warn("音视频开启完成，可以播放了");
          const div = self.$refs.large;

          await this.localStream.play(div);
          this.localStream.setLocalRenderMode({
            // 设置视频窗口大小
            width: div.clientWidth,
            height: div.clientHeight,
            cut: false, // 是否裁剪
          });
          // 发布
          await this.publish();
        })
        .catch((err) => {
          console.warn("音视频初始化失败: ", err);
          message("音视频初始化失败");
          this.localStream = null;
        });
    },
    async publish() {
      console.warn("开始发布视频流");
      //发布本地媒体给房间对端
      await this.client.publish(this.localStream);
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
          console.warn("本地 subscribe 成功");
        })
        .catch((err) => {
          console.warn("本地 subscribe 失败: ", err);
          message("订阅对方的流失败");
        });
    },
    setOrRelieveSilence() {
      const { isSilence } = this;
      this.isSilence = !isSilence;
      if (this.isSilence) {
        console.warn("关闭mic");
        this.localStream
          .close({
            type: "audio",
          })
          .then(() => {
            console.warn("关闭 mic sucess");
          })
          .catch((err) => {
            console.warn("关闭 mic 失败: ", err);
            message("关闭 mic 失败");
          });
      } else {
        console.warn("打开mic");
        if (!this.localStream) {
          message("当前不能打开mic");
          return;
        }
        this.localStream
          .open({
            type: "audio",
          })
          .then(() => {
            console.warn("打开mic sucess");
          })
          .catch((err) => {
            console.warn("打开mic失败: ", err);
            message("打开mic失败");
          });
      }
    },
    stopOrOpenVideo() {
      const { isStop } = this;
      this.isStop = !isStop;
      if (this.isStop) {
        console.warn("关闭摄像头");
        this.localStream
          .close({
            type: "video",
          })
          .then(() => {
            console.warn("关闭摄像头 sucess");
          })
          .catch((err) => {
            console.warn("关闭摄像头失败: ", err);
            message("关闭摄像头失败");
          });
      } else {
        console.warn("打开摄像头");
        if (!this.localStream) {
          message("当前不能打开camera");
          return;
        }
        this.localStream
          .open({
            type: "video",
          })
          .then(() => {
            console.warn("打开摄像头 sucess");
            const div = self.$refs.large;
            this.localStream.play(div);
            this.localStream.setLocalRenderMode({
              // 设置视频窗口大小
              width: div.clientWidth,
              height: div.clientHeight,
              cut: false, // 是否裁剪
            });
          })
          .catch((err) => {
            console.warn("打开摄像头失败: ", err);
            message("打开摄像头失败");
          });
      }
    },
    handleOver() {
      console.warn("离开房间");
      if (this.openEffect) {
        this.localStream.disableAudioEffect();
      }
      this.returnJoin(1);
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

    .control-container {
      position: absolute;
      top: 16px;
      left: 80px;
      z-index: 9;
      background-color: initial;
      width: 290px;

      .demonstration {
        color: #fff;
        vertical-align: -webkit-baseline-middle;
      }
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

  .el-row {
    height: 50px;
  }
}
</style>
