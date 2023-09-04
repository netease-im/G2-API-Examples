# G2-API-Examples
此仓库包含 NERTC SDK 的 sample code 示例项目
## 示例项目结构
音视频通话 2.0 API Example 包括如下功能：
- 基础功能
    - [单人通话](./sampleCode-Vue/OneToOneVideoCall-Web-Vue)
    - [多人通话](./sampleCode-Vue/GroupVideoCall-Web-Vue)
    - [基础视频直播](./sampleCode-Vue/basicLive-Web-Vue)
    - [H5视频通话](./sampleCode-Vue/H5VideoCall-Web-Vue)

- 进阶功能
    - [音视频设备检测](./sampleCode-Vue/DeviceDetection-Web-Vue)
    - [音视频质量](./sampleCode-Vue/ProfileConfig-Web-Vue)
    - [通话统计](./sampleCode-Vue/MediaStats-Web-Vue)
    - [旁路推流](./sampleCode-Vue/StreamLive-Web-Vue)
    - [伴音](./sampleCode-Vue/AudioMixing-Web-Vue)
    - [音效](./sampleCode-Vue/SoundEffect-Web-Vue)
    - [屏幕共享](./sampleCode-Vue/ScreenShare-Web-Vue)
    - [虚拟背景](./sampleCode-Vue/Segment-Web-Vue)
    - [美颜](./sampleCode-Vue/Beauty-Web-Vue)
    - [自定义视频采集](./sampleCode-Vue/ExternalVideo-Web-Vue)
    - [自定义加密](./sampleCode-Vue/CustomEncryption-Web-Vue)
    - [大小流](./sampleCode-Vue/DualStream-Web-Vue)
    - [多实例](./sampleCode-Vue/MultipleInstances-Web-Vue)
    - [mute控制](./sampleCode-Vue/Mute-Web-Vue)
    - [客户端录制](./sampleCode-Vue/ClientRecord-Web-Vue)
    - [视频截图](./sampleCode-Vue/ScreenShot-Web-Vue)
    - [视频编码属性](./sampleCode-Vue/VideoEncoding-Web-Vue)
    - [水印](./sampleCode-Vue/Watermark-Web-Vue)
    - [云代理](./sampleCode-Vue/CloudProxy-Web-Vue)



## 开发环境
>请确认您的开发环境满足以下要求：
- 安全环境：https 环境或者本地连接 localhost/127.0.0.1 环境
- 浏览器：Chrome 72 及以上版本、Safari 12 及以上版本。更多浏览器兼容性相关请参考 [NERTC Web SDK 支持哪些浏览器](https://doc.yunxin.163.com/nertc/docs/TU5NjUzNjU?platform=web#Web%E7%AB%AF%E6%94%AF%E6%8C%81%E5%93%AA%E4%BA%9B%E6%B5%8F%E8%A7%88%E5%99%A8%E7%B1%BB%E5%9E%8B%E5%92%8C%E7%89%88%E6%9C%AC%EF%BC%9F)

## 前提条件
>请确认您已完成以下操作：
- [创建应用](https://doc.yunxin.163.com/console/docs/TIzMDE4NTA?platform=console)
- [开通音视频 2.0 服务](https://doc.yunxin.163.com/jcyOTA0ODM/docs/DA4NjQzNTU?platform=web)

## <span id="获取 App Key">获取 App Key</span>

1. 在<a href="https://app.yunxin.163.com/index#/" target="_blank">网易云信控制台</a>的左侧导航栏中找到您的应用，并单击应用名称。

2. 在**应用配置**导航栏中，单击**AppKey 管理**页签，查看该应用的 AppKey。


    ![查看应用的AppKey](https://yx-web-nosdn.netease.im/common/ddf69f54b62b1e91f37c78169b112cc5/CreateProjectStep3.png)


## 快速跑通Sample Code
### 体验 Vue 版本的 Demo
1. 请在 sampleCode-Vue 文件中选择要体验的 demo。
2. 在 `config/index.js` 文件中配置 App Key。
```
export default {
    appkey: '', // 请输入自己的appkey
    appSecret: '' // 请输入自己的appSecret
}
```
3. 运行项目（请使用 node 开发环境 version 16+）
```
npm install

npm run dev
```
### 体验原生 js 版本 Demo
1. 请在 sampleCode-js 文件中选择要体验的 demo。
2. 请在 `js/index.js` 文件中配置 App Key。
```
let appkey= ''// 请输入自己的appkey
let appSecret= '' // 请输入自己的appSecret
```
3. 运行项目，可以用 Chrome 或者 Safari 浏览器直接打开 `index.html` 页面直接体验。
## 联系我们

- 如果您需要了解详细的官网文档，请参见[音视频通话2.0 产品文档]([https://doc.yunxin.163.com/nertc/docs/home-page?platform=android](https://doc.yunxin.163.com/nertc/docs/home-page?platform=web))
- 如果您需要了解完整的API参考，请参见[API参考](https://doc.yunxin.163.com/nertc/api-refer/web/typedoc/Latest/zh/html/index.html)
- 如果您使用遇到问题，可以先查阅[常见问题](https://doc.yunxin.163.com/nertc/docs/TU5NjUzNjU?platform=web)
- 如果您需要售后技术支持，请[提交工单](https://app.yunxin.163.com/index#/issue/submit)  

## 更多场景方案
网易云信针对 1 对 1 娱乐社交、语聊房、PK 连麦、在线教育等业务场景，推出了一体式、可扩展、功能业务融合的全链路解决方案，帮助客户快速接入、业务及时上线，提高营收增长。
- [1 对 1 娱乐社交](https://github.com/netease-kit/1V1)
- [语聊房](https://github.com/netease-kit/NEChatroom)
- [在线 K 歌](https://github.com/netease-kit/NEKaraoke)
- [一起听](https://github.com/netease-kit/NEListenTogether)
- [PK 连麦](https://github.com/netease-kit/OnlinePK)
- [在线教育](https://github.com/netease-kit/WisdomEducation)

