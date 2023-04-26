
这个开源示例 Demo 主要演示了音视频通话 2.0 NERTC SDK 重要功能的使用示例，您可以通过跑通 Sample Code，体验网易云信音视频通话 2.0 的功能，并参考示例项目源码，快速实现相应功能。

## 示例项目结构
音视频通话 2.0 API Example 包括如下功能：
- 基础功能
    - [语音通话](/Basic/AudioCall)
    - [视频通话](/Basic/VideoCall)

- 进阶功能
    - [第三方美颜](/Advanced/ThirdBeauty)
    - [云信美颜](/Advanced/NEBeauty)
    - [旁路推流](/Advanced/VideoStream)
    - [快速切换房间](/Advanced/SwitchRoom)
    - [音效伴音](/Advanced/SetBackgroundMusic)
    - [美声变声](/Advanced/AudioChange)
    - [自定义视频采集&渲染](/Advanced/CustomCamera)
    - [通话前网络测试](/Advanced/SpeedTest)
    - [音质设定](/Advanced/SetAudioQuality)
    - [画质设定](/Advanced/SetVideoQuality)

## <span id="开发环境">开发环境</span>
请确认您的开发环境满足以下要求

- Xcode 10.0 或以上版本。
- iOS 9.0 或以上版本且支持音视频的 iOS 设备。支持模拟器运行，但是由于模拟器缺少摄像头及麦克风能力，部分功能无法使用。
- iOS 设备和您的开发电脑已经连接到网络。

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

    Podfile 文件中包括以下内容：

    ```objc
    # Uncomment the next line to define a global platform for your project
    # platform :ios, '9.0'

    target 'NERTC-API-Example-OC' do
      # Comment the next line if you don't want to use dynamic frameworks
      use_frameworks!
      
      # Pods for NERtc_ios
      pod 'NERtcSDK', '4.6.20'
      pod 'SSZipArchive'
      pod 'Masonry'
    end
    ```
    您可以通过修改pod 'NERtcSDK'后的sdk版本号，使用不同版本的sdk
2. cd 到 ios 目录下，执行`pod install`，下载云信sdk以及其他第三方库，双击 `NERTC-API-Example-OC.xcworkspace`，通过 Xcode 打开工程。
3. 在 `NTESAppConfig.h` 文件中填入您的 AppKey。
4. （可选）登录 Apple 开发者账号。您可以参考此步骤登录账号，若已经登录，请忽略该步骤。
  - 打开 Xcode，依次选择左上角菜单的 **Xcode** > **Preferences**。

  ![xcode_preference.jpg](https://yx-web-nosdn.netease.im/common/26cf60702949c4a5690de468a8b99971/xcode_preference.jpg)


  - 依次单击 **Accounts** > 左下角的 **+** > **Apple ID** > **Continue**。

  ![xcode_account.jpg](https://yx-web-nosdn.netease.im/common/aff66bf004426c55e385316dc5b8413a/xcode_account.jpg)
  
  - 输入 Apple ID 和 Password 登录。
  
  ![xcode_login_app_id.jpg](https://yx-web-nosdn.netease.im/common/fc7b4464113da6dd8939a74a75956b40/xcode_login_app_id.jpg)
  
  - [设置签名并添加媒体设备权限](https://doc.yunxin.163.com/docs/jcyOTA0ODM/TM5NzI5MjI?platformId=50192#%E8%AE%BE%E7%BD%AE%E7%AD%BE%E5%90%8D%E5%B9%B6%E6%B7%BB%E5%8A%A0%E5%AA%92%E4%BD%93%E8%AE%BE%E5%A4%87%E6%9D%83%E9%99%90)。
  
   
5. 运行工程。
  - 将 iOS 设备连接到开发电脑，单击 Xcode 上方的的 **Any iOS Device**，在弹出的选项框选择该 iOS 设备。 

  ![xcode_select_device_new.png.jpg](https://yx-web-nosdn.netease.im/common/5bf7b4e6c678580f65a58ca9c2b39834/xcode_select_device_new.png.jpg)
      
  ![xcode_select_real_device_new.jpg](https://yx-web-nosdn.netease.im/common/92e2c3adaf68ada15c6058a8fc5869bb/xcode_select_real_device_new.jpg)
  
  - 单击 **Build** 按钮编译和运行示例源码。
  
  ![xcode_build.jpg](https://yx-web-nosdn.netease.im/common/7c4635d1c30e6636a706cb668c41804c/xcode_build.jpg)
  
  - 运行成功后，您可以开始操作Demo。
  
  
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
