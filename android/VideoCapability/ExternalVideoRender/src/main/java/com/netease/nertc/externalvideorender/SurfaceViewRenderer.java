/*
 *  Copyright 2015 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.netease.nertc.externalvideorender;

import static com.netease.yunxin.lite.video.render.LiteScalingType.kNERtcVideoScaleCropFill;
import static com.netease.yunxin.lite.video.render.LiteScalingType.kNERtcVideoScaleFit;
import static com.netease.yunxin.lite.video.render.LiteScalingType.kNERtcVideoScaleFullFill;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.graphics.Point;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.netease.lava.api.IVideoRender;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcExternalVideoRenderer;
import com.netease.lava.nertc.sdk.video.NERtcVideoFrame;
import com.netease.lava.webrtc.EglBase;
import com.netease.lava.webrtc.EglRenderer;
import com.netease.lava.webrtc.GlRectDrawer;
import com.netease.lava.webrtc.JavaI420Buffer;
import com.netease.lava.webrtc.Logging;
import com.netease.lava.webrtc.RendererCommon;
import com.netease.lava.webrtc.SurfaceEglRenderer;
import com.netease.lava.webrtc.TextureBufferImpl;
import com.netease.lava.webrtc.ThreadUtils;
import com.netease.lava.webrtc.VideoFrame;

import java.nio.ByteBuffer;

/**
 * Display the video stream on a SurfaceView.
 */
