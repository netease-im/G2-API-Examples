//
//  NTESMetalView.h
//  NERtcDemo
//
//  Created by zhangchenliang on 2023/2/16.
//

#import <UIKit/UIKit.h>
#import <NERtcSDK/NERtcSDK.h>

#import "NTESVideoRenderDefine.h"

@interface NTESMetalView : UIView<NERtcEngineVideoRenderSink>

- (instancetype)initWithFrame:(CGRect)frame bufferType:(NTESVideoBufferType)bufferType pixelFormat:(NTESVideoPixelFormat)pixelFormat;

@end
