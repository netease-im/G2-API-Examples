//
//  NEBeautyManager.m
//  NERTC-API-Example-OC
//
//  Created by test on 2022/11/24.
//

#import "NEBeautyManager.h"
#import "NEBeautyConfigView.h"
#import "Resource/FUBeauty/authpack.h"

#import <NERtcSDK/NERtcSDK.h>
#import <FURenderKit/FURenderKit.h>
#import <SSZipArchive/SSZipArchive.h>

static NSString * const kNEBeautyLocalFilterFolderName = @"Filter";
static NSString * const kNEBeautyLocalStickerFolderName = @"Sticker";
static NSString * const kNEBeautyLocalMakeupFolderName = @"Makeup";

@interface NEBeautyManager ()<NEBeautyConfigViewDataSource, NEBeautyConfigViewDelegate>

@property (nonatomic, copy) NSString *localResourcePath;

@property (nonatomic, strong) NSMutableDictionary<NSNumber*, NEBeautyConfigView*> *menuMap;

// 标题tab数据源（多种美颜共用）
@property (nonatomic, strong) NSArray<NETitleDisplayModel *> *beautyTitleModelArray;
@property (nonatomic, strong) NSArray<NETitleDisplayModel *> *filterTitleModelArray;
@property (nonatomic, strong) NSArray<NETitleDisplayModel *> *stickerTitleModelArray;
@property (nonatomic, strong) NSArray<NETitleDisplayModel *> *makeupTitleModelArray;

// 云信美颜
// 美颜UI数据源
@property (nonatomic, strong) NSArray<NEBeautySliderDisplayModel *> *baseSliderModelArray;
@property (nonatomic, strong) NSArray<NEBeautySliderDisplayModel *> *shapeSliderModelArray;
@property (nonatomic, strong) NSArray<NEBeautySliderDisplayModel *> *advancedSliderModelArray;
@property (nonatomic, strong) NSArray<NEBeautySliderDisplayModel *> *advancedSliderModelArray2;
@property (nonatomic, strong) NSArray<NEBeautySliderDisplayModel *> *advancedSliderModelArray3;
@property (nonatomic, strong) NSArray<NEBeautySliderDisplayModel *> *advancedSliderModelArray4;

// 滤镜UI数据源
@property (nonatomic, strong) NSArray<NECollectionViewDisplayModel *> *filterItemModelArray;
@property (nonatomic, strong) NEBeautySliderDisplayModel *filterStrengthModel;

// 贴纸UI数据源
@property (nonatomic, strong) NSMutableArray<NECollectionViewDisplayModel *> *sticker2DModelArray;
@property (nonatomic, strong) NSMutableArray<NECollectionViewDisplayModel *> *sticker3DModelArray;
@property (nonatomic, strong) NSMutableArray<NECollectionViewDisplayModel *> *stickerParticleModelArray;
@property (nonatomic, strong) NSMutableArray<NECollectionViewDisplayModel *> *stickerFaceChangeModelArray;

// 美妆UI数据源
@property (nonatomic, strong) NSArray<NECollectionViewDisplayModel *> *makeupItemModelArray;

// 相芯美颜
@property (nonatomic, strong) NSArray<NEBeautySliderDisplayModel *> *fuBaseSliderModelArray;
@property (nonatomic, strong) NSArray<NEBeautySliderDisplayModel *> *fuShapeSliderModelArray;
@property (nonatomic, strong) NSArray<NEBeautySliderDisplayModel *> *fuAdvancedSliderModelArray;
@property (nonatomic, strong) NSArray<NEBeautySliderDisplayModel *> *fuAdvancedSliderModelArray2;
@property (nonatomic, strong) NSArray<NEBeautySliderDisplayModel *> *fuAdvancedSliderModelArray3;
@property (nonatomic, strong) NSArray<NEBeautySliderDisplayModel *> *fuAdvancedSliderModelArray4;
@property (nonatomic, strong) NSArray<NECollectionViewDisplayModel *> *fuFilterItemModelArray;
@property (nonatomic, strong) NEBeautySliderDisplayModel *fuFilterStrengthModel;
@property (nonatomic, strong) NSArray<NECollectionViewDisplayModel *> *fuSticker2DModelArray;
@property (nonatomic, strong) NSArray<NECollectionViewDisplayModel *> *fuSticker3DModelArray;
@property (nonatomic, strong) FUSticker *curSticker;

@end

@implementation NEBeautyManager {
    float _filterStrength;
}

#pragma mark - Life Cycle

- (instancetype)init {
    self = [super init];
    if (self) {
        _companyType = BeautyCompanyTypeNetease;
        _localResourcePath = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES).firstObject stringByAppendingPathComponent:@"NEBeauty"];
        _filterStrength = 0;
    }
    
    return self;
}

#pragma mark - Public

+ (instancetype)sharedManager {
    static NEBeautyManager *manager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        manager = [[NEBeautyManager alloc] init];
    });
    
    return manager;
}

- (void)prepareResource {
    // 准备资源文件
    [self prepareFilterData];
    [self prepareStickerData];
    [self prepareMakeupData];
    
    // 相芯资源
    [self prepareFilterDataForFaceUnity];
    [self prepareStickerDataForFaceUnity];
}

- (void)initFUBeauty {
    CFAbsoluteTime startTime = CFAbsoluteTimeGetCurrent();
    NSString *controllerPath = [[NSBundle mainBundle] pathForResource:@"controller_cpp" ofType:@"bundle"];
    NSString *controllerConfigPath = [[NSBundle mainBundle] pathForResource:@"controller_config" ofType:@"bundle"];
    FUSetupConfig *setupConfig = [[FUSetupConfig alloc] init];
    setupConfig.authPack = FUAuthPackMake(g_auth_package, sizeof(g_auth_package));
    setupConfig.controllerPath = controllerPath;
    setupConfig.controllerConfigPath = controllerConfigPath;
    
    // 初始化 FURenderKit
    [FURenderKit setupWithSetupConfig:setupConfig];
    
#if DEBUG
    [FURenderKit setLogLevel:FU_LOG_LEVEL_INFO];
#else
    [FURenderKit setLogLevel:FU_LOG_LEVEL_OFF];
#endif
    
    // 加载人脸 AI 模型
    NSString *faceAIPath = [[NSBundle mainBundle] pathForResource:@"ai_face_processor" ofType:@"bundle"];
    [FUAIKit loadAIModeWithAIType:FUAITYPE_FACEPROCESSOR dataPath:faceAIPath];
    
    [FURenderKit shareRenderKit].internalCameraSetting.fps = 30;
    
    CFAbsoluteTime endTime = (CFAbsoluteTimeGetCurrent() - startTime);
    NSLog(@"%s, FaceUnity initialization time = %lf", __func__, endTime);
    
    NSString *path = [[NSBundle mainBundle] pathForResource:@"face_beautification.bundle" ofType:nil];
    
    FUBeauty *beauty  = [[FUBeauty alloc] initWithPath:path name:@"FUBeauty"];;
    [FURenderKit shareRenderKit].beauty = beauty;
}

- (void)initNEBeauty {
    // 初始化美颜相关资源
    [[NERtcBeauty shareInstance] startBeauty];
    [self applyDefaultSettings];
}

- (void)destroyNEBeauty {
    // 销毁美颜相关资源
    _filterStrength = 0;
    [NERtcBeauty shareInstance].filterStrength = 0;
    [[NERtcBeauty shareInstance] stopBeauty];
}

- (void)enableNEBeauty:(BOOL)enable {
    // 开启/关闭美颜效果
    [NERtcBeauty shareInstance].isOpenBeauty = enable;
}

