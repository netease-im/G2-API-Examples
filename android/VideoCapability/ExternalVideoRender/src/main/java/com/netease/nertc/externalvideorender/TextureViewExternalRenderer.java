package com.netease.nertc.externalvideorender;

import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcExternalVideoRenderer;
import com.netease.lava.nertc.sdk.video.NERtcVideoFrame;
import com.netease.lava.webrtc.EglBase;
import com.netease.lava.webrtc.JavaI420Buffer;
import com.netease.lava.webrtc.Logging;
import com.netease.lava.webrtc.RendererCommon;
import com.netease.lava.webrtc.VideoFrame;
import com.netease.yunxin.lite.video.render.LiteTextureView;

import android.content.Context;
import android.util.AttributeSet;

import java.nio.ByteBuffer;

/**
 * External NERtc renderer backed by lava's TextureView implementation.
 */
public class TextureViewExternalRenderer extends LiteTextureView implements NERtcExternalVideoRenderer {
    private static final String TAG = "TextureViewExternalRenderer";
    private int bufferType = NERtcConstants.NERtcExternalVideoRendererBufferType.I420;
    private ByteBuffer i420Buffer;
    private boolean videoBufferTypePrinted;

    public TextureViewExternalRenderer(Context context) {
        super(context);
    }

    public TextureViewExternalRenderer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(EglBase.Context sharedContext, RendererCommon.RendererEvents rendererEvents,
                     int bufferType, boolean mainStream) {
        this.bufferType = bufferType;
        setLogName(mainStream ? "external-main-texture" : "external-sub-texture");
        super.init(sharedContext, rendererEvents);
    }

    @Override
    public void onRenderFrame(NERtcVideoFrame rtcVideoFrame) {
        if (rtcVideoFrame == null) {
            Logging.e(TAG, "NERtcVideoFrame is null");
            return;
        }
        if (!videoBufferTypePrinted) {
            Logging.i(TAG, "Render videoBufferType: " + rtcVideoFrame.format);
            videoBufferTypePrinted = true;
        }

        VideoFrame videoFrame = toWebRtcVideoFrame(rtcVideoFrame);
        if (videoFrame != null) {
            onFrame(videoFrame);
        }
    }

    private VideoFrame toWebRtcVideoFrame(NERtcVideoFrame rtcVideoFrame) {
        switch (rtcVideoFrame.format) {
            case I420:
                return createI420Frame(rtcVideoFrame);
            case TEXTURE_RGB:
                return createTextureFrame(rtcVideoFrame, VideoFrame.TextureBuffer.Type.RGB);
            case TEXTURE_OES:
                return createTextureFrame(rtcVideoFrame, VideoFrame.TextureBuffer.Type.OES);
            default:
                Logging.e(TAG, "Unsupported NERtc video format: " + rtcVideoFrame.format);
                return null;
        }
    }

    private VideoFrame createI420Frame(NERtcVideoFrame frame) {
        int chromaHeight = (frame.height + 1) / 2;
        int sizeY = frame.height * frame.strideY;
        int sizeU = chromaHeight * frame.strideU;
        int sizeV = chromaHeight * frame.strideV;
        int length = sizeY + sizeU + sizeV;
        if (frame.data == null || frame.data.length < length) {
            Logging.e(TAG, "Invalid I420 data length: "
                    + (frame.data == null ? 0 : frame.data.length) + ", expected: " + length);
            return null;
        }

        if (i420Buffer == null || i420Buffer.capacity() < length) {
            i420Buffer = ByteBuffer.allocateDirect(length);
        }
        i420Buffer.clear();
        i420Buffer.put(frame.data, 0, length);

        ByteBuffer dataY = slice(i420Buffer, 0, sizeY);
        ByteBuffer dataU = slice(i420Buffer, sizeY, sizeU);
        ByteBuffer dataV = slice(i420Buffer, sizeY + sizeU, sizeV);
        JavaI420Buffer buffer = JavaI420Buffer.wrap(frame.width, frame.height, dataY,
                frame.strideY, dataU, frame.strideU, dataV, frame.strideV, null);
        return new VideoFrame(buffer, frame.rotation, frame.timeStamp);
    }

    private ByteBuffer slice(ByteBuffer source, int offset, int length) {
        source.limit(offset + length);
        source.position(offset);
        return source.slice();
    }

    private VideoFrame createTextureFrame(NERtcVideoFrame frame, VideoFrame.TextureBuffer.Type type) {
        VideoFrame.TextureBuffer textureBuffer = new com.netease.lava.webrtc.TextureBufferImpl(
                frame.width, frame.height, type, frame.textureId,
                RendererCommon.convertMatrixToAndroidGraphicsMatrix(frame.transformMatrix),
                null, null, null);
        return new VideoFrame(textureBuffer, frame.rotation, frame.timeStamp);
    }

    @Override
    public int getBufferType() {
        return bufferType;
    }
}
