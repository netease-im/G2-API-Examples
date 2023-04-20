package com.netease.nertc.beauty;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Toast;
import com.google.android.material.tabs.TabLayout;
import com.netease.lava.api.IVideoRender;
import com.netease.lava.nertc.sdk.NERtcCallback;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcOption;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;
import com.netease.nertc.beauty.module.NEAssetsEnum;
import com.netease.nertc.beauty.module.NEEffect;
import com.netease.nertc.beauty.module.NEEffectEnum;
import com.netease.nertc.beauty.module.NEFilter;
import com.netease.nertc.beauty.module.NEFilterEnum;
import com.netease.nertc.beauty.utils.AssetUtils;
import com.netease.nertc.config.DemoDeploy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;


public class BeautyActivity extends AppCompatActivity implements NERtcCallback {
    private static final String TAG = "BeautyActivity";
    private int meffectLastCheckedId = -1;
    private int mfilterLastCheckedId = -1;
    private static final String ROOMID = "1383";
    private static final long USERID = new Random().nextInt(100000);

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
    private ImageView mBackIv;
    private HashMap<Integer, NEFilter> filters;
    private HashMap<Integer, NEEffect> effects;
    private BeauyAssetsLoaderTask beauyAssetsLoaderTask;

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
        joinChannel(ROOMID, USERID);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        extFilesDirPath = getExternalFilesDir(null).getAbsolutePath();
        filters = NEFilterEnum.getFilters();
        effects = NEEffectEnum.getEffects();
    }

    private void initView() {
        tabLayout = findViewById(R.id.tab_bottom);
        tabTags = getResources().getStringArray(R.array.beauty_option_tags);
        viewPager = findViewById(R.id.vp_pager);
        mLocalUserVv = findViewById(R.id.vv_local_user);
        mBackIv = findViewById(R.id.iv_back);
        for(String tag : tabTags) {
            tabLayout.addTab(tabLayout.newTab().setText(tag));
        }

        tabViews = new ArrayList<>();
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
                    Log.d(TAG,ret + "");
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
                    NERtcEx.getInstance().setBeautyFilterLevel(level);
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
                    Log.d(TAG,ret + "");
                    NERtcEx.getInstance().setBeautyFilterLevel(level);
                }else {
                    filterLevelSlider.setProgress(0);
                    NERtcEx.getInstance().removeBeautyFilter();
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
        NERtcEx.getInstance().startBeauty();
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
        int ret = NERtcEx.getInstance().leaveChannel();
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
        for (NEEffect effect : effects.values()) {
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
    public void onUserLeave(long l, int i) {

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
        if (beauyAssetsLoaderTask != null) {
            beauyAssetsLoaderTask.cancel(true);
            beauyAssetsLoaderTask = null;
        }
    }

    @Override
    public void onClientRoleChange(int i, int i1) {

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