//
//  NTESMetalRenderer.m
//  NERtcDemo
//
//  Created by zhangchenliang on 2023/2/16.
//

#import "NTESMetalRenderer.h"
#import "NTESMetalDevice.h"
#import "NTESMetalTypes.h"

#import <Metal/Metal.h>
#import <MetalKit/MetalKit.h>

static NSString *const kRenderEncoderDebugGroup = @"NTESDrawFrame";
static const NSInteger kMaxInflightBuffers = 1;

@interface NTESMetalRenderer ()

@property (nonatomic, assign) NTESVideoBufferType bufferType;
@property (nonatomic, assign) NTESVideoPixelFormat pixelForamt;

@property (nonatomic, strong) MTKView* destination;
@property (nonatomic, strong) id<MTLCommandQueue> commandQueue;
@property (nonatomic, strong) id<MTLRenderPipelineState> pipelineState;
@property (nonatomic, strong) id<MTLBuffer> positionBuffer;
@property (nonatomic, strong) id<MTLBuffer> texCoordBuffer;

// for YUV
@property (nonatomic, assign) uint32_t luminanceWidth;
@property (nonatomic, assign) uint32_t luminanceHeight;
@property (nonatomic, assign) uint32_t chromaWidth;
@property (nonatomic, assign) uint32_t chromaHeight;
@property (nonatomic, strong) id<MTLTexture> yTexture;
@property (nonatomic, strong) id<MTLTexture> uTexture;
@property (nonatomic, strong) id<MTLTexture> vTexture;

@end

@implementation NTESMetalRenderer {
    dispatch_semaphore_t _inflight_semaphore;
    
    uint32_t _old_frame_width;
    uint32_t _old_frame_height;
    CGFloat _old_render_view_width;
    CGFloat _old_render_view_height;
}

#pragma mark - Public

- (instancetype)initWithBufferType:(NTESVideoBufferType)bufferType
                       pixelFormat:(NTESVideoPixelFormat)pixelFormat {
    self = [super init];
    if (self) {
        _bufferType = bufferType;
        _pixelForamt = pixelFormat;
        
        _luminanceWidth = 0;
        _luminanceHeight = 0;
        _chromaWidth = 0;
        _chromaHeight = 0;
        
        _inflight_semaphore = dispatch_semaphore_create(kMaxInflightBuffers);
        
        _old_frame_width = 0;
        _old_frame_height = 0;
        _old_render_view_width = 0;
        _old_render_view_height = 0;
    }
    
    return self;
}

- (void)addRenderDestination:(MTKView *)destination {
    _destination = destination;
    _destination.device = [[NTESMetalDevice sharedDevice] getDevice];
    _destination.preferredFramesPerSecond = 60;
    _destination.autoResizeDrawable = YES;
    
    [self initRenderResource];
}

- (void)renderVideoFrame:(NERtcVideoFrame *)videoFrame {
    if (![self validateVideoFrame:videoFrame]) {
        NSLog(@"%s, video frame is invalid", __func__);
        
        return;
    }
    
    if (!_destination ||
        !_commandQueue ||
        !_pipelineState ||
        !_positionBuffer ||
        !_texCoordBuffer) {
        NSLog(@"%s, render resource is not prepared", __func__);
        
        return;
    }
    
    @autoreleasepool {
        double beginTime = CACurrentMediaTime() * 1000;
        long result = dispatch_semaphore_wait(_inflight_semaphore, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(1.0 * NSEC_PER_SEC)));
        double endTime = CACurrentMediaTime() * 1000;
        double elapsedTime = endTime - beginTime;
        if (result != 0 || elapsedTime >= 1000) {
            dispatch_semaphore_signal(_inflight_semaphore);
        } else {
            [self adjustVertexDataWithFrameWidth:videoFrame.width
                                     frameHeight:videoFrame.height
                                        viewSize:_destination.bounds.size
                                     contentMode:UIViewContentModeScaleAspectFit];
            [self drawVideoFrame:videoFrame];
        }
    }
}

#pragma mark - Private

- (void)initRenderResource {
    id<MTLDevice> device = [[NTESMetalDevice sharedDevice] getDevice];
    
    _commandQueue = [device newCommandQueue];
    
    id<MTLLibrary> library = [[NTESMetalDevice sharedDevice] getLibrary];
    id<MTLFunction> vertexFunction = [library newFunctionWithName:[self getVertexFunctionName]];
    id<MTLFunction> fragmentFunction = [library newFunctionWithName:[self getFragmentFunctionName]];
    MTLRenderPipelineDescriptor* pipelineDescriptor = [[MTLRenderPipelineDescriptor alloc] init];
    pipelineDescriptor.vertexFunction = vertexFunction;
    pipelineDescriptor.fragmentFunction = fragmentFunction;
    pipelineDescriptor.colorAttachments[0].pixelFormat = _destination.colorPixelFormat;
    pipelineDescriptor.depthAttachmentPixelFormat = MTLPixelFormatInvalid;
    NSError* error = nil;
    _pipelineState = [device newRenderPipelineStateWithDescriptor:pipelineDescriptor error:&error];
    if (error) {
        NSLog(@"Metal: Failed to create pipeline state. %@", error);
        return;
    }
    
    float positions[8] = {
        -1.0, 1.0,
        1.0, 1.0,
        -1.0, -1.0,
        1.0, -1.0
    };
    _positionBuffer = [device newBufferWithBytes:positions length:sizeof(positions) options:MTLResourceCPUCacheModeWriteCombined];
    
    float texCoords[8] = {
        0.0, 0.0,
        1.0, 0.0,
        0.0, 1.0,
        1.0, 1.0
    };
    _texCoordBuffer = [device newBufferWithBytes:texCoords length:sizeof(texCoords) options:MTLResourceCPUCacheModeWriteCombined];
}

