
这个开源示例 Demo 主要演示了音视频通话 2.0 NERTC SDK 重要功能的使用示例，您可以通过跑通 Sample Code，体验网易云信音视频通话 2.0 的功能，并参考示例项目源码，快速实现相应功能。

## 示例项目结构
音视频通话 2.0 API Example 包括如下功能：
- 基础功能
    - [音频通话](/Basic/AudioCall)
    - [视频通话](/Basic/VideoCall)

- 进阶功能
    - [视频质量](/Advance/VideoQuality)
    - [快速切换房间](/Advance/FastSwitchRooms)
    - [常用视频配置](/Advance/VideoConfiguration)
    - [外部视频分享](/Advance/ExternalVideoShare)
    - [旁路推流](/Advance/VideoStream)
    - [音频质量](/Advance/AudioQuality)
    - [本地音频录制](/Advance/AudioRecord)
    - [网络测速](/Advance/NetworkTest)
    - [截图和水印](/Advance/SnapshotWatermark)
    - [媒体流加密](/Advance/MediaEncryption)
    - [设备管理](/Advance/DeviceManagement)
- 音频能力
    - [伴音](/AudioCapability/AudioMix)
    - [变声美声](/AudioCapability/AudioEffect)
    - [原始音频回调](/AudioCapability/RawAudioCallback)
    - [外部音频采集&渲染](/AudioCapability/ExternalAudioCapture)
    - [背景音效](/AudioCapability/SoundEffectSetting)
    - [音频主辅流](/AudioCapability/AudioMainSubStream)
- 视频能力
    - [外部视频采集&渲染](/VideoCapability/ExternalVideoCapture) 
    - [屏幕共享](/VideoCapability/ScreenShare) 
    - [原始视频回调](/VideoCapability/RawVideoCallback) 
    - [SEI消息收发](/VideoCapability/SendSEIMsg) 
    - [超分辨率](/VideoCapability/SuperResolution) 
    - [云信美颜](/VideoCapability/Beauty) 
    - [视频主辅流](/VideoCapability/VideoMainSubStream) 
    - [虚拟背景](/VideoCapability/VirtualBackground) 

## <span id="开发环境">开发环境</span>

在开始运行示例项目之前，请确保开发环境满足以下要求：

