//
//  AudioChangeViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/10/12.
//

#import "AudioChangeViewController.h"
#import "NSString+Common.h"
#import "NTESAppConfig.h"
#import "UIColor+Hex.h"

@interface AudioChangeViewController ()<NERtcEngineDelegateEx>

@property (strong, nonatomic) IBOutletCollection(UIView) NSArray *remoteViewArr;
@property (weak, nonatomic) IBOutlet UIView *localUserView;

@property (weak, nonatomic) IBOutlet UIButton *normalButton;
@property (weak, nonatomic) IBOutlet UIButton *robotButton;
@property (weak, nonatomic) IBOutlet UIButton *gaintButton;
@property (weak, nonatomic) IBOutlet UIButton *horrorButton;
@property (weak, nonatomic) IBOutlet UIButton *matureButton;
@property (weak, nonatomic) IBOutlet UIButton *manToWomanButton;
@property (weak, nonatomic) IBOutlet UIButton *womanToManButton;

@property (weak, nonatomic) IBOutlet UIButton *closeButton;
@property (weak, nonatomic) IBOutlet UIButton *muffledButton;
@property (weak, nonatomic) IBOutlet UIButton *mellowButton;
@property (weak, nonatomic) IBOutlet UIButton *clearButton;
@property (weak, nonatomic) IBOutlet UIButton *magneticButton;
@property (weak, nonatomic) IBOutlet UIButton *recordingStudioButton;
@property (weak, nonatomic) IBOutlet UIButton *natureButton;
@property (weak, nonatomic) IBOutlet UIButton *KTVButton;

@property (assign, nonatomic) NERtcVoiceBeautifierType voiceBeautifierType;
@property (assign, nonatomic) NERtcVoiceChangerType voiceChangerType;

@end

@implementation AudioChangeViewController

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

- (void)refreshVoiceChangerType {
    [[NERtcEngine sharedEngine] setAudioEffectPreset:self.voiceChangerType];
}

- (void)refreshVoiceBeautifierType {
    [[NERtcEngine sharedEngine] setVoiceBeautifierPreset:self.voiceBeautifierType];
}

# pragma mark - Helper

- (void)changeAudioEffectButtonColor:(UIButton *)btn {
    _normalButton.backgroundColor = [UIColor themeGrayColor];
    _robotButton.backgroundColor = [UIColor themeGrayColor];
    _gaintButton.backgroundColor = [UIColor themeGrayColor];
    _horrorButton.backgroundColor = [UIColor themeGrayColor];
    _matureButton.backgroundColor = [UIColor themeGrayColor];
    _manToWomanButton.backgroundColor = [UIColor themeGrayColor];
    _womanToManButton.backgroundColor = [UIColor themeGrayColor];
    btn.backgroundColor = [UIColor themeBlueColor];
}

- (void)changeVoiceBeautifierButtonColor:(UIButton *)btn {
    _closeButton.backgroundColor = [UIColor themeGrayColor];
    _muffledButton.backgroundColor = [UIColor themeGrayColor];
    _mellowButton.backgroundColor = [UIColor themeGrayColor];
    _clearButton.backgroundColor = [UIColor themeGrayColor];
    _magneticButton.backgroundColor = [UIColor themeGrayColor];
    _recordingStudioButton.backgroundColor = [UIColor themeGrayColor];
    _natureButton.backgroundColor = [UIColor themeGrayColor];
    _KTVButton.backgroundColor = [UIColor themeGrayColor];
    btn.backgroundColor = [UIColor themeBlueColor];
}

# pragma mark - IBActions

- (IBAction)onNormalButton:(id)sender {
    _voiceChangerType = kNERtcVoiceChangerOff;
    [self changeAudioEffectButtonColor:_normalButton];
    [self refreshVoiceChangerType];
}

- (IBAction)onRobotButton:(id)sender {
    _voiceChangerType = kNERtcVoiceChangerRobot;
    [self changeAudioEffectButtonColor:_robotButton];
    [self refreshVoiceChangerType];
}

- (IBAction)onGaintButton:(id)sender {
    _voiceChangerType = kNERtcVoiceChangerGaint;
    [self changeAudioEffectButtonColor:_gaintButton];
    [self refreshVoiceChangerType];
}

- (IBAction)onHorrorButton:(id)sender {
    _voiceChangerType = kNERtcVoiceChangerHorror;
    [self changeAudioEffectButtonColor:_horrorButton];
    [self refreshVoiceChangerType];
}

- (IBAction)onMatureButton:(id)sender {
    _voiceChangerType = kNERtcVoiceChangerMature;
    [self changeAudioEffectButtonColor:_matureButton];
    [self refreshVoiceChangerType];
}

- (IBAction)onManToWoman:(id)sender {
    _voiceChangerType = kNERtcVoiceChangerManToWoman;
    [self changeAudioEffectButtonColor:_manToWomanButton];
    [self refreshVoiceChangerType];
}

- (IBAction)onWomanToMan:(id)sender {
    _voiceChangerType = kNERtcVoiceChangerWomanToMan;
    [self changeAudioEffectButtonColor:_womanToManButton];
    [self refreshVoiceChangerType];
}

- (IBAction)onCloseButton:(id)sender {
    _voiceBeautifierType = kNERtcVoiceBeautifierOff;
    [self changeVoiceBeautifierButtonColor:_closeButton];
    [self refreshVoiceBeautifierType];
}

- (IBAction)onMuffledButton:(id)sender {
    _voiceBeautifierType = kNERtcVoiceBeautifierMuffled;
    [self changeVoiceBeautifierButtonColor:_muffledButton];
    [self refreshVoiceBeautifierType];
}

- (IBAction)onMellowButton:(id)sender {
    _voiceBeautifierType = kNERtcVoiceBeautifierMellow;
    [self changeVoiceBeautifierButtonColor:_mellowButton];
    [self refreshVoiceBeautifierType];
}

- (IBAction)onClearButton:(id)sender {
    _voiceBeautifierType = kNERtcVoiceBeautifierClear;
    [self changeVoiceBeautifierButtonColor:_clearButton];
    [self refreshVoiceBeautifierType];
}

- (IBAction)onMagneticButton:(id)sender {
    _voiceBeautifierType = kNERtcVoiceBeautifierMagnetic;
    [self changeVoiceBeautifierButtonColor:_magneticButton];
    [self refreshVoiceBeautifierType];
}

- (IBAction)onRecordingStudioButton:(id)sender {
    _voiceBeautifierType = kNERtcVoiceBeautifierRecordingStudio;
    [self changeVoiceBeautifierButtonColor:_recordingStudioButton];
    [self refreshVoiceBeautifierType];
}

- (IBAction)onNatureButton:(id)sender {
    _voiceBeautifierType = kNERtcVoiceBeautifierNature;
    [self changeVoiceBeautifierButtonColor:_natureButton];
    [self refreshVoiceBeautifierType];
}

- (IBAction)onKTVButton:(id)sender {
    _voiceBeautifierType = kNERtcVoiceBeautifierKTV;
    [self changeVoiceBeautifierButtonColor:_KTVButton];
    [self refreshVoiceBeautifierType];
}

- (void)onLeaveAction:(id)sender {
    [NERtcEngine.sharedEngine leaveChannel];
    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];//返回上一页面
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
