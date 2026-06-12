package com.lingjing.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val LingjingShapes = Shapes(
    // 按钮圆角 24dp
    extraLarge = RoundedCornerShape(24.dp),
    // 卡片圆角 16dp
    large = RoundedCornerShape(16.dp),
    // 中等圆角 12dp
    medium = RoundedCornerShape(12.dp),
    // 小圆角 8dp
    small = RoundedCornerShape(8.dp),
    // 极小圆角 4dp
    extraSmall = RoundedCornerShape(4.dp)
)

// 进度条形状
val ProgressBarShape = RoundedCornerShape(4.dp)

// 经验条形状
val ExpBarShape = RoundedCornerShape(4.dp)

// 真元条形状
val EnergyBarShape = RoundedCornerShape(6.dp)
