package com.lingjing.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// 水墨丹青·国风修仙 色彩系统
// ============================================================

// --- 浅色主题 (Light Theme) ---

// 基础色
val PaperWhite = Color(0xFFF9F6F0)       // 纸白 - 全局背景
val InkBlack = Color(0xFF2C2C2A)         // 墨黑 - 正文、标题
val LightInk = Color(0xFF5A5A58)         // 淡墨 - 辅助文字
val Ochre = Color(0xFF9B6A42)            // 赭石 - 边框、描边

// 功能色
val FlowerCyan = Color(0xFF2E5A4B)       // 花青 - 正向操作、悟性
val Vermillion = Color(0xFFC23A22)       // 朱砂 - 警告、体魄
val Gamboge = Color(0xFFD9B45B)          // 藤黄 - 奖励、丹心
val StoneGreen = Color(0xFF4A7C6F)       // 石绿 - 神识
val DawnRed = Color(0xFFB83D3D)          // 曙红 - 真元

// 渐变用色
val FlowerCyanLight = Color(0xFF4A7C6F)
val FlowerCyanDark = Color(0xFF1A3A2F)
val VermillionLight = Color(0xFFE06050)
val GambogeLight = Color(0xFFF0D080)

// 状态色
val SuccessGreen = Color(0xFF4CAF50)
val WarningOrange = Color(0xFFFF9800)
val ErrorRed = Color(0xFFE53935)

// --- 深色主题 (Dark Theme) ---

val DarkBackground = Color(0xFF1A1815)
val DarkCard = Color(0xE62D2A27)         // #2D2A27 + 0.9透明度
val DarkText = Color(0xFFE6E2D8)
val DarkSurfaceVariant = Color(0xFF3D3A37)

// --- 属性专属色 ---
object AttributeColors {
    val Wisdom = FlowerCyan       // 灵根·悟性 - 花青
    val Physique = Vermillion     // 道体·体魄 - 朱砂
    val Perception = StoneGreen   // 神识·感知 - 石绿
    val Energy = DawnRed          // 真元·精力 - 曙红
    val Will = Gamboge            // 丹心·意志 - 藤黄
}

// --- 境界色 ---
object RealmColors {
    val QiRefining = Color(0xFF8B8B83)          // 炼气 - 灰
    val Foundation = Color(0xFF5B8C5A)          // 筑基 - 绿
    val GoldenCore = Color(0xFFD4A017)          // 金丹 - 金
    val NascentSoul = Color(0xFF7B4B9A)         // 元婴 - 紫
    val SpiritTransformation = Color(0xFFC23A22) // 化神 - 红
}

// --- 灵气浓度色 ---
val QiConcentrationColor = Color(0xFF87CEEB) // 天蓝
val QiConcentrationLow = Color(0xFFB0C4DE)   // 淡钢蓝
