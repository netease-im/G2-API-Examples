//
//  UIColor+Hex.h
//
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface UIColor (Hex)

+ (UIColor *)hexColor:(NSString *)hexString;

+ (UIColor *)themeBlueColor;

+ (UIColor *)themeGrayColor;

- (UIImage *)trans2Image:(CGSize)imageSize;

@end

NS_ASSUME_NONNULL_END