public class SurfaceViewRenderer extends SurfaceView
    implements SurfaceHolder.Callback, IVideoRender, NERtcExternalVideoRenderer, RendererCommon.RendererEvents {
  private String TAG = "SurfaceViewRenderer";

  // Cached resource name.
  private final String resourceName;
  private final RendererCommon.VideoLayoutMeasure videoLayoutMeasure =
      new RendererCommon.VideoLayoutMeasure();
  private final SurfaceEglRenderer eglRenderer;

  // Callback for reporting renderer events. Read-only after initilization so no lock required.
  private RendererCommon.RendererEvents rendererEvents;

  // Accessed only on the main thread.
  private int rotatedFrameWidth;
  private int rotatedFrameHeight;
  private boolean enableFixedSize;
  private int surfaceWidth;
  private int surfaceHeight;
  private boolean mIsMirror;
  private boolean mIsExternalRender = true;
  private int bufferType = NERtcConstants.NERtcExternalVideoRendererBufferType.I420;
  private boolean videoBufferTypePrinted;
  private ByteBuffer i420Buffer = null;

  /**
   * Standard View constructor. In order to render something, you must first call init().
   */
  public SurfaceViewRenderer(Context context) {
    super(context);
    this.resourceName = getResourceName();
    eglRenderer = new SurfaceEglRenderer(resourceName);
    getHolder().addCallback(this);
    getHolder().addCallback(eglRenderer);
  }

  /**
   * Standard View constructor. In order to render something, you must first call init().
   */
  public SurfaceViewRenderer(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.resourceName = getResourceName();
    eglRenderer = new SurfaceEglRenderer(resourceName);
    getHolder().addCallback(this);
    getHolder().addCallback(eglRenderer);
  }

  /**
   * Initialize this class, sharing resources with |sharedContext|. It is allowed to call init() to
   * reinitialize the renderer after a previous init()/release() cycle.
   */
  public void init(EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents, int bufferType, boolean mainStream) {
    init(sharedContext, rendererEvents, EglBase.CONFIG_PLAIN, new GlRectDrawer(), bufferType, mainStream);
  }

  /**
   * Initialize this class, sharing resources with |sharedContext|. The custom |drawer| will be used
   * for drawing frames on the EGLSurface. This class is responsible for calling release() on
   * |drawer|. It is allowed to call init() to reinitialize the renderer after a previous
   * init()/release() cycle.
   */
  public void init(final EglBase.Context sharedContext,
      RendererCommon.RendererEvents rendererEvents, final int[] configAttributes,
      RendererCommon.GlDrawer drawer, int bufferType, boolean mainStream) {
    ThreadUtils.checkIsOnMainThread();
    TAG = "SurfaceViewRenderer_" + "_" + (mainStream ? "main" : "sub");
    this.rendererEvents = rendererEvents;
    this.bufferType = bufferType;
    rotatedFrameWidth = 0;
    rotatedFrameHeight = 0;
    eglRenderer.init(sharedContext, this /* rendererEvents */, configAttributes, drawer);
  }

  /**
   * Block until any pending frame is returned and all GL resources released, even if an interrupt
   * occurs. If an interrupt occurs during release(), the interrupt flag will be set. This function
   * should be called before the Activity is destroyed and the EGLContext is still valid. If you
   * don't call this function, the GL resources might leak.
   */
  public void release() {
    eglRenderer.release();
    i420Buffer = null;
  }

  /**
   * Register a callback to be invoked when a new video frame has been received.
   *
   * @param listener The callback to be invoked. The callback will be invoked on the render thread.
   *                 It should be lightweight and must not call removeFrameListener.
   * @param scale    The scale of the Bitmap passed to the callback, or 0 if no Bitmap is
   *                 required.
   * @param drawerParam   Custom drawer to use for this frame listener.
   */
  public void addFrameListener(
          EglRenderer.FrameListener listener, float scale, RendererCommon.GlDrawer drawerParam) {
    eglRenderer.addFrameListener(listener, scale, drawerParam);
  }

  /**
   * Register a callback to be invoked when a new video frame has been received. This version uses
   * the drawer of the EglRenderer that was passed in init.
   *
   * @param listener The callback to be invoked. The callback will be invoked on the render thread.
   *                 It should be lightweight and must not call removeFrameListener.
   * @param scale    The scale of the Bitmap passed to the callback, or 0 if no Bitmap is
   *                 required.
   */
  public void addFrameListener(EglRenderer.FrameListener listener, float scale) {
    eglRenderer.addFrameListener(listener, scale);
  }

  public void removeFrameListener(EglRenderer.FrameListener listener) {
    eglRenderer.removeFrameListener(listener);
  }

  /**
   * Enables fixed size for the surface. This provides better performance but might be buggy on some
   * devices. By default this is turned off.
   */
  public void setEnableHardwareScaler(boolean enabled) {
    ThreadUtils.checkIsOnMainThread();
    enableFixedSize = enabled;
    updateSurfaceSize();
  }

  @Override
  public void setScalingType(ScalingType type) {
    RendererCommon.ScalingType scalingType;
    switch (type) {
      case SCALE_ASPECT_FILL:
        scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FILL;
        break;
      case SCALE_ASPECT_BALANCED:
        scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;
        break;
      case SCALE_ASPECT_FIT:
      default:
        scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT;
        break;
    }
    this.setScalingType(scalingType);
  }

  @Override
  public void setExternalRender(boolean enable) {
      mIsExternalRender = enable;
  }

  @Override
  public boolean isExternalRender() {
    return mIsExternalRender;
  }

  /**
   * Set if the video stream should be mirrored or not.
   */
  public void setMirror(final boolean mirror) {
    mIsMirror = mirror;
    eglRenderer.setMirror(mirror);
  }

  @Override
  public boolean isMirror() {
    return mIsMirror;
  }

  /**
   * Set how the video will fill the allowed layout area.
   */
  public void setScalingType(RendererCommon.ScalingType scalingType) {
    ThreadUtils.checkIsOnMainThread();
    videoLayoutMeasure.setScalingType(scalingType);
    requestLayout();
  }

  public void setScalingType(RendererCommon.ScalingType scalingTypeMatchOrientation,
      RendererCommon.ScalingType scalingTypeMismatchOrientation) {
    ThreadUtils.checkIsOnMainThread();
    videoLayoutMeasure.setScalingType(scalingTypeMatchOrientation, scalingTypeMismatchOrientation);
    requestLayout();
  }

  public void setScalingType(int type) {
    Logging.i(TAG, "(0x" + Integer.toHexString(hashCode()) + ")setScalingType: " + type);
    switch (type) {
      case kNERtcVideoScaleFit:
        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        break;
      case kNERtcVideoScaleFullFill:
        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        break;
      case kNERtcVideoScaleCropFill:
        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
        break;
    }
  }

  /**
   * Limit render framerate.
   *
   * @param fps Limit render framerate to this value, or use Float.POSITIVE_INFINITY to disable fps
   *            reduction.
   */
  public void setFpsReduction(float fps) {
    eglRenderer.setFpsReduction(fps);
  }

  public void disableFpsReduction() {
    eglRenderer.disableFpsReduction();
  }

  public void pauseVideo() {
    eglRenderer.pauseVideo();
  }

  // VideoSink interface.
  @Override
  public void onFrame(VideoFrame frame) {
//    Logging.i(TAG, "0x" + Integer.toHexString(hashCode())  + ", onFrame type=" + bufferTypeToString(frame.getBuffer().getBufferType()));
//    eglRenderer.onFrame(frame);
  }

  // View layout interface.
  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    ThreadUtils.checkIsOnMainThread();
    Point size =
        videoLayoutMeasure.measure(widthSpec, heightSpec, rotatedFrameWidth, rotatedFrameHeight);
    setMeasuredDimension(size.x, size.y);
    //logD("onMeasure(). New size: " + size.x + "x" + size.y);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    ThreadUtils.checkIsOnMainThread();
    eglRenderer.setLayoutAspectRatio((right - left) / (float) (bottom - top));
    updateSurfaceSize();
  }

  private void updateSurfaceSize() {
    ThreadUtils.checkIsOnMainThread();
    if (enableFixedSize && rotatedFrameWidth != 0 && rotatedFrameHeight != 0 && getWidth() != 0
        && getHeight() != 0) {
      final float layoutAspectRatio = getWidth() / (float) getHeight();
      final float frameAspectRatio = rotatedFrameWidth / (float) rotatedFrameHeight;
      final int drawnFrameWidth;
      final int drawnFrameHeight;
      if (frameAspectRatio > layoutAspectRatio) {
        drawnFrameWidth = (int) (rotatedFrameHeight * layoutAspectRatio);
        drawnFrameHeight = rotatedFrameHeight;
      } else {
        drawnFrameWidth = rotatedFrameWidth;
        drawnFrameHeight = (int) (rotatedFrameWidth / layoutAspectRatio);
      }
      // Aspect ratio of the drawn frame and the view is the same.
      final int width = Math.min(getWidth(), drawnFrameWidth);
      final int height = Math.min(getHeight(), drawnFrameHeight);
      logD("updateSurfaceSize. Layout size: " + getWidth() + "x" + getHeight() + ", frame size: "
          + rotatedFrameWidth + "x" + rotatedFrameHeight + ", requested surface size: " + width
          + "x" + height + ", old surface size: " + surfaceWidth + "x" + surfaceHeight);
      if (width != surfaceWidth || height != surfaceHeight) {
        surfaceWidth = width;
        surfaceHeight = height;
        getHolder().setFixedSize(width, height);
      }
    } else {
      surfaceWidth = surfaceHeight = 0;
      getHolder().setSizeFromLayout();
    }
  }

  // SurfaceHolder.Callback interface.
  @Override
  public void surfaceCreated(final SurfaceHolder holder) {
    ThreadUtils.checkIsOnMainThread();
    surfaceWidth = surfaceHeight = 0;
    updateSurfaceSize();
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {}

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

  private String getResourceName() {
    try {
      return getResources().getResourceEntryName(getId());
    } catch (NotFoundException e) {
      return "";
    }
  }

  /**
   * Post a task to clear the SurfaceView to a transparent uniform color.
   */
  public void clearImage() {
    eglRenderer.clearImage();
  }

  @Override
  public void onFirstFrameRendered() {
    if (rendererEvents != null) {
      rendererEvents.onFirstFrameRendered();
    }
  }

  @Override
  public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {
    if (rendererEvents != null) {
      rendererEvents.onFrameResolutionChanged(videoWidth, videoHeight, rotation);
    }
    int rotatedWidth = rotation == 0 || rotation == 180 ? videoWidth : videoHeight;
    int rotatedHeight = rotation == 0 || rotation == 180 ? videoHeight : videoWidth;
    // run immediately if possible for ui thread tests
    postOrRun(() -> {
      rotatedFrameWidth = rotatedWidth;
      rotatedFrameHeight = rotatedHeight;
      updateSurfaceSize();
      requestLayout();
    });
  }

  @Override
  public void onReceivedFps(int i) {

  }

  @Override
  public void onRendererFps(int i) {

  }

  @Override
  public void onRenderError(String s) {

  }

  @Override
  public void onRenderFrame(NERtcVideoFrame rtcVideoFrame) {
    if (!videoBufferTypePrinted) {
      Logging.i(TAG, "Render videoBufferType: " + rtcVideoFrame.format);
      videoBufferTypePrinted = true;
    }

    if (rtcVideoFrame == null) {
      Logging.e(TAG, "rtcVideoFrame is null");
      return;
    }

    VideoFrame webRTCVideoFrame = null;
    switch (rtcVideoFrame.format) {
      case I420:
        int width = rtcVideoFrame.width;
        int height = rtcVideoFrame.height;

        int chromaHeight = (height + 1) / 2;
        int strideY = rtcVideoFrame.strideY;
        int strideU = rtcVideoFrame.strideU;
        int strideV = rtcVideoFrame.strideV;

        int sizeY = height * strideY;
        int sizeU = chromaHeight * strideU;
        int sizeV = chromaHeight * strideV;


        int length = sizeY + sizeU + sizeV;
        if (rtcVideoFrame.data == null || rtcVideoFrame.data.length < length) {
          Logging.e(TAG, "RTCVideoFrame data length: " + rtcVideoFrame.data.length
                  + " i420 buffer length: " + length);
          break;
        }
        if (i420Buffer == null) {
          i420Buffer = ByteBuffer.allocateDirect(length);
        } else if (i420Buffer.capacity() < length) {
          i420Buffer = ByteBuffer.allocateDirect(length);
        }

        final int posY = 0;
        final int posU = sizeY;
        final int posV = sizeY + sizeU;
        final int endV = posV + sizeV;
        i420Buffer.position(0);
        i420Buffer.put(rtcVideoFrame.data);

        i420Buffer.limit(posU);
        i420Buffer.position(posY);
        ByteBuffer dataY = i420Buffer.slice();

        i420Buffer.limit(posV);
        i420Buffer.position(posU);
        ByteBuffer dataU = i420Buffer.slice();

        i420Buffer.limit(endV);
        i420Buffer.position(posV);
        ByteBuffer dataV = i420Buffer.slice();

        JavaI420Buffer i420Buffer = JavaI420Buffer.wrap(width, height, dataY, strideY, dataU, strideU, dataV, strideV, null);
        webRTCVideoFrame = new VideoFrame(i420Buffer, rtcVideoFrame.rotation, rtcVideoFrame.timeStamp);
        this.i420Buffer.clear();
        break;

      case TEXTURE_RGB:
        VideoFrame.TextureBuffer textureBuffer = new TextureBufferImpl(rtcVideoFrame.width, rtcVideoFrame.height,
                VideoFrame.TextureBuffer.Type.RGB, rtcVideoFrame.textureId,
                RendererCommon.convertMatrixToAndroidGraphicsMatrix(rtcVideoFrame.transformMatrix),
                null, null, null);
        webRTCVideoFrame = new VideoFrame(textureBuffer, rtcVideoFrame.rotation, rtcVideoFrame.timeStamp);
        break;
      case TEXTURE_OES:
        VideoFrame.TextureBuffer oesTexureBuffer = new TextureBufferImpl(rtcVideoFrame.width, rtcVideoFrame.height,
                VideoFrame.TextureBuffer.Type.OES, rtcVideoFrame.textureId,
                RendererCommon.convertMatrixToAndroidGraphicsMatrix(rtcVideoFrame.transformMatrix),
                null, null, null);
        webRTCVideoFrame = new VideoFrame(oesTexureBuffer, rtcVideoFrame.rotation, rtcVideoFrame.timeStamp);
        break;
    }

    if (webRTCVideoFrame == null) {
      Logging.e(TAG, "construct webRTCVideoFrame failed");
      return;
    }

    eglRenderer.onFrame(webRTCVideoFrame);
  }

  @Override
  public int getBufferType() {
    return bufferType;
  }

  private void postOrRun(Runnable r) {
    if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
      r.run();
    } else {
      post(r);
    }
  }

  private void logD(String string) {
    Logging.d(TAG, resourceName + ": " + string);
  }
}
