# 灵境 (Lingjing) v0.1.5 — by YuRan

<div align="center">

**AI 驱动的修仙主题日常任务管理与个人成长 App**

⛰️ 以修仙为名，行自律之实 ⛰️

[![Android](https://img.shields.io/badge/Android-8.0%2B-green)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-purple)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose-BOM%202024.04-blue)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

</div>

---

## 📱 简介

灵境是一款以东方修仙/武侠为主题的**游戏化日常任务管理 App**。你将扮演一名修仙者，通过完成日常任务获取经验值，提升五维属性（灵根·悟性、道体·体魄、神识·感知、真元·精力、丹心·意志），突破修为境界。

核心 AI 引擎由 **DeepSeek** 大模型驱动，支持智能拆解任务、修炼复盘分析、个性化人设切换。

灵感来源：感谢 [LifeUp](https://github.com/Ayagikei/LifeUp) 开源项目。

---

## ✨ 核心功能

### 🎮 游戏化修炼系统
- **五大属性**：悟性 / 体魄 / 神识 / 精力 / 意志
- **修为境界**：炼气期 → 筑基期 → 金丹期 → 元婴期 → 化神期
- **等级公式**：经验 = 基础分 × 难度系数 × 完成质量 × 灵气浓度
- **真元系统**：完成任务消耗真元，枯竭时经验减半

### 🤖 AI 智能辅助
- **计划生成**：自然语言输入 → AI 拆解为具体修炼任务
- **复盘分析**：情绪分析 + 修炼建议 + 策略推荐
- **四大道人设**：理性引导 / 热血鼓励 / 毒舌吐槽 / 温柔治愈
- **多服务商**：DeepSeek / OpenAI 兼容 / 自定义 API

### 📋 任务管理 (LifeUp 风格)
- **彩色完成圆圈**：五属性对应独特配色
- **日期选择器**：可指定任务执行日期
- **重复规则**：每日/每周/每月自动生成
- **长按撤回**：已完成任务长按即可恢复
- **完成动画**：划线文字 + 半透明效果

### 🔮 其他功能
- 五维属性雷达图
- 修炼札记（情绪记录 + AI 分析）
- 机缘录成就系统
- 桌面小部件
- 数据备份/恢复
- 深色/浅色/跟随系统主题

---

## 📸 界面预览

| 今日看板 | 属性面板 | 修炼札记 |
|:---:|:---:|:---:|
| *完成任务获取经验* | *五维属性雷达图* | *AI修炼复盘分析* |

| 添加事项 | 设置 | 机缘录 |
|:---:|:---:|:---:|
| *日期选择+重复规则* | *AI服务商+人设配置* | *成就解锁进度* |

<img width="526" height="892" alt="d30fb3dd6bbf81632f586e72737b123e" src="https://github.com/user-attachments/assets/c4acdc59-290a-4bf8-a43c-c46763a49b67" />
<img width="524" height="920" alt="d9e94f6f5f97e033208daf276c1647c2" src="https://github.com/user-attachments/assets/88a9fc54-c8b6-44bd-adb5-061eafbbd456" />
<img width="515" height="909" alt="e0cc4336d7f0cc22a575d3593d22ebed" src="https://github.com/user-attachments/assets/5586d3a3-6c27-462a-8462-3010d4664708" />
<img width="524" height="916" alt="18746702aa2519bf902645cbc43a3b9a" src="https://github.com/user-attachments/assets/77ea0a8a-3b3c-4365-bf54-023754a9e351" />
<img width="527" height="926" alt="d4fe14cab409303fd132646291d39ae9" src="https://github.com/user-attachments/assets/ac3c2193-5d07-4153-a6f9-f11122db81f7" />

---

## 🛠️ 技术架构

| 层级 | 技术栈 |
|------|--------|
| **UI** | Jetpack Compose + Material3 |
| **架构** | Clean Architecture + MVVM |
| **DI** | Dagger Hilt |
| **数据库** | Room (WAL 模式) |
| **网络** | Retrofit + OkHttp |
| **AI** | DeepSeek Chat API (OpenAI 兼容) |
| **图表** | Compose Canvas 自定义 |
| **加密** | EncryptedSharedPreferences |
| **后台** | WorkManager |

---

## 📦 快速开始

### 环境要求
- Android Studio Hedgehog+ 
- JDK 17
- Android SDK 34+
- Gradle 8.10+

### 编译运行

```bash
git clone https://github.com/your-username/lingjing.git
cd lingjing
./gradlew assembleDebug
```

### 配置 DeepSeek API Key

1. 在 [DeepSeek 开放平台](https://platform.deepseek.com) 注册获取 API Key
2. 打开 App → 设置 → AI 服务配置 → 填入 Key → 保存

---

## 🗺️ 路线图

- [x] AI 计划生成
- [x] 修仙属性与境界系统
- [x] 修炼札记与复盘分析
- [x] LifeUp 风格任务卡片
- [x] 日期选择 + 重复规则
- [x] 多渠道 AI 服务商支持
- [ ] 长期目标系统
- [ ] RAG 检索增强生成
- [ ] 数据云端同步
- [ ] iOS 版本

---

## 📄 开源协议

Apache 2.0 © 2025 YuRan

---

<div align="center">

**愿你在此间修炼，终成大道。**

</div>
