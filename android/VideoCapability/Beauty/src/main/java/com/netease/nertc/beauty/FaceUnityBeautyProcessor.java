package com.netease.nertc.beauty;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.faceunity.nama.FURenderer;
import com.faceunity.nama.utils.CameraUtils;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.video.NERtcVideoCallback;
import com.netease.lava.nertc.sdk.video.NERtcVideoFrame;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

final class FaceUnityBeautyProcessor {
    private static final String TAG = "FaceUnityBeautyProcessor";
    private static final int SKIP_FRAME_COUNT = 5;

    private FURenderer fuRenderer;
    private Handler renderHandler;
    private int skipFrameCount = SKIP_FRAME_COUNT;
    private boolean firstFrame = true;
    private boolean enabled;

    void init(Context context, int cameraFacing) {
        FURenderer.setup(context);
        fuRenderer = new FURenderer.Builder(context)
                .setInputTextureType(FURenderer.INPUT_TEXTURE_EXTERNAL_OES)
                .setCameraFacing(cameraFacing)
                .setInputImageOrientation(CameraUtils.getCameraOrientation(cameraFacing))
                .setRunBenchmark(true)
                .setCreateSticker(false)
                .setCreateMakeup(false)
                .setCreateBodySlim(false)
                .setOnDebugListener((fps, callTime) -> {
                    String fpsValue = String.format(Locale.getDefault(), "%.2f", fps);
                    String callTimeValue = String.format(Locale.getDefault(), "%.2f", callTime);
                    Log.d(TAG, "fps=" + fpsValue + ", callTime=" + callTimeValue);
                })
                .build();
    }

    void start() {
        if (fuRenderer == null || enabled) {
            return;
        }
        enabled = true;
        firstFrame = true;
        skipFrameCount = SKIP_FRAME_COUNT;
        NERtcEx.getInstance().setVideoCallback(new NERtcVideoCallback() {
            @Override
            public boolean onVideoCallback(NERtcVideoFrame videoFrame) {
                return processFrame(videoFrame);
            }
        }, false);
    }

    void stop() {
        enabled = false;
        NERtcEx.getInstance().setVideoCallback(null, false);
        destroySurface();
    }

    void destroy() {
        stop();
        fuRenderer = null;
    }

    private boolean processFrame(NERtcVideoFrame videoFrame) {
        if (!enabled || fuRenderer == null || videoFrame == null || videoFrame.textureId <= 0) {
            return false;
        }
        if (firstFrame) {
            firstFrame = false;
            renderHandler = new Handler(Looper.myLooper());
            fuRenderer.onSurfaceCreated();
            return false;
        }
        int textureId = fuRenderer.onDrawFrameSingleInput(
                videoFrame.textureId,
                videoFrame.width,
                videoFrame.height);
        if (skipFrameCount-- > 0) {
            return false;
        }
        videoFrame.textureId = textureId;
        videoFrame.format = NERtcVideoFrame.Format.TEXTURE_RGB;
        return true;
    }

    private void destroySurface() {
        firstFrame = true;
        skipFrameCount = SKIP_FRAME_COUNT;
        if (fuRenderer == null || renderHandler == null) {
            renderHandler = null;
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        renderHandler.post(new Runnable() {
            @Override
            public void run() {
                fuRenderer.onSurfaceDestroyed();
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            renderHandler = null;
        }
    }
}
