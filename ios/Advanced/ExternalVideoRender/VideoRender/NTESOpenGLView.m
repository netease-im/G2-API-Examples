//
//  NTESOpenGLView.m
//  NERtcDemo
//
//  Created by Simon Blue on 2020/8/3.
//

#import "NTESOpenGLView.h"
#import "libyuv.h"

@interface NTESOpenGLView ()
{
    SDL_VoutOverlay _overlay;
    Uint16 _pitches[3];
    Uint8 *_pixels[3];
}


@end

@implementation NTESOpenGLView

static NSUInteger gExternalRenderFrameCount = 0;

- (id) initWithFrame:(CGRect)frame format:(UInt32)format
{
    if (self = [super initWithFrame:frame]) {
        
        NSAssert((format == SDL_FCC_I420) || (format == SDL_FCC_BGRA), @"unsupported format");
        
        _overlay.format = format;
        _overlay.planes = (format == SDL_FCC_I420) ? 3 : 1;
        _overlay.pitches = _pitches;
        _overlay.pixels = _pixels;
        _overlay.is_private = 0;
        _overlay.sar_num = 0;
        _overlay.sar_den = 0;
        
        [self setContentMode:UIViewContentModeScaleAspectFit];
    }
    return self;
}
- (void) render: (NSData *)yuvData
          width:(NSUInteger)width
         height:(NSUInteger)height
{
    if (yuvData) {
        _overlay.w = (int)width;
        _overlay.h = (int)height;
        
        _pitches[0] = width;
        _pitches[1] = _pitches[2] = _pitches[0] / 2;
        
        _pixels[0] = (UInt8 *)[yuvData bytes];
        _pixels[1] = _pixels[0] + width * height;
        _pixels[2] = _pixels[0] + width * height * 5 / 4;
        
        [self display:&_overlay clear:NO];
    }
    else {
        [self display:NULL clear:YES];
    }

}

- (void)render:(CVPixelBufferRef)pixelBuffer
{
    if (pixelBuffer != NULL) {
        OSType format = CVPixelBufferGetPixelFormatType(pixelBuffer);
        
        NSAssert(kCVPixelFormatType_32BGRA == format, @"invalid pixel buffer format");
        
        CVPixelBufferLockBaseAddress( pixelBuffer, 0 );
        
        _overlay.w = (int)CVPixelBufferGetWidthOfPlane(pixelBuffer, 0);
        _overlay.h = (int)CVPixelBufferGetHeightOfPlane(pixelBuffer, 0);
        
        _overlay.pitches[0] = CVPixelBufferGetBytesPerRow(pixelBuffer);
        _overlay.pixels[0] = CVPixelBufferGetBaseAddressOfPlane(pixelBuffer, 0);
        
        [self display:&_overlay clear:NO];
        
        CVPixelBufferUnlockBaseAddress( pixelBuffer, 0 );
    }
    else {
        [self display:NULL clear:YES];
    }
}

#pragma mark - NERtcEngineVideoRenderSink
- (void)onNERtcEngineRenderFrame:(NERtcVideoFrame *)frame
{
      gExternalRenderFrameCount++;
      if (gExternalRenderFrameCount == 1 || gExternalRenderFrameCount % 300 == 0) {
          NSLog(@"[ExternalVideoRender] render frame #%lu, size: %dx%d, rotation: %d",
                (unsigned long)gExternalRenderFrameCount,
                frame.width,
                frame.height,
                (int)frame.rotation);
      }

      int dataLength = frame.width * frame.height * 3 / 2;
    
      BOOL rotateWH = NO;
      enum RotationMode mode = kRotate0;
      switch (frame.rotation) {
          case kNERtcVideoRotation_0:
          {
              mode = kRotate0;
              rotateWH = NO;
          }
              break;
          case kNERtcVideoRotation_90:
          {
              mode = kRotate90;
              rotateWH = YES;
          }

              break;
          case kNERtcVideoRotation_180:
          {
              mode = kRotate180;
              rotateWH = NO;
          }

              break;
          case kNERtcVideoRotation_270:
          {
              mode = kRotate270;
              rotateWH = YES;
          }
              break;

          default:
              break;
      }

        int32_t dstWidth = rotateWH ? frame.height : frame.width;
        int32_t dstHeight = rotateWH ? frame.width : frame.height;

 
      int src_stride_y = [frame.strides[0] intValue];
      int src_stride_u = [frame.strides[1] intValue];
      int src_stride_v = [frame.strides[2] intValue];

      uint8_t *src_y = frame.buffer;
//      uint8_t *src_u = src_y + (int)(src_stride_y * frame.height);
//      uint8_t *src_v = src_u + (int)(src_stride_u * frame.height / 2);
      uint8_t *src_u = frame.uBuffer;
      uint8_t *src_v = frame.vBuffer;

      
      int dst_stride_y = dstWidth;
      int dst_stride_u = dst_stride_y / 2;
      int dst_stride_v = dst_stride_y / 2;

      uint8_t *dst_y = (uint8_t *)malloc(dataLength);
      uint8_t *dst_u = dst_y + (int)(dstWidth * dstHeight);
      uint8_t *dst_v = dst_y + (int)(dstWidth * dstHeight * 5 / 4);

    
      //根据角度旋转
      I420Rotate(src_y,  src_stride_y, src_u, src_stride_u, src_v, src_stride_v, dst_y, dst_stride_y, dst_u, dst_stride_u, dst_v, dst_stride_v, frame.width, frame.height, mode);
      NSData *data = [NSData dataWithBytes:dst_y length:dataLength];
      
      [self render:data width:dstWidth height:dstHeight];
      
      free(dst_y);

}



@end
