//
//  NTESMetalRenderer.h
//  NERtcDemo
//
//  Created by zhangchenliang on 2023/2/16.
//

#import <Foundation/Foundation.h>
#import <Metal/Metal.h>
#import <MetalKit/MetalKit.h>
#import <NERtcSDK/NERtcSDK.h>

#import "NTESVideoRenderDefine.h"

@interface NTESMetalRenderer : NSObject

- (instancetype)initWithBufferType:(NTESVideoBufferType)bufferType
                       pixelFormat:(NTESVideoPixelFormat)pixelFormat;

- (void)addRenderDestination:(MTKView *)destination;

- (void)renderVideoFrame:(NERtcVideoFrame *)videoFrame;

@end
