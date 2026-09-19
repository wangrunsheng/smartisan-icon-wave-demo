# Preview media

`bezier-vs-sine.gif` is a 5-second, 20-fps crop of the live Android
`CurveComparisonView` on a Smartisan DE106 (Android 8.1). Both sides use one
clock and the original fitted parameters. Left is Bézier, right is sine.
The waterline is fixed to isolate shape and horizontal motion. GIF replay
restarts the recording; it is not evidence of a discontinuity in either curve.

`library-demo.png` is a screenshot crop of the live `WaveRevealLayout` examples.
No user photo or Chrome artwork is included in these previews.

To reproduce: build the app, open “通用组件与曲线对比”, then record the screen:

```sh
adb shell screenrecord --time-limit 7 --bit-rate 6000000 /sdcard/waves.mp4
adb pull /sdcard/waves.mp4
ffmpeg -ss 1 -t 5 -i waves.mp4 \
  -vf 'crop=970:440:55:275,fps=20,scale=800:-1:flags=lanczos,split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4' \
  bezier-vs-sine.gif
```

Crop coordinates apply to this phone's 1080×2242 screen and current display
settings; use the visible comparison bounds on other devices.
