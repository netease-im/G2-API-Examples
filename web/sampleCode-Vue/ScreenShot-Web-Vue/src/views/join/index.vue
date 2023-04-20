<template>
  <div class="join">
    <div class="content">
      <img src="../../assets/img/logo.png" alt="" class="logo" />
      <input
        type="text"
        v-model.trim="channelName"
        placeholder="请输入房间号"
      />
      <input
        class="extra-input"
        v-if="isShowExtraInput"
        type="text"
        v-model.trim="extraChannelName"
        placeholder="请输入房间号"
      />
      <el-checkbox
        class="extra-check"
        v-if="isShowProxyCheck"
        v-model="cloudProxy"
        >开启云代理</el-checkbox
      >
      <el-checkbox
        class="extra-check"
        v-if="isShowDualStreamCheck"
        v-model="dualStream"
        >开启大小流</el-checkbox
      >
      <el-checkbox class="extra-check" v-if="isShowBeautyCheck" v-model="beauty"
        >开启美颜</el-checkbox
      >
      <div class="extra-check" v-if="isShowRoleRadio">
        <span class="mr10">角色</span>
        <el-radio v-model="role" label="host">主播</el-radio>
        <el-radio v-model="role" label="audience">观众</el-radio>
      </div>
      <!-- <div class="custom-encryption" v-if="isShowScreenShareOption">
        <span class="mr10">NERTC</span
        ><el-input v-model="maxBitrate" placeholder="请输入最大码率"></el-input
        ><br />
        <span class="mr10">策略选择</span>
        <el-select v-model="contentHint" placeholder="请选择内容类型">
          <el-option
            v-for="item in screenShareOptions"
            :key="item.value"
            :value="item.value"
            :label="item.label"
          />
        </el-select>
      </div> -->
      <div class="custom-encryption" v-if="isShowCustomEncryptionOption">
        <span class="mr10">自定义密钥</span
        ><el-input v-model="customSecret" placeholder="请输入密钥"></el-input
        ><br />
        <span class="mr10">自定义加密</span>
        <el-select v-model="customEncryption" placeholder="请选择加密类型">
          <el-option
            v-for="item in customEncryptionOptions"
            :key="item.value"
            :value="item.value"
            :label="item.label"
          />
        </el-select>
      </div>
      <div class="video-encoding" v-if="isShowVideoEncodingOption">
        <span class="mr10">视频质量</span>
        <el-select v-model="videoQuality">
          <el-option
            v-for="item in videoQualityOptions"
            :key="item.value"
            :value="item.value"
            :label="item.label"
          />
        </el-select>
        <br />
        <span class="mr10">视频帧率</span>
        <el-select v-model="videoFrameRate">
          <el-option
            v-for="item in videoFrameRateOptions"
            :key="item.value"
            :value="item.value"
            :label="item.label"
          />
        </el-select>
      </div>

      <button :disabled="!isSupport" class="submit-btn" @click="handleSubmit">
        加入房间
      </button>
      <div class="errorMsg" v-show="!isSupport">
        当前浏览器不支持体验，建议下载安装最新chrome浏览器
      </div>
    </div>
  </div>
</template>

