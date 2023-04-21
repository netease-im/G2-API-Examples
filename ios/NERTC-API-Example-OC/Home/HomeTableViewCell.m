//
//  HomeTableViewCell.m
//
//

#import "HomeTableViewCell.h"
@interface HomeTableViewCell ()
@property (weak, nonatomic) IBOutlet UILabel *titleLabel;
@property (weak, nonatomic) IBOutlet UILabel *descLabel;
@property (weak, nonatomic) IBOutlet UIView *containerView;
@property (weak, nonatomic) IBOutlet UILabel *centerTitleLabel;
@property (nonatomic, strong) CAShapeLayer *maskLayer;
@end
@implementation HomeTableViewCell

- (CAShapeLayer *)maskLayer {
    if (!_maskLayer) {
        UIBezierPath *maskPath = [UIBezierPath bezierPathWithRoundedRect:self.containerView.bounds byRoundingCorners:UIRectCornerTopLeft | UIRectCornerTopRight | UIRectCornerBottomLeft | UIRectCornerBottomRight cornerRadii:CGSizeMake(8, 8)];
        _maskLayer = [[CAShapeLayer alloc] init];
        _maskLayer.frame = self.containerView.bounds;
        _maskLayer.path = maskPath.CGPath;
    }

    return _maskLayer;
}

- (void)awakeFromNib {
    [super awakeFromNib];
    self.titleLabel.adjustsFontSizeToFitWidth = YES;
    self.descLabel.adjustsFontSizeToFitWidth = YES;
    self.centerTitleLabel.adjustsFontSizeToFitWidth = YES;
}

- (void)drawRect:(CGRect)rect {
    [super drawRect:rect];
    self.containerView.layer.mask = self.maskLayer;
}

- (void)setHomeDictionary:(NSDictionary *)homeDic {
    if ([[homeDic objectForKey:@"desc"] isEqualToString:@""]) {
        [self.centerTitleLabel setHidden:false];
        [self.descLabel setHidden:YES];
        [self.titleLabel setHidden:YES];
        self.centerTitleLabel.text = [homeDic objectForKey:@"title"];
    } else {
        [self.centerTitleLabel setHidden:YES];
        [self.descLabel setHidden:false];
        [self.titleLabel setHidden:false];
        self.titleLabel.text = [homeDic objectForKey:@"title"];
        self.descLabel.text = [homeDic objectForKey:@"desc"];
    }
}

@end
