# Feature Tools 模块规划

本目录存放 `Koishi` 的具体独立小工具组件，遵循高内聚、低耦合原则。

## 推荐子包划分：
- `ruler/`: 屏幕尺子与高精度测距
- `converter/`: 货币汇率、度量衡单位转换
- `dev/`: Base64/URL 编解码、哈希校验、系统开发者调试
- `device/`: 硬件与传感器信息、手电筒、坏点检测
- `media/`: 调色板、取色器、文本字数统计分析

每个小工具为一个独立的 Composable 页面，通过强类型路由挂载至 `navigation/KoishiNavHost.kt`。
