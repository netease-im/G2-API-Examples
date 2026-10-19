//
//  NTESVideoRenderDefine.h
//  NERtcDemo
//
//  Created by zhangchenliang on 2023/2/17.
//

#ifndef NTESVideoRenderDefine_h
#define NTESVideoRenderDefine_h

typedef NS_ENUM(NSUInteger, NTESVideoBufferType) {
    // 原始数据
    NTESVideoBufferTypeRawData = 0,
    // Metal 纹理
    NTESVideoBufferTypeMetalTexture
};

typedef NS_ENUM(NSUInteger, NTESVideoPixelFormat) {
    // I420
    NTESVideoPixelFormatI420 = 0,
    // BGRA
    NTESVideoPixelFormatBGRA
};

#endif /* NTESVideoRenderDefine_h */
