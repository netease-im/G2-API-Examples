//
//  SetVideoQualityViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/10/9.
//

#import "NSString+Common.h"
#import "NTESAppConfig.h"
#import "SetVideoQualityViewController.h"
#import "UIColor+Hex.h"

@interface Preset : NSObject
@property (assign, nonatomic) NSInteger Bitrate;
@property (assign, nonatomic) NSInteger Fps;
- (instancetype)initWithBitrate:(NSInteger)Bitrate Fps:(NSInteger)Fps;
@end

@interface SetVideoQualityViewController () <NERtcEngineDelegateEx>

@property (weak, nonatomic) IBOutlet UITextField *bitrateTextField;
@property (weak, nonatomic) IBOutlet UITextField *fpsTextField;

@property (weak, nonatomic) IBOutlet UIButton *video180PButton;
@property (weak, nonatomic) IBOutlet UIButton *video360PButton;
@property (weak, nonatomic) IBOutlet UIButton *video720PButton;
@property (weak, nonatomic) IBOutlet UIButton *video1080PButton;

@property (weak, nonatomic) IBOutlet UIView *localUserView;
@property (strong, nonatomic) IBOutletCollection(UIView) NSArray *remoteViewArr;
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *bottomConstraint;

@property (retain, nonatomic) NERtcVideoEncodeConfiguration *videoEncodeConfig;
@property (assign, nonatomic) NERtcVideoProfileType videoResolution;
@property (strong, nonatomic) NSMutableDictionary *bitrateDic;

@end

@implementation SetVideoQualityViewController

- (NERtcVideoEncodeConfiguration *)videoEncodeConfig {
    if (!_videoEncodeConfig) {
        _videoEncodeConfig = [[NERtcVideoEncodeConfiguration alloc] init];
    }

    return _videoEncodeConfig;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupUI];
    _videoResolution = kNERtcVideoProfileStandard;
    [self setupBitrateDic];
    [self addKeyboardObserver];
    [self setupNERtcEngine];
    [NERtcEngine.sharedEngine enableLocalAudio:YES];
    [NERtcEngine.sharedEngine enableLocalVideo:YES];
    [self setupLocalVideoCanvas];
    [self joinCurrentRoom];
}

- (void)setupUI {
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Leave" style:UIBarButtonItemStylePlain target:self action:@selector(onLeaveAction:)];
    self.title = [@"房间号:" stringByAppendingString:(self.roomId)];
    self.bitrateTextField.text = @"606";
    self.fpsTextField.text = @"30";
}

- (void)setupBitrateDic {
    if (!_bitrateDic) {
        _bitrateDic = [NSMutableDictionary new];
        [_bitrateDic setObject:[[Preset alloc] initWithBitrate:141 Fps:15]  forKey:[@(kNERtcVideoProfileLow) stringValue]];
        [_bitrateDic setObject:[[Preset alloc] initWithBitrate:606 Fps:30]  forKey:[@(kNERtcVideoProfileStandard) stringValue]];
        [_bitrateDic setObject:[[Preset alloc] initWithBitrate:1714 Fps:30]  forKey:[@(kNERtcVideoProfileHD720P) stringValue]];
        [_bitrateDic setObject:[[Preset alloc] initWithBitrate:3150 Fps:30]  forKey:[@(kNERtcVideoProfileHD1080P) stringValue]];
    }
}

- (void)refreshBitrate {
    Preset *preset = [_bitrateDic objectForKey:[@(_videoResolution) stringValue]];

    self.bitrateTextField.text = [NSString stringWithFormat:@"%ld", preset.Bitrate];
    self.fpsTextField.text = [NSString stringWithFormat:@"%ld", preset.Fps];
}

- (IBAction)onApplyButtonClick:(id)sender {
    self.videoEncodeConfig.width = 0;
    self.videoEncodeConfig.height = 0;
    self.videoEncodeConfig.maxProfile = _videoResolution;
    self.videoEncodeConfig.bitrate = [self.bitrateTextField.text integerValue];
    self.videoEncodeConfig.frameRate = [self.fpsTextField.text integerValue];
    NSLog(@"%ld", self.videoEncodeConfig.bitrate);
    NSLog(@"%ld", self.videoEncodeConfig.frameRate);
    [[NERtcEngine sharedEngine] setLocalVideoConfig:self.videoEncodeConfig];
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

# pragma mark - Helper

- (void)changeButtonColor:(UIButton *)btn {
    _video180PButton.backgroundColor = [UIColor themeGrayColor];
    _video360PButton.backgroundColor = [UIColor themeGrayColor];
    _video720PButton.backgroundColor = [UIColor themeGrayColor];
    _video1080PButton.backgroundColor = [UIColor themeGrayColor];
    btn.backgroundColor = [UIColor themeBlueColor];
}

# pragma mark - IBActions

- (void)onLeaveAction:(id)sender {
    [NERtcEngine.sharedEngine leaveChannel];
    [self removeKeyboardObserver];
    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];
}

- (IBAction)onVideo180PButtonClick:(id)sender {
    _videoResolution = kNERtcVideoProfileLow;   // 320x180/240
    [self changeButtonColor:_video180PButton];
    [self refreshBitrate];
}

- (IBAction)onVideo360PButtonClick:(id)sender {
    _videoResolution = kNERtcVideoProfileStandard;  // 640x360/480
    [self changeButtonColor:_video360PButton];
    [self refreshBitrate];
}

- (IBAction)onVideo720PButtonClick:(id)sender {
    _videoResolution = kNERtcVideoProfileHD720P;
    [self changeButtonColor:_video720PButton];
    [self refreshBitrate];
}

- (IBAction)onVideo1080PButtonClick:(id)sender {
    _videoResolution = kNERtcVideoProfileHD1080P;
    [self changeButtonColor:_video1080PButton];
    [self refreshBitrate];
}

#pragma mark - Notification

- (void)addKeyboardObserver {
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(keyboardWillShow:) name:UIKeyboardWillShowNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(keyboardWillHide:) name:UIKeyboardWillHideNotification object:nil];
}

- (void)removeKeyboardObserver {
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIKeyboardWillShowNotification object:nil];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIKeyboardWillHideNotification object:nil];
}

- (BOOL)keyboardWillShow:(NSNotification *)noti {
    CGFloat animationDuration = [[[noti userInfo] objectForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];
    CGRect keyboardBounds = [[[noti userInfo] objectForKey:UIKeyboardFrameEndUserInfoKey] CGRectValue];

    [UIView animateWithDuration:animationDuration
                     animations:^{
        self.bottomConstraint.constant = keyboardBounds.size.height;
    }];
    return YES;
}

- (BOOL)keyboardWillHide:(NSNotification *)noti {
    CGFloat animationDuration = [[[noti userInfo] objectForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];

    [UIView animateWithDuration:animationDuration
                     animations:^{
        self.bottomConstraint.constant = 25;
    }];
    return YES;
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

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesBegan:touches withEvent:event];
    [self.view endEditing:YES];
}

@end

#pragma mark - Preset

@implementation Preset

- (instancetype)initWithBitrate:(NSInteger)Bitrate Fps:(NSInteger)Fps {
    Preset *preset = [Preset new];

    preset.Bitrate = Bitrate;
    preset.Fps = Fps;
    return preset;
}

@end
