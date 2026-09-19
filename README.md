# Smartisan Icon Wave Demo · 双波实验

## 实现版本与通用库

本分支使用正弦公式，预生成波形路径，动画时仅平移；保留原图标 Demo 参数。

- [贝塞尔版](https://github.com/wangrunsheng/smartisan-icon-wave-demo/tree/main)：原始拟合效果。
- [正弦版](https://github.com/wangrunsheng/smartisan-icon-wave-demo/tree/sine-wave)：同参数正弦波与缓存路径。
- [Wave Reveal 通用库](https://github.com/wangrunsheng/smartisan-icon-wave-demo/tree/wave-reveal-library)：纯 Kotlin 参数核心与 Android View/ViewGroup 容器，支持纯色、照片和控件组合。尚未发布 Maven Central，Compose/CMP UI 适配尚未实现。[接入文档](https://github.com/wangrunsheng/smartisan-icon-wave-demo/blob/wave-reveal-library/docs/LIBRARY.md)。

![左贝塞尔，右正弦；同参数同步对比](https://raw.githubusercontent.com/wangrunsheng/smartisan-icon-wave-demo/wave-reveal-library/docs/media/bezier-vs-sine.gif)

对比来自锤子手机实际录屏，两侧共享时钟和波形参数，水位固定。两者最大高度差约为振幅的 0.275%，视觉非常接近。贝塞尔也可以缓存后平移；通用库选择正弦主要是因为公式和参数直接对应，不代表已证明它性能更高。GIF 是压缩预览，不用于测量亚像素误差。

---

一个用 **Kotlin + Android Canvas** 复刻 **Smartisan OS 桌面图标下载水波纹动画**的实验 Demo。两条等波长的正弦曲线以不同速度移动，分别裁切同一幅彩色图标，叠加出下载进度的水面效果。

支持暂停、四分之一慢放、曲线轮廓和手动水位。无网络请求、无运行时权限、无 Python、无 WebView。

本项目基于真机录屏进行观察与拟合，是独立的学习与研究项目，不隶属于 Smartisan，也不代表其官方实现。

## 开始使用

需要 JDK 17、Android SDK Platform 36 和网络连接（首次获取 Gradle 及依赖）。最低运行版本为 Android 8.0 / API 26。

1. 在 Android Studio 中打开本目录，选择 JDK 17 并等待 Gradle 同步。
2. 通过 Android Studio 配置 SDK，或设置 `ANDROID_HOME`；也可以在未提交的 `local.properties` 中填写 `sdk.dir`。
3. 执行：

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows 使用 `gradlew.bat`。固定工具版本为 Gradle 9.4.1、Android Gradle Plugin 9.2.0、Kotlin 2.3.21。AGP 内置 Kotlin 支持，应用模块无需重复应用 Kotlin 插件。

默认构建使用本机 Android 调试签名。如果手机已安装其他签名的演示版本，需要使用原签名重新签署 APK 或卸载旧版后安装。仓库不包含任何签名密钥。

## 项目结构

```text
app/src/main/kotlin/com/russell/wavedemo/
├── MainActivity.kt        # 控件事件、页面状态和生命周期
├── motion/
│   ├── WaveSpec.kt        # 双波参数与匀速位移
│   ├── PlaybackClock.kt   # 单调时间、暂停恢复和循环
│   └── WaterLevel.kt      # 测量得到的水位关键帧
└── ui/
    ├── WaveView.kt        # Canvas 遮罩与叠加
    ├── IconArtwork.kt     # 矢量图标缓存与底色处理
    └── DemoStyle.kt       # 配色与原生控件样式
```

字符串和主题放在 Android 资源中。参数与时间计算不依赖 Android，可直接运行 JVM 测试。绘制循环复用 Path、Paint、裁切路径和图标缓存；状态文字只在控件发生变化时更新。

## 动画参数

所有尺寸使用录屏分析时的 180 单位坐标系；图标圆心为 `(92, 88)`，半径为 `68`。绘制时统一缩放。

| 参数 | 前波 | 后波 |
| --- | ---: | ---: |
| 波长 | 180 | 180 |
| 振幅 | 3.72 | 5.74 |
| 周期（秒） | 0.8493 | 0.6993 |
| 不透明度 | 100% | 约 30% |
| 水位偏移 | 0 | −0.6454 |

当前版本直接计算正弦函数：`y = 水位 + 水位偏移 + A × sin(2π × (x / λ + t / T) + φ)`。每隔 0.5 个参考单位采样形成 Canvas 路径，保留贝塞尔版本的波长、振幅、周期、相位和透明度，便于比较曲线形状。曲线形状保持固定，只作匀速水平位移。前后波波长相同，但时间频率不同，因此相位差持续变化。

水位关键帧来自原录屏 8–12 秒片段，自动播放时每四秒重播，**边界处水位会重置**。这是一段动画研究样本，不是真实下载进度组件。手动水位可用于独立观察双波运动。

上述数值来自录屏拟合，不能证明原应用内部使用的曲线类型或实现方式。

## 图标与视觉处理

图标使用 Google Chrome 官方 SVG 的几何路径，并改用平面色。视觉恢复为最初高清版本：纯白圆环，未填充区域使用统一的亮度转换保留图标内部层次；不单独给圆环染灰，也不添加压暗遮罩。图标由矢量生成 1080×1080 透明缓存。

图标原始文件及来源见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。演示不隶属于 Google。发布时请保留第三方说明；应用代码的许可证不覆盖 Google 标识。

## 验证与已知范围

JVM 测试覆盖暂停恢复不跳时、慢放倍率、循环边界、波相位周期和水位范围。GitHub Actions 仅在手动触发时执行构建、测试及 Android Lint。UI 已在 Smartisan Android 8.1 真机检查；其他设备仍需实际验证。

当前 `targetSdk` 为 34；本项目是直接安装的动画演示，尚未配置应用商店发布流程。屏幕在演示前台保持亮起，后台停止请求动画帧；旋转屏幕时保留播放及控件状态。

## 贡献

请参阅 [CONTRIBUTING.md](CONTRIBUTING.md)。应用源代码采用 [MIT](LICENSE) 许可证，第三方图标除外。

### 桌面图标

应用桌面图标采用独立的青蓝色示波器标志：正交网格、一实一虚双波、虚线白描边和完整闭合外框。为兼容锤子桌面，实际启动图标使用确认过的 PNG，并提供 mdpi 至 xxxhdpi 五档尺寸（48、72、96、144、192 像素）；白色背景不透明，曲线裁切于闭合外框内。应用内动画展示仍使用 Chrome 图标。

## 正弦波实验分支

`sine-wave` 使用预生成的两周期正弦路径。每个 View 创建时完成采样，之后仅通过 Canvas 平移更新水平相位和水位，不逐帧计算正弦或重建路径。水平位移按一个波长取模，连续重复；填充和轮廓均复用缓存路径。`main` 保留贝塞尔实现以便比较。水位的四秒重播仍会重置，这与水平波形的无缝循环不同。
