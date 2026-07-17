package com.netease.nertc.beauty;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.tabs.TabLayout;
import com.netease.lava.api.IVideoRender;
import com.netease.lava.nertc.sdk.LastmileProbeResult;
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
import com.netease.lava.nertc.sdk.video.NERtcVideoConfig;
import com.netease.lava.nertc.sdk.video.NERtcVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.beauty.module.NEAssetsEnum;
import com.netease.nertc.beauty.module.NEEffect;
import com.netease.nertc.beauty.module.NEEffectEnum;
import com.netease.nertc.beauty.module.NEFilter;
import com.netease.nertc.beauty.module.NEFilterEnum;
import com.netease.nertc.beauty.module.NESticker;
import com.netease.nertc.beauty.module.NEStickerEnum;
import com.netease.nertc.beauty.utils.AssetUtils;
import com.netease.nertc.config.DemoDeploy;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class BeautyActivity extends AppCompatActivity implements NERtcCallbackEx {
    private static final String TAG = "BeautyActivity";
    private int meffectLastCheckedId = -1;
    private int mfilterLastCheckedId = -1;
    private int mStickerLastCheckedId = -1;
    private static final String ROOMID = "1383";
    private static final long USERID = new Random().nextInt(100000);
    private static final long STICKER_PROMPT_DURATION_MS = 6000;

    private boolean mEnableLocalAudio = true;
    private boolean mEnableLocalVideo = true;
    private boolean mJoinChannel = false;

    private NERtcVideoView mLocalUserVv;
    private TabLayout tabLayout;
    private String[] tabTags;
    private List<View> tabViews;
    private ViewPager viewPager;
    private String extFilesDirPath;
    private SeekBar effectLevelSlider;
    private SeekBar filterLevelSlider;
    private RadioGroup effectRadioGroup;
    private RadioGroup filterRadioGroup;
    private RadioGroup stickerRadioGroup;
    private ImageView mBackIv;
    private TextView stickerPromptTv;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private HashMap<Integer, NEFilter> filters;
    private HashMap<Integer, NEEffect> effects;
    private HashMap<Integer, NESticker> stickers;
    private BeauyAssetsLoaderTask beauyAssetsLoaderTask;
    private final Runnable hideStickerPromptRunnable = new Runnable() {
        @Override
        public void run() {
            hideStickerPrompt();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        extFilesDirPath = getExternalFilesDir(null).getAbsolutePath();
        setContentView(R.layout.activity_beauty);
        initView();
        beauyAssetsLoaderTask = new BeauyAssetsLoaderTask();
        beauyAssetsLoaderTask.execute();
        initData();
        setupNERtc();
        setuplocalVideo();
        NERtcVideoConfig config = new NERtcVideoConfig();
        config.videoProfile = 4;
        NERtcEx.getInstance().setLocalVideoConfig(config);
        joinChannel(ROOMID, USERID);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        extFilesDirPath = getExternalFilesDir(null).getAbsolutePath();
        filters = NEFilterEnum.getFilters();
        effects = NEEffectEnum.getEffects();
        stickers = NEStickerEnum.getStickers();
    }

    private void initView() {
        tabLayout = findViewById(R.id.tab_bottom);
        tabTags = getResources().getStringArray(R.array.beauty_option_tags);
        viewPager = findViewById(R.id.vp_pager);
        mLocalUserVv = findViewById(R.id.vv_local_user);
        stickerPromptTv = findViewById(R.id.tv_sticker_prompt);
        mBackIv = findViewById(R.id.iv_back);
        for(String tag : tabTags) {
            tabLayout.addTab(tabLayout.newTab().setText(tag));
        }

        tabViews = new ArrayList<>();
        View stickerTab = getLayoutInflater().inflate(R.layout.tab_sticker,null);
        tabViews.add(stickerTab);
        View filterTab = getLayoutInflater().inflate(R.layout.tab_filter,null);
        tabViews.add(filterTab);
        View beautyTab = getLayoutInflater().inflate(R.layout.tab_effect,null);
        tabViews.add(beautyTab);
        tabLayout.setupWithViewPager(viewPager);

        effectLevelSlider = beautyTab.findViewById(R.id.sb_slider_effect_level);
        effectLevelSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float level = (float) (seekBar.getProgress() * 1.0 / 100);
                NEEffect effect = effects.get(meffectLastCheckedId);
                if(effect != null) {
                    effect.setLevel(level);
                    int ret = NERtcEx.getInstance().setBeautyEffect(effect.getType(), level);
                }
            }
        });
        effectRadioGroup = beautyTab.findViewById(R.id.rg_effect);
        effectRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                meffectLastCheckedId = checkedId;
                NEEffect effect = effects.get(checkedId);
                if (effect != null) {
                    float level = effect.getLevel();
                    effectLevelSlider.setProgress((int)(level * 100));
                    int ret = NERtcEx.getInstance().setBeautyEffect(effect.getType(), level);
                    //Log.d(TAG,ret + "");
                }

                if (checkedId == R.id.rb_effect_recover) {
                    effectLevelSlider.setProgress(0);
                    resetEffect();
                }
            }
        });
        filterLevelSlider = filterTab.findViewById(R.id.sb_slider_filter_level);
        filterLevelSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float level = (float) (seekBar.getProgress() * 1.0 / 100);
                NEFilter filter = filters.get(mfilterLastCheckedId);
                if(filter != null) {
                    filter.setLevel(level);
                    //NERtcEx.getInstance().setBeautyFilterLevel(level);
                }
            }
        });
        filterRadioGroup = filterTab.findViewById(R.id.rg_filter);
        filterRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mfilterLastCheckedId = checkedId;
                NEFilter filter = filters.get(mfilterLastCheckedId);
                if (filter != null && filter.getResId() != R.id.rb_filter_origin) {
                    float level = filter.getLevel();
                    filterLevelSlider.setProgress((int)(level * 100));
                    Log.d(TAG, getBeautyAssetPath(NEAssetsEnum.FILTERS, filter.getName()));
                    int ret = NERtcEx.getInstance().addBeautyFilter(getBeautyAssetPath(NEAssetsEnum.FILTERS, filter.getName()));
                    //Log.d(TAG,ret + "");
                    NERtcEx.getInstance().setBeautyFilterLevel(level);
                }else {
                    filterLevelSlider.setProgress(0);
                    NERtcEx.getInstance().removeBeautyFilter();
                }
            }
        });
        stickerRadioGroup = stickerTab.findViewById(R.id.rg_sticker);
        stickerRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                mStickerLastCheckedId = checkedId;
                NESticker sticker = stickers.get(mStickerLastCheckedId);
                if (sticker != null && sticker.getResId() != R.id.rb_sticker_origin) {
                    String stickerPath = getBeautyAssetPath(NEAssetsEnum.STICKERS, sticker.getName());
                    Log.d(TAG, stickerPath);
                    //NERtcEx.getInstance().enableBeauty(true);
                    int ret = NERtcEx.getInstance().addBeautySticker(stickerPath);
                    showStickerPrompt(getStickerPromptResId(checkedId));
                } else {
                    NERtcEx.getInstance().removeBeautySticker();
                    //NERtcEx.getInstance().enableBeauty(false);
                    hideStickerPrompt();
                }
            }
        });

        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return tabViews.size();
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                View view = tabViews.get(position);
                container.addView(view);
                return view;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view == object;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View) object);
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                return tabTags[position];
            }
        });

        mBackIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });
    }
    /**
     * 初始化SDK
     */
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

    private void showStickerPrompt(int promptResId) {
        mainHandler.removeCallbacks(hideStickerPromptRunnable);
        stickerPromptTv.clearAnimation();
        stickerPromptTv.setText(promptResId);
        stickerPromptTv.setAlpha(1f);
        stickerPromptTv.setVisibility(View.VISIBLE);

        AlphaAnimation blinkAnimation = new AlphaAnimation(1f, 0.2f);
        blinkAnimation.setDuration(2000);
        blinkAnimation.setRepeatMode(Animation.REVERSE);
        blinkAnimation.setRepeatCount(Animation.INFINITE);
        stickerPromptTv.startAnimation(blinkAnimation);
        mainHandler.postDelayed(hideStickerPromptRunnable, STICKER_PROMPT_DURATION_MS);
    }

    private int getStickerPromptResId(int stickerResId) {
        if (stickerResId == R.id.rb_sticker_drinkBeer
                || stickerResId == R.id.rb_sticker_drinkBeer2
                || stickerResId == R.id.rb_sticker_eatZongzi) {
            return R.string.sticker_prompt_open_lipstick;
        } else if (stickerResId == R.id.rb_sticker_rabbiteating) {
            return R.string.sticker_prompt_blink;
        } else if (stickerResId == R.id.rb_sticker_yes) {
            return R.string.sticker_prompt_nod;
        } else if (stickerResId == R.id.rb_sticker_kiss
                || stickerResId == R.id.rb_sticker_kiss2
                || stickerResId == R.id.rb_sticker_kiss3
                || stickerResId == R.id.rb_sticker_kiss4) {
            return R.string.sticker_prompt_pout;
        } else if (stickerResId == R.id.rb_sticker_money_rain) {
            return R.string.sticker_prompt_smile;
        } else if (stickerResId == R.id.rb_sticker_flower) {
            return R.string.sticker_prompt_hand;
        } else if (stickerResId == R.id.rb_sticker_lipstick) {
            return R.string.sticker_prompt_purse_lips;
        } else if (stickerResId == R.id.rb_sticker_heart
                || stickerResId == R.id.rb_sticker_heart2
                || stickerResId == R.id.rb_sticker_heart3
                || stickerResId == R.id.rb_sticker_package) {
            return R.string.sticker_prompt_finger_heart;
        } else if (stickerResId == R.id.rb_sticker_glass) {
            return R.string.sticker_prompt_blink_red_packet;
        } else if(stickerResId == R.id.rb_sticker_gift) {
            return R.string.sticker_prompt_gift;
        }
        return R.string.sticker_prompt_open_lipstick;
    }

    private void hideStickerPrompt() {
        mainHandler.removeCallbacks(hideStickerPromptRunnable);
        stickerPromptTv.clearAnimation();
        stickerPromptTv.setAlpha(1f);
        stickerPromptTv.setVisibility(View.GONE);
    }

    private void joinChannel(String roomId, long userId) {
        NERtcEx.getInstance().startBeauty();
        NERtcEx.getInstance().enableBeauty(true);
        resetBeauty();
        meffectLastCheckedId = R.id.rb_effect_smooth;
        effectLevelSlider.setProgress((int) (effects.get(R.id.rb_effect_smooth).getLevel() * 100));
        NERtcEx.getInstance().joinChannel("", roomId, userId);

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
        NERtcEx.getInstance().enableBeauty(false);
        NERtcEx.getInstance().stopBeauty();
        int ret = NERtcEx.getInstance().leaveChannel();
        NERtcEx.getInstance().release();
        return ret == NERtcConstants.ErrorCode.OK;
    }
    /**
     * 设置美颜特效默认参数，设置默认滤镜为白皙
     */
    private void resetBeauty() {
        resetEffect();
    }
    /**
     * 设置美颜默认参数
     */
    private void resetEffect() {
        effects = NEEffectEnum.getEffects();
        setDefaultBeautyEffect(R.id.rb_effect_smooth);
        setDefaultBeautyEffect(R.id.rb_effect_whiten);
        setDefaultBeautyEffect(R.id.rb_effect_thinface);
    }

    private void setDefaultBeautyEffect(int effectResId) {
        NEEffect effect = effects.get(effectResId);
        if (effect != null) {
            NERtcEx.getInstance().setBeautyEffect(effect.getType(), effect.getLevel());
        }
    }
    /**
     * 生成滤镜和美妆模板资源文件的路径，资源文件在App启动后会拷贝到的App的外部存储路径
     * @param type @see NEAssetsEnum
     * @param name 滤镜或者美妆的名称，对应assets下的资源文件名
     * @return 滤镜或者美妆的App外部存储路径
     */
    private String getBeautyAssetPath(NEAssetsEnum type, String name) {
        String separator = File.separator;
        return String.format(Locale.getDefault(), "%s%s%s%s%s", extFilesDirPath, separator, type.getAssetsPath(), separator, name);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exit();
    }

    @Override
    public void onJoinChannel(int result, long channelId, long elapsed, long l2) {
        Log.i(TAG, "onJoinChannel result: " + result + " channelId: " + channelId + " elapsed: " + elapsed);
        if(result == NERtcConstants.ErrorCode.OK){
            mJoinChannel = true;
        }
    }

    @Override
    public void onLeaveChannel(int i) {

    }

    @Override
    public void onUserJoined(long l) {

    }

    @Override
    public void onUserJoined(long uid, NERtcUserJoinExtraInfo joinExtraInfo) {

    }

    @Override
    public void onUserLeave(long l, int i) {

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
    public void onUserVideoStart(long l, int i) {

    }

    @Override
    public void onUserVideoStop(long l) {

    }

    @Override
    public void onDisconnect(int i) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideStickerPrompt();
        if (beauyAssetsLoaderTask != null) {
            beauyAssetsLoaderTask.cancel(true);
            beauyAssetsLoaderTask = null;
        }
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

    private class BeauyAssetsLoaderTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            int ret = 0;
            for (NEAssetsEnum type : NEAssetsEnum.values()) {
                Log.d(TAG, type.toString());
                ret = AssetUtils.copyAssetRecursive(getAssets(), type.getAssetsPath(), getBeautyAssetPath(type), false);
                if (ret != 0) break;
                if (isCancelled()) break;
            }
            return ret;
        }

        @Override
        protected void onPostExecute(Integer ret) {
            super.onPostExecute(ret);
        }
    }
    /**
     * 生成滤镜和美妆模板资源文件的路径，资源文件在App启动后会拷贝到的App的外部存储路径
     * @param type @see NEAssetsEnum 对应assets目录下的美颜，滤镜或者美妆资源目录
     * @return 美颜，滤镜或者美妆的App外部存储路径
     */
    private String getBeautyAssetPath(NEAssetsEnum type) {
        String separator = File.separator;
        return String.format(Locale.getDefault(), "%s%s%s", extFilesDirPath, separator, type.getAssetsPath());
    }


}
