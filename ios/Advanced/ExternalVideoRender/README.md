# 外部视频渲染（iOS）

对应 Android 示例：`android/VideoCapability/ExternalVideoRender`。

本示例没有自行实现渲染管线，而是直接复用 NERTC iOS Demo
（`netease/lava` 仓库中的 `netrcdemo`）的渲染组件：

- `NTESExternalRenderView`：按 buffer 类型创建外部渲染视图。
- `NTESOpenGLView` / `IJKSDLGLView`：RawData（I420）渲染，使用 IJKSDL + libyuv。
- `NTESMetalView` / `NTESMetalRenderer`：MetalTexture（BGRA）渲染。
- `vendor/ijksdl`、`vendor/libyuv`、`NTESMetalShader.metallib`：Demo 自带的渲染依赖。

## 示例结构

- `ExternalVideoRenderViewController`：初始化引擎、加入房间、绑定本地和远端外部渲染画布。
- `NTESExternalRenderView`：复用 NERTC Demo 的工厂方法创建渲染视图。
- `VideoRender/`：NERTC Demo 的 OpenGL/Metal 外部渲染实现。
- `vendor/`：NERTC Demo 的 IJKSDL、libyuv 依赖。

## 关键调用

```objc
NTESExternalRenderView *renderView =
    [NTESExternalRenderView generateExternalRenderViewWithBufferType:NTESVideoBufferTypeRawData];

NERtcVideoCanvas *canvas = [[NERtcVideoCanvas alloc] init];
canvas.useExternalRender = YES;
canvas.externalVideoRender = renderView;

// 本地
[[NERtcEngine sharedEngine] setupLocalVideoCanvas:canvas];

// 远端
[[NERtcEngine sharedEngine] setupRemoteVideoCanvas:canvas forUserID:userID];
```

默认使用 `NTESVideoBufferTypeRawData`，对应 Demo 的 `NTESOpenGLView`，
回调 I420 数据并由 libyuv 处理旋转。也可以改为
`NTESVideoBufferTypeMetalTexture`，直接复用 `NTESMetalView` 绘制 SDK 回调的
Metal 纹理。

## 注意事项

- 外部渲染只影响本端看到画面，不影响编码、发送和远端画面。
- `useExternalRender = YES` 时不要设置 `container`，两者不要混用。
- 外部渲染不支持在通话中与内置渲染来回切换，建议在加入房间前确定。
- 本目录中的 `NTES*`、`ijksdl` 和 `libyuv` 均来自 NERTC iOS Demo，升级 Demo
  时同步替换即可。
- `NTESMetalShader.metallib` 是 Demo 预编译产物；本机未安装 Metal Toolchain
  时也可以直接构建。

## 运行

1. 在 `Debug/NTESAppConfig.h` 中配置 AppKey。
2. 运行 Demo，在首页选择“外部视频渲染”。
3. 输入相同的房间号、不同的用户 ID，可在两台设备上查看本地和远端画面。