- (void)switchCompanyType:(BeautyCompanyType)type {
    _companyType = type;
    
    [self.menuMap enumerateKeysAndObjectsUsingBlock:^(NSNumber * _Nonnull key, NEBeautyConfigView * _Nonnull obj, BOOL * _Nonnull stop) {
        [obj reloadData];
    }];
}

- (void)processVideoFrameUsingFaceUnityWithFrame:(CVPixelBufferRef)pixelBuffer
                                        rotation:(NERtcVideoRotationType)rotation {
    FURenderInput *input = [[FURenderInput alloc] init];
    input.pixelBuffer = pixelBuffer;
    input.renderConfig.imageOrientation = 0;
    FUImageOrientation orientation = FUImageOrientationUP;
    switch (rotation) {
        case kNERtcVideoRotation_0:
            orientation = FUImageOrientationUP;
            break;
        case kNERtcVideoRotation_90:
            orientation = FUImageOrientationLeft;
            break;
        case kNERtcVideoRotation_180:
            orientation = FUImageOrientationDown;
            break;
        case kNERtcVideoRotation_270:
            orientation = FUImageOrientationRight;
            break;
        default:
            break;
    }
    input.renderConfig.imageOrientation = orientation;

    FURenderOutput *outPut = [[FURenderKit shareRenderKit] renderWithInput:input];
    [self convertPixBuffer: outPut.pixelBuffer dstPixBuffer:pixelBuffer];
}

- (void)displayMenuWithType:(NEBeautyConfigViewType)type container:(UIView *)container {
    NEBeautyConfigView *view = [self.menuMap objectForKey:@(type)];
    if (!view) {
        view = [[NEBeautyConfigView alloc] initWithType:type dataSource:self delegate:self];
        [self.menuMap setObject:view forKey:@(type)];
    }
    [view displayWithContainer:container];
}

- (void)dismissMenuWithType:(NEBeautyConfigViewType)type {
    NEBeautyConfigView *view = [self.menuMap objectForKey:@(type)];
    if (!view) {
        return;
    }
    [view dismiss];
}

#pragma mark - Private

- (void)prepareFilterData {
    NSFileManager *fileManager = [NSFileManager defaultManager];
    
    NSString *localFilterPath = [NSString pathWithComponents:@[self.localResourcePath, kNEBeautyLocalFilterFolderName]];
    if ([fileManager fileExistsAtPath:localFilterPath]) {
        [fileManager removeItemAtPath:localFilterPath error:nil];
    }
    [fileManager createDirectoryAtPath:localFilterPath withIntermediateDirectories:YES attributes:nil error:nil];
    
    NSString *bundlePath = [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:@"filters.bundle"];
    NSArray *fileNameArray = [fileManager contentsOfDirectoryAtPath:bundlePath error:nil];
    
    NSMutableArray *modelArray = [NSMutableArray array];
    
    for (int i = 0; i < fileNameArray.count; i++) {
        NSString *fileName = fileNameArray[i];
        
        if ([fileName hasPrefix:@"filter_style"] && [fileName hasSuffix:@"zip"]) {
            NSString *filterPath = [NSString pathWithComponents:@[bundlePath, fileName]];
            [SSZipArchive unzipFileAtPath:filterPath toDestination:localFilterPath];
            
            NSString *filterName = [[filterPath lastPathComponent] stringByDeletingPathExtension];
            
            NECollectionViewDisplayModel *model = [[NECollectionViewDisplayModel alloc] init];
            model.resourcePath = [[NSString pathWithComponents:@[localFilterPath, filterName]] stringByAppendingString:@"/"];
            model.name = [filterName stringByReplacingOccurrencesOfString:@"filter_style_" withString:@""];
            model.image = [UIImage imageWithContentsOfFile:[[filterPath stringByDeletingPathExtension] stringByAppendingPathExtension:@"png"]];
            model.index = i;
            model.isSelected = NO;
            model.type = NEBeautyEffectTypeFilter;
            
            [modelArray addObject:model];
        }
    }
    
    _filterItemModelArray = modelArray;
}

- (void)prepareStickerData {
    NSFileManager *fileManager = [NSFileManager defaultManager];
    
    NSArray *stickerCategoryArray = @[@"2d_sticker",
                                      @"3d_sticker",
                                      @"particle_sticker",
                                      @"face_change_sticker"];
    NSArray *stickerTypeArray = @[@(NEBeautyEffectTypeSticker2D),
                                  @(NEBeautyEffectTypeSticker3D),
                                  @(NEBeautyEffectTypeStickerParticle),
                                  @(NEBeautyEffectTypeStickerFaceChange)];
    NSArray *stickerDataArray = @[self.sticker2DModelArray,
                                  self.sticker3DModelArray,
                                  self.stickerParticleModelArray,
                                  self.stickerFaceChangeModelArray];
    
    for (int i = 0; i < stickerCategoryArray.count; i++) {
        NSString *category = stickerCategoryArray[i];
        NSInteger type = [stickerTypeArray[i] integerValue];
        NSMutableArray *stickerModelArray = stickerDataArray[i];
        [stickerModelArray removeAllObjects];
        
        NSString *localStickerPath = [[NSString pathWithComponents:@[self.localResourcePath, kNEBeautyLocalStickerFolderName]] stringByAppendingPathComponent:category];
        if ([fileManager fileExistsAtPath:localStickerPath]) {
            [fileManager removeItemAtPath:localStickerPath error:nil];
        }
        [fileManager createDirectoryAtPath:localStickerPath withIntermediateDirectories:YES attributes:nil error:nil];
        
        NSString *bundlePath = [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:[NSString stringWithFormat:@"%@.bundle", category]];
        NSArray *fileNameArray = [fileManager contentsOfDirectoryAtPath:bundlePath error:nil];
        
        for (int j = 0; j < fileNameArray.count; j++) {
            NSString *fileName = fileNameArray[j];
            
            if ([fileName hasSuffix:@"zip"]) {
                NSString *stickerPath = [NSString pathWithComponents:@[bundlePath, fileName]];
                [SSZipArchive unzipFileAtPath:stickerPath toDestination:localStickerPath];
                
                NSString *stickerName = [[stickerPath lastPathComponent] stringByDeletingPathExtension];
                
                NECollectionViewDisplayModel *model = [[NECollectionViewDisplayModel alloc] init];
                model.resourcePath = [[NSString pathWithComponents:@[localStickerPath, stickerName]] stringByAppendingString:@"/"];
                model.name = @"";
                model.image = [UIImage imageWithContentsOfFile:[[stickerPath stringByDeletingPathExtension] stringByAppendingPathExtension:@"png"]];
                model.index = j;
                model.isSelected = NO;
                model.type = type;
                
                [stickerModelArray addObject:model];
            }
        }
    }
}

- (void)prepareMakeupData {
    NSFileManager *fileManager = [NSFileManager defaultManager];
    
    NSString *localMakeupPath = [NSString pathWithComponents:@[self.localResourcePath, kNEBeautyLocalMakeupFolderName]];
    if ([fileManager fileExistsAtPath:localMakeupPath]) {
        [fileManager removeItemAtPath:localMakeupPath error:nil];
    }
    [fileManager createDirectoryAtPath:localMakeupPath withIntermediateDirectories:YES attributes:nil error:nil];
    
    NSString *bundlePath = [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:@"makeup_sticker.bundle"];
    NSArray *fileNameArray = [fileManager contentsOfDirectoryAtPath:bundlePath error:nil];
    
    NSMutableArray *modelArray = [NSMutableArray array];
    
    for (int i = 0; i < fileNameArray.count; i++) {
        NSString *fileName = fileNameArray[i];
        
        if ([fileName hasSuffix:@"zip"]) {
            NSString *makeupPath = [NSString pathWithComponents:@[bundlePath, fileName]];
            [SSZipArchive unzipFileAtPath:makeupPath toDestination:localMakeupPath];
            
            NSString *makeupName = [[makeupPath lastPathComponent] stringByDeletingPathExtension];
            
            NECollectionViewDisplayModel *model = [[NECollectionViewDisplayModel alloc] init];
            model.resourcePath = [[NSString pathWithComponents:@[localMakeupPath, makeupName]] stringByAppendingString:@"/"];
            model.name = @"";
            model.image = [UIImage imageWithContentsOfFile:[[makeupPath stringByDeletingPathExtension] stringByAppendingPathExtension:@"png"]];
            model.index = i;
            model.isSelected = NO;
            model.type = NEBeautyEffectTypeMakeup;
            
            [modelArray addObject:model];
        }
    }
    
    _makeupItemModelArray = modelArray;
}

