//
//  SpeedTestViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/10/10.
//

#import "NSString+Common.h"
#import "NTESAppConfig.h"
#import "SpeedTestViewController.h"

@interface SpeedTestViewController () <NERtcEngineDelegateEx>

@property (weak, nonatomic) IBOutlet UIButton *startButton;
@property (weak, nonatomic) IBOutlet UITextField *userIdTextField;
@property (weak, nonatomic) IBOutlet UITextView *speedResultTextView;
@property (assign, nonatomic) BOOL isSpeedTesting;

@end

@implementation SpeedTestViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    _isSpeedTesting = false;
    [self setupUI];
    [self setupNERtcEngine];
}

- (void)setupUI {
    self.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Leave" style:UIBarButtonItemStylePlain target:self action:@selector(onLeaveAction:)];
    self.title = @"NERtc通话前网络测试";
    _userIdTextField.text = [NSString generateRandomUserId];
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

- (void)beginSpeedTest {
    _isSpeedTesting = YES;
    NERtcLastmileProbeConfig *probeConfig = [[NERtcLastmileProbeConfig alloc] init];
    probeConfig.probeUplink = YES;
    probeConfig.probeDownlink = YES;
    probeConfig.expectedUplinkBitrate = 300000; //本端期望的最高发送码率。单位为 bps，范围为 [100000, 5000000]。
    probeConfig.expectedDownlinkBitrate = 300000; //本端期望的最高接收码率。单位为 bps，范围为 [100000, 5000000]。
    [[NERtcEngine sharedEngine] startLastmileProbeTest:probeConfig];
}

- (IBAction)onStartButtonClick:(UIButton *)sender {
    if (_isSpeedTesting) {
        return;
    }

    if ([_startButton isSelected]) {
        [_startButton setTitle:@"开始测试"
                      forState:UIControlStateNormal];
        _speedResultTextView.text = @"";
    } else {
        [self beginSpeedTest];
        self.startButton.highlighted = YES;
        [_startButton setTitle:@"正在测速中" forState:UIControlStateNormal];
    }

    _startButton.selected = !_startButton.selected;
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesBegan:touches withEvent:event];
    [self.view endEditing:YES];
}

- (void)destroyEngine {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        [NERtcEngine destroyEngine];
    });
}

- (void)onLeaveAction:(id)sender {
    [self destroyEngine];
    [self.navigationController popViewControllerAnimated:YES];//返回上一页面
}

#pragma mark - NERtcEngine Delegate

- (void)onNERtcEngineLastmileQuality:(NERtcNetworkQuality)quality {
    NSString *printResult;

    switch (quality) {
        case kNERtcNetworkQualityExcellent:
            printResult = @"Excellent network quality";
            break;

        case kNERtcNetworkQualityGood:
            printResult = @"Good network quality";
            break;

        case kNERtcNetworkQualityPoor:
            printResult = @"Poor network quality";
            break;

        case kNERtcNetworkQualityBad:
            printResult = @"Bad network quality";
            break;

        case kNERtcNetworkQualityVeryBad:
            printResult = @"Very bad network quality";
            break;

        case kNERtcNetworkQualityDown:
            printResult = @"Unable to communicate";
            break;

        default:
            printResult = @"Unknown network quality";
            break;
    }
    self.speedResultTextView.text = [self.speedResultTextView.text stringByAppendingString:printResult];
}

- (void)onNERtcEngineLastmileProbeTestResult:(NERtcLastmileProbeResult *)result {
    NSString *printResult = [[NSString alloc]
                             initWithFormat:@"\nstate：%lu, rtt: %lums\n\n"
                             "up_jitter: %lums, up_packet_loss_rate: %lu, up_available_band_width: %luKbps\n\n"
                             "down_jitter: %lums, down_packet_loss_rate: %lu, down_available_band_width: %luKbps\n\n", result.state,
                             result.rtt, result.uplinkReport.jitter, result.uplinkReport.packetLossRate, result.uplinkReport.availableBandwidth,
                             result.downlinkReport.jitter, result.downlinkReport.packetLossRate, result.downlinkReport.availableBandwidth];

    self.speedResultTextView.text = [self.speedResultTextView.text stringByAppendingString:printResult];
    self.isSpeedTesting = false;
    [self.startButton setTitle:@"测试结束" forState:UIControlStateNormal];
}

@end
