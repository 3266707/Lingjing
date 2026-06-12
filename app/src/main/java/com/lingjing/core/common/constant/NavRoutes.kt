package com.lingjing.core.common.constant

import com.lingjing.R

/**
 * 导航路由定义
 */
object NavRoutes {
    // 主导航
    const val HOME = "home"
    const val TODAY_BOARD = "today_board"
    const val ATTRIBUTE_PANEL = "attribute_panel"
    const val REVIEW = "review"
    const val ACHIEVEMENTS = "achievements"
    const val SETTINGS = "settings"

    // 计划相关
    const val PLAN_GENERATE = "plan/generate"
    const val PLAN_EDIT = "plan/edit/{planId}"
    const val PLAN_DETAIL = "plan/detail/{planId}"
    const val PLAN_HISTORY = "plan/history"

    // 长期目标
    const val GOAL_LIST = "goals"
    const val GOAL_DETAIL = "goal/detail/{goalId}"
    const val GOAL_CREATE = "goal/create"

    // 复盘
    const val REVIEW_WRITE = "review/write"
    const val REVIEW_HISTORY = "review/history"

    // 统计
    const val STATS_DASHBOARD = "stats"

    // 备份
    const val BACKUP = "backup"

    // 感知
    const val PERCEPTION_DASHBOARD = "perception"

    // AI人设
    const val PERSONA = "persona"

    // 首次引导
    const val ONBOARDING = "onboarding"

    // Deep Link
    const val DEEP_LINK_PLAN = "lingjing://plans/detail/{planId}"
    const val DEEP_LINK_REVIEW = "lingjing://review"

    // 参数名
    const val ARG_PLAN_ID = "planId"
    const val ARG_GOAL_ID = "goalId"

    // 辅助函数
    fun planEdit(planId: Long) = "plan/edit/$planId"
    fun planDetail(planId: Long) = "plan/detail/$planId"
    fun goalDetail(goalId: Long) = "goal/detail/$goalId"
}

/**
 * 底部导航项
 */
enum class BottomNavItem(
    val route: String,
    val labelResId: Int,  // references R.string
    val iconName: String  // Material icon name for reference
) {
    TODAY(NavRoutes.TODAY_BOARD, R.string.nav_today, "today"),
    ATTRIBUTES(NavRoutes.ATTRIBUTE_PANEL, R.string.nav_attributes, "auto_awesome"),
    REVIEW(NavRoutes.REVIEW, R.string.nav_review, "edit_note"),
    ACHIEVEMENTS(NavRoutes.ACHIEVEMENTS, R.string.nav_achievements, "emoji_events"),
    SETTINGS(NavRoutes.SETTINGS, R.string.nav_settings, "settings")
}
