package com.netease.nertc.aicapacity;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.netease.lava.api.IVideoRender;
import com.netease.lava.nertc.sdk.LastmileProbeResult;
import com.netease.lava.nertc.sdk.NERtc;
import com.netease.lava.nertc.sdk.NERtcASRCaptionConfig;
import com.netease.lava.nertc.sdk.NERtcAsrCaptionResult;
import com.netease.lava.nertc.sdk.NERtcCallback;
import com.netease.lava.nertc.sdk.NERtcCallbackEx;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.NERtcUserJoinExtraInfo;
import com.netease.lava.nertc.sdk.NERtcUserLeaveExtraInfo;
import com.netease.lava.nertc.sdk.audio.NERtcAudioStreamType;
import com.netease.lava.nertc.sdk.stats.NERtcAudioVolumeInfo;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.lite.BuildConfig;
import com.netease.nertc.config.DemoDeploy;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Random;

public class AICapacityActivity extends AppCompatActivity implements NERtcCallbackEx,View.OnClickListener {
    private static final String TAG = "AICapacityActivity";

    private Button mStartJoinBtn;
    private EditText mRoomIdView;
    private EditText mUserIdView;
    private NERtcVideoView mLocalUserVv;
    private ScrollView ai_present_showView;
    private LinearLayout container;
    private ImageView mBackIv;
    private RelativeLayout mContainer;
    private boolean mJoinChannel = false;
    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private String mRoomId;
    private long mUserId;
    private String mAITaskId;
    private long lastTimestamp = 0;
    private long lastUid;
    private TextView lastTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ai_capacity_layout);
        initView();
    }

    private void initView() {
        mContainer = findViewById(R.id.container);
        mBackIv = findViewById(R.id.iv_back);
        mStartJoinBtn = findViewById(R.id.btn_join_channel);
        mRoomIdView = findViewById(R.id.et_room_id);
        mUserIdView = findViewById(R.id.et_user_id);
        mLocalUserVv = findViewById(R.id.vv_local_user);
        ai_present_showView = findViewById(R.id.ai_scroll_view);
        container = findViewById(R.id.ai_container);

        mUserId = new Random().nextInt(100000);
        mUserIdView.setText(String.valueOf(mUserId));

        mStartJoinBtn.setOnClickListener(this);
        mBackIv.setOnClickListener(this);

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
            closeASR();
            new Thread(() -> releaseAi(mRoomId, mUserId)).start();
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
        Editable roomIdEdit = mRoomIdView.getText();
        if(roomIdEdit == null || roomIdEdit.length() <= 0){
            return;
        }
        Editable userIdEdit = mUserIdView.getText();
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
        mLocalUserVv.setZOrderMediaOverlay(true);
        mLocalUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FIT);
        NERtcEx.getInstance().setupLocalVideoCanvas(mLocalUserVv);
    }
    private void joinChannel(String roomId, long userId) {
        NERtcEx.getInstance().joinChannel("", roomId, userId);
    }
    private void startJoinHome() {

        if(!mJoinChannel) {
            userInfo();
            setupNERtc();
            setuplocalVideo();
            joinChannel(mRoomId, mUserId);
            new Thread(() -> requestAi(mRoomId, mUserId)).start();
            openASR();
        }
    }
    private void closeASR() {
        NERtcEx.getInstance().stopASRCaption();
    }
    private void openASR() {
        NERtcASRCaptionConfig config = new NERtcASRCaptionConfig();
        config.srcLanguage = ":AUTO";
        config.dstLanguage = "";
        NERtcEx.getInstance().startASRCaption(config);
    }
    private void requestAi(final String channel, long uid) {
        try {
            TokenServer demoTokenService = new TokenServer(DemoDeploy.APP_KEY, DemoDeploy.APP_SECRET, 86400);
            String token = demoTokenService.getToken(channel,uid, 86400);
            if(token != null) {
                String API_URL = "https://rtc-ai.yunxinapi.com/ai/task/create";
                try {
                    // 生成随机数和时间戳
                    Random random = new Random();
                    String nonce = String.valueOf(random.nextInt(123456));
                    long curTime = System.currentTimeMillis() / 1000;

                    URL url_api = new URL(API_URL);
                    HttpURLConnection connection = (HttpURLConnection) url_api.openConnection();
                    connection.setRequestMethod("POST");

                    // 设置请求头
                    connection.setRequestProperty("AppKey", DemoDeploy.APP_KEY);
                    connection.setRequestProperty("Nonce", nonce);
                    connection.setRequestProperty("CurTime", String.valueOf(curTime));
                    connection.setRequestProperty("CheckSum", token);
                    connection.setRequestProperty("Content-Type", "application/json");
                    // 构建请求体
                    JSONObject data = new JSONObject();
                    JSONObject asr = new JSONObject();
                    asr.put("asrVendor", 6);
                    asr.put("maxSentenceSilence", 1000);
                    asr.put("enableSemanticVad", false);
                    data.put("asr", asr);

                    JSONObject llm = new JSONObject();
                    llm.put("llmVendor", 5);
                    llm.put("role", 0);
                    String encodedData = URLEncoder.encode("你是陪伴小助手，回答不要超过50个字", "UTF-8");
                    llm.put("customPrompt", encodedData);
                    data.put("llm", llm);

                    JSONObject tts = new JSONObject();
                    tts.put("ttsVendor", 6);
                    tts.put("language", "Chinese");
                    tts.put("gender", "Female");
                    data.put("tts", tts);

                    JSONObject body = new JSONObject();
                    body.put("cname", channel);
                    body.put("requestId", generateRandomString());
                    body.put("taskType", 7);
                    body.put("data", data);

                    String requestBody = body.toString();
                    // 允许输出流
                    connection.setDoOutput(true);
                    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                    outputStream.writeBytes(requestBody);
                    outputStream.flush();
                    outputStream.close();

                    // 获取响应
                    int responseCode = connection.getResponseCode();
                    BufferedReader reader;
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } else {
                        reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    }
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    JSONObject jsonObject = new JSONObject(response.toString());
                    String result = jsonObject.getString("result");
                    mAITaskId = new JSONObject(result).getString("taskId");
                } catch (Exception e) {
                    e.printStackTrace();
                    return ;
                }

            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseAi(final String channel, long uid) {
        try {
            TokenServer demoTokenService = new TokenServer(DemoDeploy.APP_KEY, DemoDeploy.APP_SECRET, 86400);
            String token = demoTokenService.getToken(channel,uid, 86400);
            if(token != null) {
                String API_URL = "https://rtc-ai.yunxinapi.com/ai/task/close";
                try {
                    // 生成随机数和时间戳
                    Random random = new Random();
                    String nonce = String.valueOf(random.nextInt(123456));
                    long curTime = System.currentTimeMillis() / 1000;

                    URL url_api = new URL(API_URL);
                    HttpURLConnection connection = (HttpURLConnection) url_api.openConnection();
                    connection.setRequestMethod("POST");

                    // 设置请求头
                    connection.setRequestProperty("AppKey", DemoDeploy.APP_KEY);
                    connection.setRequestProperty("Nonce", nonce);
                    connection.setRequestProperty("CurTime", String.valueOf(curTime));
                    connection.setRequestProperty("CheckSum", token);

                    JSONObject body = new JSONObject();
                    body.put("cid", channel);
                    body.put("taskId", mAITaskId);
                    body.put("taskType", 7);

                    String requestBody = body.toString();
                    // 允许输出流
                    connection.setDoOutput(true);
                    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                    outputStream.writeBytes(requestBody);
                    outputStream.flush();
                    outputStream.close();

                    // 获取响应
                    int responseCode = connection.getResponseCode();
                    BufferedReader reader;
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    } else {
                        reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    }
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    return ;
                }

            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addTextView(long uid, String text) {
        if(container == null || ai_present_showView == null){
            return;
        }
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp - lastTimestamp > 2000 || lastUid != uid) {
            TextView textView = new TextView(this);
            textView.setText("uid:" + uid + "  " + text);
            textView.setPadding(16, 16, 16, 16); // 增加间距
            container.addView(textView);
            lastTextView = textView;
        } else if(lastTextView != null){
            // 时间间隔小于等于 2 秒，更新旧的 TextView
            lastTextView.setText("uid:" + uid + "  " + text);
        }
        lastTimestamp = currentTimestamp;
        lastUid = uid;
        // 延迟滚动：使用 viewTreeObserver 确保布局完成
        ViewTreeObserver viewTreeObserver = ai_present_showView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // 计算滚动位置（减去 ScrollView 的 padding）
                int scrollHeight = container.getHeight() - ai_present_showView.getHeight() - ai_present_showView.getPaddingBottom();
                if (scrollHeight > 0) {
                    ai_present_showView.smoothScrollBy(0, scrollHeight); // 相对滚动
                    // 或 scrollView.smoothScrollTo(0, scrollHeight); // 绝对位置
                }
                // 移除监听，避免内存泄漏
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    ai_present_showView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    ai_present_showView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    private String generateRandomString() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder randomString = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 32; i++) {
            int index = random.nextInt(characters.length());
            randomString.append(characters.charAt(index));
        }
        return randomString.toString();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.iv_back){
            exit();
        }else if(id == R.id.btn_join_channel) {
            startJoinHome();
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

    }

    @Override
    public void onUserJoined(long uid, NERtcUserJoinExtraInfo joinExtraInfo) {

    }

    @Override
    public void onUserLeave(long userId, int i) {
        Log.i(TAG, "onUserLeave uid: " + userId);
    }

    @Override
    public void onUserLeave(long uid, int reason, NERtcUserLeaveExtraInfo leaveExtraInfo) {

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
    public void onUserSubStreamVideoStart(long uid, int maxProfile) {

    }

    @Override
    public void onUserSubStreamVideoStop(long uid) {

    }

    @Override
    public void onUserAudioMute(long uid, boolean muted) {

    }

    @Override
    public void onUserVideoMute(long uid, boolean muted) {

    }

    @Override
    public void onUserVideoMute(NERtcVideoStreamType streamType, long uid, boolean muted) {

    }

    @Override
    public void onUserVideoStart(long uid, NERtcVideoStreamType streamType, int maxProfile) {

    }

    @Override
    public void onUserVideoStop(long uid, NERtcVideoStreamType streamType) {

    }

    @Override
    public void onFirstAudioDataReceived(long uid) {

    }

    @Override
    public void onLocalAudioFirstPacketSent(NERtcAudioStreamType audioStreamType) {

    }

    @Override
    public void onFirstVideoDataReceived(long uid) {

    }

    @Override
    public void onFirstVideoDataReceived(NERtcVideoStreamType streamType, long uid) {

    }

    @Override
    public void onFirstAudioFrameDecoded(long userID) {

    }

    @Override
    public void onFirstVideoFrameDecoded(long userID, int width, int height) {

    }

    @Override
    public void onFirstVideoFrameRender(long userID, NERtcVideoStreamType streamType, int width, int height, long elapsedTime) {

    }

    @Override
    public void onFirstVideoFrameDecoded(NERtcVideoStreamType streamType, long userID, int width, int height) {

    }

    @Override
    public void onUserVideoProfileUpdate(long uid, int maxProfile) {

    }

    @Override
    public void onAudioDeviceChanged(int selected) {

    }

    @Override
    public void onAudioDeviceStateChange(int deviceType, int deviceState) {

    }

    @Override
    public void onVideoDeviceStageChange(int deviceState) {

    }

    @Override
    public void onConnectionTypeChanged(int newConnectionType) {

    }

    @Override
    public void onReconnectingStart() {

    }

    @Override
    public void onReJoinChannel(int result, long channelId) {

    }

    @Override
    public void onAudioMixingStateChanged(int reason) {

    }

    @Override
    public void onAudioMixingTimestampUpdate(long timestampMs) {

    }

    @Override
    public void onAudioEffectTimestampUpdate(long id, long timestampMs) {

    }

    @Override
    public void onAudioEffectFinished(int effectId) {

    }

    @Override
    public void onLocalAudioVolumeIndication(int volume) {

    }

    @Override
    public void onLocalAudioVolumeIndication(int volume, boolean vadFlag) {

    }

    @Override
    public void onRemoteAudioVolumeIndication(NERtcAudioVolumeInfo[] volumeArray, int totalVolume) {

    }

    @Override
    public void onLiveStreamState(String taskId, String pushUrl, int liveState) {

    }

    @Override
    public void onConnectionStateChanged(int state, int reason) {

    }

    @Override
    public void onCameraFocusChanged(Rect rect) {

    }

    @Override
    public void onCameraExposureChanged(Rect rect) {

    }

    @Override
    public void onRecvSEIMsg(long userID, String seiMsg) {

    }

    @Override
    public void onAudioRecording(int code, String filePath) {

    }

    @Override
    public void onError(int code) {

    }

    @Override
    public void onWarning(int code) {

    }

    @Override
    public void onApiCallExecuted(String apiName, int result, String message) {

    }

    @Override
    public void onMediaRelayStatesChange(int state, String channelName) {

    }

    @Override
    public void onMediaRelayReceiveEvent(int event, int code, String channelName) {

    }

    @Override
    public void onAsrCaptionStateChanged(int asrState, int code, String message) {

    }

    @Override
    public void onAsrCaptionResult(NERtcAsrCaptionResult[] result, int resultCount) {
        Log.i(TAG, "onAsrCaptionResult result: " + result + " resultCount:" + resultCount);
        for (int i = 0; i < resultCount; ++i) {
            Log.i(TAG, "onAsrCaptionResult uid: " + result[i].uid + " language: " + result[i].language + " translationLanguage: " + result[i].translationLanguage + " content: "+result[i].content + " translatedText: " +result[i].translatedText);
            addTextView(result[i].uid, result[i].content);
        }
    }

    @Override
    public void onPlayStreamingStateChange(String streamId, int state, int reason) {

    }

    @Override
    public void onPlayStreamingReceiveSeiMessage(String streamId, String message) {

    }

    @Override
    public void onPlayStreamingFirstAudioFramePlayed(String streamId, long timeMs) {

    }

    @Override
    public void onPlayStreamingFirstVideoFrameRender(String streamId, long timeMs, int width, int height) {

    }

    @Override
    public void onPlayStreamingDuration(String streamId, long totalDurationS, long currentPtsS) {

    }

    @Override
    public void onLocalPublishFallbackToAudioOnly(boolean isFallback, NERtcVideoStreamType streamType) {

    }

    @Override
    public void onRemoteSubscribeFallbackToAudioOnly(long uid, boolean isFallback, NERtcVideoStreamType streamType) {

    }

    @Override
    public void onLastmileQuality(int quality) {

    }

    @Override
    public void onLastmileProbeResult(LastmileProbeResult result) {

    }

    @Override
    public void onMediaRightChange(boolean isAudioBannedByServer, boolean isVideoBannedByServer) {

    }

    @Override
    public void onRemoteVideoSizeChanged(long userId, NERtcVideoStreamType videoType, int width, int height) {

    }

    @Override
    public void onLocalVideoRenderSizeChanged(NERtcVideoStreamType videoType, int width, int height) {

    }

    @Override
    public void onVirtualBackgroundSourceEnabled(boolean enabled, int reason) {

    }

    @Override
    public void onUserSubStreamAudioStart(long uid) {

    }

    @Override
    public void onUserSubStreamAudioStop(long uid) {

    }

    @Override
    public void onUserSubStreamAudioMute(long uid, boolean muted) {

    }

    @Override
    public void onPermissionKeyWillExpire() {

    }

    @Override
    public void onUpdatePermissionKey(String key, int error, int timeout) {

    }

    @Override
    public void onLocalVideoWatermarkState(NERtcVideoStreamType videoStreamType, int state) {

    }

    @Override
    public void onUserDataStart(long uid) {

    }

    @Override
    public void onUserDataStop(long uid) {

    }

    @Override
    public void onUserDataReceiveMessage(long uid, ByteBuffer bufferData, long bufferSize) {

    }

    @Override
    public void onUserDataStateChanged(long uid) {

    }

    @Override
    public void onUserDataBufferedAmountChanged(long uid, long previousAmount) {

    }

    @Override
    public void onLabFeatureCallback(String key, Object param) {

    }

    @Override
    public void onAiData(String type, String data) {

    }

}