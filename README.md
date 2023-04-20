# G2-API-Examples
此仓库包含 NERTC SDK 的 sample code 示例项目

## 开发环境
>请确认您的开发环境满足以下要求：
- 安全环境：https 环境或者本地连接 localhost/127.0.0.1 环境
- 浏览器：Chrome 72 及以上版本、Safari 12 及以上版本。更多浏览器兼容性相关请参考 [NERTC Web SDK 支持哪些浏览器](https://doc.yunxin.163.com/nertc/docs/TU5NjUzNjU?platform=web#Web%E7%AB%AF%E6%94%AF%E6%8C%81%E5%93%AA%E4%BA%9B%E6%B5%8F%E8%A7%88%E5%99%A8%E7%B1%BB%E5%9E%8B%E5%92%8C%E7%89%88%E6%9C%AC%EF%BC%9F)

## 前提条件
>请确认您已完成以下操作：
- [创建应用并开获取 App Key](https://doc.yunxin.163.com/jcyOTA0ODM/docs/zY4MjE3NDA?platform=web)
- [开通音视频 2.0 服务](https://doc.yunxin.163.com/jcyOTA0ODM/docs/DA4NjQzNTU?platform=web)

## 操作步骤
> 体验 Vue 版本的 demo：
1. 请在 sampleCode-Vue 文件中选择要体验的 demo。
2. 在 `config/index.js` 文件中配置 App Key。
```
export default {
    appkey: '', // 请输入自己的appkey
    appSecret: '' // 请输入自己的appSecret
}
```
3. 运行项目（请使用 node 开发环境 version 16+）
```
npm install

npm run dev
```
> 体验原生 js 版本 demo：
1. 请在 sampleCode-js 文件中选择要体验的 demo。
2. 请在 `js/index.js` 文件中配置 App Key。
```
let appkey= ''// 请输入自己的appkey
let appSecret= '' // 请输入自己的appSecret
```
3. 运行项目，可以用 Chrome 或者 Safari 浏览器直接打开 `index.html` 页面直接体验。