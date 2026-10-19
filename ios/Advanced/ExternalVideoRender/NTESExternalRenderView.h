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

@interface NTESExternalRenderView : UIView<NERtcEngineVideoRenderSink>

+ (NTESExternalRenderView *)generateExternalRenderViewWithBufferType:(NTESVideoBufferType)bufferType;

@end

NS_ASSUME_NONNULL_END
