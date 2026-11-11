# 猫娘助手

TIM / QQ 的 Xposed 增强模块，基于 LSPosed 框架。

面向个人使用的本地模块，不含埋点、不含崩溃上报、不含统计 SDK。

网络请求的去向见下方「对外网络请求」一节。

> **本仓库仅供学习与技术交流使用，请勿用于任何商业用途或违法用途。**

---

## 功能

### 聊天增强

| 功能 | 说明 |
|---|---|
| 防撤回 | 拦截撤回指令并提示 |
| 长按复读 | 长按消息出现「复读」按钮 |
| 自定义点数 | 发骰子 / 猜拳时自选结果 |
| 表情看图 | 以图片的方式打开表情 |
| 闪照破解 | 以图片的方式打开闪照 |
| 改本地文本 | 长按消息改显示文本（仅本地生效） |
| 去除回复自动艾特 | 回复消息时不再自动 @ 对方 |
| 显示艾特对象 | 群聊里把 @ 的对象显示出来 |
| 文字表情复合消息 | 文字和表情可以一起发 |


### 消息时间与发送者版本

| 功能 | 说明 |
|---|---|
| 显示消息时间 | 每条消息下方显示收到时间，颜色 / 深浅 / 字号 / 四种格式可调 |
| 显示发送者版本 | 在时间后面显示对方用的 QQ / TIM 版本号 |
| 版本排版 | 同行 / 版本在上 / 时间在上 三种排法 |
| 版本字号 | 版本号单独调字号，不受时间字号影响 |
| 渠道推测 | 按 subId 与版本基准的差值推测下载渠道（应用宝 / vivo / 荣耀 / 小米 / OPPO / 豌豆荚 / 应用汇），会标注「推测」 |
| 长按手动标注 | 遇到认不出的版本，长按消息填写版本和渠道，同一 SubId 全部生效 |

**版本号是怎么来的**：QQ 消息协议里带的是 `subAppId`（一串数字，随版本变化），
需要靠 `app/assets/sender_version.json` 反查成版本号。这个表随 QQ 更新会滞后，
所以提供了「长按手动标注」作为兜底。

数据来源见 [NOTICE.md](NOTICE.md)。

### 猫娘化引擎

把发送前的文字自动转换成猫娘语气。

| 开关 | 说明 |
|---|---|
| 总开关 | 猫娘化功能的全局开关 |
| 基础猫娘化 | 核心转换 |
| 撒娇模式 | 语气增强 |
| 结尾加喵 | 句尾追加「喵」 |
| 加颜文字 | 自动追加颜文字 |
| 随机卖萌 | 随机插入萌系表达 |
| 波浪号 | 标点替换为「~」 |
| 称呼替换 | 自定义称呼映射 |
| 主人称呼 | 对特定对象的称呼 |

### 其他

- 多选转发
- 拍一拍增强
- 本地消息导出
- 内置 Web 控制台页面

---

## 安装

1. 安装 APK
2. 在 **LSPosed** 中启用本模块
3. 作用域勾选 **TIM**（`com.tencent.tim`）
4. 强制停止 TIM 后重新打开

> 本模块是 Xposed 模块，单纯安装不会出现独立功能界面。
> 需要 Root + LSPosed 环境。

---

## 构建

需要 JDK 11 与 Android SDK 构建工具（aapt2 / d8 / apksigner）。

```bash
sh 构建APK.sh            # 完整构建
sh 构建APK.sh --clean    # 清理后构建
sh 构建APK.sh --java     # 只编译 Java
```

构建配置在 `toolchain.env`（路径、版本、签名信息）。
`toolchain.env` 含签名密码，**不要提交**。

### 目录结构

```
app/
├── AndroidManifest.xml
├── java/                源码
│   ├── com/cathelperawa/owo/app/    本项目代码
│   └── top/artmoe/inao/entries/     第三方 protobuf 生成类（见 NOTICE.md）
├── res/                 布局、图标、样式
├── assets/
│   ├── xposed_init      Xposed 入口声明
│   └── console/         控制台资源
│       └── assets/      界面插画（被 Images.java 引用）
└── proto/               上游 proto 定义（来源证明）
```

> 说明：`assets/console/` 下的 `index.html` / `style.css` / `app.js` 是控制台早期的
> Web 原型设计稿，**当前版本的原生 UI 并未加载它们**，保留仅供设计参考。

## 权限

