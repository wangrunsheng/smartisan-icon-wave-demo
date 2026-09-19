# Wave Reveal：参数与接入

第一版为源码模块库，尚未发布 Maven Central，不要使用虚构的 Maven 坐标。

- `wave-core`：Kotlin Multiplatform `commonMain`，无 Android / UI 依赖。当前只配置并验证 JVM target，供 Android 适配使用；尚未产出 iOS、Web 等平台制品。
- `wave-view`：Android `WaveRevealLayout`，最低 API 26，使用正弦波。支持普通 View、ImageView、ViewGroup。
- Compose / Compose Multiplatform：**尚未实现 Modifier 或 Composable 适配器**。核心公式可复用，不等于当前已有跨平台 UI 控件。

## 接入已有 Android 工程

将 `wave-core` 和 `wave-view` 两个目录拷入工程并在 settings 中 include：

```kotlin
include(":wave-core", ":wave-view")
```

根工程需要声明 `com.android.library` 和 `org.jetbrains.kotlin.multiplatform` 插件；本仓库验证版本分别为 9.2.0 与 2.3.21，JDK 17、Gradle 9.4.1、compileSdk 36。使用其他工具版本时需自行验证。应用模块依赖：

```kotlin
dependencies { implementation(project(":wave-view")) }
```

库与 Chrome 素材完全无关。无需拷贝 demo 的图片、图标、录屏或 Activity。

## 最简单的白底、浅蓝后波、蓝色前波

```kotlin
import android.graphics.Color
import android.view.View
import com.russell.wave.core.WaveLayerSpec
import com.russell.wave.core.WaveRevealSpec
import com.russell.wave.view.WaveRevealLayout

val reveal = WaveRevealLayout(context).apply {
    cornerRadiusPx = 16f * resources.displayMetrics.density
    setBackdrop(View(context).apply { setBackgroundColor(Color.WHITE) })
    setContent(View(context).apply { setBackgroundColor(0xFF00B9F2.toInt()) })
    spec = WaveRevealSpec(
        waves = listOf(
            WaveLayerSpec(
                amplitudeFraction = .032f,
                periodSeconds = .70,
                phaseRadians = .77,
                opacity = .30f,
            ),
            WaveLayerSpec(
                amplitudeFraction = .021f,
                periodSeconds = .85,
                phaseRadians = -.29,
                opacity = 1f,
            ),
        ),
        baseOpacity = 0f,
        contentOpacity = 1f,
    )
    progress = .55f
}
```

后波单独覆盖的区域显示 30% 蓝色与白底的混合，形成浅蓝色；前波覆盖处为完整蓝色。

这里是**一份蓝色内容、两张波形蒙版**。默认不创建两份内容，不重复注册交互或无障碍节点。

## 换成照片或日期卡片

仅替换内容：

```kotlin
reveal.setContent(ImageView(context).apply {
    scaleType = ImageView.ScaleType.CENTER_CROP
    setImageBitmap(photo)
})
reveal.setOverlay(TextView(context).apply {
    text = "19"
    textSize = 24f
})
```

也可以传入未挂到父容器上的 LinearLayout / FrameLayout 等 ViewGroup。图片加载、裁剪模式、文字颜色由调用方负责。三个内容槽都填满容器，内部布局由传入的内容自己控制。

- `setBackdrop(view)`：底层内容，不受波形影响，透明度可用 `view.alpha` 调整。
- `setContent(view)`：唯一的揭示内容；整组透明度用 `spec.contentOpacity` 调整。
- `setOverlay(view)`：固定标签或角标，不受波形影响，可用 `view.alpha` 调整。
- 底层与覆盖层传 `null` 可移除。不要直接用 `addView` 添加第四个槽。

外层圆角裁切所有槽；对象只有一个父容器，不会自动从别的布局抢走。绘制蒙版只改变视觉，不改变点击命中范围；如需“未出现时不可点击”，请由业务设置 `isEnabled`、可访问性等。

