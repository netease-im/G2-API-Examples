//
//  ExternalVideoRenderViewController.m
//  NERTC-API-Example-OC
//

#import "ExternalVideoRenderViewController.h"

#import <AVFoundation/AVFoundation.h>

#import "NTESExternalRenderView.h"
#import "NTESOpenGLView.h"
#import "NTESAppConfig.h"

@interface ExternalVideoRenderViewController () <NERtcEngineDelegateEx>

@property (nonatomic, strong) NTESExternalRenderView *localRenderView;
@property (nonatomic, strong) NTESExternalRenderView *remoteRenderView;
@property (nonatomic, strong) UILabel *remoteStatusLabel;

@property (nonatomic, strong) NERtcVideoCanvas *localCanvas;
@property (nonatomic, strong) NERtcVideoCanvas *remoteCanvas;
@property (nonatomic, assign) uint64_t remoteUserId;
@property (nonatomic, assign) BOOL isLeaving;

@end

@implementation ExternalVideoRenderViewController

- (instancetype)initWithRoomId:(NSString *)roomId userId:(uint64_t)userId {
    self = [super initWithNibName:nil bundle:nil];
    if (self) {
        self.roomId = roomId;
        self.userId = userId;
    }
    return self;
}

- (void)loadView {
    self.view = [[UIView alloc] initWithFrame:UIScreen.mainScreen.bounds];
    self.view.backgroundColor = [UIColor colorWithWhite:0.11 alpha:1.0];
}

- (void)viewDidLoad {
    [super viewDidLoad];

    NSLog(@"[ExternalVideoRender] appKey: %@, bundleId: %@",
          AppKey,
          NSBundle.mainBundle.bundleIdentifier);
    [self logCameraAuthorizationStatus];
    [self setupUI];
    int setupResult = [self setupNERtcEngine];
    NSLog(@"[ExternalVideoRender] setupEngineWithContext result: %d", setupResult);
    int audioResult = [NERtcEngine.sharedEngine enableLocalAudio:YES];
    NSLog(@"[ExternalVideoRender] enableLocalAudio result: %d", audioResult);
    int videoResult = [NERtcEngine.sharedEngine enableLocalVideo:YES];
    NSLog(@"[ExternalVideoRender] enableLocalVideo result: %d", videoResult);
    [self setupLocalExternalVideoCanvas];
    [self joinCurrentRoom];
}

- (void)logCameraAuthorizationStatus {
    AVAuthorizationStatus status = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    NSLog(@"[ExternalVideoRender] camera authorization status: %ld", (long)status);
    if (status == AVAuthorizationStatusNotDetermined) {
        [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo
                                 completionHandler:^(BOOL granted) {
            NSLog(@"[ExternalVideoRender] camera authorization granted: %d", granted);
        }];
    }
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    if (self.isMovingFromParentViewController) {
        [self leaveChannelAndCleanup];
    }
}

- (void)setupUI {
    self.title = @"外部视频渲染";
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Leave"
                                                                            style:UIBarButtonItemStylePlain
                                                                           target:self
                                                                           action:@selector(onLeaveAction:)];

    UIStackView *stackView = [[UIStackView alloc] init];
    stackView.axis = UILayoutConstraintAxisVertical;
    stackView.spacing = 12.0;
    stackView.distribution = UIStackViewDistributionFillEqually;
    stackView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:stackView];

    UIView *localPanel = [self createPanelWithTitle:@"本地（外部渲染）" isRemote:NO];
    UIView *remotePanel = [self createPanelWithTitle:@"远端（等待加入）" isRemote:YES];
    [stackView addArrangedSubview:localPanel];
    [stackView addArrangedSubview:remotePanel];

    UIButton *leaveButton = [UIButton buttonWithType:UIButtonTypeSystem];
    leaveButton.translatesAutoresizingMaskIntoConstraints = NO;
    [leaveButton setTitle:@"退出房间" forState:UIControlStateNormal];
    [leaveButton setTitleColor:UIColor.whiteColor forState:UIControlStateNormal];
    leaveButton.titleLabel.font = [UIFont systemFontOfSize:16 weight:UIFontWeightMedium];
    leaveButton.backgroundColor = [UIColor colorWithRed:0.0 green:0.47 blue:0.89 alpha:1.0];
    leaveButton.layer.cornerRadius = 22.0;
    [leaveButton addTarget:self action:@selector(onLeaveAction:) forControlEvents:UIControlEventTouchUpInside];
    [self.view addSubview:leaveButton];

    UILayoutGuide *safeArea = self.view.safeAreaLayoutGuide;
    [NSLayoutConstraint activateConstraints:@[
        [stackView.topAnchor constraintEqualToAnchor:safeArea.topAnchor constant:12.0],
        [stackView.leadingAnchor constraintEqualToAnchor:safeArea.leadingAnchor constant:12.0],
        [stackView.trailingAnchor constraintEqualToAnchor:safeArea.trailingAnchor constant:-12.0],
        [stackView.bottomAnchor constraintEqualToAnchor:leaveButton.topAnchor constant:-12.0],

        [leaveButton.leadingAnchor constraintEqualToAnchor:safeArea.leadingAnchor constant:24.0],
        [leaveButton.trailingAnchor constraintEqualToAnchor:safeArea.trailingAnchor constant:-24.0],
        [leaveButton.bottomAnchor constraintEqualToAnchor:safeArea.bottomAnchor constant:-16.0],
        [leaveButton.heightAnchor constraintEqualToConstant:44.0],
    ]];
}

