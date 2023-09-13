# 生成云信 RTC 鉴权的 token

* 基础token：https://doc.yunxin.163.com/nertc/docs/TQ0MTI2ODQ?platform=android


## 代码目录

* 本目录下是一个完整的 Python3 项目。`./token_server.py`  文件包含了基础 token 的完整代码

## 使用示例

```python
from . import token_server

# 建议项目初始化时候创建对象， 通过单例全局维护一个 TokenServer 对象
# appKey、appSecret 请替换成自己的，具体在云信管理后台查看。
# 7200 代表默认有效的时间，单位秒。不能超过 86400，即 24 小时
my_token_server = token_server.TokenServer(appKey, appSecret, 7200)

# 在需要的时候，提供 channelName（房间名）、uid（用户标识）、ttlSec（有效时间，单位秒） 参数，生成 token
token = my_token_server.get_token(channelName, uid, ttlSec);
```

## 代码引入说明

1. 复制 `./token_server.py` 文件到你的项目中
2. 初始化时候调用 `TokenServer(appKey, appSecret, ttlSec)`，生成 token 时候调用 `get_token` 方法

