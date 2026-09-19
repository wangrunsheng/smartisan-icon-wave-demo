# Wave Reveal：参数与接入

源码模块库，尚未发布 Maven Central，不使用虚构的 Maven 坐标。

- `wave-core`：公共公式和参数，无 UI 依赖；JVM、iOS arm64 / 模拟器 arm64、Wasm targets。
- `wave-view`：传统 Android View / ViewGroup 适配，最低 API 26。
- `wave-compose`：同一个公共 Composable，供 Android Compose 和 CMP 使用；Android、JVM、iOS、Wasm targets。

## Compose / CMP 接入

将 `wave-core`、`wave-compose` 纳入工程，调用方依赖 `project(":wave-compose")`。需要根工程同版本的 Kotlin Multiplatform、Compose Multiplatform、Compose compiler 与 Android KMP library 插件，具体配置见仓库 Gradle 文件。Android 应用也应用 Compose compiler 插件并启用 Compose。

### 可直接编译的最小示例

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.russell.wave.compose.WaveReveal

@Composable
fun BlueCard(progress: Float) {
    WaveReveal(
        progress = progress,
        modifier = Modifier.size(180.dp),
        backdrop = { Box(Modifier.fillMaxSize().background(Color.White)) },
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF00B9F2)))
    }
}
```

普通场景只需 `WaveReveal(progress) { ExistingCard() }`。组件内部自动管理独立时钟，进度到 0 / 1 时暂停；暂停期间不累计时间，恢复时接着原相位移动。`running=false` 暂停，`speed=.25` 四分之一速度，`speed=0` 停止。进度只控制水位，库不会自行增加进度。

调用方仍应把宿主活跃/可见状态接到 running；自动时钟不承诺判断所有平台的后台状态或列表屏外状态。离开 composition 后帧循环自动取消。

### 直接应用到现有组件

```kotlin
import com.russell.wave.compose.waveReveal

// 在 @Composable 中构造；ExistingCard 代表宿主已有组件。
ExistingCard(modifier = Modifier.waveReveal(progress = progress, running = playing))
```

Modifier 不添加布局节点，不强制尺寸、形状或背景，不改变点击/无障碍语义。它作用于后续绘制内容，顺序很重要：

```kotlin
Modifier.background(Color.White).waveReveal(progress) // 白底不参与蒙版
Modifier.waveReveal(progress).background(Color.Blue) // 蓝底参与蒙版
Modifier.clip(RoundedCornerShape(18.dp)).waveReveal(progress) // 外层圆角裁切
```

若只想让卡片内部照片波动，应该把 Modifier 放在照片上，固定标签由原布局照常绘制。需要三个槽时使用容器。容器的 content 决定自然尺寸，或者由 modifier 指定；backdrop / overlay 匹配容器大小，shape 裁切所有槽位。

### 修改默认参数

```kotlin
import androidx.compose.runtime.remember
import com.russell.wave.core.WaveRevealDefaults

val spec = remember {
    WaveRevealDefaults.DoubleWave.copy(
        waves = listOf(
            WaveRevealDefaults.RearWave.copy(opacity = .2f, periodSeconds = 1.0),
            WaveRevealDefaults.FrontWave,
        ),
        contentOpacity = .9f,
    )
}
WaveReveal(progress = progress, spec = spec) { ExistingCard() }
```

`DoubleWave` 是共享默认配置；`RearWave`、`FrontWave` 是默认两条波。copy 返回新值，不修改预设；传入的列表会防御性复制。相同内容的配置具有值相等语义。组合内需要自定义配置时建议 remember，参数来自状态时把该状态作为 remember 的 key。

| 参数 | 单位 / 范围 | 默认值与含义 |
| --- | --- | --- |
| amplitudeFraction | 短边比例，0..1 | 后 .032 / 前 .021；控制波高 |
| wavelengthFraction | 宽度比例，.1..10 | 两条均 1；越大波越宽 |
| periodSeconds | 秒，有限且 >0 | 后 .70 / 前 .85；越大流动越慢 |
| phaseRadians | 有限弧度 | 后 .77 / 前 -.29；控制初始相位 |
| opacity | 0..1 | 后 .30 / 前 1；单条蒙版覆盖率 |
| levelOffsetFraction | 短边比例，-1..1 | 0；正值使该波水面下移 |
| direction | Left / Right | Left；水平移动方向 |
| baseOpacity | 0..1 | 0；未被波覆盖部分的基础可见度 |
| contentOpacity | 0..1 | 1；整组内容透明度 |

支持 1–8 条波。无效配置会抛出注明参数名称的 IllegalArgumentException。背景和内容颜色由宿主决定，浅蓝色来自透明蓝色内容与白底混合，不是内置第三种颜色。

### 外部时钟：同步、预览与精确控制

```kotlin
import com.russell.wave.compose.rememberWaveTime