- (void)drawVideoFrame:(NERtcVideoFrame *)videoFrame {
    id<MTLCommandBuffer> commandBuffer = [_commandQueue commandBuffer];
    MTLRenderPassDescriptor* renderPassDescriptor = _destination.currentRenderPassDescriptor;
    if (renderPassDescriptor) {
        id<MTLRenderCommandEncoder> commandEncoder = [commandBuffer renderCommandEncoderWithDescriptor:renderPassDescriptor];
        
        __block dispatch_semaphore_t block_semaphore = _inflight_semaphore;
        [commandBuffer addCompletedHandler:^(id<MTLCommandBuffer> _Nonnull buffer) {
            dispatch_semaphore_signal(block_semaphore);
        }];
        
        [commandEncoder pushDebugGroup:kRenderEncoderDebugGroup];
        
        [commandEncoder setRenderPipelineState:_pipelineState];
        [commandEncoder setVertexBuffer:_positionBuffer offset:0 atIndex:NTESMetalVertexBufferIndexPosition];
        [commandEncoder setVertexBuffer:_texCoordBuffer offset:0 atIndex:NTESMetalVertexBufferIndexTextureCoordinates];
        [self uploadTextureWithRenderEncoder:commandEncoder videoFrame:videoFrame];
        [commandEncoder drawPrimitives:MTLPrimitiveTypeTriangleStrip
                           vertexStart:0
                           vertexCount:4
                         instanceCount:1];
        
        [commandEncoder popDebugGroup];
        
        [commandEncoder endEncoding];
        
        [commandBuffer presentDrawable:_destination.currentDrawable];
    }
    
    [commandBuffer commit];
    
//    [commandBuffer waitUntilCompleted];
}

- (void)uploadTextureWithRenderEncoder:(id<MTLRenderCommandEncoder>)renderEncoder
                            videoFrame:(NERtcVideoFrame *)videoFrame {
    if (_bufferType == NTESVideoBufferTypeMetalTexture &&
        _pixelForamt == NTESVideoPixelFormatBGRA) {
        [renderEncoder setFragmentTexture:videoFrame.texture atIndex:NTESMetalTextureIndex0];
    } else if (_bufferType == NTESVideoBufferTypeRawData &&
               _pixelForamt == NTESVideoPixelFormatI420) {
        uint32_t yStride = [videoFrame.strides[0] unsignedIntValue];
        uint32_t uStride = [videoFrame.strides[1] unsignedIntValue];
        uint32_t vStride = [videoFrame.strides[2] unsignedIntValue];
        
        if (!_yTexture ||
            _luminanceWidth != videoFrame.width ||
            _luminanceHeight != videoFrame.height) {
            _luminanceWidth = videoFrame.width;
            _luminanceHeight = videoFrame.height;
            
            MTLTextureDescriptor* textureDescriptor = [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatR8Unorm width:_luminanceWidth height:_luminanceHeight mipmapped:NO];
            textureDescriptor.usage = MTLTextureUsageShaderRead;
            _yTexture = [[[NTESMetalDevice sharedDevice] getDevice] newTextureWithDescriptor:textureDescriptor];
        }
        [_yTexture replaceRegion:MTLRegionMake2D(0, 0, _luminanceWidth, _luminanceHeight) mipmapLevel:0 withBytes:videoFrame.buffer bytesPerRow:yStride];
        
        uint32_t expectedChromaWidth = videoFrame.width / 2;
        uint32_t expectedChromaHeight = videoFrame.height / 2;
        if (!_uTexture ||
            !_vTexture ||
            _chromaWidth != expectedChromaWidth ||
            _chromaHeight != expectedChromaHeight) {
            _chromaWidth = expectedChromaWidth;
            _chromaHeight = expectedChromaHeight;
            
            MTLTextureDescriptor* textureDescriptor = [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:MTLPixelFormatR8Unorm width:_chromaWidth height:_chromaHeight mipmapped:NO];
            textureDescriptor.usage = MTLTextureUsageShaderRead;
            _uTexture = [[[NTESMetalDevice sharedDevice] getDevice] newTextureWithDescriptor:textureDescriptor];
            _vTexture = [[[NTESMetalDevice sharedDevice] getDevice] newTextureWithDescriptor:textureDescriptor];
        }
        [_uTexture replaceRegion:MTLRegionMake2D(0, 0, _chromaWidth, _chromaHeight) mipmapLevel:0 withBytes:videoFrame.uBuffer bytesPerRow:uStride];
        [_vTexture replaceRegion:MTLRegionMake2D(0, 0, _chromaWidth, _chromaHeight) mipmapLevel:0 withBytes:videoFrame.vBuffer bytesPerRow:vStride];
        
        [renderEncoder setFragmentTexture:_yTexture atIndex:NTESMetalTextureIndex0];
        [renderEncoder setFragmentTexture:_uTexture atIndex:NTESMetalTextureIndex1];
        [renderEncoder setFragmentTexture:_vTexture atIndex:NTESMetalTextureIndex2];
    } else {
        // 其他组合暂不支持
        NSLog(@"%s, unsupported combination, buffer type is %lu, pixel format is %lu", __func__, _bufferType, _pixelForamt);
    }
}

