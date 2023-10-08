# 生成云信 RTC 鉴权的 token

* 基础token：https://doc.yunxin.163.com/nertc/docs/TQ0MTI2ODQ?platform=android
* 高级 token：https://doc.yunxin.163.com/nertc/docs/zA3MjAwNzM?platform=android


## 代码目录

* 本目录下是一个完整的 NodeJS 项目。`./src/TokenBuilder.js`  文件包含了基础token、高级 token 的完整代码

## 使用示例

```javascript
const { GetToken, GetPermissionKey } = require('TokenBuilder.js');

// 提供 channelName（房间名）、uid（用户标识）、ttlSec（有效时间，单位秒） 参数，生成 token
// appKey, appSecret 请替换成自己的，具体在云信管理后台查看。
// 7200 代表默认有效的时间，单位秒。不能超过 86400，即 24 小时
var token = GetToken(appKey, appSecret, channelName, uid, 7200);

var ttlSec = 1000;
var privilege  = 1;
const permKey = GetPermissionKey(appKey, permissionSecret, "room1", 10000, privilege, ttlSec);
```

## 代码引入说明

1. 复制 `./src/TokenBuilder.js` 文件到你的项目中
2. 生成 token 的代码中引入文件， 然后调用 `GetToken` 方法
3. 如果需要生成高级 token，调用 `GetPermissionKey` 方法

