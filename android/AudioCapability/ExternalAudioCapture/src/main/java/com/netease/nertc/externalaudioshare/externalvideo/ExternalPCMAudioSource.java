package com.netease.nertc.externalaudioshare.externalvideo;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.netease.lava.api.Trace;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.audio.NERtcAudioExternalFrame;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExternalPCMAudioSource extends ExternalAudioSource {
    private static final String TAG = "ExternalPCMAudioSource";
    private static final int CUSTOM_AUDIO_TIMER_DUR = 20;
    private RandomAccessFile pcmFile = null;
    private final AtomicBoolean mRunning = new AtomicBoolean(false);
    private long mFileLen, mPosition;
    private long startRunTime;
    private long postRunnableCount;
    private long lastSyncTimestamp;
    private Handler mHandler;
    private int mSampleLength;
    private int loopCount;
    private int mSampleRate;
    private int mChannels;
    private boolean isSubAudio = false;
    private volatile boolean muteStatus = false;
    private volatile boolean keepPushMuteData = false;


    public ExternalPCMAudioSource(String path, int sampleRate, int channel) {
        if (TextUtils.isEmpty(path)) {
            throw new IllegalArgumentException("Invalid PCM file path!");
        }
        File encodedFile = new File(path);
        if (!encodedFile.exists() || !encodedFile.isFile() || !encodedFile.canRead()) {
            throw new IllegalArgumentException("Illegal pcm file!");
        }
        try {
            mSampleRate = sampleRate;
            mChannels = channel;
            mSampleLength = mSampleRate * mChannels * CUSTOM_AUDIO_TIMER_DUR * 2 / 1000;
            pcmFile = new RandomAccessFile(path, "r");

            pcmFile.seek(0);
            mFileLen = pcmFile.getChannel().size();
            mPosition = 0;
            loopCount = 0;
            Trace.i(TAG, "file length: " + mFileLen);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mFileLen <= 0) {
            throw new RuntimeException("Read pcm file failed");
        }

    }

    private MediaPlayer mediaPlayer;

    @Override
    public boolean start() {
        Trace.i(TAG, "start");
        if (pcmFile == null) {
            Trace.e(TAG, "pcm file parse error cannot start");
            return false;
        }
        if (mRunning.get()) {
            Trace.w(TAG, "already start");
            return false;
        }
        if (mHandler != null) {
            return false;
        }
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        startRunTime = 0;
        mHandler = new Handler(thread.getLooper());
        mRunning.compareAndSet(false, true);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mRunning.get()) {
                    return;
                }
                long nowTime = SystemClock.elapsedRealtime();
                long fallBehindTime = 0;
                if (startRunTime == 0) {
                    startRunTime = nowTime;
                } else {
                    fallBehindTime = (nowTime - startRunTime) - postRunnableCount * CUSTOM_AUDIO_TIMER_DUR;
                }
                postRunnableCount++;

                if (muteStatus && !keepPushMuteData) {
                    postNext(fallBehindTime);
                    return;
                }
                if(muteStatus && keepPushMuteData){
                    pushAudioData(new byte[mSampleLength]);
                    postNext(fallBehindTime);
                    return;
                }
                try{
                    if(mPosition + mSampleLength > mFileLen){
                        mPosition = 0;
                        pcmFile.seek(0);
                        loopCount++;
                        postNext(fallBehindTime);
                        Trace.i(TAG, "data length is wrong, so loop again , loop: " + loopCount);
                        return;
                    }
                    byte[] sampleData = new byte[mSampleLength];
                    int bytes = pcmFile.read(sampleData);
                    mPosition = mPosition + bytes;
                    lastSyncTimestamp = CUSTOM_AUDIO_TIMER_DUR * (mFileLen * loopCount + mPosition) / mSampleLength;
                    pushAudioData(sampleData);
                    postNext(fallBehindTime);
                }catch (Exception e){
                    e.printStackTrace();
                    Trace.w(TAG, "push warning , sub: " + isSubAudio + " , post count: " + postRunnableCount + " , exception: " + Log.getStackTraceString(e));
                }
            }

            private int pushAudioData(byte[] sampleData) {
                NERtcAudioExternalFrame audioFrame = new NERtcAudioExternalFrame();
                audioFrame.audioData = sampleData;
                audioFrame.numberOfChannels = mChannels;
                audioFrame.sampleRate = mSampleRate;
                audioFrame.samplesPerChannel = mSampleRate * CUSTOM_AUDIO_TIMER_DUR / 1000;
                audioFrame.syncTimestamp = lastSyncTimestamp;
                int ret = 0;
                if(isSubAudio){
                    ret = NERtcEx.getInstance().pushExternalSubStreamAudioFrame(audioFrame);
                }else{
                    ret = NERtcEx.getInstance().pushExternalAudioFrame(audioFrame);
                }
                return ret;
            }

            private void postNext(long fallBehindTime) {
                long delay = CUSTOM_AUDIO_TIMER_DUR - fallBehindTime;
                if(delay < 0){
                    Trace.w(TAG, "push warning , sub: " + isSubAudio
                            + " , post count: " + postRunnableCount
                            + " , fall behind: " + fallBehindTime
                            + " , delay: " + delay);
                    delay = 0;
                }
                mHandler.postDelayed(this, delay);
            }
        });
        return true;
    }

    @Override
    public void stop() {
        Trace.i(TAG, "stop");
        mRunning.compareAndSet(true, false);
        if(mHandler != null){
            mHandler.removeCallbacksAndMessages(null);
            mHandler.getLooper().quitSafely();
            mHandler = null;
        }
        if(pcmFile != null){
            try {
                pcmFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            pcmFile = null;
        }
    }

    @Override
    public void mute(boolean mute, boolean keepPushMuteData) {
        if (!mRunning.get()) {
            Trace.w(TAG, "mute warning , not running : is sub " + isSubAudio );
            return;
        }
        Trace.i(TAG, "mute: " + mute + " , keep push: " + keepPushMuteData);
        this.muteStatus = mute;
        this.keepPushMuteData = keepPushMuteData;
    }
}
