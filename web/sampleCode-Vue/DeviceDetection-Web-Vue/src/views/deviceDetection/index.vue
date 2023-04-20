<template>
  <div class="wrapper">
    <el-card v-if="isShowDetectCard" class="detection-card">
      <el-steps
        class="step"
        :active="active"
        :process-status="processStatus"
        align-center
        slot="header"
      >
        <el-step title="浏览器兼容性" :status="browserInfo.status"></el-step>
        <el-step title="麦克风" :status="micInfo.status"></el-step>
        <el-step title="扬声器" :status="speakerInfo.status"></el-step>
        <el-step title="摄像头" :status="cameraInfo.status"></el-step>
        <el-step title="分辨率" :status="resolutionInfo.status"></el-step>
        <el-step title="网络连接" :status="connectionInfo.status"></el-step>
        <el-step title="发布接收" :status="pubRecInfo.status"></el-step>
      </el-steps>
      <div
        :class="`content ${
          description[4].show || description[5].show || description[6].show
            ? 'heighten'
            : ''
        }`"
      >
        <div class="detection-description">
          <div
            v-for="(item, index) in description"
            :key="index"
            :class="item.class"
            v-show="item.show"
          >
            <h2>{{ item.h2 }}</h2>
            <div>{{ item.content }}</div>
          </div>
        </div>
        <div class="detection-result">
          <div
            v-show="description[0].show"
            class="common"
            v-loading="description[0].show && !browserInfo.done"
          >
            <h2>检测结果</h2>
            <div v-for="(item, index) in browserInfo.content" :key="index">
              <div>{{ index + 1 }}.{{ item }}</div>
            </div>
          </div>
          <div
            v-show="description[1].show"
            class="common"
            v-loading="description[1].show && !micInfo.done"
          >
            <h2>检测结果</h2>
            <div>{{ micInfo.content }}</div>
          </div>
          <div v-show="description[2].show" class="common">
            <div v-if="isShowSpeakerResult">
              <h2>检测结果</h2>
              <div>{{ speakerInfo.content }}</div>
            </div>
            <div v-else>
              <div v-if="isShowSpeakerTest">
                <h2><b>麦克风测试中，请对麦克风说话</b></h2>
                <el-progress
                  :percentage="audioLevel"
                  :show-text="false"
                ></el-progress>
              </div>
              <div v-else>
                <h2>示例</h2>
                <div class="flex aic jce">
                  <audio autoplay :src="speakerInfo.recordUrl" controls></audio>
                  <el-button @click="hearSuccess">听得到</el-button
                  ><el-button @click="hearFail">听不到</el-button>
                </div>
              </div>
            </div>
          </div>
          <div
            v-show="description[3].show"
            v-loading="description[3].show && !cameraInfo.done"
            class="common"
          >
            <h2>检测结果</h2>
            <div>{{ cameraInfo.content }}</div>
          </div>
          <div v-show="description[4].show" class="common">
            <h2>检测结果</h2>
            <div ref="resolution" class="flex jcc"></div>
            <div v-for="(item, index) in resolutionInfo.content" :key="index">
              <div>{{ index + 1 }}.{{ item }}</div>
            </div>
          </div>
          <div v-show="description[5].show" class="common">
            <h2>检测结果</h2>
            <div ref="connection" class="flex jcc"></div>
            <div v-for="(item, index) in connectionInfo.content" :key="index">
              <div>{{ index + 1 }}.{{ item }}</div>
            </div>
            <div ref="sendChart" class="lineChart"></div>
          </div>
          <div v-show="description[6].show" class="common">
            <h2>检测结果</h2>
            <div ref="pubRec" class="flex jcc"></div>
            <div v-for="(item, index) in pubRecInfo.content" :key="index">
              <div>{{ index + 1 }}.{{ item }}</div>
            </div>
            <div ref="receiveChart" class="lineChart"></div>
          </div>
        </div>
      </div>
      <div class="step-btn">
        <el-button
          type="primary"
          :disabled="lastBtnDisabled"
          icon="el-icon-arrow-left"
          @click="last"
          >上一步</el-button
        >
        <el-button type="primary" :disabled="nextBtnDisabled" @click="next"
          >下一步<i class="el-icon-arrow-right"></i
        ></el-button>
      </div>
    </el-card>
    <el-card v-else class="summary-card">
      <div slot="header">
        <div class="title">测试报告</div>
        <!-- <div><el-button>退出</el-button></div> -->
      </div>
      <el-collapse accordion>
        <el-collapse-item class="content">
          <template slot="title" v-if="browserInfo.status === 'success'">
            <i class="el-icon-circle-check success ml10 mr10"></i>浏览器兼容性
          </template>
          <template slot="title" v-else>
            <i class="el-icon-circle-close error ml10 mr10"></i>浏览器兼容性
          </template>
          <div v-if="browserInfo.status === 'success'" class="ml10">
            完全支持
          </div>
          <div v-else class="ml10">不支持</div>
        </el-collapse-item>
        <el-collapse-item>
          <template slot="title" v-if="micInfo.status === 'success'">
            <i class="el-icon-circle-check success ml10 mr10"></i>麦克风
          </template>
          <template slot="title" v-else>
            <i class="el-icon-circle-close error ml10 mr10"></i>麦克风
          </template>
          <div v-if="micInfo.status === 'success'" class="ml10">
            麦克风工作正常
          </div>
          <div v-else class="ml10">麦克风工作异常</div>
        </el-collapse-item>
        <el-collapse-item>
          <template slot="title" v-if="speakerInfo.status === 'success'">
            <i class="el-icon-circle-check success ml10 mr10"></i>扬声器
          </template>
          <template slot="title" v-else-if="speakerInfo.status === 'finish'">
            <i class="el-icon-warning warn ml10 mr10"></i>扬声器
          </template>
          <template slot="title" v-else>
            <i class="el-icon-circle-close error ml10 mr10"></i>扬声器
          </template>
          <div v-if="speakerInfo.status === 'success'" class="ml10">
            扬声器工作正常
          </div>
          <div v-else-if="speakerInfo.status === 'finish'" class="ml10">
            跳过相关检测
          </div>
          <div v-else class="ml10">扬声器工作异常</div>
        </el-collapse-item>
        <el-collapse-item>
          <template slot="title" v-if="cameraInfo.status === 'success'">
            <i class="el-icon-circle-check success ml10 mr10"></i>摄像头
          </template>
          <template slot="title" v-else>
            <i class="el-icon-circle-close error ml10 mr10"></i>摄像头
          </template>
          <div v-if="cameraInfo.status === 'success'" class="ml10">
            摄像头工作正常
          </div>
          <div v-else class="ml10">摄像头工作异常</div>
        </el-collapse-item>
        <el-collapse-item>
          <template slot="title" v-if="resolutionInfo.status === 'success'">
            <i class="el-icon-circle-check success ml10 mr10"></i>分辨率
          </template>
          <template slot="title" v-else-if="resolutionInfo.status === 'finish'">
            <i class="el-icon-warning warn ml10 mr10"></i>分辨率
          </template>
          <template slot="title" v-else>
            <i class="el-icon-circle-close error ml10 mr10"></i>分辨率
          </template>
          <div v-if="resolutionInfo.status === 'success'" class="ml10">
            <div>支持分辨率320*180</div>
            <div>支持分辨率640*480</div>
            <div>支持分辨率1280*720</div>
            <div>支持分辨率1920*1080</div>
          </div>
          <div v-else-if="resolutionInfo.status === 'finish'" class="ml10">
            <div
              v-for="(item, index) in resolutionInfo.statusArray"
              :key="index"
            >
              <div v-if="item === 'success'">
                支持分辨率{{ resolutions[index][1] }}*{{
                  resolutions[index][2]
                }}
              </div>
              <div v-else-if="tem === 'finish'">
                不支持分辨率{{ resolutions[index][1] }}*{{
                  resolutions[index][2]
                }}, 但可以通过兼容分辨率进行通话
              </div>
              <div v-else>
                不支持分辨率{{ resolutions[index][1] }}*{{
                  resolutions[index][2]
                }}
              </div>
            </div>
          </div>
          <div v-else class="ml10">
            <div
              v-for="(item, index) in resolutionInfo.statusArray"
              :key="index"
            >
              <div v-if="item === 'success'">
                支持分辨率{{ resolutions[index][1] }}*{{
                  resolutions[index][2]
                }}
              </div>
              <div v-else>
                不支持分辨率{{ resolutions[index][1] }}*{{
                  resolutions[index][2]
                }}
              </div>
            </div>
          </div>
        </el-collapse-item>
        <el-collapse-item>
          <template slot="title" v-if="connectionInfo.status === 'success'">
            <i class="el-icon-circle-check success ml10 mr10"></i>网络连接
          </template>
          <template slot="title" v-else>
            <i class="el-icon-circle-close error ml10 mr10"></i>网络连接
          </template>
          <div v-if="connectionInfo.status === 'success'" class="ml10">
            网络连接正常
          </div>
          <div v-else class="ml10">网络连接异常</div>
        </el-collapse-item>
        <el-collapse-item>
          <template slot="title" v-if="pubRecInfo.status === 'success'">
            <i class="el-icon-circle-check success ml10 mr10"></i>发布接收
          </template>
          <template slot="title" v-else>
            <i class="el-icon-circle-close error ml10 mr10"></i>发布接收
          </template>
          <div v-if="pubRecInfo.status === 'success'" class="ml10">
            发布接收正常
          </div>
          <div v-else class="ml10">发布接收异常</div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script>
    import browserDetect from '../../lib/browser-detect.umd.js';
    import NERTC from 'nertc-web-sdk'
    import config from '../../../config';
    export default {
        name: 'deviceDetection',
        data() {
            return {
                isShowDetectCard: true,
                active: 0,
                processStatus: 'process',
                supportedBrowser: {
                    chrome: [72],
                    safari: [12],
                },
                description: [
                    {
                        show: true,
                        class: 'common',
                        h2: '浏览器检测',
                        content: '在此步骤中，我们将检测运行设备、浏览器类型以及浏览器版本。',
                    },
                    {
                        show: false,
                        class: 'common',
                        h2: '麦克风检测',
                        content: '在此步骤中，我们将检测麦克风是否可以打开。',
                    },
                    {
                        show: false,
                        class: 'common',
                        h2: '扬声器检测',
                        content: '在此步骤中，我们将检测是否可以听到自己讲的话。',
                    },
                    {
                        show: false,
                        class: 'common',
                        h2: '摄像头检测',
                        content: '在此步骤中，我们将检测摄像头是否可以打开。',
                    },
                    {
                        show: false,
                        class: 'common',
                        h2: '分辨率检测',
                        content: '在此步骤中，我们将检测摄像头支持的分辨率。',
                    },
                    {
                        show: false,
                        class: 'common',
                        h2: '网络连接检测',
                        content: '在此步骤中，我们将检测网络连接以及不同分辨率下的发送码率。',
                    },
                    {
                        show: false,
                        class: 'common',
                        h2: '发布接收检测',
                        content:
                            '在此步骤中，我们将检测发布视频与接收视频能力以及音频和视频的接收码率。',
                    },
                ],
                browserInfo: {
                    content: [],
                    done: false,
                    status: 'wait',
                },
                micInfo: {
                    content: '',
                    done: false,
                    status: 'wait',
                },
                speakerInfo: {
                    content: '',
                    done: false,
                    status: 'wait',
                    recordUrl: '',
                },
                cameraInfo: {
                    content: '',
                    done: false,
                    status: 'wait',
                },
                resolutionInfo: {
                    content: [],
                    done: false,
                    status: 'wait',
                    statusArray: [],
                },
                connectionInfo: {
                    content: [],
                    done: false,
                    status: 'wait',
                },
                pubRecInfo: {
                    content: [],
                    done: false,
                    status: 'wait',
                },
                counter: 0,
                token: '',
                isShowSpeakerTest: true,
                isShowAudio: false,
                isShowSpeakerResult: false,
                audioLevel: 0,
                lastBtnDisabled: true, // 检测完成再取消禁用
                nextBtnDisabled: true, // 检测没有完成，禁用next按钮
                resolutions: [
                    ['VIDEO_QUALITY_180p', 320, 180],
                    ['VIDEO_QUALITY_480p', 640, 480],
                    ['VIDEO_QUALITY_720p', 1280, 720],
                    ['VIDEO_QUALITY_1080p', 1920, 1080],
                ],
                sendChartOption: {
                    title: {
                        left: 'center',
                        text: '发送端码率',
                    },
                    legend: {
                        data: [
                            'VIDEO_QUALITY_180p',
                            'VIDEO_QUALITY_480p',
                            'VIDEO_QUALITY_720p',
                            'VIDEO_QUALITY_1080p',
                        ],
                        bottom: 0,
                    },
                    xAxis: {
                        type: 'category',
                        boundaryGap: false,
                        data: [],
                    },
                    yAxis: {
                        type: 'value',
                        name: 'bitrate',
                    },
                    series: [],
                },
                receiveChartOption: {
                    title: {
                        left: 'center',
                        text: '接收端码率',
                    },
                    legend: {
                        data: ['视频', '音频'],
                        bottom: 0,
                    },
                    xAxis: {
                        type: 'category',
                        boundaryGap: false,
                        data: [],
                    },
                    yAxis: {
                        type: 'value',
                        name: 'bitrate',
                    },
                    series: [],
                },
            };
        },
        mounted() {
            this.client = NERTC.createClient({
                appkey: config.appkey,
                debug: true,
            });
            this.subClient = NERTC.createClient({
                appkey: config.appkey,
                debug: true,
            });
            this.browserTest();
            const timer = setInterval(() => {
                if (this.pubRecInfo.done) {
                    clearInterval(timer);
                } else {
                    this.next();
                }
            }, 2000);
        },
        destroyed() {},
        watch: {
            'pubRecInfo.done'() {
                this.lastBtnDisabled = false;
            },
        },
        methods: {
            async destroyStream(localStream) {
                localStream.stop();
                localStream.destroy();
            },
            createStream(client, streamType) {
                const uid = this.counter++;
                const localStream = NERTC.createStream({
                    client: client,
                    uid: uid,
                    ...streamType,
                });
                return localStream;
            },
            last() {
                if (this.lastBtnDisabled) {
                    return;
                }
                if (this.active-- < 0) {
                    this.active = 0;
                }
                this.currentShow();
            },
            next() {
                if (this.nextBtnDisabled) {
                    return;
                }
                if (this.active++ === 6) {
                    // this.active = 6;
                    // 在发布接收检测项完成后点击按钮，进入测试报告页
                    this.isShowDetectCard = false;
                }
                this.currentShow();
            },
            setDone(info, done, status, btnDisabled) {
                if (status) {
                    this[info].status = status;
                }
                this[info].done = done;
                this.nextBtnDisabled = btnDisabled;
            },
            setIsShow(index) {
                this.description.forEach((item) => {
                    item.show = false;
                });
                this.description[index].show = true;
            },
            currentShow() {
                if (this.active === 0) {
                    this.setIsShow(0);
                } else if (this.active === 1) {
                    this.setIsShow(1);
                    if (!this.micInfo.done) {
                        this.micphoneTest();
                    }
                } else if (this.active === 2) {
                    this.setIsShow(2);
                    if (!this.speakerInfo.done) {
                        this.speakerTest();
                    }
                } else if (this.active === 3) {
                    this.setIsShow(3);
                    if (!this.cameraInfo.done) {
                        this.cameraTest();
                    }
                } else if (this.active === 4) {
                    this.setIsShow(4);
                    if (!this.resolutionInfo.done) {
                        this.resolutionTest();
                    }
                } else if (this.active === 5) {
                    this.setIsShow(5);
                    if (!this.connectionInfo.done) {
                        this.connectionTest();
                    }
                } else if (this.active === 6) {
                    this.setIsShow(6);
                    if (!this.pubRecInfo.done) {
                        this.pubRecTest();
                    }
                }
            },
            browserTest() {
                const browserInfo = browserDetect();
                this.browserInfo.status = 'process';
                if (!browserInfo.mobile) {
                    this.browserInfo.content.push('桌面端');
                } else {
                    this.browserInfo.content.push('手机端');
                }
                if (this.supportedBrowser[browserInfo.name]) {
                    this.browserInfo.content.push(`支持的浏览器：${browserInfo.name}`);
                    if (
                        browserInfo.versionNumber >=
                        this.supportedBrowser[browserInfo.name][0]
                    ) {
                        this.browserInfo.content.push(
                            `支持的浏览器版本：${browserInfo.version}`
                        );
                    } else {
                        this.browserInfo.content.push(
                            `不支持的浏览器版本：${browserInfo.version}`
                        );
                        this.setDone('browserInfo', true, 'error', false);
                        return;
                    }
                } else {
                    this.browserInfo.content.push(`不支持的浏览器：${browserInfo.name}`);
                    this.setDone('browserInfo', true, 'error', false);
                    return;
                }
                this.setDone('browserInfo', true, 'success', false);
            },
            async micphoneTest() {
                this.setDone('micInfo', false, 'process', true);
                try {
                    const ms = await navigator.mediaDevices.getUserMedia({ audio: true });
                    this.micInfo.content = '可以拿到音频Track';
                    ms.getTracks().forEach((track) => {
                        track.stop();
                    });
                } catch (error) {
                    this.micInfo.content = `不可以拿到音频Track:${error}`;
                    this.setDone('micInfo', true, 'error', false);
                    return;
                }
                this.setDone('micInfo', true, 'success', false);
            },
            async speakerTest() {
                this.setDone('speakerInfo', false, 'process', true);
                const browserInfo = browserDetect();
                if (browserInfo.name === 'safari') {
                    this.speakerInfo.content = 'Safari不支持录制，跳过相关检测。';
                    this.isShowSpeakerResult = true;
                    this.setDone('speakerInfo', true, 'finish', false);
                    return;
                }
                const localStream = this.createStream(this.client, {
                    audio: true,
                    video: false,
                });
                try {
                    await localStream.init();
                } catch (error) {
                    this.speakerInfo.content = `音频流初始化失败。${error}。`;
                    this.setDone('speakerInfo', true, 'error', false);
                    this.destroyStream(localStream);
                    return;
                }
                if (!localStream.audio) {
                    this.speakerInfo.content = '未发现音频输入设备，退出扬声器检测。';
                    this.setDone('speakerInfo', true, 'error', false);
                    this.destroyStream(localStream);
                    return;
                }
                localStream.startMediaRecording({
                    type: 'audio',
                });
                // 录制时长(接近值)
                const recordTime = 5000;
                const startTs = Date.now();
                let end = false;
                while (!end) {
                    await new Promise((resolve) => {
                        setTimeout(() => {
                            this.audioLevel = localStream.getAudioLevel() * 100;
                            if (Date.now() - startTs > recordTime) {
                                end = true;
                            }
                            resolve();
                        }, 50);
                    });
                }
                try {
                    await localStream.stopMediaRecording({ recordId: 'abc' });
                } catch (error) {
                    console.error(error);
                    this.destroyStream(localStream);
                }
                this.isShowSpeakerTest = false;
                this.isShowAudio = true;
                this.speakerInfo.recordUrl = localStream._record._status.recordUrl;
                this.destroyStream(localStream);
            },
            hearSuccess() {
                this.isShowSpeakerResult = true;
                this.speakerInfo.content = '可以听到自己说的话';
                this.setDone('speakerInfo', true, 'success', false);
            },
            hearFail() {
                this.isShowSpeakerResult = true;
                this.speakerInfo.content = '不能听到自己说的话';
                this.setDone('speakerInfo', true, 'error', false);
            },
            async cameraTest() {
                this.nextBtnDisabled = true;
                this.cameraInfo.status = 'process';
                try {
                    const ms = await navigator.mediaDevices.getUserMedia({ video: true });
                    ms.getTracks().forEach((track) => {
                        track.stop();
                    });
                    this.cameraInfo.content = '可以打开摄像头';
                    this.setDone('cameraInfo', true, 'success', false);
                } catch (error) {
                    this.cameraInfo.content = `不可以打开摄像头。${error}`;
                    this.setDone('cameraInfo', true, 'error', false);
                }
            },
            async resolutionTest() {
                this.nextBtnDisabled = true;
                this.resolutionInfo.status = 'process';
                const ms = await navigator.mediaDevices.getUserMedia({
                    video: true,
                    audio: false,
                });
                const videoTrack = ms.getVideoTracks()[0];
                const capabilities = videoTrack.getCapabilities();
                videoTrack.stop();
                for (const resolution of this.resolutions) {
                    let statusFlag = true;
                    if (
                        resolution[1] > capabilities.width.max ||
                        resolution[2] > capabilities.height.max
                    ) {
                        this.resolutionInfo.content.push(
                            `不支持分辨率 ${resolution[0]}，但仍可通过兼容的分辨率进行通话`
                        );
                        this.resolutionInfo.status = 'finish';
                        this.resolutionInfo.statusArray.push('finish');
                        continue;
                    }
                    const localStream = this.createStream(this.client, {
                        audio: false,
                        video: true,
                    });
                    localStream.setVideoProfile({
                        resolution: NERTC.VIDEO_QUALITY[resolution[0]],
                    });
                    const div = this.$refs.resolution;
                    try {
                        await localStream.init();
                    } catch (error) {
                        this.resolutionInfo.content.push('视频流初始化失败。');
                        this.setDone('resolutionInfo', true, 'error', false);
                        this.destroyStream(localStream);
                        return;
                    }
                    try {
                        await localStream.play(div);
                    } catch (error) {
                        this.resolutionInfo.content.push('视频流播放失败。');
                        this.setDone('resolutionInfo', true, 'error', false);
                        this.destroyStream(localStream);
                        return;
                    }
                    this.resolutionInfo.content.push(
                        `正在使用分辨率${resolution[0]} ${resolution[1]}x${resolution[2]}进行摄像头测试`
                    );
                    localStream.setLocalRenderMode({
                        // 本地视频容器尺寸
                        width: 200,
                        height: 200,
                        cut: false, // 默认不裁剪
                    });
                    for (var i = 0; i < 10; i++) {
                        if (!div.children[0].children[0].videoWidth) {
                            console.log('Video没有播放，等待中。。。');
                            await new Promise((resolve) => {
                                setTimeout(resolve, 500);
                            });
                        }
                    }
                    if (div.children[0].children[0].videoWidth === resolution[1]) {
                        this.resolutionInfo.content.push(
                            `视频的宽度为 ${div.children[0].children[0].videoWidth}`
                        );
                    } else {
                        this.resolutionInfo.content.push(
                            `视频的宽度为 ${div.children[0].children[0].videoWidth}，预期为 ${resolution[1]}，摄像头并不支持该分辨率`
                        );
                        this.resolutionInfo.status = 'error';
                        statusFlag = false;
                    }
                    if (div.children[0].children[0].videoHeight === resolution[2]) {
                        this.resolutionInfo.content.push(
                            `视频的高度为 ${div.children[0].children[0].videoHeight}`
                        );
                    } else {
                        this.resolutionInfo.content.push(
                            `视频的高度为 ${div.children[0].children[0].videoHeight}，预期为 ${resolution[2]}，摄像头并不支持该分辨率`
                        );
                        this.resolutionInfo.status = 'error';
                        statusFlag = false;
                    }
                    if (statusFlag) {
                        this.resolutionInfo.statusArray.push('success');
                    } else {
                        this.resolutionInfo.statusArray.push('error');
                    }
                    this.destroyStream(localStream);
                }
                this.setDone('resolutionInfo', true, '', false);
                if (
                    this.resolutionInfo.status !== 'error' ||
                    this.resolutionInfo.status !== 'finish'
                ) {
                    this.resolutionInfo.status = 'success';
                }
            },
            async connectionTest() {
                this.nextBtnDisabled = true;
                this.connectionInfo.status = 'process';
                const echart = this.$refs.sendChart;
                var myChart = this.$echarts.init(echart);
                for (const resolution of this.resolutions) {
                    const series = this.sendChartOption.series;
                    const data = [];
                    series.push({
                        name: resolution[0],
                        type: 'line',
                        data,
                    });
                    const localStream = this.createStream(this.client, {
                        audio: false,
                        video: true,
                    });
                    const uid = this.counter;
                    try {
                        await this.client.join({
                            channelName: `channel_${uid}`,
                            uid: uid,
                        });
                        this.connectionInfo.content.push(
                            `uid ${uid} 加入房间 channel_${uid} 成功，信令通道可以连通。`
                        );
                    } catch (error) {
                        this.connectionInfo.content.push(
                            `uid ${uid} 加入房间 channel_${uid} 失败，信令通道不可以连通。error:${error}`
                        );
                        this.setDone('connectionInfo', true, 'error', false);
                        this.destroyStream(localStream);
                        return;
                    }
                    localStream.setVideoProfile({
                        resolution: NERTC.VIDEO_QUALITY[resolution[0]],
                    });
                    try {
                        await localStream.init();
                        this.connectionInfo.content.push('视频流初始化成功。');
                    } catch (error) {
                        this.connectionInfo.content.push(`视频流初始化失败。${error}`);
                        this.setDone('connectionInfo', true, 'error', false);
                        this.destroyStream(localStream);
                        return;
                    }
                    const localViewConfig = {
                        width: 200,
                        height: 200,
                    };
                    const div = this.$refs.connection;
                    try {
                        await localStream.play(div);
                    } catch (error) {
                        this.connectionInfo.content.push(`视频播放失败。${error}`);
                        this.setDone('connectionInfo', true, 'error', false);
                        this.destroyStream(localStream);
                        return;
                    }
                    localStream.setLocalRenderMode(localViewConfig);
                    try {
                        await this.client.publish(localStream);
                        this.connectionInfo.content.push(
                            `uid ${uid} 发布音视频流成功，WebRTC通道可以连通。`
                        );
                    } catch (error) {
                        this.connectionInfo.content.push(
                            `uid ${uid} 发布音视频流失败，WebRTC通道不可以连通。${error}`
                        );
                        this.setDone('connectionInfo', true, 'error', false);
                        this.destroyStream(localStream);
                        return;
                    }
                    let videoSendMax = 0;
                    const addPoint = async () => {
                        try {
                            const videoStats = await this.client.getLocalVideoStats();
                            if (videoStats && videoStats[0]) {
                                data.push(videoStats[0].SendBitrate);
                                videoSendMax = Math.max(videoSendMax, videoStats[0].SendBitrate);
                            }
                        } catch (error) {
                            console.error(error);
                        }
                    };
                    addPoint();
                    const interval = setInterval(addPoint, 1000);
                    await new Promise((resolve) => {
                        setTimeout(resolve, 10500);
                    });
                    clearInterval(interval);
                    const browserInfo = browserDetect();
                    if (browserInfo.name === 'safari') {
                        this.connectionInfo.content.push(
                            'Safari不支持getLocalStats，跳过相关检测。'
                        );
                        this.setDone('connectionInfo', true, 'success', false);
                        this.destroyStream(localStream);
                        return;
                    } else {
                        this.connectionInfo.content.push(
                            `Video send Bitrate OK ${videoSendMax}`
                        );
                        myChart.setOption(this.sendChartOption);
                    }
                    this.destroyStream(localStream);
                    await this.client.leave();
                }
                this.setDone('connectionInfo', true, 'success', false);
            },
            async pubRecTest() {
                this.nextBtnDisabled = true;
                this.pubRecInfo.status = 'process';
                const echart = this.$refs.receiveChart;
                var myChart = this.$echarts.init(echart);
                const series = this.receiveChartOption.series;
                const videoData = [];
                const audioData = [];
                series.push(
                    {
                        name: '视频',
                        type: 'line',
                        data: videoData,
                    },
                    {
                        name: '音频',
                        type: 'line',
                        data: audioData,
                    }
                );
                // 发布端
                const localStream = this.createStream(this.client, {
                    audio: true,
                    video: true,
                });
                const uid = this.counter;
                try {
                    await this.client.join({
                        channelName: `channel_${uid}`,
                        uid: uid,
                    });
                    this.pubRecInfo.content.push(
                        `uid ${uid} 加入房间 channel_${uid} 成功。`
                    );
                } catch (error) {
                    this.pubRecInfo.content.push(
                        `uid ${uid} 加入房间 channel_${uid} 失败。${error}`
                    );
                    this.setDone('pubRecInfo', true, 'error', false);
                    this.destroyStream(localStream);
                    return;
                }
                try {
                    await localStream.init();
                    this.pubRecInfo.content.push('音视频流初始化成功。');
                } catch (error) {
                    this.pubRecInfo.content.push(`音视频流初始化失败。${error}`);
                    this.setDone('pubRecInfo', true, 'error', false);
                    this.destroyStream(localStream);
                    return;
                }
                // 接收端
                const subUid = (++this.counter);
                let flag = 0;
                let videoRecvMax = 0;
                let audioRecvMax = 0;
                let interval = null;
                this.subClient.on('stream-subscribed', async (evt) => {
                    // 音频流和视频流
                    flag++;
                    if (flag !== 2) {
                        console.error('Skip');
                        return;
                    }
                    console.warn('订阅别人的流成功的通知');
                    var remoteStream = evt.stream;
                    const div = this.$refs.pubRec;
                    try {
                        remoteStream.play(div);
                    } catch (error) {
                        this.pubRecInfo.content.push(`音视频播放失败。${error}`);
                        this.setDone('pubRecInfo', true, 'error', false);
                        this.destroyStream(localStream);
                        return;
                    }
                    remoteStream.setRemoteRenderMode({
                        width: 200,
                        height: 200,
                    });
                    const addPoint = async () => {
                        try {
                            const videoStats = await this.subClient.getRemoteVideoStats();
                            if (videoStats && videoStats[uid] && videoStats[uid].RecvBitrate) {
                                videoData.push(videoStats[uid].RecvBitrate);
                                videoRecvMax = Math.max(
                                    videoRecvMax,
                                    videoStats[uid].RecvBitrate
                                );
                            }
                        } catch (error) {
                            console.error(error);
                        }
                        try {
                            const audioStats = await this.subClient.getRemoteAudioStats();
                            if (audioStats && audioStats[uid] && audioStats[uid].RecvBitrate) {
                                audioData.push(audioStats[uid].RecvBitrate);
                                audioRecvMax = Math.max(
                                    audioRecvMax,
                                    audioStats[uid].RecvBitrate
                                );
                            }
                        } catch (error) {
                            console.error(error);
                        }
                    };
                    addPoint();
                    interval = setInterval(addPoint, 1000);
                });
                try {
                    await this.subClient.join({
                        channelName: `channel_${uid}`,
                        uid: subUid,
                    });
                    this.pubRecInfo.content.push(
                        `uid ${subUid} 加入房间 channel_${uid} 成功。`
                    );
                } catch (error) {
                    this.pubRecInfo.content.push(
                        `uid ${subUid} 加入房间 channel_${uid} 失败。${error}`
                    );
                    this.setDone('pubRecInfo', true, 'error', false);
                    this.destroyStream(localStream);
                    return;
                }
                try {
                    await this.client.publish(localStream);
                    this.pubRecInfo.content.push('音视频流发布成功');
                } catch (error) {
                    this.pubRecInfo.content.push(`音视频流发布失败。${error}`);
                    this.setDone('pubRecInfo', true, 'error', false);
                    this.destroyStream(localStream);
                    return;
                }
                this.subClient.on('stream-added', async (evt) => {
                    evt.stream.setSubscribeConfig({
                        audio: true,
                        video: true,
                    });
                    try {
                        await this.subClient.subscribe(evt.stream);
                        this.pubRecInfo.content.push('视频流订阅成功');
                    } catch (error) {
                        this.pubRecInfo.content.push(`视频流订阅失败。${error}`);
                        this.setDone('pubRecInfo', true, 'error', false);
                        this.destroyStream(localStream);
                    }
                });
                this.subClient.on('stream-removed', (evt) => {
                    const stream = evt.stream;
                    stream.stop();
                    console.warn('远端流停止订阅，需要更新');
                });
                await new Promise((resolve) => {
                    setTimeout(resolve, 10500);
                });
                clearInterval(interval);
                const browserInfo = browserDetect();
                if (browserInfo.name === 'safari') {
                    this.pubRecInfo.content.push(
                        'Safari不支持getLocalStats，跳过相关检测。'
                    );
                    this.setDone('pubRecInfo', true, 'success', false);
                    this.destroyStream(localStream);
                    return;
                } else {
                    this.pubRecInfo.content.push(`Video Recv Bitrate OK ${videoRecvMax}`);
                    this.pubRecInfo.content.push(`Audio Recv Bitrate OK ${audioRecvMax}`);
                }
                if (videoRecvMax !== 0 || audioRecvMax !== 0){
                    myChart.setOption(this.receiveChartOption);
                }

                try {
                    await this.client.leave();
                    await this.subClient.leave();
                } catch (error) {
                    console.error(error);
                }
                this.destroyStream(localStream);
                this.setDone('pubRecInfo', true, 'success', false);
            },
        },
    };
</script>
<style lang="less" scoped>
.wrapper {
  /deep/ .el-card__body {
    background: rgb(238, 238, 238);
  }
  .detection-card {
    width: 1100px;
    margin: auto;
    margin-top: 50px;
    .heighten {
      height: 500px !important;
    }
    .content {
      display: flex;
      height: 200px;
      .detection-description {
        width: 100%;
        height: 100%;
        padding: 5px;
        .common {
          width: 100%;
          height: 100%;
          background-color: #2196f3;
          border-radius: 3px;
          padding-left: 5px;
          color: #fff;
          h2 {
            margin-bottom: 10px;
          }
        }
      }
      .detection-result {
        width: 100%;
        height: 100%;
        padding: 5px;
        .common {
          width: 100%;
          height: 100%;
          background-color: #fff;
          border-radius: 3px;
          padding-left: 5px;
          overflow: scroll;
          word-break: break-word;
          h2 {
            margin-bottom: 10px;
          }
          div {
            margin-bottom: 10px;
          }
          .lineChart {
            width: 514px;
            height: 300px;
          }
        }
      }
    }
    .step-btn {
      margin-top: 20px;
      padding: 5px;
      display: flex;
      justify-content: space-between;
    }
  }
  .summary-card {
    width: 1100px;
    margin: auto;
    margin-top: 50px;
    .title {
      font-size: 25px;
    }

    /deep/ .el-collapse-item__header {
      font-size: 15px;
    }
    .error {
      color: red;
      font-weight: bold;
    }
    .success {
      color: green;
      font-weight: bold;
    }
    .warn {
      //color: yellow;
      font-weight: bold;
    }
  }
}
</style>
