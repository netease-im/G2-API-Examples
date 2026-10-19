//
//  NTESExternalRenderView.m
//  NERtcDemo
//
//  Created by Simon Blue on 2020/8/3.
//  Ported from NERtcDemo; DDLogInfo is replaced with NSLog to avoid CocoaLumberjack.
//

#import "NTESExternalRenderView.h"
#import "NTESOpenGLView.h"
#import "NTESMetalView.h"


@implementation NTESExternalRenderView

+ (NTESExternalRenderView *)generateExternalRenderViewWithBufferType:(NTESVideoBufferType)bufferType {
    NTESExternalRenderView* renderView = nil;
    NSLog(@"%s, videoBufferType = %lu", __func__, (unsigned long)bufferType);
    switch (bufferType) {
        case NTESVideoBufferTypeRawData:
            renderView = (NTESExternalRenderView *)[[NTESOpenGLView alloc] initWithFrame:CGRectZero format:SDL_FCC_I420];
//            renderView = (NTESExternalRenderView *)[[NTESMetalView alloc] initWithFrame:CGRectZero bufferType:NTESVideoBufferTypeRawData pixelFormat:NTESVideoPixelFormatI420];
            break;
        case NTESVideoBufferTypeMetalTexture:
            renderView = (NTESExternalRenderView *)[[NTESMetalView alloc] initWithFrame:CGRectZero bufferType:NTESVideoBufferTypeMetalTexture pixelFormat:NTESVideoPixelFormatBGRA];
            break;
        default:
            break;
    }
    
    return renderView;
}

@end