| 权限 | 用途 |
|---|---|
| `INTERNET` | 网络请求（各功能都要） |
| `SYSTEM_ALERT_WINDOW` | 悬浮球、悬浮面板 |
| `POST_NOTIFICATIONS` | 下载进度、前台服务通知 |
| `FOREGROUND_SERVICE` | 网盘下载、音乐播放常驻 |
| `FOREGROUND_SERVICE_DATA_SYNC` | 网盘分片下载 |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 后台播放 |
| `WAKE_LOCK` | 下载/播放时保持唤醒 |
| `WRITE_EXTERNAL_STORAGE`（仅 Android 9 及以下） | 保存下载文件 |
| `REQUEST_INSTALL_PACKAGES` | 安装更新包 |

模块不含开机自启、不读通讯录、不读短信、不申请定位、不申请无障碍权限。

---

## 对外网络请求

代码里硬编码的外部域名共 **71 个**，按用途分：

### 腾讯自家（QQ / TIM 相关）

```
*.qlogo.cn  *.qpic.cn  *.gtimg.cn  multimedia.nt.qq.com.cn
mqq.tenpay.com  downv6.qq.com  channel.qpic.cn  y.qq.com
```

用途：头像、表情、聊天图片、音乐卡封面等展示资源。

### 内容平台解析

```
api.bilibili.com  passport.bilibili.com  b23.tv
aweme.snssdk.com  m.douyin.com
www.kuaishou.com  www.xiaohongshu.com  www.pixiv.net
```

用途：把分享链接解析成可播放的直链。

### 音乐

```
music.163.com  p1.music.126.net  antiserver.kuwo.cn
music-api.gdstudio.xyz  y.qq.com
```

用途：搜歌、取播放地址、封面。

### 网盘

```
pan.baidu.com  d.pcs.baidu.com  yun.baidu.com
pan.quark.cn  drive-pc.quark.cn
drive.uc.cn  pc-api.uc.cn
api-pan.xunlei.com  xluser-ssl.xunlei.com
yun.139.com  yun.123pan.cn  www.guangyapan.com
www.lanzou.com  www.ilanzou.com  pc.woozooo.com 115.com
```

用途：登录态校验、目录列表、创建分享、分片下载。**这些请求会带上你自己在该平台的 Cookie**（仅本地存储，不上传到第三方）。

### AI 服务（可选，需自己填 Key）

```
api.openai.com  api.deepseek.com
api.search.brave.com  api.tavily.com  google.serper.dev
```

用途：猫娘化回复、联网搜索。**不填 Key 就不发请求。**

### 图床与语音合成

```
freeimage.host  uguu.se
fanyi.baidu.com/gettts
```

用途：图片外链（发图用）、文字转语音。

---

## 会离开设备的内容

需要说明白，以下几处**会把你输入的内容发给第三方服务**：

| 功能 | 发出去什么 | 发给谁 |
|---|---|---|
| 文字转语音 | 待转换的文字 | 百度翻译 TTS |
| AI 回复 | 消息内容 + 上下文 | 你配置的 AI 服务商 |
| 联网搜索 | 搜索关键词 | Brave / Tavily / Serper |
| 图片外链 | 图片本体 | freeimage.host / uguu.se |
| 网盘操作 | Cookie + 文件信息 | 对应网盘官方接口 |
| 视频解析 | 分享链接 | 平台官方接口 |

**不开这些功能就不会有任何内容离开设备。**

代码里没有埋点、没有崩溃上报、没有统计 SDK。

---

## 其他

- **不收集**设备信息（IMEI / MAC / 序列号一概不读）；「自定义设备信息」功能是**伪造**给宿主看的，方向相反
- **不主动上传**聊天内容；只有你开启 AI 回复 / 联网搜索 / 语音合成时，相应文本才会发给对应的第三方服务
- 无账号体系、无统计、无广告、无内购
- 配置文件导出时会过滤 `uin / uid / token / cookie / password / skey / guid / imei / mac` 等字段

---

## 免责声明

本模块修改的是宿主（TIM / QQ）的运行行为，可能违反腾讯的服务条款。
使用后果由使用者自行承担。作者不对账号封禁、数据丢失等任何后果负责。

**请勿用于商业用途，请勿传播给无判断能力的使用者。**

---

## 许可证

**GPL-3.0**，详见 [LICENSE](LICENSE)。

第三方代码与许可证归属见 [NOTICE.md](NOTICE.md)。