- (void)prepareFilterDataForFaceUnity {
    NSArray *filterNameArray = @[@"origin",@"ziran1",@"ziran2",@"ziran3",@"ziran4",@"ziran5",@"ziran6",@"ziran7",@"ziran8",
                                         @"zhiganhui1",@"zhiganhui2",@"zhiganhui3",@"zhiganhui4",@"zhiganhui5",@"zhiganhui6",@"zhiganhui7",@"zhiganhui8",
                                         @"mitao1",@"mitao2",@"mitao3",@"mitao4",@"mitao5",@"mitao6",@"mitao7",@"mitao8",
                                         @"bailiang1",@"bailiang2",@"bailiang3",@"bailiang4",@"bailiang5",@"bailiang6",@"bailiang7"
                                         ,@"fennen1",@"fennen2",@"fennen3",@"fennen5",@"fennen6",@"fennen7",@"fennen8",
                                         @"lengsediao1",@"lengsediao2",@"lengsediao3",@"lengsediao4",@"lengsediao7",@"lengsediao8",@"lengsediao11",
                                         @"nuansediao1",@"nuansediao2",
                                         @"gexing1",@"gexing2",@"gexing3",@"gexing4",@"gexing5",@"gexing7",@"gexing10",@"gexing11",
                                         @"xiaoqingxin1",@"xiaoqingxin3",@"xiaoqingxin4",@"xiaoqingxin6",
                                         @"heibai1",@"heibai2",@"heibai3",@"heibai4"];
    
    NSDictionary *filterNameMap = @{@"origin":@"原图",@"bailiang1":@"白亮1",@"bailiang2":@"白亮2",@"bailiang3":@"白亮3",@"bailiang4":@"白亮4",@"bailiang5":@"白亮5",@"bailiang6":@"白亮6",@"bailiang7":@"白亮7"
                                    ,@"fennen1":@"粉嫩1",@"fennen2":@"粉嫩2",@"fennen3":@"粉嫩3",@"fennen4":@"粉嫩4",@"fennen5":@"粉嫩5",@"fennen6":@"粉嫩6",@"fennen7":@"粉嫩7",@"fennen8":@"粉嫩8",
                                    @"gexing1":@"个性1",@"gexing2":@"个性2",@"gexing3":@"个性3",@"gexing4":@"个性4",@"gexing5":@"个性5",@"gexing6":@"个性6",@"gexing7":@"个性7",@"gexing8":@"个性8",@"gexing9":@"个性9",@"gexing10":@"个性10",@"gexing11":@"个性11",
                                    @"heibai1":@"黑白1",@"heibai2":@"黑白2",@"heibai3":@"黑白3",@"heibai4":@"黑白4",@"heibai5":@"黑白5",
                                    @"lengsediao1":@"冷色调1",@"lengsediao2":@"冷色调2",@"lengsediao3":@"冷色调3",@"lengsediao4":@"冷色调4",@"lengsediao5":@"冷色调5",@"lengsediao6":@"冷色调6",@"lengsediao7":@"冷色调7",@"lengsediao8":@"冷色调8",@"lengsediao9":@"冷色调9",@"lengsediao10":@"冷色调10",@"lengsediao11":@"冷色调11",
                                    @"nuansediao1":@"暖色调1",@"nuansediao2":@"暖色调2",@"nuansediao3":@"暖色调3",@"xiaoqingxin1":@"小清新1",@"xiaoqingxin2":@"小清新2",@"xiaoqingxin3":@"小清新3",@"xiaoqingxin4":@"小清新4",@"xiaoqingxin5":@"小清新5",@"xiaoqingxin6":@"小清新6",
                                    @"ziran1":@"自然1",@"ziran2":@"自然2",@"ziran3":@"自然3",@"ziran4":@"自然4",@"ziran5":@"自然5",@"ziran6":@"自然6",@"ziran7":@"自然7",@"ziran8":@"自然8",
                                    @"mitao1":@"蜜桃1",@"mitao2":@"蜜桃2",@"mitao3":@"蜜桃3",@"mitao4":@"蜜桃4",@"mitao5":@"蜜桃5",@"mitao6":@"蜜桃6",@"mitao7":@"蜜桃7",@"mitao8":@"蜜桃8",
                                    @"zhiganhui1":@"质感灰1",@"zhiganhui2":@"质感灰2",@"zhiganhui3":@"质感灰3",@"zhiganhui4":@"质感灰4",@"zhiganhui5":@"质感灰5",@"zhiganhui6":@"质感灰6",@"zhiganhui7":@"质感灰7",@"zhiganhui8":@"质感灰8"};
    
    NSMutableArray *modelArray = [NSMutableArray array];
    
    for (int i = 0; i < filterNameArray.count; i++) {
        NSString *filterName = filterNameArray[i];
        
        NECollectionViewDisplayModel *model = [[NECollectionViewDisplayModel alloc] init];
        model.name = filterName;
        model.image = [UIImage imageNamed:filterName];
        model.index = i;
        model.isSelected = NO;
        model.type = NEBeautyEffectTypeFilter;
        
        [modelArray addObject:model];
    }
    
    _fuFilterItemModelArray = modelArray;
}

- (void)prepareStickerDataForFaceUnity {
    NSArray *totalArray = [NSArray arrayWithContentsOfFile:[[NSBundle mainBundle] pathForResource:@"dataSource" ofType:@"plist"]];
    for (NSArray *dataArray in totalArray) {
        for (NSDictionary *dataDic in dataArray) {
            int type = [dataDic[@"itemType"] intValue];
            if (type == 1) {
                // 道具贴纸 <=> 2D
                NSArray *items = dataDic[@"items"];
                NSUInteger count = items.count;
                NSMutableArray *modelArray = [NSMutableArray array];
                for (int i = 0; i < count; i++) {
                    NSString *itemName = items[i];
                    NECollectionViewDisplayModel *model = [[NECollectionViewDisplayModel alloc] init];
                    model.name = itemName;
                    model.image = [UIImage imageNamed:[itemName stringByAppendingPathExtension:@"png"]];
                    model.index = i;
                    model.isSelected = NO;
                    model.type = NEBeautyEffectTypeSticker2D;
                    
                    [modelArray addObject:model];
                }
                _fuSticker2DModelArray = modelArray;
            } else if (type == 5) {
                // AR面具 <=> 3D
                NSArray *items = dataDic[@"items"];
                NSUInteger count = items.count;
                NSMutableArray *modelArray = [NSMutableArray array];
                for (int i = 0; i < count; i++) {
                    NSString *itemName = items[i];
                    NECollectionViewDisplayModel *model = [[NECollectionViewDisplayModel alloc] init];
                    model.name = itemName;
                    model.image = [UIImage imageNamed:[itemName stringByAppendingPathExtension:@"png"]];
                    model.index = i;
                    model.isSelected = NO;
                    model.type = NEBeautyEffectTypeSticker3D;
                    
                    [modelArray addObject:model];
                }
                _fuSticker3DModelArray = modelArray;
            }
        }
    }
}

