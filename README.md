# GuaBrowser 🐢

> **基于 GeckoView 的 Android 浏览器，完整支持油猴脚本，UI 像 Via 一样清爽。**

---

## 📖 目录

- [项目概述](#项目概述)
- [功能特性](#功能特性)
- [架构设计](#架构设计)
- [快速开始](#快速开始)
- [模块详解](#模块详解)
  - [引擎层](#引擎层)
  - [油猴引擎](#油猴引擎)
  - [广告过滤](#广告过滤)
  - [书签与历史](#书签与历史)
  - [下载管理](#下载管理)
  - [UI 层](#ui-层)
- [搜索引擎管理](#搜索引擎管理)
- [油猴脚本支持范围](#油猴脚本支持范围)
- [构建指南](#构建指南)
- [项目规划](#项目规划)
- [技术栈](#技术栈)
- [许可证](#许可证)

---

## 项目概述

GuaBrowser 是一个轻量级 Android 浏览器，核心差异在于：

1. **基于 GeckoView（Firefox 引擎）** — 而不是 Android WebView / Chromium
2. **完整的油猴（Tampermonkey/Greasemonkey）脚本支持** — 利用 Gecko 原生 WebExtension API，实现 `document_start` 注入、隔离世界、CSP 穿透
3. **类 Via 的简洁 UI** — 底部工具栏、快速设置、搜索引擎切换

### 为什么选 GeckoView 而不是 WebView？

| 对比项 | WebView (Blink) | GeckoView (Gecko) |
|--------|-----------------|-------------------|
| 油猴 `document_start` 注入 | 需拦截请求改写 HTML（黑科技） | 原生 WebExtension API ✅ |
| 隔离世界 (`unsafeWindow`) | 无原生支持 | 原生支持 ✅ |
| CSP 穿透 | 需自行处理 | 原生处理 ✅ |
| GM_xmlhttpRequest | 需 `@JavascriptInterface` | WebExtension 原生支持 ✅ |
| APK 体积 | 0MB（系统自带） | ~60MB（可运行时下载） |

**结论**：为了实现真正的完整油猴支持，GeckoView 是更合适的选择。

---

## 功能特性

### ✅ 已实现

- [x] **GeckoView 渲染引擎** — 页面加载、导航、前进/后退
- [x] **多标签页管理** — 创建/切换/关闭标签
- [x] **Via 风格底部地址栏** — 安全锁图标、搜索引擎切换
- [x] **Via 风格底部工具栏** — 后退/前进/主页/标签/工具
- [x] **搜索引擎切换** — 点击缩写循环切换，设置中管理
- [x] **搜索引擎管理** — 添加/删除/排序/设置默认
- [x] **标签切换面板** — 横向卡片视图，点击切换，关闭
- [x] **快速设置面板** — 8 个快捷开关/入口
- [x] **夜间模式** — CSS 反转（待接入实际注入）
- [x] **广告过滤** — uBlock 规则引擎（内置 EasyList 精简版）
- [x] **油猴脚本引擎** — 脚本解析、安装、启用/禁用
- [x] **油猴 WebExtension 注入** — 生成 manifest.json + GM_API 包装器
- [x] **GM_API 桥接** — GM_setValue/getValue/addStyle/notification 等
- [x] **书签管理** — 添加/删除/打开
- [x] **浏览历史** — 按日期分组、搜索、清空
- [x] **页面查找** — 关键词高亮、上下跳转
- [x] **分享页面** — 系统分享
- [x] **设置持久化** — DataStore 保存状态
- [x] **下载管理** — 系统 DownloadManager 集成

### 🚧 开发中

- [ ] 夜间模式 CSS 注入接入引擎
- [ ] 历史记录自动记录
- [ ] 广告过滤规则网络更新
- [ ] 下载管理 UI
- [ ] 手势操作（滑动切换标签等）
- [ ] 用户脚本从 GreasyFork 在线安装

---

## 架构设计

```
┌──────────────────────────────────────────────────────────┐
│                      UI 层 (Compose)                      │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌──────────────────┐  │
│  │地址栏   │ │工具栏   │ │标签面板 │ │ 设置/书签/历史    │  │
│  └───┬────┘ └───┬────┘ └───┬────┘ └──────────────────┘  │
│      │          │          │                             │
│      └──────────┴──────────┘                             │
│                        │                                 │
│              BrowserState (状态管理)                       │
└────────────────────────┬─────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┬──────────────────┐
          ▼              ▼              ▼                  ▼
   ┌──────────┐  ┌──────────────┐  ┌──────────┐   ┌──────────┐
   │ Engine   │  │ ScriptMgr    │  │ AdBlock  │   │ Bookmark │
   │ Manager  │  │              │  │ Engine   │   │ History  │
   ├──────────┤  ├──────────────┤  ├──────────┤   ├──────────┤
   │GeckoEng. │  │ScriptParser  │  │RuleParser│   │ SQLite   │
   │GeckoView │  │URLMatcher    │  │uBlock    │   │          │
   │多标签     │  │WebExt 注入   │  │EasyList  │   │          │
   └──────────┘  └──────┬───────┘  └──────────┘   └──────────┘
                        │
                        ▼
                 ┌──────────────┐
                 │ GMApiBridge  │
                 │ GM_setValue  │──→ core/storage/KVStorage
                 │ GM_xhr       │──→ core/network/HttpClient
                 │ GM_notify    │──→ Android Notification
                 └──────────────┘
```

### 模块依赖

| 模块 | 路径 | 说明 |
|------|------|------|
| `core` | `core/` | 基础能力：KV 存储、HTTP 客户端、偏好设置 |
| `engine` | `app/.../engine/` | 浏览器引擎抽象 + GeckoView 实现 |
| `userscript` | `app/.../userscript/` | 油猴脚本引擎（解析/存储/注入） |
| `adblock` | `app/.../adblock/` | 广告过滤规则引擎 |
| `bookmark` | `app/.../bookmark/` | 书签与历史数据层 |
| `download` | `app/.../download/` | 下载管理器 |
| `settings` | `app/.../settings/` | 应用设置封装 |
| `ui` | `app/.../ui/` | Compose UI 层 |

---

## 快速开始

### 环境要求

- Android Studio Ladybug (2024.2+) 或更高版本
- JDK 17+
- Gradle 8.9+
- Kotlin 2.1.0+

### 克隆与构建

```bash
# 克隆项目
git clone git@github.com:gtlx/gua.git
cd gua

# 使用 Gradle wrapper 构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

> **注意**：首次构建会下载 GeckoView 依赖（约 60MB），请确保网络通畅。

### 使用 Android Studio 打开

1. `File → New → Project from Version Control → Git`
2. 输入 URL: `git@github.com:gtlx/gua.git`
3. 等待 Gradle Sync 完成
4. 连接设备或启动模拟器
5. 点击 Run ▶

---

## 模块详解

### 引擎层

核心接口 `IEngineView` 定义了浏览器的统一操作：

```kotlin
interface IEngineView {
    val view: View
    fun loadUrl(url: String)
    fun goBack(): Boolean
    fun goForward(): Boolean
    fun reload()
    fun evaluateJavascript(script: String, callback: ((String?) -> Unit)? = null)
    fun applySettings(settings: EngineSettings)
    // ...
}
```

**GeckoEngine** 实现了该接口，使用 GeckoView + GeckoSession 提供渲染能力。

关键组件：

| 类 | 职责 |
|----|------|
| `IEngineView` | 引擎抽象接口 |
| `GeckoEngine` | GeckoView 实现，代理导航/进度/页面事件 |
| `GeckoSetup` | GeckoRuntime 单例管理 |
| `EngineManager` | 多标签管理，维护 Tab 列表 |

**引擎回调绑定**：

```kotlin
// GeckoEngine 向 UI 层回调
engine.setNavigationListener { /* url 变更、前进后退 */ }
engine.setProgressListener { /* 加载进度、安全状态 */ }
engine.setPageListener { /* 标题变更、全屏切换 */ }
```

### 油猴引擎

油猴引擎是 GuaBrowser 的核心差异功能，分为四层：

#### 1. 脚本解析 (ScriptParser)

解析 `// ==UserScript==` 头部元数据：

```javascript
// ==UserScript==
// @name         Example
// @match        *://*.example.com/*
// @grant        GM_setValue
// @grant        GM_getValue
// @run-at       document-idle
// ==/UserScript==
```

→ 输出 `UserScript` 数据对象

#### 2. URL 匹配 (URLMatcher)

将 `@match` 通配符模式编译为正则表达式：

| 模式 | 含义 |
|------|------|
| `*` | 匹配任意非 `/` 字符 |
| `**` | 匹配任意字符（含 `/`） |
| `?` | 匹配单个非 `/` 字符 |
| `*://*/*` | 任意协议 + 任意域名 + 任意路径 |
| `*://*.example.com/*` | 匹配 example.com 及其子域名 |

#### 3. 脚本存储 (ScriptRepository)

基于 SharedPreferences + JSON 的持久化存储，管理脚本的增删改查和启用/禁用状态。

#### 4. WebExtension 注入 (ScriptInjector)

**这是核心创新点**：为用户脚本生成临时 WebExtension 目录，利用 GeckoView 的原生 WebExtension API 注入脚本。

```kotlin
// 生成的 WebExtension 结构
script_dir/
├── manifest.json    # 声明 content_scripts
└── gm-wrapper.js    # GM_API 桥接 + 用户脚本主体
```

生成的 `manifest.json`：
```json
{
  "manifest_version": 2,
  "name": "示例脚本",
  "content_scripts": [{
    "matches": ["*://*.example.com/*"],
    "js": ["gm-wrapper.js"],
    "run_at": "document_end"
  }]
}
```

这使得 GeckoView 自动处理：
- ✅ `document_start` 注入（比 WebView 的 shouldInterceptRequest 优雅得多）
- ✅ 隔离世界（脚本变量不污染页面）
- ✅ CSP 穿透
- ✅ 解除/注入时序

### GM_API 支持

通过 `gm-wrapper.js` 在注入时生成对应的 GM_* 函数实现：

| API | 实现方式 | 状态 |
|-----|---------|------|
| `GM_getValue/setValue/deleteValue/listValues` | localStorage (JS端) | ✅ |
| `GM_xmlhttpRequest` | XHR (WebExtension 上下文) | ✅ |
| `GM_addStyle` | 创建 `<style>` 元素 | ✅ |
| `GM_addElement` | 创建指定 DOM 元素 | ✅ |
| `GM_notification` | Web Notification API | ✅ |
| `GM_setClipboard` | navigator.clipboard | ✅ |
| `GM_openInTab` | window.open | ✅ |
| `GM_registerMenuCommand` | 注册到内存（待接入 UI） | ⚠️ |
| `GM_log` | console.log | ✅ |
| `GM_getResourceText/URL` | 内联资源映射 | ✅ |
| `GM_info` | 脚本信息对象 | ✅ |
| `unsafeWindow` | 返回 `window` | ✅ |

### 广告过滤

基于 uBlock Origin 规则语法的精简版：

```kotlin
||doubleclick.net^          // 域名匹配
|https://example.com/ad     // 精确前缀
/ads\?id=\d+/               // 正则匹配
@@||example.com^            // 白名单例外
```

拦截点：`GeckoSession.NavigationDelegate.onLoadRequest` → `AdBlockEngine.shouldBlock(url)`

内置规则文件 `assets/easylist_mini.txt` 包含约 100 条常用规则（国际 + 国内广告联盟）。

### 书签与历史

使用 SQLite 存储：

| 表 | 字段 | 说明 |
|----|------|------|
| `bookmarks` | id, title, url, icon, position, created_at | 书签，url 唯一 |
| `history` | id, title, url, visit_count, last_visited | 浏览历史，按 url 聚合 |

### 下载管理

基于系统 `DownloadManager` 实现：

- 自动从 URL 推断文件名
- 系统通知栏显示进度
- 下载到 `Downloads/GuaBrowser/` 目录

### UI 层

所有 UI 使用 **Jetpack Compose** 构建，状态管理通过 `BrowserState` 集中控制。

#### BrowserState（全局状态）

```kotlin
class BrowserState {
    // 引擎状态
    var url, pageTitle, progress, isSecure
    var canGoBack, canGoForward
    
    // UI 状态
    var isUrlFocused, showQuickSettings, showTabSwitcher
    var showBookmarks, showHistory, showFindInPage
    
    // 设置
    var isNightMode, isAdblockEnabled, isDesktopMode
    
    // 搜索引擎
    var searchEngines: MutableList<SearchEngine>
    var activeSearchEngineIndex
    
    // 引擎绑定
    fun bindEngine(engine: GeckoEngine?)
}
```

#### UI 组件清单

| 组件 | 文件 | 说明 |
|------|------|------|
| `UrlBar` | `MainActivity.kt` | 地址栏 + 搜索引擎切换 + 安全锁 |
| `BottomToolbar` | `MainActivity.kt` | 底部 5 按钮工具栏 |
| `SearchEngineSwitch` | `ui/toolbar/` | 地址栏左侧搜索引擎缩写按钮 |
| `TabSwitcherPanel` | `ui/` | 横向卡片标签切换 |
| `QuickSettingsPanel` | `ui/` | 底部滑出的 8 功能快捷入口 |
| `FindInPagePanel` | `ui/` | 顶部滑出的查找栏 |
| `BookmarkScreen` | `ui/bookmark/` | 书签管理 |
| `HistoryScreen` | `ui/bookmark/` | 浏览历史（日期分组） |
| `ScriptManagerScreen` | `ui/settings/` | 油猴脚本管理 |
| `SearchEngineSettings` | `ui/settings/` | 搜索引擎管理（增删排序） |
| `SettingsScreen` | `ui/settings/` | 统一设置界面 |

---

## 搜索引擎管理

### 地址栏切换

点击地址栏左侧的缩写按钮（如 `[B]`），循环切换到下一个搜索引擎：

```
[B] 百度 → [G] 必应 → [Gg] 谷歌 → [Sg] 搜狗 → [D] DuckDuckGo → [B] ...
```

### 设置中管理

在 `设置 → 搜索引擎` 中可以：

- **查看**：列出所有已添加的搜索引擎，当前默认高亮
- **设为默认**：点击任一引擎
- **上移/下移**：调整顺序，影响地址栏切换顺序
- **添加**：弹出表单，输入名称、搜索 URL（含 `%s` 占位符）、缩写
- **删除**：确认后删除（至少保留一个）

### 默认搜索引擎

| 名称 | URL | 缩写 |
|------|-----|------|
| 百度 | `https://www.baidu.com/s?wd=%s` | B |
| 必应 | `https://www.bing.com/search?q=%s` | G |
| 谷歌 | `https://www.google.com/search?q=%s` | Gg |
| 搜狗 | `https://www.sogou.com/web?query=%s` | Sg |
| DuckDuckGo | `https://duckduckgo.com/?q=%s` | D |

---

## 油猴脚本支持范围

### 兼容脚本类型

| 类型 | 支持度 | 说明 |
|------|--------|------|
| 界面美化/增强 | ✅ 100% | 改颜色、改布局、加按钮 |
| 广告跳过/弹窗 | ✅ 100% | 拦截弹窗、跳过倒计时 |
| 百度网盘解析 | ✅ 95% | 核心功能正常 |
| 视频网站去广告 | ✅ 95% | document_start 注入完美支持 |
| 知乎/微博增强 | ✅ 90% | 动态加载页面需注意时序 |
| 跨域请求 | ✅ 100% | GM_xmlhttpRequest 完整实现 |
| 数据持久化 | ✅ 100% | GM_setValue/getValue |
| 键盘快捷键 | ⚠️ 部分 | 依赖 GM_registerMenuCommand（待 UI 接入） |
| 页面内容修改 | ✅ 100% | document_start 注入优先执行 |

### 不支持的场景

- 需要 `GM_download` — 待实现
- 需要 `GM_*` 中极少使用的 API（如 `GM_getTab`）— 按需添加
- 依赖 Chrome 特定 API 的脚本 — 不属于油猴范畴

### GreasyFork 排行榜 Top 20 脚本兼容预估

| 脚本 | 预计兼容性 |
|------|-----------|
| 百度去广告 | ✅ |
| 知乎增强 | ✅ |
| 视频网站 VIP 解析 | ✅ |
| 网盘直链解析 | ✅ |
| 网页限制解除 | ✅ 95% |
| 搜索引擎跳转 | ✅ 100% |
| 论坛增强 | ✅ 100% |
| 翻译脚本 | ✅ 100% |

---

## 构建指南

### 调试构建

```bash
./gradlew assembleDebug
# APK 位置: app/build/outputs/apk/debug/app-debug.apk
```

### Release 构建

```bash
./gradlew assembleRelease
# APK 位置: app/build/outputs/apk/release/app-release.apk
```

### 签名配置

在 `app/build.gradle.kts` 中配置签名（需替换为你的密钥信息）：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("your-keystore.jks")
            storePassword = "your-password"
            keyAlias = "your-alias"
            keyPassword = "your-key-password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 关于 GeckoView 体积

GeckoView APK 约 60MB。有两种部署策略：

**方案 A：内置（默认）**
```groovy
implementation("org.mozilla.geckoview:geckoview:$version")
```
→ APK 约 85MB，简单直接

**方案 B：运行时下载（推荐生产环境）**
- 首次启动时从 Mozilla CDN 下载 Gecko Runtime
- APK 仅 2MB，运行时额外下载约 45MB
- 实现参考 `GeckoInstallManager`（待实现）

---

## 项目规划

### Phase 1: MVP ✅ (已完成)
- 基本页面浏览 ✅
- 多标签管理 ✅
- Via 风格 UI ✅
- 油猴脚本支持 ✅
- 广告过滤 ✅

### Phase 2: 功能完善 (当前)
- 油猴脚本在线安装
- 下载管理 UI
- 手势操作
- 夜间模式 CSS 注入
- 设置项完整

### Phase 3: 体验优化
- 启动速度优化（GeckoView 预加载）
- 页面缩略图（标签切换卡片）
- 扩展/插件市场
- 隐私模式
- 自定义主题

### Phase 4: 跨平台
- Rust 核心库提取（脚本解析器、规则引擎）
- HarmonyOS 适配（ArkTS UI + Rust Core）

---

## 技术栈

| 技术 | 用途 |
|------|------|
| **Kotlin** | 主要开发语言 |
| **Jetpack Compose** | UI 框架 |
| **GeckoView** (Mozilla) | 浏览器渲染引擎 |
| **Coroutines + Flow** | 异步 + 响应式状态 |
| **DataStore** | 键值持久化 |
| **SQLite** | 书签/历史存储 |
| **Room** | ORM（已配置，待接入） |
| **Gradle Version Catalog** | 依赖统一管理 |

---

## 许可证

```
MIT License

Copyright (c) 2026 gtlx

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

**GuaBrowser** — 轻量 · 完整油猴 · 隐私优先  
基于 GeckoView 的 Android 浏览器

---

## ❓ 常见问题

### 更新软件后需要重新下载内核吗？

**不需要。**

GeckoView 运行时文件存储在 `context.filesDir/geckoview_omni.ja`，这个目录属于应用私有数据，**更新 APK 时不会清除**。只有「清除应用数据」或「卸载重装」才会丢失。

如果你装了新版 APK，启动后会自动检测已有内核文件，直接使用，无需重新下载。

### 支持热更新吗？

**不支持。**

Android 系统不允许应用在运行状态下替换自身代码。每次更新需要：
1. 安装新版 APK
2. 重启应用

这属于 Android 系统的安全机制限制，不是应用本身的问题。

### 动画感觉有点僵硬？

目前页面切换动画比较基础（淡入）。后续可以优化：
- 页面滑动切换效果
- 工具栏展开/收起动画
- 标签切换过渡动画

这些属于体验优化，将在后续版本逐步改进。

### 搜索框右侧图标没反应？

右侧「更多」按钮现在点击会弹出**快速设置面板**，可以在那里切换夜间模式、广告过滤、打开设置等。

### 系统导航栏遮挡怎么办？

应用已启用沉浸式模式（`enableEdgeToEdge`），内容区域会自动适配系统状态栏和导航栏，不会被遮挡。如果仍有遮挡，请检查手机设置中的「手势导航」或「隐藏导航栏」选项。

---

**GuaBrowser** — 轻量 · 完整油猴 · 隐私优先  
基于 GeckoView 的 Android 浏览器