- (UIView *)createPanelWithTitle:(NSString *)title isRemote:(BOOL)isRemote {
    UIView *panel = [[UIView alloc] init];
    panel.translatesAutoresizingMaskIntoConstraints = NO;
    panel.backgroundColor = UIColor.blackColor;
    panel.layer.cornerRadius = 8.0;
    panel.clipsToBounds = YES;

    NTESExternalRenderView *renderView = [NTESExternalRenderView generateExternalRenderViewWithBufferType:NTESVideoBufferTypeRawData];
    renderView.translatesAutoresizingMaskIntoConstraints = NO;
    [panel addSubview:renderView];

    UILabel *titleLabel = [[UILabel alloc] init];
    titleLabel.translatesAutoresizingMaskIntoConstraints = NO;
    titleLabel.text = title;
    titleLabel.textColor = UIColor.whiteColor;
    titleLabel.font = [UIFont systemFontOfSize:12 weight:UIFontWeightMedium];
    titleLabel.textAlignment = NSTextAlignmentCenter;
    titleLabel.backgroundColor = [UIColor colorWithWhite:0.0 alpha:0.45];
    titleLabel.layer.cornerRadius = 4.0;
    titleLabel.clipsToBounds = YES;
    [panel addSubview:titleLabel];

    [NSLayoutConstraint activateConstraints:@[
        [renderView.leadingAnchor constraintEqualToAnchor:panel.leadingAnchor],
        [renderView.trailingAnchor constraintEqualToAnchor:panel.trailingAnchor],
        [renderView.topAnchor constraintEqualToAnchor:panel.topAnchor],
        [renderView.bottomAnchor constraintEqualToAnchor:panel.bottomAnchor],

        [titleLabel.leadingAnchor constraintEqualToAnchor:panel.leadingAnchor constant:8.0],
        [titleLabel.topAnchor constraintEqualToAnchor:panel.topAnchor constant:8.0],
        [titleLabel.heightAnchor constraintEqualToConstant:22.0],
        [titleLabel.widthAnchor constraintGreaterThanOrEqualToConstant:120.0],
    ]];

    if (isRemote) {
        self.remoteRenderView = renderView;
        self.remoteStatusLabel = titleLabel;
        renderView.hidden = YES;
    } else {
        self.localRenderView = renderView;
        // 本地预览镜像；Demo 的 NTESOpenGLView 未提供 mirror 属性，直接翻转视图。
        self.localRenderView.transform = CGAffineTransformMakeScale(-1.0, 1.0);
    }

    return panel;
}

- (void)clearRenderView:(NTESExternalRenderView *)renderView {
    if ([renderView isKindOfClass:[NTESOpenGLView class]]) {
        [(NTESOpenGLView *)renderView display:NULL clear:YES];
    }
}

- (int)setupNERtcEngine {
    NERtcLogSetting *logSetting = [[NERtcLogSetting alloc] init];
#if DEBUG
    logSetting.logLevel = kNERtcLogLevelInfo;
#else
    logSetting.logLevel = kNERtcLogLevelWarning;
#endif

    NERtcEngine *coreEngine = [NERtcEngine sharedEngine];
    NERtcEngineContext *context = [[NERtcEngineContext alloc] init];
    context.engineDelegate = self;
    context.appKey = AppKey;
    context.logSetting = logSetting;
    return [coreEngine setupEngineWithContext:context];
}

- (void)setupLocalExternalVideoCanvas {
    NERtcVideoCanvas *canvas = [[NERtcVideoCanvas alloc] init];
    canvas.useExternalRender = YES;
    canvas.externalVideoRender = self.localRenderView;
    self.localCanvas = canvas;

    int result = [NERtcEngine.sharedEngine setupLocalVideoCanvas:canvas];
    if (result != 0) {
        NSLog(@"[ExternalVideoRender] setupLocalVideoCanvas failed: %d", result);
    }
}