- (void)convertPixBuffer:(CVPixelBufferRef) srcPixelBuffer dstPixBuffer:(CVPixelBufferRef) outPixelBuffer {
    CVPixelBufferLockBaseAddress(srcPixelBuffer, 0);
    
//    size_t width = CVPixelBufferGetWidth(srcPixelBuffer);
    size_t height = CVPixelBufferGetHeight(srcPixelBuffer);
//    size_t bytesPerRow = CVPixelBufferGetBytesPerRow(srcPixelBuffer);
    
    // Y分量
    void *yBaseAddress = CVPixelBufferGetBaseAddressOfPlane(srcPixelBuffer, 0);
    size_t yBytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(srcPixelBuffer, 0);
    size_t yLength = yBytesPerRow * height;
    
    // UV分量
    void *uvBaseAddress = CVPixelBufferGetBaseAddressOfPlane(srcPixelBuffer, 1);
    size_t uvBytesPerRow = CVPixelBufferGetBytesPerRowOfPlane(srcPixelBuffer, 1);
    size_t uvLength = uvBytesPerRow * height / 2;
    
    CVPixelBufferUnlockBaseAddress(srcPixelBuffer, 0);
    CVPixelBufferLockBaseAddress(outPixelBuffer, 0);
    void *yBaseAddressOld = CVPixelBufferGetBaseAddressOfPlane(outPixelBuffer, 0);
    memcpy(yBaseAddressOld, yBaseAddress, yLength);
    void *uvBaseAddressOld = CVPixelBufferGetBaseAddressOfPlane(outPixelBuffer, 1);
    memcpy(uvBaseAddressOld, uvBaseAddress, uvLength);
    CVPixelBufferUnlockBaseAddress(outPixelBuffer, 0);
}

- (void)applyDefaultSettings {
    for (NEBeautySliderDisplayModel *model in self.baseSliderModelArray) {
        if (model.type == NEBeautySliderTypeSmooth) {
            model.value = 0.65;
        } else if (model.type == NEBeautySliderTypeWhiten) {
            model.value = 0.8;
        } else if (model.type == NEBeautySliderTypeMouth) {
            model.value = 0.8;
        } else if (model.type == NEBeautySliderTypeThinFace) {
            model.value = 0.35;
        } else if (model.type == NEBeautySliderTypeFaceRuddy) {
            model.value = 0.1;
        } else if (model.type == NEBeautySliderTypeFaceSharpen) {
            model.value = 0.1;
        }
    }
    
    for (NEBeautySliderDisplayModel *model in self.shapeSliderModelArray) {
        if (model.type == NEBeautySliderTypeBigEye) {
            model.value = 0.3;
        } else if (model.type == NEBeautySliderTypeSmallFace) {
            model.value = 0.1;
        } else if (model.type == NEBeautySliderTypeJaw) {
            model.value = 0.4;
        }
    }
    
    for (NEBeautySliderDisplayModel *model in self.advancedSliderModelArray) {
        if (model.type == NEBeautySliderTypeLightEye) {
            model.value = 0.6;
        } else if (model.type == NEBeautySliderTypeWhiteTeeth) {
            model.value = 0.3;
        } else if (model.type == NEBeautySliderTypeSmallNose) {
            model.value = 0.4;
        } else if (model.type == NEBeautySliderTypeEyeDis) {
            model.value = 0.4;
        } else if (model.type == NEBeautySliderTypeEyeAngle) {
            model.value = 0.5;
        }
    }
    
    for (NEBeautySliderDisplayModel *model in self.advancedSliderModelArray2) {
        if (model.type == NEBeautySliderTypeLongNose) {
            model.value = 0;
        } else if (model.type == NEBeautySliderTypeRenZhong) {
            model.value = 0.5;
        } else if (model.type == NEBeautySliderTypeMouthAngle) {
            model.value = 0.5;
        } else if (model.type == NEBeautySliderTypeRoundEye) {
            model.value = 0.8;
        } else if (model.type == NEBeautySliderTypeOpenEyeAngle) {
            model.value = 0;
        }
    }
    
    for (NEBeautySliderDisplayModel *model in self.advancedSliderModelArray3) {
        if (model.type == NEBeautySliderTypeVFace) {
            model.value = 0;
        } else if (model.type == NEBeautySliderTypeThinUnderjaw) {
            model.value = 0;
        } else if (model.type == NEBeautySliderTypeNarrowFace) {
            model.value = 0;
        } else if (model.type == NEBeautySliderTypeCheekBone) {
            model.value = 0;
        }
    }
    
    [NERtcBeauty shareInstance].smoothSkin = 0.65;
    [NERtcBeauty shareInstance].whiteSkin = 0.8;
    [NERtcBeauty shareInstance].mouth = 0.8;
    [NERtcBeauty shareInstance].thinFace = 0.35;
    [NERtcBeauty shareInstance].faceRuddyStrength = 0.1;
    [NERtcBeauty shareInstance].faceSharpenStrength = 0.1;
    [NERtcBeauty shareInstance].bigEye = 0.3;
    [NERtcBeauty shareInstance].smallFace = 0.1;
    [NERtcBeauty shareInstance].jaw = 0.4;
    [NERtcBeauty shareInstance].brightEye = 0.6;
    [NERtcBeauty shareInstance].teeth = 0.3;
    [NERtcBeauty shareInstance].smallNose = 0.4;
    [NERtcBeauty shareInstance].eyesDistance = 0.4;
    [NERtcBeauty shareInstance].eyesAngle = 0.5;
    [NERtcBeauty shareInstance].longNoseStrength = 0;
    [NERtcBeauty shareInstance].renZhongStrength = 0.5;
    [NERtcBeauty shareInstance].mouthAngle = 0.5;
    [NERtcBeauty shareInstance].roundEyeStrength = 0.8;
    [NERtcBeauty shareInstance].openEyeAngleStrength = 0;
    [NERtcBeauty shareInstance].vFaceStrength = 0;
    [NERtcBeauty shareInstance].thinUnderjawStrength = 0;
    [NERtcBeauty shareInstance].narrowFaceStrength = 0;
    [NERtcBeauty shareInstance].cheekBoneStrength = 0;
    
//    self.filterStrengthModel.value = 0.7;
//    NECollectionViewDisplayModel *selectedModel = nil;
//    for (NECollectionViewDisplayModel *model in self.filterItemModelArray) {
//        if ([model.name isEqualToString:@"白皙"]) {
//            selectedModel = model;
//
//            break;
//        }
//    }
//    if (!selectedModel) {
//        return;
//    }
//    selectedModel.isSelected = YES;
//    _filterStrength = 0.7;
//    [[NERtcBeauty shareInstance] removeBeautyFilter];
//    [[NERtcBeauty shareInstance] addBeautyFilterWithPath:selectedModel.resourcePath andName:@"template.json"];
//    [NERtcBeauty shareInstance].filterStrength = 0.7;
}

#pragma mark - NEBeautyConfigViewDelegate

- (void)didTriggerResetActionWithConfigViewType:(NEBeautyConfigViewType)type {
    switch (type) {
        case NEBeautyConfigViewTypeFilter: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [[NERtcBeauty shareInstance] removeBeautyFilter];
            } else {
                [FURenderKit shareRenderKit].beauty.filterName = @"origin";
            }
            
            break;
        }
        case NEBeautyConfigViewTypeSticker: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [[NERtcBeauty shareInstance] removeBeautySticker];
            } else {
                [[FURenderKit shareRenderKit].stickerContainer removeSticker:self.curSticker completion:NULL];
                self.curSticker = nil;
            }
            
            break;
        }
        case NEBeautyConfigViewTypeMakeup: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [[NERtcBeauty shareInstance] removeBeautyMakeup];
            }
            
            break;
        }
            
        default:
            break;
    }
}

