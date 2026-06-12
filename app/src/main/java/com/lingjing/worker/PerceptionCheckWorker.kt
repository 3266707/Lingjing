package com.lingjing.worker

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lingjing.LingjingApplication
import com.lingjing.domain.repository.DailyStateRepository
import com.lingjing.feature.perception.engine.StrategySuggestionEngine
import com.lingjing.feature.perception.engine.UserModeDetector
import com.lingjing.feature.perception.engine.UserModeDetector.DetectionResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 用户状态感知检测 Worker
 * 每4小时检查一次用户模式，基于近期修炼数据判断用户状态（超神/平稳/挣扎/宕机），
 * 并在模式变化时生成策略建议并通过通知推送。
 */
@HiltWorker
class PerceptionCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dailyStateRepository: DailyStateRepository,
    private val userModeDetector: UserModeDetector,
    private val strategySuggestionEngine: StrategySuggestionEngine
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "PerceptionCheckWorker"
        private const val NOTIFICATION_ID_ENCOURAGE = 2001
        private const val RECENT_DAYS = 7
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "开始检测用户模式")

            // 1. 加载最近 RECENT_DAYS 天的每日状态
            val recentStates = dailyStateRepository.getRecentDailyStates(RECENT_DAYS)

            if (recentStates.isEmpty()) {
                Log.d(TAG, "无历史状态数据，跳过检测")
                return Result.success()
            }

            val latestState = recentStates.first()

            // 2. 运行 UserModeDetector.detect() 获取当前模式
            val detectionResult: DetectionResult = userModeDetector.detect(recentStates)

            Log.d(TAG, "检测到模式: ${detectionResult.mode.displayName}, " +
                    "完成率: ${"%.1f".format(detectionResult.completionRate * 100)}%, " +
                    "趋势: ${detectionResult.trend}")

            // 3. 生成策略建议（引擎内部根据模式产出差异化建议）
            val suggestions = strategySuggestionEngine.generateSuggestions(
                mode = detectionResult.mode,
                dailyState = latestState,
                recentStates = recentStates
            )

            // 4. 如果有建议，发送通知展示最重要的条目（priority 最小 = 最优先）
            if (suggestions.isNotEmpty()) {
                val topSuggestion = suggestions.first()
                Log.d(TAG, "发送鼓舞通知: ${topSuggestion.title}")
                sendEncourageNotification(
                    title = topSuggestion.title,
                    content = topSuggestion.description
                )
            } else {
                Log.d(TAG, "当前无需要通知的策略建议")
            }

            // 5. 更新每日状态的灵气浓度（基于近期趋势补偿/衰减）
            dailyStateRepository.calculateAndUpdateQiConcentration(latestState.date)

            Log.d(TAG, "用户模式检测完成: ${detectionResult.mode.displayName}")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "用户模式检测失败", e)
            Result.retry()
        }
    }

    /**
     * 使用 CHANNEL_ENCOURAGE 渠道发送道心鼓舞通知
     */
    private fun sendEncourageNotification(title: String, content: String) {
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(
            applicationContext,
            LingjingApplication.CHANNEL_ENCOURAGE
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID_ENCOURAGE, notification)
    }
}
