//
//  SetBGMViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/10/13.
//

#import "NSString+Common.h"
#import "NTESAppConfig.h"
#import "SetBGMViewController.h"
#import "UIColor+Hex.h"

@interface SetBGMViewController ()<NERtcEngineDelegateEx>

@property (weak, nonatomic) IBOutlet UILabel *musicVolumeLabel;
@property (weak, nonatomic) IBOutlet UILabel *effectVolumeLabel;

@property (weak, nonatomic) IBOutlet UIButton *music01Button;
@property (weak, nonatomic) IBOutlet UIButton *music02Button;
@property (weak, nonatomic) IBOutlet UIButton *effect01Button;
@property (weak, nonatomic) IBOutlet UIButton *effect02Button;
@property (weak, nonatomic) IBOutlet UISlider *adjustMusicVolumeSlider;
@property (weak, nonatomic) IBOutlet UISlider *adjustEffectVolumeSlider;

@property (weak, nonatomic) IBOutlet UIView *localUserView;
@property (strong, nonatomic) IBOutletCollection(UIView) NSArray *remoteViewArr;

@end

@implementation SetBGMViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupUI];
    [self setupNERtcEngine];
    [NERtcEngine.sharedEngine enableLocalAudio:YES];
    [NERtcEngine.sharedEngine enableLocalVideo:YES];
    [self setupLocalVideoCanvas];
    [self joinCurrentRoom];
}

- (void)setupUI {
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Leave" style:UIBarButtonItemStylePlain target:self action:@selector(onLeaveAction:)];
    self.title = [@"房间号:" stringByAppendingString:(self.roomId)];
    self.musicVolumeLabel.text = [NSString stringWithFormat:@"%d", (int)self.adjustMusicVolumeSlider.value];
    self.effectVolumeLabel.text = [NSString stringWithFormat:@"%d", (int)self.adjustEffectVolumeSlider.value];
}

- (void)setupNERtcEngine {
    //默认情况下日志会存储在App沙盒的Documents目录下
    NERtcLogSetting *logSetting = [[NERtcLogSetting alloc] init];

#if DEBUG
    logSetting.logLevel = kNERtcLogLevelInfo;
#else
    logSetting.logLevel = kNERtcLogLevelWarning;
#endif
    NERtcEngine *coreEngine = [NERtcEngine sharedEngine];
    NERtcEngineContext *context = [[NERtcEngineContext alloc] init];
    // 设置通话相关信息的回调
    context.engineDelegate = self;
    // 设置当前应用的appKey
    context.appKey = AppKey;
    context.logSetting = logSetting;
    [coreEngine setupEngineWithContext:context];
}

- (void)setupLocalVideoCanvas {
    NERtcVideoCanvas *canvas = [[NERtcVideoCanvas alloc] init];

    canvas.container = self.localUserView;
    [NERtcEngine.sharedEngine setupLocalVideoCanvas:canvas];
}

- (void)joinCurrentRoom {
    //如采用安全模式，则需填入相应Token
    [NERtcEngine.sharedEngine joinChannelWithToken:@""
                                       channelName:self.roomId
                                             myUid:self.userId
                                        completion:^(NSError *_Nullable error, uint64_t channelId, uint64_t elapesd, uint64_t uid) {
        if (error) {
            NSLog(@"加入房间失败");
        } else {
            NSLog(@"加入房间成功");
        }
    }];
}

- (void)destroyEngine {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        [NERtcEngine destroyEngine];
    });
}

#pragma mark - IBActions

- (IBAction)onMusic01Click:(id)sender {
    _music01Button.backgroundColor = [UIColor themeBlueColor];
    _music02Button.backgroundColor = [UIColor themeGrayColor];
    [[NERtcEngine sharedEngine] stopAudioMixing];
    NSString *path = [[NSBundle mainBundle] pathForResource:@"1" ofType:@"m4a"];
    NERtcCreateAudioMixingOption *option = [[NERtcCreateAudioMixingOption alloc] init];
    option.path = path;                                        //伴音文件路径
    option.playbackEnabled = YES;    //是否本地播放(默认为YES)
    option.playbackVolume = [self.musicVolumeLabel.text intValue];      //本地播放音量
    option.sendEnabled = YES;                                  //是否编码发送(默认为YES)
    option.sendVolume = [self.musicVolumeLabel.text intValue]; //发送音量
    option.loopCount = 1;                                      //循环次数
    option.startTimeStamp = 0;                                 //音乐文件开始播放的时间，UTC 时间戳
    [[NERtcEngine sharedEngine] startAudioMixingWithOption:option];
}

- (IBAction)onMusic02Click:(id)sender {
    _music01Button.backgroundColor = [UIColor themeGrayColor];
    _music02Button.backgroundColor = [UIColor themeBlueColor];
    [[NERtcEngine sharedEngine] stopAudioMixing];
    NSString *path = [[NSBundle mainBundle] pathForResource:@"2" ofType:@"m4a"];
    NERtcCreateAudioMixingOption *option = [[NERtcCreateAudioMixingOption alloc] init];
    option.path = path;                                        //伴音文件路径
    option.playbackEnabled = YES;    //是否本地播放(默认为YES)
    option.playbackVolume = [self.musicVolumeLabel.text intValue];      //本地播放音量
    option.sendEnabled = YES;                                  //是否编码发送(默认为YES)
    option.sendVolume = [self.musicVolumeLabel.text intValue]; //发送音量
    option.loopCount = 1;                                      //循环次数
    option.startTimeStamp = 0;                                 //音乐文件开始播放的时间，UTC 时间戳
    [[NERtcEngine sharedEngine] startAudioMixingWithOption:option];
}