- (void)didSelectItemWithConfigViewType:(NEBeautyConfigViewType)type model:(NECollectionViewDisplayModel *)model {
    switch (type) {
        case NEBeautyConfigViewTypeFilter: {
            if (!model) {
                if (_companyType == BeautyCompanyTypeNetease) {
                    [[NERtcBeauty shareInstance] removeBeautyFilter];
                } else {
                    [FURenderKit shareRenderKit].beauty.filterName = @"origin";
                }
            } else {
                if (_companyType == BeautyCompanyTypeNetease) {
                    [[NERtcBeauty shareInstance] addBeautyFilterWithPath:model.resourcePath andName:@"template.json"];
                    [NERtcBeauty shareInstance].filterStrength = _filterStrengthModel.value;
                } else {
                    [FURenderKit shareRenderKit].beauty.filterName = model.name;
                    [FURenderKit shareRenderKit].beauty.filterLevel = _fuFilterStrengthModel.value;
                }
            }
            
            break;
        }
        case NEBeautyConfigViewTypeSticker: {
            if (!model) {
                if (_companyType == BeautyCompanyTypeNetease) {
                    [[NERtcBeauty shareInstance] removeBeautySticker];
                } else {
                    [[FURenderKit shareRenderKit].stickerContainer removeSticker:self.curSticker completion:NULL];
                    self.curSticker = nil;
                }
            } else {
                if (_companyType == BeautyCompanyTypeNetease) {
                    [[NERtcBeauty shareInstance] addBeautyStickerWithPath:model.resourcePath andName:@"template.json"];
                } else {
                    NSString *path = [[NSBundle mainBundle] pathForResource:[model.name   stringByAppendingString:@".bundle"] ofType:nil];
                    
                    FUSticker *newItem = [[FUSticker alloc] initWithPath:path name:@"sticker"];
                    [[FURenderKit shareRenderKit].stickerContainer replaceSticker:self.curSticker withSticker:newItem completion:NULL];
                    self.curSticker = newItem;
                }
            }
            
            break;
        }
        case NEBeautyConfigViewTypeMakeup: {
            if (!model) {
                if (_companyType == BeautyCompanyTypeNetease) {
                    [[NERtcBeauty shareInstance] removeBeautyMakeup];
                }
            } else {
                if (_companyType == BeautyCompanyTypeNetease) {
                    [[NERtcBeauty shareInstance] addBeautyMakeupWithPath:model.resourcePath andName:@"template.json"];
                }
            }
        }
            
        default:
            break;
    }
}

- (void)didChangeSliderValueWithType:(NEBeautySliderType)type value:(float)value {
    switch (type) {
        case NEBeautySliderTypeWhiteTeeth: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].teeth = value;
            } else {
                [FURenderKit shareRenderKit].beauty.toothWhiten = value;
            }
            
            break;
        }
        case NEBeautySliderTypeLightEye: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].brightEye = value;
            } else {
                [FURenderKit shareRenderKit].beauty.eyeBright = value;
            }
            
            break;
        }
        case NEBeautySliderTypeWhiten: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].whiteSkin = value;
            } else {
                [FURenderKit shareRenderKit].beauty.colorLevel = value;
            }
            
            break;
        }
        case NEBeautySliderTypeSmooth: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].smoothSkin = value;
            } else {
                [FURenderKit shareRenderKit].beauty.blurLevel = value * 6;
            }
            
            break;
        }
        case NEBeautySliderTypeSmallNose: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].smallNose = value;
            } else {
                [FURenderKit shareRenderKit].beauty.intensityNose = value;
            }
            
            break;
        }
        case NEBeautySliderTypeEyeDis: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].eyesDistance = value;
            } else {
                [FURenderKit shareRenderKit].beauty.intensityEyeSpace = value;
            }
            
            break;
        }
        case NEBeautySliderTypeEyeAngle: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].eyesAngle = value;
            } else {
                [FURenderKit shareRenderKit].beauty.intensityEyeRotate = value;
            }
            
            break;
        }
        case NEBeautySliderTypeMouth: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].mouth = value;
            } else {
                [FURenderKit shareRenderKit].beauty.intensityMouth = value;
            }
            
            break;
        }
        case NEBeautySliderTypeBigEye: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].bigEye = value;
            } else {
                [FURenderKit shareRenderKit].beauty.eyeEnlarging = value;
            }
            
            break;
        }
        case NEBeautySliderTypeSmallFace: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].smallFace = value;
            } else {
                [FURenderKit shareRenderKit].beauty.cheekSmall = value;
            }
            
            break;
        }
        case NEBeautySliderTypeJaw: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].jaw = value;
            } else {
                [FURenderKit shareRenderKit].beauty.intensityChin = value;
            }
            
            break;
        }
        case NEBeautySliderTypeThinFace: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].thinFace = value;
            } else {
                [FURenderKit shareRenderKit].beauty.cheekThinning = value;
            }
            
            break;
        }
        case NEBeautySliderTypeFaceRuddy: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].faceRuddyStrength = value;
            } else {
                [FURenderKit shareRenderKit].beauty.redLevel = value;
            }
            
            break;
        }
        case NEBeautySliderTypeLongNose: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].longNoseStrength = value;
            } else {
                [FURenderKit shareRenderKit].beauty.intensityLongNose = value;
            }
            
            break;
        }
        case NEBeautySliderTypeRenZhong: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].renZhongStrength = value;
            } else {
                [FURenderKit shareRenderKit].beauty.intensityPhiltrum = value;
            }
            
            break;
        }
        case NEBeautySliderTypeMouthAngle: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].mouthAngle = value;
            } else {
                [FURenderKit shareRenderKit].beauty.intensitySmile = value;
            }
            
            break;
        }
        case NEBeautySliderTypeRoundEye: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].roundEyeStrength = value;
            } else {
                [FURenderKit shareRenderKit].beauty.intensityEyeCircle = value;
            }
            
            break;
        }
        case NEBeautySliderTypeOpenEyeAngle: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].openEyeAngleStrength = value;
            } else {
                [FURenderKit shareRenderKit].beauty.intensityCanthus = value;
            }
            
            break;
        }
        case NEBeautySliderTypeVFace: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].vFaceStrength = value;
            } else {
                [FURenderKit shareRenderKit].beauty.cheekV = value;
            }
            
            break;
        }
        case NEBeautySliderTypeThinUnderjaw: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].thinUnderjawStrength = value;
            } else {
                [FURenderKit shareRenderKit].beauty.intensityLowerJaw = value;
            }
            
            break;
        }
        case NEBeautySliderTypeNarrowFace: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].narrowFaceStrength = value;
            } else {
                [FURenderKit shareRenderKit].beauty.cheekNarrow = value;
            }
            
            break;
        }
        case NEBeautySliderTypeCheekBone: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].cheekBoneStrength = value;
            } else {
                [FURenderKit shareRenderKit].beauty.intensityCheekbones = value;
            }
            
            break;
        }
        case NEBeautySliderTypeFaceSharpen: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].faceSharpenStrength = value;
            } else {
                [FURenderKit shareRenderKit].beauty.sharpen = value;
            }
            
            break;
        }
        case NEBeautySliderTypeMouthWider: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].mouthWiderStrength = value;
            } else {
                
            }
            
            break;
        }
        case NEBeautySliderTypeForeheadWrinkles: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].foreheadWrinklesStrength = value;
            } else {
                
            }
            
            break;
        }
        case NEBeautySliderTypeDarkCircles: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].darkCirclesStrength = value;
            } else {
                
            }
            
            break;
        }
        case NEBeautySliderTypeSmileLines: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].smileLinesStrength = value;
            } else {
                
            }
            
            break;
        }
        case NEBeautySliderTypeShortFace: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].shortFaceStrength = value;
            } else {
                
            }
            
            break;
        }
        case NEBeautySliderTypeFilterStrength: {
            if (_companyType == BeautyCompanyTypeNetease) {
                [NERtcBeauty shareInstance].filterStrength = value;
            } else {
                [FURenderKit shareRenderKit].beauty.filterLevel = value;
            }
            
            break;
        }
            
        default:
            break;
    }
}

