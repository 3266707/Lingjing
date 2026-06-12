package com.lingjing.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lingjing.R
import com.lingjing.core.common.constant.NavRoutes
import com.lingjing.feature.achievement.AchievementScreen
import com.lingjing.feature.attribute.dashboard.AttributeDashboardScreen
import com.lingjing.feature.plan.detail.PlanDetailScreen
import com.lingjing.feature.plan.edit.PlanEditScreen
import com.lingjing.feature.plan.generation.PlanGenerationScreen
import com.lingjing.feature.plan.history.PlanHistoryScreen
import com.lingjing.feature.review.ReviewScreen
import com.lingjing.feature.settings.SettingsScreen
import com.lingjing.feature.stats.StatsDashboardScreen
import com.lingjing.ui.components.TodayBoard

/**
 * 底部导航栏数据类
 */
data class BottomNavItem(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = NavRoutes.TODAY_BOARD,
        labelResId = R.string.nav_today,
        selectedIcon = Icons.AutoMirrored.Filled.Assignment,
        unselectedIcon = Icons.AutoMirrored.Outlined.Assignment
    ),
    BottomNavItem(
        route = NavRoutes.ATTRIBUTE_PANEL,
        labelResId = R.string.nav_attributes,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    ),
    BottomNavItem(
        route = NavRoutes.REVIEW,
        labelResId = R.string.nav_review,
        selectedIcon = Icons.Filled.EditNote,
        unselectedIcon = Icons.Outlined.EditNote
    ),
    BottomNavItem(
        route = NavRoutes.ACHIEVEMENTS,
        labelResId = R.string.nav_achievements,
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents
    ),
    BottomNavItem(
        route = NavRoutes.SETTINGS,
        labelResId = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

/**
 * 主导航图 - 包含底部导航和全屏路由
 */
@Composable
fun LingjingNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 判断是否应该显示底部导航栏
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = stringResource(item.labelResId)
                                )
                            },
                            label = { Text(stringResource(item.labelResId)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.TODAY_BOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 底部导航 Tab
            composable(NavRoutes.TODAY_BOARD) {
                TodayBoard(navController = navController)
            }
            composable(NavRoutes.ATTRIBUTE_PANEL) {
                AttributeDashboardScreen(navController = navController)
            }
            composable(NavRoutes.REVIEW) {
                ReviewScreen(navController = navController)
            }
            composable(NavRoutes.ACHIEVEMENTS) {
                AchievementScreen(navController = navController)
            }
            composable(NavRoutes.SETTINGS) {
                SettingsScreen(navController = navController)
            }

            // 计划相关
            composable(NavRoutes.PLAN_GENERATE) {
                PlanGenerationScreen(navController = navController)
            }
            composable(NavRoutes.PLAN_EDIT) { backStackEntry ->
                val planId = backStackEntry.arguments?.getString(NavRoutes.ARG_PLAN_ID)?.toLongOrNull()
                PlanEditScreen(navController = navController, planId = planId)
            }
            composable(NavRoutes.PLAN_DETAIL) { backStackEntry ->
                val planId = backStackEntry.arguments?.getString(NavRoutes.ARG_PLAN_ID)?.toLongOrNull()
                PlanDetailScreen(navController = navController, planId = planId)
            }
            composable(NavRoutes.PLAN_HISTORY) {
                PlanHistoryScreen(navController = navController)
            }

            // 复盘详情
            composable(NavRoutes.REVIEW_WRITE) {
                ReviewScreen(navController = navController, fullScreen = true)
            }

            // 统计
            composable(NavRoutes.STATS_DASHBOARD) {
                StatsDashboardScreen(navController = navController)
            }
        }
    }
}
