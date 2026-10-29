# 桌面时钟（Desk Clock）

一个全屏的安卓桌面时钟，为三星 Galaxy Note 9 设计（Android 8.0+ 均可用）。

- 显示时、分、秒、日期、星期几
- 随手机方向自动切换横屏 / 竖屏布局（即使系统关闭了自动旋转也会跟随重力感应）
- 跟随系统设置的 12/24 小时制和语言（中文系统显示「2026年10月28日 / 星期三」）
- 全屏、常亮、黑色背景
- 点一下屏幕切换夜间暗光模式，再点一下恢复
- 每分钟把画面随机挪动几个像素，减轻 AMOLED 屏烧屏

## 获取 APK

最新版直接下载（固定链接，永久有效）：

https://github.com/lxu28973/desk-clock/releases/latest/download/desk-clock.apk

每次往 `main` 分支 push，GitHub Actions 都会自动编译并发布一个新的 Release，上面的链接始终指向最新版。历史版本在仓库的 **Releases** 页。

## 安装到 Note 9

1. 把 APK 传到手机（数据线、微信文件传输、网盘都行）
2. 在「我的文件」里点开 desk-clock.apk，按提示允许「安装未知应用」
3. 打开「桌面时钟」

也可以用 adb：`adb install desk-clock.apk`

## 本地编译

需要 JDK 17+ 和 Android SDK（API 35）：

```bash
./gradlew assembleDebug
```

输出在 `app/build/outputs/apk/debug/app-debug.apk`。