#pragma mark - NEBeautyConfigViewDataSource

- (NSArray<NETitleDisplayModel *> *)titleModelArrayForConfigViewWithType:(NEBeautyConfigViewType)type {
    switch (type) {
        case NEBeautyConfigViewTypeBeauty: {
            return self.beautyTitleModelArray;
        }
        case NEBeautyConfigViewTypeFilter: {
            return self.filterTitleModelArray;
        }
        case NEBeautyConfigViewTypeSticker: {
            return self.stickerTitleModelArray;
        }
        case NEBeautyConfigViewTypeMakeup: {
            return self.makeupTitleModelArray;
        }
            
        default: {
            return nil;
        }
    }
}

- (NSArray<NECollectionViewDisplayModel *> *)itemModelArrayForConfigViewWithType:(NEBeautyConfigViewType)type effectType:(NEBeautyEffectType)effectType {
    if (_companyType == BeautyCompanyTypeNetease) {
        switch (type) {
            case NEBeautyConfigViewTypeFilter: {
                return self.filterItemModelArray;
            }
            case NEBeautyConfigViewTypeSticker: {
                switch (effectType) {
                    case NEBeautyEffectTypeSticker2D: {
                        return self.sticker2DModelArray;
                    }
                    case NEBeautyEffectTypeSticker3D: {
                        return self.sticker3DModelArray;
                    }
                    case NEBeautyEffectTypeStickerParticle: {
                        return self.stickerParticleModelArray;
                    }
                    case NEBeautyEffectTypeStickerFaceChange: {
                        return self.stickerFaceChangeModelArray;
                    }
                        
                    default:
                        return nil;
                }
            }
            case NEBeautyConfigViewTypeMakeup: {
                return self.makeupItemModelArray;
            }
                
            default: {
                return nil;
            }
        }
    } else if (_companyType == BeautyCompanyTypeFaceUnity) {
        switch (type) {
            case NEBeautyConfigViewTypeFilter: {
                return self.fuFilterItemModelArray;
            }
            case NEBeautyConfigViewTypeSticker: {
                switch (effectType) {
                    case NEBeautyEffectTypeSticker2D: {
                        return self.fuSticker2DModelArray;
                    }
                    case NEBeautyEffectTypeSticker3D: {
                        return self.fuSticker3DModelArray;
                    }
                    case NEBeautyEffectTypeStickerParticle: {
                        return nil;
                    }
                    case NEBeautyEffectTypeStickerFaceChange: {
                        return nil;
                    }
                        
                    default:
                        return nil;
                }
            }
            case NEBeautyConfigViewTypeMakeup: {
                return nil;
            }
                
            default: {
                return nil;
            }
        }
    }
    
    return nil;
}

- (NSArray<NEBeautySliderDisplayModel *> *)sliderModelArrayForTitleType:(NEBeautyEffectType)type {
    if (_companyType == BeautyCompanyTypeNetease) {
        switch (type) {
            case NEBeautyEffectTypeBeautyBase: {
                return self.baseSliderModelArray;
            }
            case NEBeautyEffectTypeBeautyShape: {
                return self.shapeSliderModelArray;
            }
            case NEBeautyEffectTypeBeautyAdv: {
                return self.advancedSliderModelArray;
            }
            case NEBeautyEffectTypeBeautyAdv2: {
                return self.advancedSliderModelArray2;
            }
            case NEBeautyEffectTypeBeautyAdv3: {
                return self.advancedSliderModelArray3;
            }
//            case NEBeautyEffectTypeBeautyAdv4: {
//                return self.advancedSliderModelArray4;
//            }
                
            default: {
                return nil;
            }
        }
    } else if (_companyType == BeautyCompanyTypeFaceUnity) {
        switch (type) {
            case NEBeautyEffectTypeBeautyBase: {
                return self.fuBaseSliderModelArray;
            }
            case NEBeautyEffectTypeBeautyShape: {
                return self.fuShapeSliderModelArray;
            }
            case NEBeautyEffectTypeBeautyAdv: {
                return self.fuAdvancedSliderModelArray;
            }
            case NEBeautyEffectTypeBeautyAdv2: {
                return self.fuAdvancedSliderModelArray2;
            }
            case NEBeautyEffectTypeBeautyAdv3: {
                return self.fuAdvancedSliderModelArray3;
            }
//            case NEBeautyEffectTypeBeautyAdv4: {
//                return self.fuAdvancedSliderModelArray4;
//            }
                
            default: {
                return nil;
            }
        }
    }
    
    return nil;
}

- (NEBeautySliderDisplayModel *)sliderModelForFilterStrength {
    if (_companyType == BeautyCompanyTypeNetease) {
        return self.filterStrengthModel;
    } else if (_companyType == BeautyCompanyTypeFaceUnity) {
        return self.fuFilterStrengthModel;
    }
    
    return nil;
}

#pragma mark - Getter

- (NSMutableDictionary<NSNumber *,NEBeautyConfigView *> *)menuMap {
    if (!_menuMap) {
        _menuMap = [NSMutableDictionary dictionary];
    }
    
    return _menuMap;
}

- (NSArray<NETitleDisplayModel *> *)beautyTitleModelArray {
    if (!_beautyTitleModelArray) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"基础美颜", @"美形", @"高级", @"高级2", @"高级3"];
        NSArray *typeArray = @[@(NEBeautyEffectTypeBeautyBase),
                               @(NEBeautyEffectTypeBeautyShape),
                               @(NEBeautyEffectTypeBeautyAdv),
                               @(NEBeautyEffectTypeBeautyAdv2),
                               @(NEBeautyEffectTypeBeautyAdv3)];
//                               @(NEBeautyEffectTypeBeautyAdv4)];
        for (int i = 0; i < titleArray.count; i++) {
            NETitleDisplayModel *model = [[NETitleDisplayModel alloc] init];
            model.type = [typeArray[i] integerValue];
            model.title = titleArray[i];
            [modelArray addObject:model];
        }
        
        _beautyTitleModelArray = modelArray;
    }
    
    return _beautyTitleModelArray;
}

- (NSArray<NETitleDisplayModel *> *)filterTitleModelArray {
    if (!_filterTitleModelArray) {
        NETitleDisplayModel *model = [[NETitleDisplayModel alloc] init];
        model.type = NEBeautyEffectTypeFilter;
        model.title = @"滤镜";
        
        _filterTitleModelArray = @[model];
    }
    
    return _filterTitleModelArray;
}

- (NSArray<NETitleDisplayModel *> *)stickerTitleModelArray {
    if (!_stickerTitleModelArray) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"2D", @"3D", @"粒子", @"换脸"];
        NSArray *typeArray = @[@(NEBeautyEffectTypeSticker2D),
                               @(NEBeautyEffectTypeSticker3D),
                               @(NEBeautyEffectTypeStickerParticle),
                               @(NEBeautyEffectTypeStickerFaceChange)];
        for (int i = 0; i < titleArray.count; i++) {
            NETitleDisplayModel *model = [[NETitleDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            [modelArray addObject:model];
        }
        
        _stickerTitleModelArray = modelArray;
    }
    
    return _stickerTitleModelArray;
}

