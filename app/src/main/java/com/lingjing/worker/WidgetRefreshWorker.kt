package com.lingjing.worker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lingjing.R
import com.lingjing.domain.model.TaskStatus
import com.lingjing.domain.repository.PlanRepository
import com.lingjing.feature.widget.LingjingWidgetReceiver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 桌面小组件刷新 Worker
 * 按固定间隔刷新 LingjingWidgetReceiver 小组件，展示今日待办修炼任务（最多前3项）。
 * 无任务时显示占位文案 "今日暂无修炼计划"。
 */
@HiltWorker
class WidgetRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val planRepository: PlanRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "WidgetRefreshWorker"
        private const val MAX_TASK_DISPLAY = 3
        private const val EMPTY_STATE_TEXT = "今日暂无修炼计划" // 今日暂无修炼计划

        /** widget_lingjing.xml 中定义的 TextView ID 数组 */
        private val TASK_VIEW_IDS = intArrayOf(
            R.id.widget_task_1,
            R.id.widget_task_2,
            R.id.widget_task_3
        )
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "开始刷新桌面小组件")

            // 1. 获取今日所有任务并筛选出待办项
            val todayTasks = planRepository.getTodayTasks()
            val pendingTasks = todayTasks
                .filter { it.status == TaskStatus.PENDING }
                .take(MAX_TASK_DISPLAY)

            Log.d(TAG, "今日待办任务数: ${pendingTasks.size}/${todayTasks.size}")

            // 2. 构建 RemoteViews
            val views = RemoteViews(
                applicationContext.packageName,
                R.layout.widget_lingjing
            )

            // 3. 填充任务文本
            if (pendingTasks.isEmpty()) {
                // 无任务 —— 显示空状态占位
                views.setTextViewText(R.id.widget_task_1, EMPTY_STATE_TEXT)
                // 清空其余行
                for (i in 1 until TASK_VIEW_IDS.size) {
                    views.setTextViewText(TASK_VIEW_IDS[i], "")
                }
                Log.d(TAG, "无待办任务，显示空状态")
            } else {
                // 填充最多 3 个任务
                for (i in TASK_VIEW_IDS.indices) {
                    if (i < pendingTasks.size) {
                        val task = pendingTasks[i]
                        val displayText = buildTaskDisplayText(task)
                        views.setTextViewText(TASK_VIEW_IDS[i], displayText)
                    } else {
                        views.setTextViewText(TASK_VIEW_IDS[i], "")
                    }
                }
            }

            // 4. 通过 AppWidgetManager 推送到所有已添加的 LingjingWidgetReceiver 实例
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val componentName = ComponentName(
                applicationContext,
                LingjingWidgetReceiver::class.java
            )
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (widgetIds.isNotEmpty()) {
                appWidgetManager.updateAppWidget(widgetIds, views)
                Log.d(TAG, "已刷新 ${widgetIds.size} 个小组件实例")
            } else {
                Log.d(TAG, "当前无已添加的小组件实例")
            }

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "小组件刷新失败", e)
            Result.retry()
        }
    }

    /**
     * 构建任务单行展示文本
     * 格式: "序号. 任务名称 [预估分钟数min]"
     */
    private fun buildTaskDisplayText(task: com.lingjing.domain.model.PlanTask): String {
        val prefix = when (task.priority) {
            com.lingjing.domain.model.Priority.HIGH -> "★ "       // ★ 高优先级标记
            else -> ""
        }
        val timeInfo = if (task.estimatedMinutes > 0) {
            " [${task.estimatedMinutes}min]"
        } else {
            ""
        }
        return "$prefix${task.name}$timeInfo"
    }
}
