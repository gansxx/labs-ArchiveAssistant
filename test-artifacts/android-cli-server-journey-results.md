# Journey: Android CLI cloud server interaction

## Results

### Action: Verify that the ArchiveAssistant home screen is visible ✅

- **Commands**:
  - `android --sdk=/root/Android/Sdk layout --device=emulator-5554 --pretty --full`
  - `android --sdk=/root/Android/Sdk screen capture --device=emulator-5554 --output=test-artifacts/screenshots/01-home-cloud-loaded.png`
- **Screenshot**: [01-home-cloud-loaded.png](screenshots/01-home-cloud-loaded.png)
- **Comment**: 布局树包含“聚合拾遗”和六部主题；截图中“工 · 营造”为九篇，包含注入数据。

### Action: Tap the 工 · 营造 topic ✅

- **Commands**:
  - `adb -s emulator-5554 shell input tap 540 2355`
  - `android --sdk=/root/Android/Sdk layout --device=emulator-5554 --pretty --full`
- **Comment**: 使用布局树给出的可点击主题卡中心坐标；进入后标题为“工 · 营造”。

### Action: Swipe up slowly on the item grid ✅

- **Commands**:
  - `adb -s emulator-5554 shell input swipe 293 2200 293 1700 700`
- **Comment**: 在布局树标记为可滚动的条目网格内执行 700 ms 慢速滑动。

### Action: Verify that 云端联调验证文档 is visible ✅

- **Commands**:
  - `android --sdk=/root/Android/Sdk layout --device=emulator-5554 --flat --full`
  - `android --sdk=/root/Android/Sdk screen capture --device=emulator-5554 --output=test-artifacts/screenshots/02-cloud-item-visible.png`
- **Screenshot**: [02-cloud-item-visible.png](screenshots/02-cloud-item-visible.png)
- **Comment**: 布局树定位到文本及中心坐标 `[249,2203]`；截图底部完整显示云端条目卡。

### Action: Tap 云端联调验证文档 ✅

- **Commands**:
  - `adb -s emulator-5554 shell input tap 249 2203`
- **Comment**: 点击后打开文章奏章阅读器；首次进入时按布局树坐标关闭系统全屏提示。

### Action: Swipe left slowly on the opened 奏章 to reveal its contents ✅

- **Commands**:
  - `adb -s emulator-5554 shell input swipe 800 1200 200 1200 900`
- **Comment**: 在自绘奏章视图内执行 900 ms 慢速左滑，翻到正文摘要页。

### Action: Verify that 来自 PostgreSQL 的 Android CLI 端到端测试数据 is visible ✅

- **Commands**:
  - `android --sdk=/root/Android/Sdk screen capture --device=emulator-5554 --output=test-artifacts/screenshots/04-cloud-item-content.png`
- **Screenshot**: [04-cloud-item-content.png](screenshots/04-cloud-item-content.png)
- **Comment**: 奏章是自绘 Canvas，语义布局只暴露“收起”；已按技能要求视觉检查 PNG，标题和 PostgreSQL 测试摘要均清晰可见。

## Server 与数据库证据

- server 记录到同一 Android 客户端连接上的多组 `GET /snapshot` 与 `PUT /snapshot`，状态均为 HTTP 200。
- PostgreSQL 反查结果：`android-cli-e2e|6|6|50|t`，表示修订号 6、6 个主题、50 个合并后条目，且测试条目仍存在。
- 初始种子为 6 个主题和 2 个测试条目；App 保留云端种子后合并内置示例并通过 `PUT` 原子写回，验证了双向交互。

## 环境说明

- Android CLI：`1.0.16261425`
- AVD：`medium_phone` / `emulator-5554`
- Emulator：`37.1.11`
- Emulator 37.1.11 在 WSL 硬件渲染路径中触发 `RenderThread` SIGSEGV；测试构建使用
  `ARCHIVE_TEST_HARDWARE_ACCELERATED=false` 走软件 Canvas。该开关默认值为 `true`，release 不受影响。
- 所有截图均已逐张视觉核验；`test-artifacts/screenshots/` 已由 `.gitignore` 排除。