- (NSArray<NETitleDisplayModel *> *)makeupTitleModelArray {
    if (!_makeupTitleModelArray) {
        NETitleDisplayModel *model = [[NETitleDisplayModel alloc] init];
        model.type = NEBeautyEffectTypeMakeup;
        model.title = @"美妆";
        
        _makeupTitleModelArray = @[model];
    }
    
    return _makeupTitleModelArray;
}

- (NSArray<NEBeautySliderDisplayModel *> *)baseSliderModelArray {
    if (!_baseSliderModelArray) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"磨皮", @"美白", @"瘦脸", @"嘴巴", @"红润", @"锐化"];
        NSArray *typeArray = @[@(NEBeautySliderTypeSmooth),
                               @(NEBeautySliderTypeWhiten),
                               @(NEBeautySliderTypeThinFace),
                               @(NEBeautySliderTypeMouth),
                               @(NEBeautySliderTypeFaceRuddy),
                               @(NEBeautySliderTypeFaceSharpen)];
        NSArray *imageNameArray = @[@"mopi", @"meibai", @"thinner_face", @"mouth", @"btn_beauty", @"btn_beauty"];
        NSArray *initialValueArray = @[@(0), @(0), @(0), @(0.5), @(0), @(0)];
        for (int i = 0; i < titleArray.count; i++) {
            NEBeautySliderDisplayModel *model = [[NEBeautySliderDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            model.imageName = imageNameArray[i];
            model.value = [initialValueArray[i] floatValue];
            
            [modelArray addObject:model];
        }
        
        _baseSliderModelArray = modelArray;
    }
    
    return _baseSliderModelArray;
}

- (NSArray<NEBeautySliderDisplayModel *> *)shapeSliderModelArray {
    if (!_shapeSliderModelArray) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"小脸", @"大眼", @"下巴"];
        NSArray *typeArray = @[@(NEBeautySliderTypeSmallFace),
                               @(NEBeautySliderTypeBigEye),
                               @(NEBeautySliderTypeJaw)];
        NSArray *imageNameArray = @[@"thin_face", @"enlarge_eyes", @"small_face"];
        NSArray *initialValueArray = @[@(0), @(0), @(0)];
        for (int i = 0; i < titleArray.count; i++) {
            NEBeautySliderDisplayModel *model = [[NEBeautySliderDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            model.imageName = imageNameArray[i];
            model.value = [initialValueArray[i] floatValue];
            
            [modelArray addObject:model];
        }
        
        _shapeSliderModelArray = modelArray;
    }
    
    return _shapeSliderModelArray;
}

- (NSArray<NEBeautySliderDisplayModel *> *)advancedSliderModelArray {
    if (!_advancedSliderModelArray) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"亮眼", @"美牙", @"小鼻", @"眼距", @"眼角"];
        NSArray *typeArray = @[@(NEBeautySliderTypeLightEye),
                               @(NEBeautySliderTypeWhiteTeeth),
                               @(NEBeautySliderTypeSmallNose),
                               @(NEBeautySliderTypeEyeDis),
                               @(NEBeautySliderTypeEyeAngle)];
        NSArray *imageNameArray = @[@"eyebright", @"whitenteeth", @"thinnose", @"eyedis", @"eyeangle"];
        NSArray *initialValueArray = @[@(0), @(0), @(0), @(0.5), @(0.5)];
        for (int i = 0; i < titleArray.count; i++) {
            NEBeautySliderDisplayModel *model = [[NEBeautySliderDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            model.imageName = imageNameArray[i];
            model.value = [initialValueArray[i] floatValue];
            
            [modelArray addObject:model];
        }
        
        _advancedSliderModelArray = modelArray;
    }
    
    return _advancedSliderModelArray;
}

- (NSArray<NEBeautySliderDisplayModel *> *)advancedSliderModelArray2 {
    if (!_advancedSliderModelArray2) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"长鼻", @"人中", @"嘴角", @"圆眼", @"开眼角"];
        NSArray *typeArray = @[@(NEBeautySliderTypeLongNose),
                               @(NEBeautySliderTypeRenZhong),
                               @(NEBeautySliderTypeMouthAngle),
                               @(NEBeautySliderTypeRoundEye),
                               @(NEBeautySliderTypeOpenEyeAngle)];
        NSArray *initialValueArray = @[@(0.5), @(0.5), @(0.5), @(0), @(0)];
        for (int i = 0; i < titleArray.count; i++) {
            NEBeautySliderDisplayModel *model = [[NEBeautySliderDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            model.imageName = @"btn_beauty";
            model.value = [initialValueArray[i] floatValue];
            
            [modelArray addObject:model];
        }
        
        _advancedSliderModelArray2 = modelArray;
    }
    
    return _advancedSliderModelArray2;
}

- (NSArray<NEBeautySliderDisplayModel *> *)advancedSliderModelArray3 {
    if (!_advancedSliderModelArray3) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"V脸", @"瘦下巴", @"窄脸", @"瘦颧骨"];
        NSArray *typeArray = @[@(NEBeautySliderTypeVFace),
                               @(NEBeautySliderTypeThinUnderjaw),
                               @(NEBeautySliderTypeNarrowFace),
                               @(NEBeautySliderTypeCheekBone)];
        NSArray *initialValueArray = @[@(0), @(0), @(0), @(0)];
        for (int i = 0; i < titleArray.count; i++) {
            NEBeautySliderDisplayModel *model = [[NEBeautySliderDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            model.imageName = @"btn_beauty";
            model.value = [initialValueArray[i] floatValue];
            
            [modelArray addObject:model];
        }
        
        _advancedSliderModelArray3 = modelArray;
    }
    
    return _advancedSliderModelArray3;
}

- (NSArray<NEBeautySliderDisplayModel *> *)advancedSliderModelArray4 {
    if (!_advancedSliderModelArray4) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"嘴巴宽度", @"祛抬头纹", @"祛黑眼圈", @"祛法令纹", @"短脸"];
        NSArray *typeArray = @[@(NEBeautySliderTypeMouthWider),
                               @(NEBeautySliderTypeForeheadWrinkles),
                               @(NEBeautySliderTypeDarkCircles),
                               @(NEBeautySliderTypeSmileLines),
                               @(NEBeautySliderTypeShortFace)];
        NSArray *initialValueArray = @[@(0.5), @(0), @(0), @(0), @(0)];
        for (int i = 0; i < titleArray.count; i++) {
            NEBeautySliderDisplayModel *model = [[NEBeautySliderDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            model.imageName = @"btn_beauty";
            model.value = [initialValueArray[i] floatValue];
            
            [modelArray addObject:model];
        }
        
        _advancedSliderModelArray4 = modelArray;
    }
    
    return _advancedSliderModelArray4;
}

- (NEBeautySliderDisplayModel *)filterStrengthModel {
    if (!_filterStrengthModel) {
        _filterStrengthModel = [[NEBeautySliderDisplayModel alloc] init];
        _filterStrengthModel.type = NEBeautySliderTypeFilterStrength;
        _filterStrengthModel.title = @"强度";
        _filterStrengthModel.imageName = nil;
        _filterStrengthModel.value = 0;
    }
    
    return _filterStrengthModel;
}

- (NSMutableArray<NECollectionViewDisplayModel *> *)sticker2DModelArray {
    if (!_sticker2DModelArray) {
        _sticker2DModelArray = [NSMutableArray array];
    }
    
    return _sticker2DModelArray;
}

- (NSMutableArray<NECollectionViewDisplayModel *> *)sticker3DModelArray {
    if (!_sticker3DModelArray) {
        _sticker3DModelArray = [NSMutableArray array];
    }
    
    return _sticker3DModelArray;
}

