package com.lingjing.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 字体家族
// Noto Serif SC - 衬线体，用于标题/境界
// Noto Sans SC - 无衬线体，用于正文/任务
// JetBrains Mono - 等宽字体，用于数字/经验值

val SerifFamily = FontFamily.Default   // fallback to system; ideally Noto Serif SC
val SansFamily = FontFamily.Default    // fallback to system; ideally Noto Sans SC
val MonoFamily = FontFamily.Monospace  // JetBrains Mono

val LingjingTypography = Typography(
    // 境界大标题 - 24sp Semibold Serif
    headlineLarge = TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.W600,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    // 页面主标题 - 20sp Semibold Serif
    headlineMedium = TextStyle(
        fontFamily = SerifFamily,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // 副标题 - 18sp Medium
    headlineSmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    // 卡片标题 - 16sp Medium Sans
    titleLarge = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    // 任务名称 - 16sp Medium Sans
    titleMedium = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    // 小标题 - 14sp Medium
    titleSmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // 正文 - 16sp Regular
    bodyLarge = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // 辅助说明 - 12sp Regular Sans
    bodySmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // 经验数字 - 14sp Medium Mono
    labelLarge = TextStyle(
        fontFamily = MonoFamily,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // 按钮文字 - 14sp Medium
    labelMedium = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    // 小标签 - 11sp Regular
    labelSmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.W400,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