val time = rememberWaveTime(running = playing, speed = .5)
WaveReveal(progress = progress, timeSeconds = { time.value }) { ExistingCard() }
// 同一个 time 也能传给 Modifier；这些入口不会启动自己的时钟。
ExistingCard(modifier = Modifier.waveReveal(progress, timeSeconds = { time.value }))
```

自动时钟和外部时钟是两个重载。外部时钟入口不接收 running / speed，避免两套控制冲突；共享时钟是否在全部端点时停止，由宿主决定。可传 `{ 2.5 }` 固定相位做截图。timeSeconds 单位为秒，必须返回有限值，只在 draw 阶段读取，不会让内容树每帧重组。

不同颜色/内容的独立波层可叠放多个 WaveReveal 并共享一个 time。所有入口使用相同的正弦和 alpha 公式，不重复组合内容。透明内容保持透明，视觉隐藏不自动禁用点击或无障碍；平台原生视图、独立视频/地图表面不在保证范围内。

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

## 换成照片或组合卡片

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

若要三份**不同内容、不同颜色**各自使用不同波形，可在普通 FrameLayout 中堆叠多个 WaveRevealLayout，各自只配置一条波、不同的 content，背景仅放在最底层。这与“一份内容、多个蒙版”不同，成本也更高，当前不额外提供 WaveStack DSL。

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

View 不支持 SurfaceView、独立视频/地图渲染表面、跨窗口内容，外形为矩形或圆角矩形。Compose 使用公共绘图 API 和 Shape，支持普通 Composable 内容树；没有重复组合内容，也不依赖 Android Canvas。

示例照片由系统文档选择器读取，无网络上传。当前演示 Activity 旋转后恢复进度/播放状态，但不恢复用户临时选取的照片；实际产品应由自己的状态层保存照片 URI。

## 验证与产物

```sh
./gradlew :wave-core:jvmTest :wave-view:assembleRelease :wave-view:lintDebug
./gradlew :app:assembleDebug :app:lintDebug
```

Android AAR：`wave-view/build/outputs/aar/wave-view-release.aar`。
核心 JVM JAR：`wave-core/build/libs/` 下的 JAR。

手动分发 AAR 时还需提供 core JVM JAR 和 Kotlin 标准库；建议用源码模块依赖，由 Gradle 自动处理依赖。不应把 AAR 误称为没有任何依赖的单文件库。

## 平台验证

本次验证（2026-09-19）：

| 目标 | 验证结果 |
| --- | --- |
| Android | APK 构建、Lint 通过；Smartisan DE106 / Android 8.1 真机检查 View 与 Compose、0% / 100%、圆角、固定覆盖层、暂停恢复和前后台切换 |
| JVM / macOS arm64 | 6 项 core 测试、5 项 Compose 渲染/时钟测试通过 |
| iOS arm64、iOS 模拟器 arm64 | 公共组件编译通过；未运行 iOS 宿主应用 |
| Web / Wasm | 公共组件编译通过；未在浏览器运行宿主应用 |

Compose 像素测试覆盖蒙版 source-over、内容自身透明度、整组 alpha、背景/覆盖层、圆角裁切，以及时间改变确实重绘但不重新组合内容。真机暂停前后卡片区域像素一致。照片选择流程尚未用实际照片验证。

配置/编译 target 不等于已经在设备上验证；未宣称不同后端像素完全一致，也没有未经测量的 FPS / 功耗结论。

```sh
./gradlew :wave-core:jvmTest :wave-compose:jvmTest
./gradlew :wave-compose:compileKotlinWasmJs
./gradlew :wave-compose:compileKotlinIosArm64 :wave-compose:compileKotlinIosSimulatorArm64
```

JVM 测试使用无窗口的 Skia surface；iOS 编译需要 macOS / Xcode。