- (NSMutableArray<NECollectionViewDisplayModel *> *)stickerParticleModelArray {
    if (!_stickerParticleModelArray) {
        _stickerParticleModelArray = [NSMutableArray array];
    }
    
    return _stickerParticleModelArray;
}

- (NSMutableArray<NECollectionViewDisplayModel *> *)stickerFaceChangeModelArray {
    if (!_stickerFaceChangeModelArray) {
        _stickerFaceChangeModelArray = [NSMutableArray array];
    }
    
    return _stickerFaceChangeModelArray;
}

- (NSArray<NEBeautySliderDisplayModel *> *)fuBaseSliderModelArray {
    if (!_fuBaseSliderModelArray) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"磨皮", @"美白", @"瘦脸", @"嘴巴", @"红润", @"锐化"];
        NSArray *typeArray = @[@(NEBeautySliderTypeSmooth),
                               @(NEBeautySliderTypeWhiten),
                               @(NEBeautySliderTypeThinFace),
                               @(NEBeautySliderTypeMouth),
                               @(NEBeautySliderTypeFaceRuddy),
                               @(NEBeautySliderTypeFaceSharpen)];
        NSArray *imageNameArray = @[@"mopi", @"meibai", @"thinner_face", @"mouth", @"btn_beauty", @"btn_beauty"];
        NSArray *initialValueArray = @[@(0), @(0), @(0), @(0.5), @(0), @(0)];
        for (int i = 0; i < titleArray.count; i++) {
            NEBeautySliderDisplayModel *model = [[NEBeautySliderDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            model.imageName = imageNameArray[i];
            model.value = [initialValueArray[i] floatValue];
            
            [modelArray addObject:model];
        }
        
        _fuBaseSliderModelArray = modelArray;
    }
    
    return _fuBaseSliderModelArray;
}

- (NSArray<NEBeautySliderDisplayModel *> *)fuShapeSliderModelArray {
    if (!_fuShapeSliderModelArray) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"小脸", @"大眼", @"下巴"];
        NSArray *typeArray = @[@(NEBeautySliderTypeSmallFace),
                               @(NEBeautySliderTypeBigEye),
                               @(NEBeautySliderTypeJaw)];
        NSArray *imageNameArray = @[@"thin_face", @"enlarge_eyes", @"small_face"];
        NSArray *initialValueArray = @[@(0), @(0), @(0)];
        for (int i = 0; i < titleArray.count; i++) {
            NEBeautySliderDisplayModel *model = [[NEBeautySliderDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            model.imageName = imageNameArray[i];
            model.value = [initialValueArray[i] floatValue];
            
            [modelArray addObject:model];
        }
        
        _fuShapeSliderModelArray = modelArray;
    }
    
    return _fuShapeSliderModelArray;
}

- (NSArray<NEBeautySliderDisplayModel *> *)fuAdvancedSliderModelArray {
    if (!_fuAdvancedSliderModelArray) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"亮眼", @"美牙", @"小鼻", @"眼距", @"眼角"];
        NSArray *typeArray = @[@(NEBeautySliderTypeLightEye),
                               @(NEBeautySliderTypeWhiteTeeth),
                               @(NEBeautySliderTypeSmallNose),
                               @(NEBeautySliderTypeEyeDis),
                               @(NEBeautySliderTypeEyeAngle)];
        NSArray *imageNameArray = @[@"eyebright", @"whitenteeth", @"thinnose", @"eyedis", @"eyeangle"];
        NSArray *initialValueArray = @[@(0), @(0), @(0), @(0.5), @(0.5)];
        for (int i = 0; i < titleArray.count; i++) {
            NEBeautySliderDisplayModel *model = [[NEBeautySliderDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            model.imageName = imageNameArray[i];
            model.value = [initialValueArray[i] floatValue];
            
            [modelArray addObject:model];
        }
        
        _fuAdvancedSliderModelArray = modelArray;
    }
    
    return _fuAdvancedSliderModelArray;
}

- (NSArray<NEBeautySliderDisplayModel *> *)fuAdvancedSliderModelArray2 {
    if (!_fuAdvancedSliderModelArray2) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"长鼻", @"人中", @"嘴角", @"圆眼", @"开眼角"];
        NSArray *typeArray = @[@(NEBeautySliderTypeLongNose),
                               @(NEBeautySliderTypeRenZhong),
                               @(NEBeautySliderTypeMouthAngle),
                               @(NEBeautySliderTypeRoundEye),
                               @(NEBeautySliderTypeOpenEyeAngle)];
        NSArray *initialValueArray = @[@(0.5), @(0.5), @(0.5), @(0), @(0)];
        for (int i = 0; i < titleArray.count; i++) {
            NEBeautySliderDisplayModel *model = [[NEBeautySliderDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            model.imageName = @"btn_beauty";
            model.value = [initialValueArray[i] floatValue];
            
            [modelArray addObject:model];
        }
        
        _fuAdvancedSliderModelArray2 = modelArray;
    }
    
    return _fuAdvancedSliderModelArray2;
}

- (NSArray<NEBeautySliderDisplayModel *> *)fuAdvancedSliderModelArray3 {
    if (!_fuAdvancedSliderModelArray3) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"V脸", @"瘦下巴", @"窄脸", @"瘦颧骨"];
        NSArray *typeArray = @[@(NEBeautySliderTypeVFace),
                               @(NEBeautySliderTypeThinUnderjaw),
                               @(NEBeautySliderTypeNarrowFace),
                               @(NEBeautySliderTypeCheekBone)];
        NSArray *initialValueArray = @[@(0), @(0), @(0), @(0)];
        for (int i = 0; i < titleArray.count; i++) {
            NEBeautySliderDisplayModel *model = [[NEBeautySliderDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            model.imageName = @"btn_beauty";
            model.value = [initialValueArray[i] floatValue];
            
            [modelArray addObject:model];
        }
        
        _fuAdvancedSliderModelArray3 = modelArray;
    }
    
    return _fuAdvancedSliderModelArray3;
}

- (NSArray<NEBeautySliderDisplayModel *> *)fuAdvancedSliderModelArray4 {
    if (!_fuAdvancedSliderModelArray4) {
        NSMutableArray *modelArray = [NSMutableArray array];
        NSArray *titleArray = @[@"嘴巴宽度", @"祛抬头纹", @"祛黑眼圈", @"祛法令纹", @"短脸"];
        NSArray *typeArray = @[@(NEBeautySliderTypeMouthWider),
                               @(NEBeautySliderTypeForeheadWrinkles),
                               @(NEBeautySliderTypeDarkCircles),
                               @(NEBeautySliderTypeSmileLines),
                               @(NEBeautySliderTypeShortFace)];
        NSArray *initialValueArray = @[@(0.5), @(0), @(0), @(0), @(0)];
        for (int i = 0; i < titleArray.count; i++) {
            NEBeautySliderDisplayModel *model = [[NEBeautySliderDisplayModel alloc] init];
            model.title = titleArray[i];
            model.type = [typeArray[i] integerValue];
            model.imageName = @"btn_beauty";
            model.value = [initialValueArray[i] floatValue];
            
            [modelArray addObject:model];
        }
        
        _fuAdvancedSliderModelArray4 = modelArray;
    }
    
    return _fuAdvancedSliderModelArray4;
}

- (NEBeautySliderDisplayModel *)fuFilterStrengthModel {
    if (!_fuFilterStrengthModel) {
        _fuFilterStrengthModel = [[NEBeautySliderDisplayModel alloc] init];
        _fuFilterStrengthModel.type = NEBeautySliderTypeFilterStrength;
        _fuFilterStrengthModel.title = @"强度";
        _fuFilterStrengthModel.imageName = nil;
        _fuFilterStrengthModel.value = 0;
    }
    
    return _fuFilterStrengthModel;
}

@end
