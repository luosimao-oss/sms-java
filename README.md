# Luosimao SMS Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.luosimao/luosimao-sms-sdk)](https://search.maven.org/artifact/com.luosimao/luosimao-sms-sdk)
[![Java Version](https://img.shields.io/badge/Java-1.8%2B-blue)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://github.com/luosimao-oss/sms-java/workflows/Release/badge.svg)](https://github.com/luosimao-oss/sms-java/actions)

这是一个现代化的、用于对接 [Luosimao 短信 API](https://luosimao.com/docs/api/) 的 Java SDK。

## 特性

- **统一的错误码枚举**：所有 API 错误码（如 `-10`, `-20`, `-31` 等）都被映射到 `LuosimaoErrorCode` 枚举中。
- **自动重试机制**：内置网络抖动处理，当遇到网络连接失败或 5xx 服务器错误时，会自动进行重试（默认 3 次）。
- **安全的签名封装**：只需提供 API Key 即可，SDK 会自动将其拼接封装为 `api:key-xxx` 的 HTTP Basic Auth 格式，不暴露底层拼接细节。
- **完备的单元测试**：使用 MockWebServer 进行了充分的单元测试保障。
- **开箱即用**：基于 `OkHttp3` 和 `Gson` 构建，易于集成。

## 安装

通过 Maven Central 安装：

```xml
<dependency>
    <groupId>com.luosimao</groupId>
    <artifactId>luosimao-sms-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

或通过 Gradle：

```groovy
implementation 'com.luosimao:luosimao-sms-sdk:1.0.0'
```

## 快速开始

### 初始化客户端

> **⚠️ 重要提示**：`LuosimaoClient` 内部维护了 HTTP 连接池，建议在应用程序中将其作为**全局单例**进行复用，以获得最佳性能并避免资源泄露。

```java
import com.luosimao.sms.LuosimaoClient;

// 使用默认配置（网络失败自动重试 3 次，间隔 1000 毫秒，超时均为 10 秒）
// 注意：只需传入 API Key 即可，无需包含 "key-" 前缀
LuosimaoClient client = new LuosimaoClient("your-api-key-here");

// 或者使用自定义重试配置 (API Key, 最大重试次数, 重试间隔毫秒)
LuosimaoClient clientWithRetry = new LuosimaoClient("your-api-key-here", 5, 2000);

// 或者完全自定义配置 (API Key, 最大重试次数, 重试间隔毫秒, 连接超时秒数, 读取超时秒数, 写入超时秒数)
LuosimaoClient fullyCustomClient = new LuosimaoClient("your-api-key-here", 3, 1000, 15, 15, 15);
```

### 1. 发送单条短信

```java
import com.luosimao.sms.model.SmsResponse;
import com.luosimao.sms.LuosimaoException;

try {
    // 手机号，短信内容（必须包含签名，如【铁壳测试】）
    SmsResponse response = client.send("13800138000", "您的验证码是：123456【公司名称】");
    System.out.println("发送成功，批次号: " + response.getBatchId());
} catch (LuosimaoException e) {
    // 处理 API 返回的业务错误或网络异常
    System.err.println("错误码: " + e.getRawCode());
    System.err.println("错误描述: " + e.getErrorCode().getDescription());
    System.err.println("详细信息: " + e.getMessage());
}
```

### 2. 批量发送短信

```java
import com.luosimao.sms.model.SmsResponse;
import java.util.Arrays;

try {
    // 手机号列表，短信内容
    SmsResponse response = client.sendBatch(
        Arrays.asList("13800138000", "13800138001"),
        "这是一条批量发送的通知短信【公司名称】"
    );
    System.out.println("批量发送成功，批次号: " + response.getBatchId());
} catch (LuosimaoException e) {
    e.printStackTrace();
}
```

### 3. 查询账户余额

```java
import com.luosimao.sms.model.StatusResponse;

try {
    StatusResponse status = client.getStatus();
    System.out.println("当前账户余额: " + status.getDeposit());
} catch (LuosimaoException e) {
    e.printStackTrace();
}
```

## 异常处理与错误码

SDK 会将所有的 API 错误（如余额不足、签名缺失等）转化为 `LuosimaoException` 抛出，可以通过捕获该异常来做精细化的业务处理。你可以使用 `e.getErrorCode()` 获取到强类型的错误枚举。

部分常见错误码：
- `-10`: 验证信息失败
- `-20`: 短信余额不足
- `-31`: 短信内容存在敏感词
- `-32`: 短信内容缺少签名信息
- `-40`: 错误的手机号

更多详情请参考 `LuosimaoErrorCode.java` 类或官方 API 文档。

## 项目结构

```
luosimao-sms-sdk/
├── pom.xml                                        # Maven 构建配置
├── README.md                                      # 项目文档
├── RELEASE_GUIDE.md                               # 发布指南
├── .github/
│   └── workflows/
│       └── release.yml                            # GitHub Actions 自动发布
└── src/
    ├── main/java/com/luosimao/sms/
    │   ├── LuosimaoClient.java                   # SDK 主入口
    │   ├── LuosimaoErrorCode.java                # 统一错误码枚举
    │   ├── LuosimaoException.java                # 自定义异常
    │   ├── RetryInterceptor.java                  # 自动重试拦截器
    │   └── model/
    │       ├── BaseResponse.java                  # 基础响应模型
    │       ├── SmsResponse.java                   # 短信响应模型
    │       └── StatusResponse.java                # 余额查询响应模型
    └── test/java/com/luosimao/sms/
        └── LuosimaoClientTest.java               # 单元测试
```

## 许可证

本项目基于 [MIT License](https://opensource.org/licenses/MIT) 开源。

## 发布日志

### v1.0.0 (2026-03-28)
- 首次发布
- 支持单条短信发送、批量短信发送、余额查询
- 内置 HTTP Basic Auth 自动封装
- 内置网络自动重试机制（5xx 错误 + IO 异常）
- 完整的单元测试覆盖
