您可以通过跑通 Sample Code，体验网易云信音视频通话功能。

## 前提条件
请确认您已完成以下操作：

>* 创建应用并获取 App Key。
>* 开通音视频通话 2.0 服务。

## 开发环境要求
请确保开发环境满足以下要求：

>* 开发环境：Microsoft Visual Studio 2017（推荐）及以上版本
>* 开发环境：Unreal Engine 4.25（推荐）及以上版本
>* 操作系统：Microsoft Windows 7 及以上版本
>* 编译器：Microsoft Visual C++ 2017 及以上版本

## 快速跑通 Sample Code
在运行示例项目之前，请在云信控制台中为指定应用开通调试模式。调试模式建议只在集成开发阶段使用，请在应用正式上线前改回安全模式。

1. 下载[示例项目](https://github.com/netease-im/G2-API-Examples)源码至您本地，并在SDK [下载中心](https://doc.yunxin.163.com/nertc/sdk-download?platform=UE)下载 Unreal engine Plugin 压缩包。

2. 进入到 Unreal Engine Sample Code 的 G2-API-Examples\unreal\win 目录，将 NertcPlugin 压缩包解压到 Plugins\ 目录下

3. 右键单击 NertcSampleCode.uproject ，选择 Generate Visual Studio project files, 生成 Visual Studio 解决方案。

    ![](https://yx-web-nosdn.netease.im/common/45f0c71b96872f6e452e7500a5b5f574/%E5%9B%BE%E7%89%871.png)
	
4. 双击上一步骤中生成的 NertcSampleCode.sln，打开解决方案。

5. 在 NertcUserWidget.cpp 配置文件中设置您的 AppKey 和 channeName 等信息。
  ![](https://yx-web-nosdn.netease.im/common/514d0000e4e5587d74cad332f4f084db/%E5%9B%BE%E7%89%872.png)

   | 参数 | 是否必选 | 描述|
   | ------- | ------- |-----|
   |g_appKey |  必选   | 请替换为云信控制台上您的应用对应的 AppKey。获取 AppKey的方法请参见[创建应用并获取 AppKey]   (https://doc.yunxin.163.com/console/docs/TIzMDE4NTA?platform=console#%E8%8E%B7%E5%8F%96-appkey)。    |
   |g_logPath|必选|设置日志路径|
   |g_channelNam|必选|自定义的房间名。运行 Demo 时会加入该房间。|
   |g_userID|必选|自定义的用户 ID。需要保证同一个房间中 g_userID 的唯一性。运行 Demo 时会使用该用户 ID 加入房间。|


6. 将 NertcSampleCode 工程设置为启动项目，按 F5 编译并运行工程。

7. 单击 Play 按钮，运行程序。
  ![](https://yx-web-nosdn.netease.im/common/dff0716bfce56deaef9c07cbf56eedf2/%E5%9B%BE%E7%89%873.png)
  
8. 体验 Demo。

>* 单击 JoinChannel 按钮加入房间，房间名即为步骤 5 中您设置的 g_channelNam。若有两个人同时加入该房间内，即可进行语音通话。

>* 单击 LeaveChannel 离开房间。
