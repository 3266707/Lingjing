package com.lingjing.feature.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * 桌面小组件 Provider
 */
class LingjingWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Phase 5: 使用 Glance 更新小组件内容
        // 显示今日待办任务前3项
    }

    override fun onEnabled(context: Context) {
        // 首次添加小组件时启动
    }

    override fun onDisabled(context: Context) {
        // 最后一个小组件被移除时停止
    }
}