- (IBAction)onEffect01Click:(id)sender {
    _effect01Button.backgroundColor = [UIColor themeBlueColor];
    _effect02Button.backgroundColor = [UIColor themeGrayColor];
    [[NERtcEngine sharedEngine] stopAllEffects];
    NERtcCreateAudioEffectOption *option = [[NERtcCreateAudioEffectOption alloc] init];
    NSString *path = [[NSBundle mainBundle] pathForResource:@"audio_effect_0" ofType:@"wav"];
    option.path = path;
    option.loopCount = 1;
    option.sendEnabled = YES;
    option.playbackEnabled = YES;
    option.sendVolume = [self.effectVolumeLabel.text intValue];
    option.playbackVolume = [self.effectVolumeLabel.text intValue];
    [[NERtcEngine sharedEngine] playEffectWitdId:1 effectOption:option];
}

- (IBAction)onEffect02Click:(id)sender {
    _effect01Button.backgroundColor = [UIColor themeGrayColor];
    _effect02Button.backgroundColor = [UIColor themeBlueColor];
    [[NERtcEngine sharedEngine] stopAllEffects];
    NERtcCreateAudioEffectOption *option = [[NERtcCreateAudioEffectOption alloc] init];
    NSString *path = [[NSBundle mainBundle] pathForResource:@"audio_effect_1" ofType:@"wav"];
    option.path = path;
    option.loopCount = 1;
    option.sendEnabled = YES;
    option.playbackEnabled = YES;
    option.sendVolume = [self.effectVolumeLabel.text intValue];
    option.playbackVolume = [self.effectVolumeLabel.text intValue];
    [[NERtcEngine sharedEngine] playEffectWitdId:2 effectOption:option];
}

- (void)onLeaveAction:(id)sender {
    [NERtcEngine.sharedEngine leaveChannel];
    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];//返回上一页面
}

#pragma mark - Slider ValueChange

- (IBAction)onMusicVolumSliderChange:(UISlider *)sender {
    NSInteger volume = sender.value;

    self.musicVolumeLabel.text = [NSString stringWithFormat:@"%ld", volume];
    [NERtcEngine.sharedEngine setAudioMixingPlaybackVolume:(uint32_t)volume]; //设置伴音文件的播放音量
    [NERtcEngine.sharedEngine setAudioMixingSendVolume:(uint32_t)volume]; //设置伴音文件的发送音量
}

- (IBAction)onEffectVolumeSliderChange:(UISlider *)sender {
    NSInteger volume = sender.value;

    self.effectVolumeLabel.text = [NSString stringWithFormat:@"%ld", volume];
    [[NERtcEngine sharedEngine] setEffectSendVolumeWithId:1 volume:(uint32_t)volume]; //设置所有音效文件发送音量
    [[NERtcEngine sharedEngine] setEffectPlaybackVolumeWithId:1 volume:(uint32_t)volume]; //设置指定音效文件的播放音量
    [[NERtcEngine sharedEngine] setEffectSendVolumeWithId:2 volume:(uint32_t)volume];
    [[NERtcEngine sharedEngine] setEffectPlaybackVolumeWithId:2 volume:(uint32_t)volume];
}

#pragma mark - NERtcEngine Delegate

- (void)onNERtcEngineUserDidJoinWithUserID:(uint64_t)userID userName:(NSString *)userName {
    //建立远端canvas，用来渲染远端画面
    for (UIView *view in self.remoteViewArr) {
        if (view.tag == 0) {
            NERtcVideoCanvas *canvas = [[NERtcVideoCanvas alloc] init];
            canvas.container = view;
            [NERtcEngine.sharedEngine setupRemoteVideoCanvas:canvas forUserID:userID];
            view.tag = (NSInteger)userID;
            break;
        }
    }
}

- (void)onNERtcEngineUserVideoDidStartWithUserID:(uint64_t)userID videoProfile:(NERtcVideoProfileType)profile {
    [NERtcEngine.sharedEngine subscribeRemoteVideo:YES forUserID:userID streamType:kNERtcRemoteVideoStreamTypeHigh];
}

- (void)onNERtcEngineUserDidLeaveWithUserID:(uint64_t)userID reason:(NERtcSessionLeaveReason)reason {
    //如果远端的人离开了，重置远端模型和UI
    [self.view viewWithTag:(NSInteger)userID].tag = 0;
}

- (void)onNERtcEngineDidDisconnectWithReason:(NERtcError)reason {
    NSLog(@"%s", __FUNCTION__);

    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];
}

- (void)onNERtcEngineDidLeaveChannelWithResult:(NERtcError)result {
}

@end
