//
//  SwitchRoomViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/10/17.
//

#import "NSString+Common.h"
#import "NTESAppConfig.h"
#import "SwitchRoomViewController.h"
#import "UIColor+Hex.h"

@interface SwitchRoomViewController ()<NERtcEngineDelegateEx>

@property (weak, nonatomic) IBOutlet UIButton *switchClientRoleButton;
@property (weak, nonatomic) IBOutlet UITextField *roomIDTextField;
@property (weak, nonatomic) IBOutlet NSLayoutConstraint *bottomBtnConstraint;
@property (weak, nonatomic) IBOutlet UIView *localUserView;
@property (strong, nonatomic) IBOutletCollection(UIView) NSArray *remoteViewArr;

@end

@implementation SwitchRoomViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupUI];
    [self setupNERtcEngine];
    [NERtcEngine.sharedEngine enableLocalAudio:YES];
    [NERtcEngine.sharedEngine enableLocalVideo:YES];
    [self setupLocalVideoCanvas];
    [self joinCurrentRoom];
    [self addKeyboardObserver];
}

- (void)setupUI {
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Leave" style:UIBarButtonItemStylePlain target:self action:@selector(onLeaveAction:)];
    self.title = [@"房间号:" stringByAppendingString:(self.roomId)];
}

- (void)switchRoom {
    int result = [NERtcEngine.sharedEngine switchChannelWithToken:@""
                                                      channelName:self.roomIDTextField.text
                                                       completion:^(NSError *_Nullable error, uint64_t channelId, uint64_t elapesd, uint64_t uid) {
        if (error) {
            NSLog(@"%@", [NSString stringWithFormat:@"SwitchChannel Error:%@, 请退出房间重试", error.localizedDescription]);
        } else {
            NSLog(@"SwitchChannel Success");
        }
    }];

    if (result != 0) {
        if (result == kNERtcErrSwitchChannelInvalidState) {
            NSLog(@"Error SwitchChannelInvalidState");
        } else if (result == KNERtcErrChannelReservePermissionDenied) {
            NSLog(@"Error 请使用观众角色");
        } else if (result == kNERtcErrChannelAlreadyJoined) {
            NSLog(@"Error 请更换房间名");
        } else {
            NSLog(@"SwitchChannel Not Support");
        }
    }
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

#pragma mark - Actions

- (void)onLeaveAction:(id)sender {
    [NERtcEngine.sharedEngine leaveChannel];
    [self removeKeyboardObserver];
    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];//返回上一页面
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesBegan:touches withEvent:event];
    [self.roomIDTextField resignFirstResponder];
}

- (IBAction)onSwitchClientRoleButtonClick:(UIButton *)sender {
    _switchClientRoleButton.backgroundColor = [UIColor themeGrayColor];
    _switchClientRoleButton.enabled = NO;
    [NERtcEngine.sharedEngine setClientRole:kNERtcClientRoleAudience];
}

- (IBAction)onSwitchRoomButtonClick:(UIButton *)sender {
    self.title = [@"房间号：" stringByAppendingString:self.roomIDTextField.text];
    //切换房间
    [self switchRoom];
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
        self.bottomBtnConstraint.constant = keyboardBounds.size.height;
    }];
    return YES;
}

- (BOOL)keyboardWillHide:(NSNotification *)noti {
    CGFloat animationDuration = [[[noti userInfo] objectForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];

    [UIView animateWithDuration:animationDuration
                     animations:^{
        self.bottomBtnConstraint.constant = 0;
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

@end
