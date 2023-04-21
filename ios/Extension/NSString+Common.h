//
//  NSString+Common.h
//  NERtc_ios
//
//  Created by test on 2022/9/22.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface NSString (Common)
+ (NSString *)convertToJsonData:(NSDictionary *)dict;

+ (NSString *)generateRandomRoomNumber;

+ (NSString *)generateRandomUserId;

+ (NSString *)generateRandomStreamId;
@end

NS_ASSUME_NONNULL_END
