//
//  NTESExternalRenderView.h
//  NERtcDemo
//
//  Created by Simon Blue on 2020/8/3.
//

#import <UIKit/UIKit.h>
#import <NERtcSDK/NERtcSDK.h>

#import "NTESVideoRenderDefine.h"

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSUInteger, NTESExternalRenderViewType) {
    NTESExternalRenderViewTypeOpenGL = 0,
    NTESExternalRenderViewTypeMetal,
};

@interface NTESExternalRenderView : UIView<NERtcEngineVideoRenderSink>

+ (NTESExternalRenderView *)generateExternalRenderViewWithBufferType:(NTESVideoBufferType)bufferType;
+ (NTESExternalRenderView *)generateExternalRenderViewWithType:(NTESExternalRenderViewType)viewType;

@end

NS_ASSUME_NONNULL_END
