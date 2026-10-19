# 第三方代码声明 / Third-Party Notices

本项目包含来自以下开源项目的代码。原项目的许可证一并适用于对应部分。

---

## 1. TCQT — callng/TCQT

- 仓库：https://github.com/callng/TCQT
- 许可证：**GPL-3.0**
- 用途：QQ / TIM 推送协议的 protobuf 定义

**涉及的 proto 文件**（存放于 `app/proto/`）：

```
InfoSyncPush.proto
MsgPush.proto
QQMessage.proto
RecallPush.proto
```

**由上述 proto 生成、存放于 `app/java/top/artmoe/inao/entries/` 的 Java 类**：

```
InfoSyncPushOuterClass$1.java
InfoSyncPushOuterClass$InfoSyncPush.java
MsgPushOuterClass$1.java
MsgPushOuterClass$MsgPush.java
QQMessageOuterClass$1.java
QQMessageOuterClass$QQMessage.java
QQMessageOuterClass$QQMessageOrBuilder.java
```

**使用位置**：`app/java/com/cathelperawa/owo/app/feature/AntiRevoke.java`（防撤回功能）

这些类由 protoc 从上游 `.proto` 生成，包名 `top.artmoe.inao.entries` 为上游原始包名，**未做修改**。
本项目未对这批生成类做逻辑改动。

---

## 2. ono — cwuom/ono

- 仓库：https://github.com/cwuom/ono
- 许可证：**GPL-3.0**
- 用途：同上，protobuf 定义的共同上游

---

## 3. protobuf-javalite

- 项目：https://github.com/protocolbuffers/protobuf
- 许可证：BSD-3-Clause
- 用途：protobuf 运行时（`com.google.protobuf` 包）

---

## 关于本项目自身的许可证

由于本项目链接了上述 **GPL-3.0** 代码，依据 GPL-3.0 的传染性条款，
本项目整体同样以 **GPL-3.0** 发布，详见根目录 `LICENSE` 文件。

如果你希望以其他许可证发布，必须先移除或重写 `top/artmoe/inao/` 下的全部代码。