<script>
    import { message } from '../../components/message';
    import { checkBrowser } from '../../common';
    import NERTC from 'nertc-web-sdk'
    export default {
        name: 'join',
        data() {
            return {
                channelName: '',
                extraChannelName: '',
                isSupport: true,
                isShowCustomEncryptionOption: false,
                isShowExtraInput: false,
                isShowProxyCheck: false,
                isShowBeautyCheck: false,
                isShowDualStreamCheck: false,
                isShowRoleRadio: false,
                isShowVideoEncodingOption: false,
                isShowScreenShareOption: false,
                role: 'host',
                customEncryption: '',
                cloudProxy: true,
                dualStream: true,
                beauty: true,
                customSecret: '',
                customEncryptionOptions: [
                    {
                        value: 'rc4',
                        label: 'rc4',
                    },
                    {
                        value: 'sm4',
                        label: 'sm4-128-ecb',
                    },
                ],
                screenShareOptions: [
                    {
                        value: 'detail',
                        label: '清晰度优先',
                    },
                    {
                        value: 'motion',
                        label: '流畅度优先',
                    },
                ],
                videoQuality: NERTC.VIDEO_QUALITY_720p,
                videoQualityOptions: [
                    {
                        value: NERTC.VIDEO_QUALITY_180p,
                        label: '320x180',
                    },
                    {
                        value: NERTC.VIDEO_QUALITY_480p,
                        label: '640x480',
                    },
                    {
                        value: NERTC.VIDEO_QUALITY_720p,
                        label: '1280x720',
                    },
                    {
                        value: NERTC.VIDEO_QUALITY_1080p,
                        label: '1920x1080',
                    },
                ],
                videoFrameRate: 15,
                videoFrameRateOptions: [
                    {
                        value: NERTC.CHAT_VIDEO_FRAME_RATE_5,
                        label: 5,
                    },
                    {
                        value: NERTC.CHAT_VIDEO_FRAME_RATE_10,
                        label: 10,
                    },
                    {
                        value: NERTC.CHAT_VIDEO_FRAME_RATE_15,
                        label: 15,
                    },
                    {
                        value: NERTC.CHAT_VIDEO_FRAME_RATE_20,
                        label: 20,
                    },
                    {
                        value: NERTC.CHAT_VIDEO_FRAME_RATE_25,
                        label: 25,
                    },
                ],
            };
        },
        mounted() {
            if (!NERTC.checkSystemRequirements()) {
                this.isSupport = false;
            }
            // 自定义加密需要进入房间前设置
            if (this.$route.query.path === 'customEncryption') {
                this.isShowCustomEncryptionOption = true;
            } else if (this.$route.query.path === 'multipleInstances') {
                this.isShowExtraInput = true;
            } else if (this.$route.query.path === 'cloudProxy') {
                this.isShowProxyCheck = true;
            } else if (this.$route.query.path === 'dualStream') {
                this.isShowDualStreamCheck = true;
            } else if (this.$route.query.path === 'beauty') {
                this.isShowBeautyCheck = true;
            } else if (this.$route.query.path === 'basicLive') {
                this.isShowRoleRadio = true;
            } else if (this.$route.query.path === 'videoEncoding') {
                this.isShowVideoEncodingOption = true;
            } else if (this.$route.query.path === 'screenShare') {
                this.isShowScreenShareOption = true;
            }
        },
        methods: {
            handleSubmit() {
                const {
                    channelName,
                    isShowExtraInput,
                    extraChannelName,
                    isShowCustomEncryptionOption,
                    customEncryption,
                    isShowProxyCheck,
                    cloudProxy,
                    customSecret,
                    isShowDualStreamCheck,
                    dualStream,
                    isShowBeautyCheck,
                    beauty,
                    isShowRoleRadio,
                    role,
                    isShowVideoEncodingOption,
                    videoQuality,
                    videoFrameRate,
                    isShowScreenShareOption,
                } = this;

                if (!channelName) {
                    message('请输入房间号');
                    return;
                } else if (!/^[0-9]{1,12}$/.test(channelName)) {
                    message('房间号为12位以内的数字');
                    return;
                }
                if (isShowExtraInput) {
                    if (!extraChannelName) {
                        message('请输入房间号');
                        return;
                    } else if (!/^[0-9]{1,12}$/.test(extraChannelName)) {
                        message('房间号为12位以内的数字');
                        return;
                    }
                }
                if (isShowCustomEncryptionOption && !customSecret) {
                    message('请输入密钥');
                    return;
                }

                if (isShowCustomEncryptionOption && !customEncryption) {
                    message('请选择加密类型');
                    return;
                }
                const { path = 'screenShot' } = this.$route.query;
                if (isShowCustomEncryptionOption) {
                    this.$router.push({
                        path: `/${path}`,
                        query: { channelName, customEncryption, customSecret },
                    });
                } else if (isShowExtraInput) {
                    this.$router.push({
                        path: `/${path}`,
                        query: { channelName, extraChannelName },
                    });
                } else if (isShowProxyCheck) {
                    this.$router.push({
                        path: `/${path}`,
                        query: { channelName, cloudProxy },
                    });
                } else if (isShowDualStreamCheck) {
                    this.$router.push({
                        path: `/${path}`,
                        query: { channelName, dualStream },
                    });
                } else if (isShowBeautyCheck) {
                    this.$router.push({
                        path: `/${path}`,
                        query: { channelName, beauty },
                    });
                } else if (isShowRoleRadio) {
                    this.$router.push({
                        path: `/${path}`,
                        query: { channelName, role },
                    });
                } else if (isShowVideoEncodingOption) {
                    this.$router.push({
                        path: `/${path}`,
                        query: { channelName, videoQuality, videoFrameRate },
                    });
                } else if (isShowScreenShareOption) {
                    this.$router.push({
                        path: `/${path}`,
                        query: { channelName },
                    });
                } else {
                    this.$router.push({
                        path: `/${path}`,
                        query: { channelName },
                    });
                }
            },
        },
    };
</script>

<style scoped lang="less">
.join {
  height: 100vh;
  background: #f7f8fa;
  display: flex;
  align-items: center;
  justify-content: center;
  .content {
    width: 400px;
    height: 400px;
    padding-top: 60px;
    background: #fff;
    box-shadow: 0 4px 10px 0 rgba(47, 56, 111, 0.1);
    border-radius: 8px;
    .extra-check {
      margin-left: 40px;
      margin-bottom: 20px;
    }
    .extra-check {
      margin-left: 40px;
      margin-bottom: 20px;
    }
    .extra-check {
      margin-left: 40px;
      margin-bottom: 20px;
    }
    .custom-encryption {
      margin-top: -20px;
      margin-left: 40px;
      margin-bottom: 20px;
      .el-input {
        width: 193px;
        margin-bottom: 10px;
      }
    }
    .video-encoding {
      margin-top: -20px;
      margin-left: 40px;
      margin-bottom: 20px;
    }
    .logo {
      display: block;
      height: 55px;
      margin: 0 auto;
    }

    input {
      display: block;
      width: 315px;
      height: 44px;
      margin: 50px auto 40px;
      border: none;
      outline: medium;
      border-bottom: 1px solid #dcdfe5;
      font-family: PingFangSC-Regular;
      font-size: 17px;

      &::placeholder {
        color: #b0b6be;
      }
    }
    .extra-input{
      margin-top: -20px;
    }

    .submit-btn {
      display: block;
      display: block;
      width: 315px;
      height: 50px;
      margin: 0 auto;
      border: none;
      outline: medium;
      background: #337eff;
      border-radius: 25px;
      font-family: PingFangSC-Regular;
      font-size: 16px;
      color: #ffffff;
      cursor: pointer;
      &:active {
        background: darken(#337eff, 5%);
      }
      &:disabled {
        background: #dddddd;
        cursor: not-allowed;
      }
    }

    .errorMsg {
      font-size: 14px;
      text-align: center;
      color: red;
      margin-top: 10px;
    }
  }
}
</style>
