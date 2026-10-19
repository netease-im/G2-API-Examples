//
//  ExternalVideoRenderViewController.h
//  NERTC-API-Example-OC
//

#import "basicViewController.h"

NS_ASSUME_NONNULL_BEGIN

/// 外部视频渲染示例。
///
/// 本地和远端视频都不再使用 SDK 内置渲染，而是复用 NERTC Demo 的
/// NTESExternalRenderView / NTESOpenGLView 接收并绘制视频帧。
@interface ExternalVideoRenderViewController : basicViewController

@end

NS_ASSUME_NONNULL_END