- (void)adjustVertexDataWithFrameWidth:(uint32_t)frameWidth
                           frameHeight:(uint32_t)frameHeight
                             viewSize:(CGSize)viewSize
                          contentMode:(UIViewContentMode)contentMode {
    if (frameWidth != _old_frame_width ||
        frameHeight != _old_frame_height ||
        viewSize.width != _old_render_view_width ||
        viewSize.height != _old_render_view_height) {
        NSLog(@"%s, need update vertex data, new frame size is (%u, %u), new view size is (%.1f, %.1f)", __func__, frameWidth, frameHeight, viewSize.width, viewSize.height);
        
        _old_frame_width = frameWidth;
        _old_frame_height = frameHeight;
        _old_render_view_width = viewSize.width;
        _old_render_view_height = viewSize.height;
        
        vector_float2 point = [self getVertexPointWithFrameSize:CGSizeMake(frameWidth, frameHeight) viewSize:viewSize contentMode:contentMode];
        float positions[8] = {
            -point[0], point[1],
            point[0], point[1],
            -point[0], -point[1],
            point[0], -point[1],
        };
        memcpy(_positionBuffer.contents, positions, sizeof(positions));
    }
}

- (vector_float2)getVertexPointWithFrameSize:(CGSize)frameSize
                                    viewSize:(CGSize)viewSize
                                 contentMode:(UIViewContentMode)contentMode {
    vector_float2 point = {1.0, 1.0};
    
    if (frameSize.width == 0 ||
        frameSize.height == 0 ||
        viewSize.width == 0 ||
        viewSize.height == 0) {
        return point;
    }
    
    CGFloat viewAspectRatio = viewSize.width / viewSize.height;
    CGFloat frameAspectRatio = frameSize.width / frameSize.height;
    
    switch (contentMode) {
        case UIViewContentModeScaleAspectFit: {
            if (viewAspectRatio > frameAspectRatio) {
                point[0] = frameAspectRatio / viewAspectRatio;
            } else {
                point[1] = viewAspectRatio / frameAspectRatio;
            }
            break;
        }
        case UIViewContentModeScaleAspectFill: {
            if (viewAspectRatio > frameAspectRatio) {
                point[1] = viewAspectRatio / frameAspectRatio;
            } else {
                point[0] = frameAspectRatio / viewAspectRatio;
            }
            break;
        }
        case UIViewContentModeScaleToFill: {
            break;
        }

        default:
            break;
    }
    
    return point;
}

- (BOOL)validateVideoFrame:(NERtcVideoFrame *)videoFrame {
    if (!videoFrame) {
        return NO;
    }
    
    if (videoFrame.width == 0 || videoFrame.height == 0) {
        return NO;
    }
    
    if ((_bufferType == NTESVideoBufferTypeMetalTexture) &&
        (_pixelForamt == NTESVideoPixelFormatBGRA)) {
        if (videoFrame.texture) {
            return YES;
        } else {
            return NO;
        }
    } else if ((_bufferType == NTESVideoBufferTypeRawData) &&
               (_pixelForamt == NTESVideoPixelFormatI420)) {
        if (!videoFrame.buffer ||
            !videoFrame.uBuffer ||
            !videoFrame.vBuffer ||
            !videoFrame.strides ||
            (videoFrame.strides.count != 3)) {
            return NO;
        }
        
        uint32_t chromaWidth = videoFrame.width / 2;
        if (([videoFrame.strides[0] unsignedIntValue] < videoFrame.width) ||
            ([videoFrame.strides[1] unsignedIntValue] < chromaWidth) ||
            ([videoFrame.strides[2] unsignedIntValue] < chromaWidth)) {
            return NO;
        }
        
        return YES;
    }
    
    return NO;
}

- (NSString *)getVertexFunctionName {
    switch (_bufferType) {
        case NTESVideoBufferTypeRawData:
            return @"i420_vertex_shader";
        case NTESVideoBufferTypeMetalTexture:
            return @"rgb_vertex_shader";
        default:
            return @"rgb_vertex_shader";
    }
}

- (NSString *)getFragmentFunctionName {
    switch (_pixelForamt) {
        case NTESVideoPixelFormatI420:
            return @"i420_fragment_shader";
        case NTESVideoPixelFormatBGRA:
            return @"rgb_fragment_shader";
        default:
            return @"rgb_fragment_shader";;
    }
}

@end
