//
//  VideoCallingEnterViewController.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/9/26.
//

#import "basicViewController.h"
#include "NSString+Common.h"
#import "VideoCallingEnterViewController.h"

@interface VideoCallingEnterViewController ()
@property (weak, nonatomic) IBOutlet UITextField *userIdTextField;
@property (weak, nonatomic) IBOutlet UITextField *roomIdTextField;
@end

@implementation VideoCallingEnterViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    [self setupRandomId];
}

- (void)setupRandomId {
    _roomIdTextField.text = [NSString generateRandomRoomNumber];
    _userIdTextField.text = [NSString generateRandomUserId];
}

- (IBAction)onStartClick:(id)sender {
    Class class = NSClassFromString(_vcName);
    id controller = [[class alloc] initWithRoomId:_roomIdTextField.text userId:[_userIdTextField.text longLongValue]];

    if (controller) {
        [self.navigationController pushViewController:controller animated:YES];
    }
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesBegan:touches withEvent:event];
    [self.view endEditing:YES];
}

@end