- (void)joinCurrentRoom {
    // 如采用安全模式，则需填入相应 Token。
    int joinResult = [NERtcEngine.sharedEngine joinChannelWithToken:@""
                                                        channelName:self.roomId
                                                              myUid:self.userId
                                                         completion:^(NSError *_Nullable error, uint64_t channelId, uint64_t elapesd, uint64_t uid) {
        if (error) {
            NSLog(@"[ExternalVideoRender] Join channel failed: code=%ld, %@, userInfo=%@",
                  (long)error.code,
                  error.localizedDescription,
                  error.userInfo);
        } else {
            NSLog(@"[ExternalVideoRender] Join channel success, channelId: %llu, uid: %llu, elapsed: %llu",
                  channelId,
                  (unsigned long long)uid,
                  (unsigned long long)elapesd);
        }
    }];
    NSLog(@"[ExternalVideoRender] joinChannelWithToken result: %d, roomId: %@, uid: %llu",
          joinResult,
          self.roomId,
          (unsigned long long)self.userId);
}

- (void)bindRemoteUserID:(uint64_t)userID {
    if (userID == 0 || userID == self.userId || self.remoteUserId != 0) {
        return;
    }

    self.remoteUserId = userID;

    NERtcVideoCanvas *canvas = [[NERtcVideoCanvas alloc] init];
    canvas.useExternalRender = YES;
    canvas.externalVideoRender = self.remoteRenderView;
    self.remoteCanvas = canvas;

    [NERtcEngine.sharedEngine setupRemoteVideoCanvas:canvas forUserID:userID];
    [NERtcEngine.sharedEngine subscribeRemoteVideo:YES
                                        forUserID:userID
                                       streamType:kNERtcRemoteVideoStreamTypeHigh];

    self.remoteRenderView.hidden = NO;
    self.remoteStatusLabel.text = @"远端（外部渲染）";
}

- (void)unbindRemoteUserID:(uint64_t)userID {
    if (self.remoteUserId != userID) {
        return;
    }

    [NERtcEngine.sharedEngine setupRemoteVideoCanvas:nil forUserID:userID];
    [self clearRenderView:self.remoteRenderView];
    self.remoteRenderView.hidden = YES;
    self.remoteStatusLabel.text = @"远端（等待加入）";
    self.remoteUserId = 0;
    self.remoteCanvas = nil;
}

- (void)destroyEngine {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        [NERtcEngine destroyEngine];
    });
}

- (void)leaveChannelAndCleanup {
    if (self.isLeaving) {
        return;
    }
    self.isLeaving = YES;

    [NERtcEngine.sharedEngine setupLocalVideoCanvas:nil];
    if (self.remoteUserId != 0) {
        [NERtcEngine.sharedEngine setupRemoteVideoCanvas:nil forUserID:self.remoteUserId];
    }
    [self clearRenderView:self.localRenderView];
    [self clearRenderView:self.remoteRenderView];

    [NERtcEngine.sharedEngine leaveChannel];
    [self destroyEngine];
}

- (void)onLeaveAction:(id)sender {
    [self leaveChannelAndCleanup];
    [self.navigationController popViewControllerAnimated:YES];
}

#pragma mark - NERtcEngineDelegateEx

- (void)onNERtcEngineUserDidJoinWithUserID:(uint64_t)userID userName:(NSString *)userName {
    [self bindRemoteUserID:userID];
}

- (void)onNERtcEngineUserVideoDidStartWithUserID:(uint64_t)userID videoProfile:(NERtcVideoProfileType)profile {
    if (self.remoteUserId == 0) {
        [self bindRemoteUserID:userID];
    }
    if (self.remoteUserId == userID) {
        [NERtcEngine.sharedEngine subscribeRemoteVideo:YES
                                            forUserID:userID
                                           streamType:kNERtcRemoteVideoStreamTypeHigh];
    }
}

- (void)onNERtcEngineUserDidLeaveWithUserID:(uint64_t)userID reason:(NERtcSessionLeaveReason)reason {
    [self unbindRemoteUserID:userID];
}

- (void)onNERtcEngineDidDisconnectWithReason:(NERtcError)reason {
    NSLog(@"[ExternalVideoRender] Engine disconnected: %ld", (long)reason);
    if (self.isLeaving) {
        return;
    }
    [self leaveChannelAndCleanup];
    [self.navigationController popViewControllerAnimated:YES];
}

- (void)onNERtcEngineDidLeaveChannelWithResult:(NERtcError)result {
    NSLog(@"[ExternalVideoRender] Leave channel result: %ld", (long)result);
}

@end
