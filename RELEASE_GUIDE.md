# 发布指南 / Release Guide

本文档详细说明如何将 `luosimao-sms-sdk` 发布到 GitHub 和 Maven Central。

---

## 一、发布到 GitHub

### 1.1 初始化 Git 仓库并推送

```bash
# 初始化 Git 仓库
git init

# 添加所有文件
git add .

# 提交
git commit -m "feat: initial release of luosimao-sms-sdk v1.0.0"

# 添加远程仓库地址
git remote add origin git@github.com:luosimao-oss/sms-java.git

# 推送到 GitHub
git push -u origin master
```

---

## 二、发布到 Maven Central（新流程 2024+）

> ⚠️ **重要变更**：自 2024 年 1 月起，Sonatype 已废弃旧的 `issues.sonatype.org` Jira 注册流程，全面升级为 **Central Publishing Portal**（`central.sonatype.com`）。旧流程不再可用。

### 2.1 核心变化对比

| 项目 | 旧流程（已废弃） | 新流程（2024+） |
|------|----------------|----------------|
| 注册入口 | `issues.sonatype.org` (Jira) | `central.sonatype.com` |
| 命名空间验证 | 人工审批（Jira Ticket） | 自动验证（GitHub / DNS） |
| 凭证 | OSSRH 用户名/密码 | Central Portal Token |
| 支持方式 | Jira Ticket | 邮件 `central-support@sonatype.com` |
| 仓库地址 | `s01.oss.sonatype.org` | 不变 |

### 2.2 前置要求

1. **GitHub 账号** + **公开仓库**（用于命名空间自动验证）
2. **Central Portal 账号**：访问 [central.sonatype.com](https://central.sonatype.com) 注册
3. **GPG 密钥**：用于给发布的 Artifacts 进行签名

### 2.3 第一步：注册 Central Portal 并验证命名空间

1. 访问 [central.sonatype.com](https://central.sonatype.com)，使用 GitHub 账号登录
2. 点击 **"Add Namespace"**，输入命名空间：
   - 推荐使用 GitHub Namespace：`io.github.luosimao-oss`（需仓库在 `github.com/luosimao-oss/` 下）
   - 或使用域名命名空间：`com.luosimao`（需 DNS 验证）
3. 如果选择 GitHub Namespace，系统会自动验证，无需人工审批
4. 验证通过后，生成 **Token**（后续用于 Maven 发布认证）

### 2.4 第二步：配置 GPG 签名

1. **生成 GPG 密钥对**：
```bash
gpg --full-generate-key
# 选择 RSA 4096 位，不过期
```

2. **查看并导出私钥**（用于 CI/CD 环境）：
```bash
# 查看密钥 ID
gpg --list-keys

# 导出私钥（base64 编码，用于 GitHub Secrets）
gpg --armor --export-secret-keys <YOUR_KEY_ID> | base64
```

3. **上传公钥到密钥服务器**：
```bash
gpg --keyserver keyserver.ubuntu.com --send-keys <YOUR_KEY_ID>
gpg --keyserver keys.openpgp.org --send-keys <YOUR_KEY_ID>
```

### 2.5 第三步：配置 Maven Settings

在 `~/.m2/settings.xml` 中添加 Central Portal Token 配置：

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_CENTRAL_PORTAL_USERNAME</username>
      <password>YOUR_CENTRAL_PORTAL_TOKEN</password>
    </server>
  </servers>
</settings>
```

> 💡 **Token 获取方式**：登录 [central.sonatype.com](https://central.sonatype.com) → Account Settings → Generate Token

### 2.6 第四步：发布命令

#### 本地发布（推荐先在本地测试）

```bash
# 清理并运行测试
mvn clean test

# 打包（生成 sources、javadoc、签名）
mvn clean package -P release

# 发布到 Maven Central
mvn deploy -P release -DskipTests
```

> **注意**：使用 `-P release` profile 会激活 `gpg-plugin` 进行自动签名。

#### GitHub Actions 自动发布（推荐）

在 GitHub 仓库的 **Settings → Secrets** 中添加：

| Secret 名称 | 说明 |
|------------|------|
| `OSSRH_USERNAME` | Central Portal 用户名 |
| `OSSRH_PASSWORD` | Central Portal Token |
| `GPG_PASSPHRASE` | GPG 密钥密码 |
| `GPG_PRIVATE_KEY` | GPG 私钥（base64 编码） |

推送 Tag 即可自动触发发布：
```bash
git tag v1.0.0
git push origin v1.0.0
```

### 2.7 第五步：验证发布

发布成功后：

1. 登录 [central.sonatype.com](https://central.sonatype.com) → **Deployments** 页面查看状态
2. 状态变为 **PUBLISHED** 后，等待 10-30 分钟
3. 在 [Maven Central Search](https://search.maven.org/artifact/com.luosimao/luosimao-sms-sdk) 验证

---

## 三、常见问题

### Q1: GitHub Namespace 验证失败
确保仓库地址为 `github.com/luosimao-oss/sms-java`（与申请的命名空间一致）

### Q2: GPG 签名失败
```bash
# 确认密钥存在
gpg --list-secret-keys

# 如果使用 CI，确保私钥正确导入
echo "$GPG_PRIVATE_KEY" | base64 --decode | gpg --import
```

### Q3: 发布被拒绝（401/403）
检查 `settings.xml` 中的凭证是否为 **Central Portal Token**，而非旧版 OSSRH 密码

### Q4: 命名空间审核被拒
- GitHub Namespace：确保仓库是 **Public** 的
- 域名 Namespace：确保 DNS TXT 记录正确配置

---

## 四、版本管理规范

- **SNAPSHOT 版本**：开发中使用，如 `1.0.1-SNAPSHOT`
- **Release 版本**：正式发布，如 `1.0.0`
- **版本号规范**：遵循 [语义化版本](https://semver.org/lang/zh-CN/)
