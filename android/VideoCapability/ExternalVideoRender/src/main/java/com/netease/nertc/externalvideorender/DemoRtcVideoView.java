package com.netease.nertc.externalvideorender;

import static com.netease.lava.api.IVideoRender.ScalingType.SCALE_ASPECT_BALANCED;
import static com.netease.lava.api.IVideoRender.ScalingType.SCALE_ASPECT_FILL;
import static com.netease.lava.api.IVideoRender.ScalingType.SCALE_ASPECT_FIT;

import android.content.Context;

import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.netease.lava.api.IVideoRender;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.webrtc.EglBase;
import com.netease.lava.webrtc.Logging;
import com.netease.lava.webrtc.VideoFrame;

public class DemoRtcVideoView extends FrameLayout implements IVideoRender {
    public enum RendererType {
        SURFACE,
        TEXTURE
    }

    private String TAG = "DemoRtcVideoView";
    private IVideoRender render;
    private boolean videoBufferTypePrinted;

    public DemoRtcVideoView(@NonNull Context context) {
        super(context);
    }

    public DemoRtcVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(Context context, EglBase.Context eglContext, boolean mainStream, int bufferType) {
        init(context, eglContext, mainStream, bufferType, RendererType.SURFACE);
    }

    public void init(Context context, EglBase.Context eglContext, boolean mainStream, int bufferType,
                     RendererType rendererType) {
        TAG = TAG + "_" + (mainStream ? "main" : "sub");

        if (rendererType == RendererType.TEXTURE) {
            TextureViewExternalRenderer textureRenderer = new TextureViewExternalRenderer(context);
            textureRenderer.init(eglContext, null, bufferType, mainStream);
            render = textureRenderer;
        } else {
            SurfaceViewRenderer surfaceRenderer = new SurfaceViewRenderer(context);
            surfaceRenderer.init(eglContext, null, bufferType, mainStream);
            render = surfaceRenderer;
        }

        render.setExternalRender(true);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        this.addView((View) render, layoutParams);
    }

    @Deprecated
    public void setScalingType(int type) {
        ScalingType scalingType;
        switch (type) {
            case NERtcConstants.VideoScalingType.SCALE_ASPECT_FILL:
                scalingType = SCALE_ASPECT_FILL;
                break;
            case NERtcConstants.VideoScalingType.SCALE_ASPECT_BALANCED:
                scalingType = SCALE_ASPECT_BALANCED;
                break;
            case NERtcConstants.VideoScalingType.SCALE_ASPECT_FIT:
            default:
                scalingType = SCALE_ASPECT_FIT;
                break;
        }
        this.setScalingType(scalingType);
    }

    @Override
    public void setScalingType(ScalingType type) {
        render.setScalingType(type);
    }

    @Override
    public void setVideoBufferType(VideoBufferType bufferType) {
        Logging.i(TAG, "setVideoBufferType: " + bufferType);
        render.setVideoBufferType(bufferType);
    }

    @Override
    public VideoBufferType getVideoBufferType() {
        return render.getVideoBufferType();
    }

    @Override
    public void setExternalRender(boolean enable) {
        render.setExternalRender(enable);
    }

    @Override
    public boolean isExternalRender() {
        return render.isExternalRender();
    }

    @Override
    public void setMirror(boolean mirror) {
        render.setMirror(mirror);
    }

    @Override
    public boolean isMirror() {
        return render.isMirror();
    }

    @Override
    public void clearImage() {
        render.clearImage();
    }

    @Override
    public void onFrame(VideoFrame videoFrame) {
        if (!videoBufferTypePrinted) {
            Logging.i(TAG, "Render videoBufferType: " + bufferTypeToString(videoFrame.getBuffer().getBufferType()));
            videoBufferTypePrinted = true;
        }
        render.onFrame(videoFrame);
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (render != null) {
            ((View) render).setVisibility(visibility);
        }
    }

    private String bufferTypeToString(int type) {
        switch (type) {
            case 1:
                return "I420";
            case 4:
                return "Texture";
            default:
                return "";
        }
    }

    public void release() {
        if (render instanceof SurfaceViewRenderer) {
            ((SurfaceViewRenderer) render).release();
        } else if (render instanceof TextureViewExternalRenderer) {
            ((TextureViewExternalRenderer) render).release();
        }
    }
    public IVideoRender getRenderer() {
        return render;
    }
}
