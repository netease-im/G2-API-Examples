
这个开源示例 Demo 主要演示了音视频通话 2.0 NERTC SDK 重要功能的使用示例，您可以通过跑通 Sample Code，体验网易云信音视频通话 2.0 的功能，并参考示例项目源码，快速实现相应功能。

## 示例项目结构
音视频通话 2.0 API Example 包括如下功能：
- 基础功能
    - [语音通话](/Basic/AudioCall)
    - [视频通话](/Basic/VideoCall)

## <span id="开发环境">开发环境</span>
请确认您的开发环境满足以下要求

- Flutter 1.22.5 及以上版本。
    Dart 2.17.0 及以上版本。
- Android 端开发：
    Android Studio 4.1 及以上版本。
    App 要求 Android 5.0 及以上版本 Android 设备。
    使用 Java 作为开发语言。
- iOS 端开发：
    Xcode 11.0 及以上版本。
    App 要求 iOS 11 及以上版本 iOS 设备。
    请确保您的项目已设置有效的开发者签名。
    使用 Objective-C 作为开发语言。
- 设备和您的开发电脑已经连接到网络。

## <span id="前提条件">前提条件</span>
请确认您已完成以下操作：

- <a href="https://doc.yunxin.163.com/nertc/docs/jE3OTc5NTY?platform=android" target="_blank">已创建应用</a>。
- <a href="https://doc.yunxin.163.com/nertc/docs/jY3MzMwODA?platform=android" target="_blank">已开通音视频通话 2.0 服务</a>。

## <span id="获取 App Key">获取 App Key</span>

1. 在<a href="https://app.yunxin.163.com/index#/" target="_blank">网易云信控制台</a>的左侧导航栏中找到该应用，并单击应用名称。
2. 单击 **App Key 管理**。
3. 查看该应用的 App Key。

    ![查看应用的AppKey](https://yx-web-nosdn.netease.im/quickhtml%2Fassets%2Fyunxin%2Fdoc%2FG2-GettingStarted-AppKey.png)



## <span id="快速跑通Sample Code">快速跑通Sample Code</span>

在运行示例项目之前，请在云信控制台中为指定应用[开通调试模式](/docs/jcyOTA0ODM/TQ0MTI2ODQ?platformId=50002#修改鉴权方式)。调试模式建议只在集成开发阶段使用，请在应用正式上线前改回安全模式。

1. 下载示例项目源码至您本地目录。
2. 在 nertc_api_example/lib/config.dart 文件中配置 AppKey。
    ```dart
    class Config {
        //替换为你自己的AppKey
        static const String APP_KEY = '';
    }
    ```
3. 添加依赖项，并添加必要的设备权限。
4. 运行工程。  
  
  
## 联系我们

- 如果您需要了解详细的官网文档，请参见[音视频通话2.0 产品文档](https://doc.yunxin.163.com/nertc/docs/home-page?platform=android)
- 如果您需要了解完整的API参考，请参见[API参考](https://doc.yunxin.163.com/nertc/api-refer)
- 如果您使用遇到问题，可以先查阅[常见问题](https://faq.yunxin.163.com/kb/main/#/)
- 如果您需要售后技术支持，请[提交工单](https://app.yunxin.163.com/index#/issue/submit)  

## 更多场景方案
网易云信针对 1 对 1 娱乐社交、语聊房、PK 连麦、在线教育等业务场景，推出了一体式、可扩展、功能业务融合的全链路解决方案，帮助客户快速接入、业务及时上线，提高营收增长。
- [1 对 1 娱乐社交](https://github.com/netease-kit/1V1)
- [语聊房](https://github.com/netease-kit/NEChatroom)
- [在线 K 歌](https://github.com/netease-kit/NEKaraoke)
- [一起听](https://github.com/netease-kit/NEListenTogether)
- [PK 连麦](https://github.com/netease-kit/OnlinePK)
- [在线教育](https://github.com/netease-kit/WisdomEducation)
