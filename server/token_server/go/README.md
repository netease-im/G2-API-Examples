# 生成云信 RTC 鉴权的 token

* 基础 token：https://doc.yunxin.163.com/nertc/docs/TQ0MTI2ODQ?platform=android
* 高级 token：https://doc.yunxin.163.com/nertc/docs/zA3MjAwNzM?platform=android

## 代码目录

* `./token` 是一个完整的 go 项目，里面包含的 `token.go` 文件包含了基础 token、高级 token 的完整代码

## 使用示例

```go

// 建议项目初始化时候调用一次 NewTokenServer， 通过单例全局维护一个 TokenServer 对象
// appKey、appSecret 请替换成自己的，具体在云信管理后台查看。
// 7200 代表默认有效的时间，单位秒。不能超过 86400，即 24 小时
tokenServer, err := NewTokenServer("${appKey}", "${appSecret}", 7200)

// 在需要的时候，提供 channelName（房间名）、uid（用户标识）、ttlSec（有效时间，单位秒） 参数，生成 token
token, err := tokenServer.GetToken(channelName, uid, ttlSec)
if err != nil {
    return err
}
// 具体权限说明见函数注释
privilege := uint8(1)
ttlSec := 1800
// permSecret 见云信管理后台，具体见文档说明：https://doc.yunxin.163.com/nertc/docs/zA3MjAwNzM?platform=android
permissionToken := tokenServer.GetPermissionKey(channelName, permSecret, uid, privilege, ttlSec)
```

## 代码引入说明

1. 使用 go get 本项目github 路径，或者 复制 `./token/token.go` 文件到你的项目中
2. 初始化时候调用 `NewTokenServer`，生成 token 时候调用 `GetToken` 方法
3. 如果需要生成高级 token，调用 `GetPermissionKey` 方法

