package com.netease.nertc.snapshotwatermark;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.netease.lava.api.IVideoRender;
import com.netease.lava.nertc.sdk.LastmileProbeResult;
import com.netease.lava.nertc.sdk.NERtc;
import com.netease.lava.nertc.sdk.NERtcCallbackEx;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.stats.NERtcAudioVolumeInfo;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcTakeSnapshotCallback;
import com.netease.lava.nertc.sdk.video.NERtcVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.lava.nertc.sdk.watermark.NERtcVideoWatermarkConfig;
import com.netease.lava.nertc.sdk.watermark.NERtcVideoWatermarkImageConfig;
import com.netease.lava.nertc.sdk.watermark.NERtcVideoWatermarkTextConfig;
import com.netease.nertc.config.DemoDeploy;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;

public class SnapshotWatermarkActivity extends AppCompatActivity implements NERtcCallbackEx,View.OnClickListener{
    private static final String TAG = "SnapshotWatermark";

    private static final String Font_DIR = "Fonts";
    private static final String Img_DIR = "Image";
    private NERtcVideoView mRemoteUserVv;
    private Button mStartJoinBtn;
    private EditText mRoomIdEt;
    private EditText mUserIdEt;
    private NERtcVideoView mLocalUserVv;
    private ImageView mBackIv;
    private RelativeLayout mContainer;
    private boolean mJoinChannel = false;
    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private String mRoomId;
    private long mUserId;
    private Button mlocalshotBtn;
    private Button mRemoteshotBtn;
    private Button mImgWatermarkBtn;
    private Button mTextWatermarkBtn;
    private long mRemoteUserId;
    private String mImagePath;
    private String mFontPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snapshot_watermark);
        initView();
    }

    private void initView() {
        mContainer = findViewById(R.id.rl_container);
        mBackIv = findViewById(R.id.iv_back);
        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mRoomIdEt = findViewById(R.id.et_room_id);
        mUserIdEt = findViewById(R.id.et_user_id);
        mLocalUserVv = findViewById(R.id.vv_local_user);
        mlocalshotBtn = findViewById(R.id.btn_local_snapshot);
        mRemoteshotBtn = findViewById(R.id.btn_remote_snapshot);
        mImgWatermarkBtn = findViewById(R.id.btn_image_watermark);
        mRemoteUserVv = findViewById(R.id.vv_remote_user_1);
        mUserId = new Random().nextInt(100000);
        mUserIdEt.setText(String.valueOf(mUserId));
        mTextWatermarkBtn = findViewById(R.id.btn_text_watermark);

        mStartJoinBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);
        mlocalshotBtn.setOnClickListener(this);
        mRemoteshotBtn.setOnClickListener(this);
        mImgWatermarkBtn.setOnClickListener(this);
        mTextWatermarkBtn.setOnClickListener(this);

        initOptions();

    }
    /**
     * 初始化字体
     */
    protected void initOptions(){
        new Thread(() -> {
            String fontRoot = ensureFontDirectory();
            String imageRoot = ensureFontDirectory();
            mFontPath = extractFontFile(fontRoot, "宋体.ttf");
            mImagePath = extractImgFile(imageRoot, "bag.png");
        }).start();
    }

    private String extractImgFile(String path, String name) {
        copyAssetToFile(this, Img_DIR + "/" + name, path, name);
        return new File(path, name).getAbsolutePath();
    }

    private String extractFontFile(String path, String name) {
        copyAssetToFile(this, Font_DIR + "/" + name, path, name);
        return new File(path, name).getAbsolutePath();
    }
    private void copyAssetToFile(Context context, String assetsName,
                                 String savePath, String saveName) {

        File dir = new File(savePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File destFile = new File(dir, saveName);
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = context.getResources().getAssets().open(assetsName);
            if (destFile.exists() && inputStream.available() == destFile.length()) {
                return;
            }
            destFile.deleteOnExit();
            outputStream = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int count;
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuiet(inputStream);
            closeQuiet(outputStream);
        }
    }
    private void closeQuiet(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String ensureFontDirectory() {
        File dir = getExternalFilesDir(Font_DIR);
        if (dir == null) {
            dir = getDir(Font_DIR, 0);
        }
        if (dir != null) {
            dir.mkdirs();
            return dir.getAbsolutePath();
        }
        return "";
    }
    /**
     * 设置本地音频可用性
     * @param enable
     */
    private void setLocalAudioEnable(boolean enable) {
        mEnableLocalAudio = enable;
        NERtcEx.getInstance().enableLocalAudio(mEnableLocalAudio);
    }

    /**
     * 设置本地视频的可用性
     */
    private void setLocalVideoEnable(boolean enable) {
        mEnableLocalVideo = enable;
        NERtcEx.getInstance().enableLocalVideo(mEnableLocalVideo);
        mLocalUserVv.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
    }
    /**
     * 退出房间并关闭页面
     */
    private void exit(){
        if(mJoinChannel){
            leaveChannel();
        }
        finish();
    }
    private boolean leaveChannel(){
        mJoinChannel = false;
        setLocalAudioEnable(false);
        setLocalVideoEnable(false);
        int ret = NERtcEx.getInstance().leaveChannel();
        return ret == NERtcConstants.ErrorCode.OK;
    }
    private void userInfo() {
        Editable roomIdEdit = mRoomIdEt.getText();
        if(roomIdEdit == null || roomIdEdit.length() <= 0){
            return;
        }
        Editable userIdEdit = mUserIdEt.getText();
        if(userIdEdit == null || userIdEdit.length() <= 0){
            return;
        }
        mRoomId = roomIdEdit.toString();
        mUserId = Long.parseLong(userIdEdit.toString());
    }
    private void setupNERtc() {
        NERtcParameters parameters = new NERtcParameters();
        NERtcEx.getInstance().setParameters(parameters); //先设置参数，后初始化

        NERtcOption options = new NERtcOption();

        if (BuildConfig.DEBUG) {
            options.logLevel = NERtcConstants.LogLevel.INFO;
        } else {
            options.logLevel = NERtcConstants.LogLevel.WARNING;
        }

        try {
            NERtcEx.getInstance().init(getApplicationContext(), DemoDeploy.APP_KEY, this, options);
        } catch (Exception e) {
            // 可能由于没有release导致初始化失败，release后再试一次
            NERtcEx.getInstance().release();
            try {
                NERtcEx.getInstance().init(getApplicationContext(), DemoDeploy.APP_KEY, this, options);
            } catch (Exception ex) {
                Toast.makeText(this, "SDK初始化失败", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        setLocalAudioEnable(true);
        setLocalVideoEnable(true);
    }
    private void setuplocalVideo() {
        mLocalUserVv.setZOrderMediaOverlay(false);
        mLocalUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtcEx.getInstance().setupLocalVideoCanvas(mLocalUserVv);
    }
    private void setupRemoteVideo(long userId) {
        mRemoteUserVv.setZOrderMediaOverlay(true);
        mRemoteUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtc.getInstance().setupRemoteVideoCanvas(mRemoteUserVv, userId);
    }
    private void joinChannel(String roomId, long userId) {
        NERtcEx.getInstance().joinChannel("", roomId, userId);
    }
    private void startPushStream() {
        if(!mJoinChannel) {
            userInfo();
            setupNERtc();
            setuplocalVideo();
            joinChannel(mRoomId, mUserId);
        }
    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.iv_back){
            exit();
        }else if(id == R.id.btn_join_channel){
            startPushStream();
        }else if(id == R.id.btn_local_snapshot){
            if(mJoinChannel)
                localSnapshot();
        }else if(id == R.id.btn_remote_snapshot){
            if(mJoinChannel && mRemoteUserVv.getTag() != null)
                remoteSnapshot();
        }else if(id == R.id.btn_image_watermark){
            if(mJoinChannel)
                setWaterMark();
        }else if(id == R.id.btn_text_watermark){
            if(mJoinChannel)
                setTextMark();
        }
    }

    private void setTextMark() {
        NERtcVideoWatermarkConfig  config = new NERtcVideoWatermarkConfig();
        config.textWatermark = new NERtcVideoWatermarkTextConfig();
        config.watermarkType = NERtcVideoWatermarkConfig.WatermarkType.kNERtcWatermarkTypeText;
        config.textWatermark.content = "NERtc很棒！";
        config.textWatermark.fontPath = mFontPath;
        config.textWatermark.fontSize = 30;

        NERtcEx.getInstance().setLocalVideoWatermarkConfigs(NERtcVideoStreamType.kNERtcVideoStreamTypeMain, config);
    }

    private void setWaterMark() {
        NERtcVideoWatermarkConfig  config = new NERtcVideoWatermarkConfig();
        config.imageWatermark = new NERtcVideoWatermarkImageConfig();
        config.watermarkType = NERtcVideoWatermarkConfig.WatermarkType.kNERtcWatermarkTypeImage;
        config.imageWatermark.imagePaths = new ArrayList<>();
        config.imageWatermark.imagePaths.add(mImagePath);

        NERtcEx.getInstance().setLocalVideoWatermarkConfigs(NERtcVideoStreamType.kNERtcVideoStreamTypeMain, config);
    }

    private void remoteSnapshot() {
        NERtcEx.getInstance().takeRemoteSnapshot(mRemoteUserId, NERtcVideoStreamType.kNERtcVideoStreamTypeMain, new NERtcTakeSnapshotCallback() {
            @Override
            public void onTakeSnapshotResult(int errorCode, Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(bitmap == null) {
                            Toast.makeText(SnapshotWatermarkActivity.this, "远端主流截图失败 uid: " + mRemoteUserId,Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SnapshotWatermarkActivity.this, "远端主流截图成功 uid: " + mRemoteUserId,Toast.LENGTH_SHORT).show();
                            saveBitmapToJpeg(mRemoteUserId,NERtcVideoStreamType.kNERtcVideoStreamTypeMain,bitmap);
                        }
                    }
                });
            }
        });
    }
    private void localSnapshot() {
        NERtcEx.getInstance().takeLocalSnapshot(NERtcVideoStreamType.kNERtcVideoStreamTypeMain,new NERtcTakeSnapshotCallback() {
            @Override
            public void onTakeSnapshotResult(int errorCode, Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(bitmap == null) {
                            Toast.makeText(SnapshotWatermarkActivity.this,"本地主流截图失败",Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SnapshotWatermarkActivity.this,"本地主流截图成功",Toast.LENGTH_SHORT).show();
                            saveBitmapToJpeg(mUserId, NERtcVideoStreamType.kNERtcVideoStreamTypeMain, bitmap);
                        }
                    }
                });
            }
        });
    }
    private void saveBitmapToJpeg(long uid, NERtcVideoStreamType type, Bitmap bitmap) {
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        Calendar now = new GregorianCalendar();
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String fileName = simpleDate.format(now.getTime()) + "_" + uid + "_"
                + (type == NERtcVideoStreamType.kNERtcVideoStreamTypeMain ? "main" : "sub") + bitmap.getWidth() + "x" + bitmap.getHeight();
        try {
            File file = new File(dir + fileName + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onJoinChannel(int result, long channelId, long elapsed, long l2) {
        Log.i(TAG, "onJoinChannel result: " + result + " channelId: " + channelId + " elapsed: " + elapsed);
        if(result == NERtcConstants.ErrorCode.OK){
            mJoinChannel = true;
        }
    }

    @Override
    public void onLeaveChannel(int result) {
        Log.i(TAG, "onLeaveChannel result: " + result);
        NERtc.getInstance().release();
        finish();
    }

    @Override
    public void onUserJoined(long userId) {
        Log.i(TAG, "onUserJoined userId: " + userId);
        mRemoteUserId = userId;
        if(mRemoteUserVv.getTag() == null){
            setupRemoteVideo(userId);
            mRemoteUserVv.setTag(userId);
        }
    }

    @Override
    public void onUserLeave(long userId, int i) {
        Log.i(TAG, "onUserLeave uid: " + userId);
        NERtcVideoView userView = mContainer.findViewWithTag(userId);
        if(userView != null){
            //设置TAG为null，代表当前没有订阅
            userView.setTag(null);
            NERtcEx.getInstance().subscribeRemoteVideoStream(userId, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, false);
            //不展示远端
            userView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserAudioStart(long l) {

    }

    @Override
    public void onUserAudioStop(long l) {

    }

    @Override
    public void onUserVideoStart(long userId, int profile) {
        Log.i(TAG, "onUserVideoStart uid: " + userId + " profile: " + profile);
        NERtcVideoView userView = mContainer.findViewWithTag(userId);
        NERtcEx.getInstance().subscribeRemoteVideoStream(userId, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, true);
        userView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUserVideoStop(long userId) {
        Log.i(TAG, "onUserVideoStop, uid=" + userId);
        NERtcVideoView userView = mContainer.findViewWithTag(userId);
        if(userView != null){
            userView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onDisconnect(int i) {

    }

    @Override
    public void onClientRoleChange(int i, int i1) {

    }

    @Override
    public void onUserSubStreamVideoStart(long l, int i) {

    }

    @Override
    public void onUserSubStreamVideoStop(long l) {

    }

    @Override
    public void onUserAudioMute(long l, boolean b) {

    }

    @Override
    public void onUserVideoMute(long l, boolean b) {

    }

    @Override
    public void onUserVideoMute(NERtcVideoStreamType neRtcVideoStreamType, long l, boolean b) {

    }

    @Override
    public void onFirstAudioDataReceived(long l) {

    }

    @Override
    public void onFirstVideoDataReceived(long l) {

    }

    @Override
    public void onFirstVideoDataReceived(NERtcVideoStreamType neRtcVideoStreamType, long l) {

    }

    @Override
    public void onFirstAudioFrameDecoded(long l) {

    }

    @Override
    public void onFirstVideoFrameDecoded(long l, int i, int i1) {

    }

    @Override
    public void onFirstVideoFrameDecoded(NERtcVideoStreamType neRtcVideoStreamType, long l, int i, int i1) {

    }

    @Override
    public void onUserVideoProfileUpdate(long l, int i) {

    }

    @Override
    public void onAudioDeviceChanged(int i) {

    }

    @Override
    public void onAudioDeviceStateChange(int i, int i1) {

    }

    @Override
    public void onVideoDeviceStageChange(int i) {

    }

    @Override
    public void onConnectionTypeChanged(int i) {

    }

    @Override
    public void onReconnectingStart() {

    }

    @Override
    public void onReJoinChannel(int i, long l) {

    }

    @Override
    public void onAudioMixingStateChanged(int i) {

    }

    @Override
    public void onAudioMixingTimestampUpdate(long l) {

    }

    @Override
    public void onAudioEffectFinished(int i) {

    }

    @Override
    public void onLocalAudioVolumeIndication(int i) {

    }

    @Override
    public void onLocalAudioVolumeIndication(int i, boolean b) {

    }

    @Override
    public void onRemoteAudioVolumeIndication(NERtcAudioVolumeInfo[] neRtcAudioVolumeInfos, int i) {

    }

    @Override
    public void onLiveStreamState(String s, String s1, int i) {

    }

    @Override
    public void onConnectionStateChanged(int i, int i1) {

    }

    @Override
    public void onCameraFocusChanged(Rect rect) {

    }

    @Override
    public void onCameraExposureChanged(Rect rect) {

    }

    @Override
    public void onRecvSEIMsg(long l, String s) {

    }

    @Override
    public void onAudioRecording(int i, String s) {

    }

    @Override
    public void onError(int i) {

    }

    @Override
    public void onWarning(int i) {

    }

    @Override
    public void onMediaRelayStatesChange(int i, String s) {

    }

    @Override
    public void onMediaRelayReceiveEvent(int i, int i1, String s) {

    }

    @Override
    public void onLocalPublishFallbackToAudioOnly(boolean b, NERtcVideoStreamType neRtcVideoStreamType) {

    }

    @Override
    public void onRemoteSubscribeFallbackToAudioOnly(long l, boolean b, NERtcVideoStreamType neRtcVideoStreamType) {

    }

    @Override
    public void onLastmileQuality(int i) {

    }

    @Override
    public void onLastmileProbeResult(LastmileProbeResult lastmileProbeResult) {

    }

    @Override
    public void onMediaRightChange(boolean b, boolean b1) {

    }

    @Override
    public void onVirtualBackgroundSourceEnabled(boolean b, int i) {

    }

    @Override
    public void onUserSubStreamAudioStart(long l) {

    }

    @Override
    public void onUserSubStreamAudioStop(long l) {

    }

    @Override
    public void onUserSubStreamAudioMute(long l, boolean b) {

    }

    @Override
    public void onLocalVideoWatermarkState(NERtcVideoStreamType neRtcVideoStreamType, int i) {
    }
}