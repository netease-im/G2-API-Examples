//
//  NTESMetalView.m
//  NERtcDemo
//
//  Created by zhangchenliang on 2023/2/16.
//

#import "NTESMetalView.h"
#import "NTESMetalRenderer.h"

#import <Metal/Metal.h>
#import <MetalKit/MetalKit.h>

@interface NTESMetalView ()<MTKViewDelegate>

@property (nonatomic, strong) MTKView* metalView;
@property (nonatomic, assign) NTESVideoBufferType bufferType;
@property (nonatomic, assign) NTESVideoPixelFormat pixelFormat;
@property (nonatomic, strong) NTESMetalRenderer* metalRenderer;

@property (nonatomic, strong) NERtcVideoFrame* currentFrame;
@property (nonatomic, assign) uint64_t currentFrameTimeStamp;

@end

@implementation NTESMetalView

- (instancetype)initWithFrame:(CGRect)frame
                   bufferType:(NTESVideoBufferType)bufferType
                  pixelFormat:(NTESVideoPixelFormat)pixelFormat {
    self = [super initWithFrame:frame];
    if (self) {
        _bufferType = bufferType;
        _pixelFormat = pixelFormat;
        _currentFrameTimeStamp = 0;
        
        _metalView = [[MTKView alloc] initWithFrame:self.bounds];
        _metalView.delegate = self;

        [self addSubview:_metalView];
        [self initVideoRender];
    }
    
    return self;
}

- (void)initVideoRender {
    _metalRenderer = [[NTESMetalRenderer alloc] initWithBufferType:_bufferType pixelFormat:_pixelFormat];
    [_metalRenderer addRenderDestination:_metalView];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    
    self.metalView.frame = self.bounds;
    
    if ((self.metalView.drawableSize.width >= 16 && self.metalView.drawableSize.width <= [self supportMaxTextureWidth]) &&
        (self.metalView.drawableSize.height >= 16 && self.metalView.drawableSize.height <= [self supportMaxTextureWidth])) {
        // 针对正常的尺寸，恢复drawableSize的自动调整
        self.metalView.autoResizeDrawable = YES;
    }
    
    //FIX //validateTextureDimensions:1227: failed assertion `MTLTextureDescriptor has width (32772) greater than the maximum allowed size of 16384.'
    if (self.metalView.drawableSize.width > [self supportMaxTextureWidth] ||
        self.metalView.drawableSize.height > [self supportMaxTextureWidth]) {
        int width = MIN([self supportMaxTextureWidth], MAX(self.metalView.drawableSize.width, 16));
        int height = MIN([self supportMaxTextureWidth], MAX(self.metalView.drawableSize.height, 16));
        self.metalView.autoResizeDrawable = NO;
        self.metalView.drawableSize = CGSizeMake(width, height);
    }
    
    //FIX //validateRenderPassDescriptor:954: failed assertion `Either set rendertargets in RenderPassDescriptor or set renderTargetWidth and renderTargetHeight.'
    if ((self.metalView.drawableSize.width > 0 && self.metalView.drawableSize.width < 16) || (self.metalView.drawableSize.height > 0 && self.metalView.drawableSize.height < 16)) {
        int width = MAX(16, self.metalView.drawableSize.width);
        int height = MAX(16, self.metalView.drawableSize.height);
        self.metalView.autoResizeDrawable = NO;
        self.metalView.drawableSize = CGSizeMake(width, height);
    }
}

- (NSInteger)supportMaxTextureWidth {
    id<MTLDevice> device = self.metalView.device;
    //See.https://developer.apple.com/metal/Metal-Feature-Set-Tables.pdf
    const int k4096px = 4096;
    const int k8192px = 8192;
    const int k16384px = 16384;
    if (@available(iOS 12, *)) {
        if ([device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily1_v5] || [device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily2_v1]) {
            return k4096px;
        }
        if ([device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily2_v5] || [device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily3_v4]) {
            return k8192px;
        }
        if ([device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily4_v2] || [device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily5_v1]) {
            return k16384px;
        }
    }
    if (@available(iOS 11, *)) {
        if ([device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily1_v4] || [device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily2_v4]) {
            return k4096px;
        }
        if ([device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily3_v3]) {
            return k8192px;
        }
        if ([device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily4_v1]) {
            return k16384px;
        }
    }
    if (@available(iOS 10, *)) {
        if ([device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily1_v3] || [device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily2_v3]) {
            return k4096px;
        }
        if ([device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily3_v2]) {
            return k8192px;
        }
    }
    if ([device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily1_v2] || [device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily2_v2]) {
        return k8192px;
    }
    if ([device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily3_v1]) {
        return k16384px;
    }
    if ([device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily1_v1] || [device supportsFeatureSet:MTLFeatureSet_iOS_GPUFamily2_v1]) {
        return k4096px;
    }
    
    return k16384px;
}

#pragma mark - MTKViewDelegate

- (void)mtkView:(MTKView *)view drawableSizeWillChange:(CGSize)size {
    
}

- (void)drawInMTKView:(MTKView *)view {
    if (!_currentFrame || !_metalRenderer) {
//        NSLog(@"%s, frame not found", __func__);
        return;
    }
    
    if (_currentFrameTimeStamp == _currentFrame.timestamp) {
//        NSLog(@"%s, frame is duplicate", __func__);
        return;
    }
    
    [_metalRenderer renderVideoFrame:_currentFrame];
    
    _currentFrameTimeStamp = _currentFrame.timestamp;
}

#pragma mark - NERtcEngineVideoRenderSink

- (void)onNERtcEngineRenderFrame:(NERtcVideoFrame *)frame {
    _currentFrame = frame;
}

- (NERtcVideoBufferType)getFrameBufferType {
    return (NERtcVideoBufferType)_bufferType;
}

@end
