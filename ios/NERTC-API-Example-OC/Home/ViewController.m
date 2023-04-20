//
//  ViewController.m
//  NERtc_ios
//
//  Created by test on 2022/9/22.
//

#import "HomeTableViewCell.h"
#import "SpeedTestViewController.h"
#import "ThirdBeautyEntranceViewController.h"
#import "VideoCallingEnterViewController.h"
#import "ViewController.h"


@interface ViewController ()<UITableViewDelegate, UITableViewDataSource>
@property (weak, nonatomic) IBOutlet UITableView *homeTableView;
@property (nonatomic, strong) NSArray *homeData;
@end

@implementation ViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.navigationController.interactivePopGestureRecognizer.delegate = (id)self;
    [self setupNaviBarStatus];
    [self setupTableView];
}

- (NSArray *)homeData {
    if (!_homeData) {
        _homeData = @[
            @{ @"type": (@"基础功能"),
               @"module": @[
                   @{
                       @"title": (@"语音通话"),
                       @"desc": (@"一对一或多人语音通话，包含免提/静音等功能"),
                       @"class": @"AudioCallingViewController"
                   },
                   @{
                       @"title": (@"视频通话"),
                       @"desc": (@"一对一或多人视频通话，包含免提/静音等功能"),
                       @"class": @"VideoCallingViewController"
                   }
            ] },
            @{ @"type": (@"进阶功能"),
               @"module": @[
                   @{
                       @"title": (@"画质设定"),
                       @"desc": @"",
                       @"class": @"SetVideoQualityViewController"
                   },
                   @{
                       @"title": (@"音质设定"),
                       @"desc": @"",
                       @"class": @"SetAudioQualityViewController"
                   },
                   @{
                       @"title": (@"通话前网络测试"),
                       @"desc": @"",
                       @"class": @"SpeedTestViewController"
                   },
                   @{
                       @"title": (@"美声变声"),
                       @"desc": @"",
                       @"class": @"AudioChangeViewController"
                   },
                   @{
                       @"title": (@"音效伴音"),
                       @"desc": @"",
                       @"class": @"SetBGMViewController"
                   },
                   @{
                       @"title": (@"快速切换房间"),
                       @"desc": @"",
                       @"class": @"SwitchRoomViewController"
                   },
                   @{
                       @"title": (@"自定义视频采集&渲染"),
                       @"desc": @"",
                       @"class": @"CustomCaptureViewController"
                   },
                   @{
                       @"title": (@"旁路推流"),
                       @"desc": @"",
                       @"class":@"VideoStreamViewController"
                   },
                   @{
                       @"title": (@"自研美颜"),
                       @"desc": @"",
                       @"class":@"NEBeautyViewController"
                   },
                   @{
                       @"title": (@"第三方美颜"),
                       @"desc": @"",
                       @"class":@"ThirdBeautyEntranceViewController"
                   }
        ] }];
    }

    return _homeData;
}

- (void)setupNaviBarStatus {
    self.navigationItem.title = (@"网易云信NERtc API Example");
    [self.navigationController setNavigationBarHidden:false animated:false];
    [self.navigationController.navigationBar setBackgroundImage:[[UIImage alloc] init] forBarMetrics:UIBarMetricsDefault];
    [self.navigationController.navigationBar setShadowImage:[[UIImage alloc] init]];
    self.navigationController.navigationBar.translucent = YES;
    [self.navigationController.navigationBar setTitleTextAttributes:@{ NSForegroundColorAttributeName: [UIColor whiteColor] }];
}

- (void)setupTableView {
    [self.homeTableView registerNib:[UINib nibWithNibName:@"HomeTableViewCell" bundle:nil] forCellReuseIdentifier:HomeTableViewCellReuseIdentify];
    self.homeTableView.separatorStyle = UITableViewCellSeparatorStyleNone;
}

#pragma mark - UITableViewDataSource
- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return self.homeData.count;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    NSDictionary *homeDic = self.homeData[section];
    NSArray *homeArray = [homeDic objectForKey:@"module"];

    return homeArray.count;
}

- (UIView *)tableView:(UITableView *)tableView viewForHeaderInSection:(NSInteger)section {
    UIView *headerView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, tableView.bounds.size.width, 40)];
    UILabel *titleLabel = [[UILabel alloc] initWithFrame:CGRectMake(20, 0, headerView.bounds.size.width, 40)];

    titleLabel.textColor = UIColor.whiteColor;
    titleLabel.font = [UIFont systemFontOfSize:16];
    NSDictionary *homeDic = self.homeData[section];
    titleLabel.text = [homeDic objectForKey:@"type"];
    [headerView addSubview:titleLabel];
    return headerView;
}

- (CGFloat)tableView:(UITableView *)tableView heightForHeaderInSection:(NSInteger)section {
    return 40;
}

#pragma mark - UITableViewDelegate
- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    HomeTableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:HomeTableViewCellReuseIdentify forIndexPath:indexPath];
    NSDictionary *homeDic = self.homeData[indexPath.section];
    NSArray *homeArray = [homeDic objectForKey:@"module"];

    [cell setHomeDictionary:homeArray[indexPath.row]];
    return cell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    NSDictionary *homeDic = self.homeData[indexPath.section];
    NSArray *homeArray = [homeDic objectForKey:@"module"];
    NSDictionary *homeFeaturesDic = homeArray[indexPath.row];

    [self pushFeaturesViewController:homeFeaturesDic[@"class"]];
}

- (void)pushFeaturesViewController:(NSString *)className {
    VideoCallingEnterViewController *vc = [[VideoCallingEnterViewController alloc] init];

    vc.vcName = className;

    if ([className isEqualToString:@"SpeedTestViewController"]) {
        SpeedTestViewController *controller = [[SpeedTestViewController alloc] init];
        [self.navigationController pushViewController:controller animated:YES];
        return;
    }
    else if ([className isEqualToString:@"ThirdBeautyEntranceViewController"]) {
        ThirdBeautyEntranceViewController *controller = [[ThirdBeautyEntranceViewController alloc] init];
        [self.navigationController pushViewController:controller animated:YES];
        return;
    }
    [self.navigationController pushViewController:vc animated:YES];
}

@end
