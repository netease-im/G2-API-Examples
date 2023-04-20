package com.netease.nertc.externalvideocapture.externalvideo;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.video.NERtcVideoFrame;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExternalTextureVideoSource extends ExternalVideoSource {
    private static final String TAG = "ExternalTexture";
    private int mWidth;
    private int mHeight;
    private int mFps;
    private int mRotation;
    private FileInputStream mFileInputStream;
    private FileChannel mYUVFileChannel;
    private long mFileLen,mPosition;
    private ByteBuffer mBuffer;
    private final AtomicBoolean mRunning = new AtomicBoolean(false);
    private final Object mFileLock = new Object();
    private Handler mHandler;

    public ExternalTextureVideoSource(String path,int width,int height,int fps,int rotation) {
        Log.i(TAG,"ExternalYuvVideoSource width: " + width + " height: " + height
                + " fps: " + fps + " path: " + path);
        if (TextUtils.isEmpty(path)){
            throw new IllegalArgumentException("Invalid yuv file path!");
        }
        File yuvFile = new File(path);
        if(yuvFile.exists() && yuvFile.isFile() && yuvFile.canRead()){
            mWidth = width;
            mHeight = height;
            mFps = fps;
            mRotation = rotation;

            try {
                mFileInputStream =  new FileInputStream(yuvFile);
                mYUVFileChannel = mFileInputStream.getChannel();
                mFileLen = mYUVFileChannel.size();
                mPosition = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
            mBuffer = ByteBuffer.allocateDirect(width * height * 3 / 2);
        }else{
            throw new IllegalArgumentException("Illegal yuv file!");
        }
    }

    @Override
    public boolean start() {
        Log.i(TAG,"start");
        if(mFileInputStream == null){
            Log.e(TAG,"yuv file parse error cannot start");
            return false;
        }
        if(mRunning.get()) {
            Log.w(TAG,"already start");
            return false;
        }
        if(mHandler == null){
            HandlerThread thread = new HandlerThread(TAG);
            thread.start();
            mHandler = new Handler(thread.getLooper());
            mRunning.compareAndSet(false, true);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mFileLock) {
                        if(mRunning.get()){
                            long start = System.currentTimeMillis();
                            int bytes = 0;
                            mBuffer.clear();
                            try {
                                bytes = mYUVFileChannel.read(mBuffer, mPosition);
                                NERtcVideoFrame videoFrame =  new NERtcVideoFrame();
                                videoFrame.format = NERtcVideoFrame.Format.I420;
                                videoFrame.data = mBuffer.array();
                                videoFrame.width = mWidth;
                                videoFrame.height = mHeight;
                                videoFrame.rotation = mRotation;
                                NERtcEx.getInstance().pushExternalVideoFrame(videoFrame);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            mBuffer.flip();
                            mPosition += bytes;
                            if(mPosition >= mFileLen){
                                mPosition = 0;
                            }
                            long use = System.currentTimeMillis() - start;
                            long delay = 1000/mFps - use;
                            if(delay < 0){
                                delay = 0;
                            }
                            mHandler.postDelayed(this, delay);
                        }

                    }
                }
            });
        }
        return true;
    }

    @Override
    public void stop() {
        Log.i(TAG,"stop");
        synchronized (mFileLock) {
            mRunning.compareAndSet(true, false);
            if(mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler.getLooper().quitSafely();
                mHandler = null;
            }

            if(mFileInputStream != null) {
                try {
                    mFileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mFileInputStream = null;
            }
        }
    }
}
