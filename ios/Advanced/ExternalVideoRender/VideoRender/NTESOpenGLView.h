//
//  NTESOpenGLView.h
//  NERtcDemo
//
//  Created by Simon Blue on 2020/8/3.
//

#import <UIKit/UIKit.h>
#import <NERtcSDK/NERtcSDK.h>
#import "IJKSDLGLView.h"

NS_ASSUME_NONNULL_BEGIN

@interface NTESOpenGLView : NTESIJKSDLGLView<NERtcEngineVideoRenderSink>

 /**
 初始化GLView

 @param frame  frame
 @param format SDL_FCC_I420 或者 SDL_FCC_BGRA

 @return return instance
 */
- (id) initWithFrame:(CGRect)frame format:(UInt32)format;


/**
 渲染紧凑的I420数据

 @param yuvData I420
 @param width   width
 @param height  height
 */
- (void) render: (NSData *)yuvData
          width:(NSUInteger)width
         height:(NSUInteger)height;



/**
 渲染CVPixelBuffer

 @param pixelBuffer 仅支持kCVPixelFormatType_32BGRA格式
 */
- (void)render:(CVPixelBufferRef)pixelBuffer;

@end

NS_ASSUME_NONNULL_END