若要三份**不同内容、不同颜色**各自使用不同波形，可在普通 FrameLayout 中堆叠多个 WaveRevealLayout，各自只配置一条波、不同的 content，背景仅放在最底层。这与“一份内容、多个蒙版”不同，成本也更高，第一版不额外提供 WaveStack DSL。

## 公式与单位

屏幕坐标 y 轴向下，宽高为 W/H，短边 S=min(W,H)。每条波：

```text
A = amplitudeFraction × S
λ = wavelengthFraction × W
D = levelOffsetFraction × S
Y(x,t,p) = B(p) + D + A × sin(2πx/λ + direction × 2πt/T + φ)
```

`direction=+1` 向左，`-1` 向右。`T` 是秒/周期；`φ` 是弧度；水面以下被覆盖。共享相同 wavelengthFraction 就是等波长双波，周期不同就会产生持续变化的相位差。

```text
margin = max(A + abs(D)) + 1px
B(p) = (H + margin) × (1-p) - margin × p
```

因此 p=0 时所有波形都在容器下方，p=1 时都在上方，不会在边缘残留波峰。进度被限制到 0…1，它是水位进度，不是精确的面积占比。

最终蒙版使用 source-over 覆盖率：

```text
M = contentOpacity × [1 - (1-baseOpacity) × ∏(1-opacity_i × mask_i)]
最终像素 alpha = 原内容 alpha × M
```

两层各 30% 的覆盖重叠是 51%，不是 60%。p=1 保证波形**全覆盖**，不覆盖用户设置的透明度；若希望最终完全显示，应像默认预设那样有一层 opacity=1 且 contentOpacity=1。p=0 时仍会保留用户指定的 baseOpacity。

非有限参数会报错。当前支持 1–8 条波、振幅 0–1 个短边、波长 0.1–10 个宽度、水位偏移 ±1 个短边，避免不受控路径规模。

## 播放与缓存

```kotlin
reveal.progress = importProgress // 业务进度，不由波动时钟决定
reveal.isRunning = false         // 暂停水平方向流动
reveal.timeSeconds = 2.5         // 外部时钟 / 同步多个卡片
reveal.speed = .25               // 四分之一速度
```

自动时钟只在已挂载、可见、正在播放且进度处于 (0,1) 时请求下一帧。隐藏或暂停期间不累计时间；不内置录屏 Demo 的四秒水位重置。宿主可在 onPause 中停止，在 onResume 恢复。列表滚动时容器仅判断 View 可见性，不精确检测是否移出屏幕；大量卡片由列表宿主停止屏外实例。

几何只在尺寸/spec 变化时重新生成。采样密度依据曲线振幅和 0.15px 插值误差预算确定，按整周期生成足够长的路径；每帧取模平移，不重算正弦或分配路径。

内容是正常的实时 View 树，每帧绘制一次，然后用离屏层进行蒙版合成。这不是把内容永久截图，也不意味着整套效果每帧完全没有 GPU 开销。大量复杂卡片仍需做设备性能评估，目前未提供 FPS / 功耗基准。

## 支持范围

支持纯色、透明图片、普通自定义 View、嵌套 ViewGroup；动态内容会随正常 invalidate 更新。需要两层离屏合成来正确处理整组 alpha 和透明像素。

不支持 SurfaceView、独立视频/地图渲染表面、跨窗口内容。Compose/CMP 需原生绘制适配，后续适配应复用相同 core 语义，不复制多份 Composable 内容树。第一版不提供任意 Path 外形接口，仅提供矩形/圆角矩形。

示例照片由系统文档选择器读取，无网络上传。当前演示 Activity 旋转后恢复进度/播放状态，但不恢复用户临时选取的照片；实际产品应由自己的状态层保存照片 URI。

## 验证与产物

```sh
./gradlew :wave-core:jvmTest :wave-view:assembleRelease :wave-view:lintDebug
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Android AAR：`wave-view/build/outputs/aar/wave-view-release.aar`。
核心 JVM JAR：`wave-core/build/libs/` 下的 JAR。

手动分发 AAR 时还需提供 core JVM JAR 和 Kotlin 标准库；建议用源码模块依赖，由 Gradle 自动处理依赖。不应把 AAR 误称为没有任何依赖的单文件库。