| 环境要求         | 说明                                                         |
| :---------------- | :------------------------------------------------------------ |
| Android Studio 版本 | Android Studio 3.0 及以上版本  <note type="note">Android Studio 版本编号系统的变更请参考 [Android Studio 版本说明](https://developer.android.google.cn/studio/releases/index.html)。</note>                              |
| Android API 版本 | Level 为 18 或以上版本。                              |
| Android SDK 版本     | Android SDK 29、Android SDK Platform-Tools 29.x.x 或以上版本。                   |
| Gradle 及所需的依赖库| 在 [Gradle Services](https://services.gradle.org/distributions/) 页面下载特定版本的 Gradle 及所需的依赖库。<br>在示例项目源码的 `./gradle/wrapper/gradle-wrapper.properties` 文件中查看对应的版本号。<br>示例项目源码中使用的 Gradle 版本如下：<ul>   <li> 1 对 1 通话： 6.1.1<li> 多人视频通话：5.4.1   |
|Android Gradle 插件|在示例项目源码的 `./build.gradle` 文件中查看对应的版本号。示例项目源码中使用的 Gradle 插件版本如下：<li> 1 对 1 通话： 4.0.1<li> 多人视频通话：3.5.0<br>关于 Android Gradle 插件、Gradle、SDK Tool 之间的版本依赖关系，请参见 [Android Gradle 插件版本说明](https://developer.android.com/studio/releases/gradle-plugin)。
| IDE              | Android Studio                                               |
| Android 设备             | Android 系统 4.3  及以上版本的真机。<note type="note">由于模拟器缺少摄像头及麦克风能力，因此工程需要在真机运行，请确保已正确连接 Android 设备。</note> |




## <span id="前提条件">前提条件</span>
请确认您已完成以下操作：

- <a href="/docs/jcyOTA0ODM/DkyMDM2Mzk" target="_blank">已创建应用</a>。
- <a href="/docs/jcyOTA0ODM/TYzODcyNjE" target="_blank">已开通音视频通话 2.0 服务</a>。

## <span id="获取 App Key">获取 App Key</span>

1. 在<a href="https://app.yunxin.163.com/index#/" target="_blank">网易云信控制台</a>的左侧导航栏中找到该应用，并单击应用名称。
2. 单击 **App Key 管理**。
3. 查看该应用的 App Key。

    ![查看应用的AppKey](https://yx-web-nosdn.netease.im/quickhtml%2Fassets%2Fyunxin%2Fdoc%2FG2-GettingStarted-AppKey.png)

## <span id="快速跑通 Sample Code">快速跑通 Sample Code</span>

::: note note
示例项目需要在 **RTC 调试模式**下使用，此时无需传入 Token。修改鉴权方式的方法请参见 <a href="https://doc.yunxin.163.com/nertc/docs/TQ0MTI2ODQ?platform=android" target="_blank">Token 鉴权</a> 。调试模式建议只在集成开发阶段使用，请在应用正式上线前改回安全模式。
:::

1. 下载示例项目源码仓库至您本地工程。

2. 使用 Android studio 打开示例项目源码仓库。

    在菜单栏中选择 **File** > **Open**，选择示例项目源码仓库所在目录。

    ![打开示例项目源码.png](https://yx-web-nosdn.netease.im/common/a1f48633bd011e46565f2a8f6aa173d9/Deploy.png)


    ::: note note
    本地工程目录请使用英文路径，不要包含中文字符。
    :::

  
3. 在 `Deploy/src/main/java/com/netease/nertc/config/DemoDeploy` 文件中配置 AppKey。

    ```
    //替换为您自己的 App Key
    public static final String APP_KEY = appkey;
    ```


    ![修改AppKey.png](https://yx-web-nosdn.netease.im/common/e1e0a4b183efaa14f71f09f52a4afc80/修改AppKey.png)


4. 运行工程。
    1. 开启设备的**开发者模式**和**USB 调试**功能。将 Android 设备连接到开发电脑，在弹出的授予调试权限对话框中，**授予调试权限**，具体步骤请参见[在硬件设备上运行应用](https://developer.android.com/studio/run/device?hl=zh-cn)。

        可以看到 Android Studio 上方的 **Running Devices** 选项框由下图：

        ![as_no_device.png](https://yx-web-nosdn.netease.im/common/c516d242032d3151dd767ed2e9143d3f/noDevices.png)

        
        变为下图：

        ![as_new_device.png](https://yx-web-nosdn.netease.im/common/dc9d6cec73e8eee67f4bc27fedd9df4a/addedDevice.png)

        
        此时表示设备已成功连接到 Android Studio。

 

    2. 单击 **Run** 按钮，编译并运行示例源码。

        ![run_android_demo_new.png](https://yx-web-nosdn.netease.im/common/225b00746cd98d5a0e258606b53789a1/run.png)

        
    ::: note note
    首次编译示例源码时，如果没有对应的依赖库或者构建工具，Android Studio 会自动下载示例源码，可能需要较长时间，请耐心等待。
    :::


5. 体验音视频通话。

    运行成功后，您可以开始操作Demo。


    ![groupvideocall.png](https://yx-web-nosdn.netease.im/common/20bd6fffa15406ae7269a11af525878b/Demo界面.jpg)



## 常见问题

### 编译时提示 “NDK not configured”，或 “No toolchains found in the NDK toolchains folder for ABI with prefix: mips64el-linux-android”


![NDK版本不匹配.png](https://yx-web-nosdn.netease.im/common/78c6a3deb8dc7f8071b50305986a3c79/NDK版本不匹配.png)


**可能原因：**

Android Studio NDK 版本不匹配。

**问题解决：**

1. 下载对应版本的 NDK，例如 21.xx。

    1. 在 Android Studio 的菜单栏中选择 **Tools** > **SDK Manager**，单击 **SDK Tools** 页签。
    2. 选中 **NDK (Side by side)** 下方待安装的 NDK 版本对应的复选框，例如 21.xx。

    ![安装NDK.png](https://yx-web-nosdn.netease.im/common/db98346be326323c346d7f506f9dcac7/安装NDK.png)


2. 在项目中配置特定版本的 NDK。

    1. 在 Android Studio 的菜单栏中选择 **File** > **Project Structure**，左侧导航栏选择 **SDK Location** 。
    2. 在 **Android NDK location** 中选择已安装的对应版本的 NDK。

    ![配置NDK版本.png](https://yx-web-nosdn.netease.im/common/e0012e941d90236ca3cff442cff6d43f/配置NDK版本.png)



### 手机连接电脑后，Android Studio 中没有出现对应的手机
如果 Android 设备连接电脑后，Android Studio 的 **Running Devices** 中没有出现对应的手机，可能原因如下：
- 您的数据线不支持连接存储。
- 电脑没有安装对应的驱动。请参考下图，安装和您的 Android 设备匹配的驱动。

    ![Google_USB_driver.png](https://yx-web-nosdn.netease.im/common/a312dea2cd1368c4df3df75c14fc0ba0/Google_USB_driver.png)
- Android 设备没有开启**开发者模式**和**USB 调试**，或者连接手机时，在弹出的授予调试权限对话框中，没有授予权限。

  
  
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
