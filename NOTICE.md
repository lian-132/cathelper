# 第三方代码声明 / Third-Party Notices

本项目包含来自以下开源项目的代码或数据。原项目的许可证一并适用于对应部分。

---

## 1. TCQT — callng/TCQT

- 仓库：https://github.com/callng/TCQT
- 许可证：**GPL-3.0**
- 用途：QQ / TIM 推送协议的 protobuf 定义

**涉及的上游 proto 定义**（原文件不随本仓库分发，需从 TCQT 仓库获取）：

```
InfoSyncPush.proto
MsgPush.proto
QQMessage.proto
RecallPush.proto
```

**由上述 proto 生成、存放于本仓库 `app/java/top/artmoe/inao/entries/` 的 Java 类**：

```
InfoSyncPushOuterClass$1.java
InfoSyncPushOuterClass$InfoSyncPush.java
MsgPushOuterClass$1.java
MsgPushOuterClass$MsgPush.java
QQMessageOuterClass$1.java
QQMessageOuterClass$QQMessage.java
...
```

---

## 2. QQ 协议版本数据 — protocol-versions 系列

- 仓库：
  - https://github.com/GuZhi223/qq-protocol-versions
  - https://github.com/MrXiaoM/protocol-versions
  - https://github.com/QsignDevelop/Auto-protocol-versions
- 用途：`app/assets/sender_version.json` 中的 `subId → 版本号` 映射表

**说明**：这些仓库汇集的是 QQ 官方客户端对外公开的版本信息
（`subAppId`、`sort_version_name`、`QUA` 等，任何人从安装包即可读取）。
本项目只做了合并去重，按原样引用。

如需自行生成，把上述仓库的 `android_phone/*.json` 里的
`sub_app_id` 与 `sort_version_name` 提取出来即可，格式见文件本身。

---

## 3. 运行时依赖库

以下 jar 在构建时打入 APK，均为公开发布的第三方库，遵循各自许可证
（仓库不含这些 jar 本体，构建时从工具链目录读取）：

| 库 | 许可证 |
|---|---|
| protobuf-java 4.31.1 | BSD-3-Clause |
| bsh (BeanShell) 3.0.0 | Apache-2.0 |
| rhino 1.7.13 | MPL-2.0 |
| luaj 3.0.1 | MIT |
| dx (Android SDK Build Tools) | Apache-2.0 |
