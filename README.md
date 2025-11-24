本项目已有上位替代。

<del>
# 取餐码识别 (HyperNotification)

一个用于识别取餐码的Android应用，支持截图识别和焦点通知(岛通知)功能。

## 功能特性

- 📸 屏幕截图
- 🖼️ 图片上传到PicGo图床
- 🤖 AI识别取餐码和商家名称（使用智谱AI）
- 🔔 发送焦点通知（MIUI岛通知）
- 📝 实时日志查看
- ⚡ 快速运行模式

## 项目结构

```
HyperNotification/
├── app/
│   ├── src/main/
│   │   ├── java/com/test/hypernotification/
│   │   │   ├── MainActivity.java          # 主界面
│   │   │   ├── PickupCodeService.java     # 核心识别服务
│   │   │   ├── FocusNotificationHelper.java # 焦点通知辅助类
│   │   │   ├── ScreenCaptureService.java  # 截图服务
│   │   │   ├── QuickRunActivity.java      # 快速运行活动
│   │   │   ├── LogActivity.java           # 日志查看
│   │   │   └── LogManager.java            # 日志管理
│   │   ├── res/                           # 资源文件
│   │   └── AndroidManifest.xml            # 清单文件
│   ├── build.gradle                       # 模块构建配置
│   └── proguard-rules.pro                 # 混淆规则
├── gradle/wrapper/                        # Gradle包装器
├── .github/workflows/build.yml            # GitHub Actions工作流
├── build.gradle                            # 项目构建配置
├── settings.gradle                         # 项目设置
├── gradle.properties                       # Gradle属性
├── gradlew                                 # Unix/Linux构建脚本
└── gradlew.bat                            # Windows构建脚本
```

## 编译前准备

### 重要：下载gradle-wrapper.jar

由于文件大小限制，您需要手动下载gradle-wrapper.jar文件：

1. 下载地址：https://github.com/gradle/gradle/raw/master/gradle/wrapper/gradle-wrapper.jar
2. 将下载的文件放置到：`gradle/wrapper/gradle-wrapper.jar`

或者在项目根目录执行：
```bash
curl -L -o gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/master/gradle/wrapper/gradle-wrapper.jar
```

## 构建方法

### 方法1：GitHub Actions自动构建

1. Fork或上传此项目到您的GitHub仓库
2. 确保已添加gradle-wrapper.jar文件
3. Push到main分支或手动触发workflow
4. 在Actions页面下载构建好的APK

### 方法2：本地构建

```bash
# 给予执行权限
chmod +x gradlew

# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本
./gradlew assembleRelease
```

构建完成后，APK文件位于：
- Debug版本：`app/build/outputs/apk/debug/app-debug.apk`
- Release版本：`app/build/outputs/apk/release/app-release-unsigned.apk`

## 使用说明

### 首次配置

1. 打开应用，填写以下配置：
   - **AI Token**：智谱AI的API Token
   - **PicGo API Key**：PicGo图床的API密钥
   - **PicGo URL**：图床上传地址（默认已填写）
   - **Album ID**：相册ID

2. 选择截图模式：
   - **普通模式**：使用Android系统的MediaProjection API（默认）
   - **Root模式**：使用screencap命令（需要Root权限）

3. 点击"保存配置"

### 运行识别

1. 确保屏幕上显示有取餐码
2. 点击"运行识别"按钮
3. 如果使用普通模式，需要授予截屏权限
4. 等待识别完成，查看结果

### 快速运行

可以通过以下方式快速启动识别：
- 创建桌面快捷方式指向QuickRunActivity
- 使用Intent：`com.test.hypernotification.QUICK_RUN`

## 权限说明

应用需要以下权限：
- INTERNET：网络访问
- POST_NOTIFICATIONS：发送通知
- FOREGROUND_SERVICE：前台服务
- READ/WRITE_EXTERNAL_STORAGE：文件存储

## 焦点通知说明

应用使用原生通知 + MIUI扩展参数的方式发送焦点通知。
在支持焦点通知的设备上，会显示为岛通知样式。

## 技术栈

- Android SDK 26+
- OkHttp3：网络请求
- Gson：JSON解析
- Material Design：UI组件

## 注意事项

1. 需要Android 8.0（API 26）及以上版本
2. Root模式截图功能需要Root权限（使用screencap命令）
3. 普通模式使用MediaProjection API，需要授予截屏权限
4. 焦点通知功能仅在支持的ROM上生效

## 开发者

此应用基于提供的Shell脚本逻辑开发，完整实现了取餐码识别的全部流程。

## License

MIT License
</del>
