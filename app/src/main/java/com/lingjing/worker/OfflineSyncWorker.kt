package com.lingjing.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lingjing.service.network.NetworkMonitor
import com.lingjing.service.network.OfflineRequestManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * 离线请求同步 Worker
 * 检查网络状态，若有网络则处理离线队列中的待发送 AI 请求；
 * 处理完成后调度下一次检查（1小时后）；若无网络则使用
 * NetworkType.CONNECTED 约束调度，待网络恢复后自动触发。
 */
@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val offlineRequestManager: OfflineRequestManager,
    private val networkMonitor: NetworkMonitor
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "OfflineSyncWorker"

        /** 两次常规轮询之间的间隔 */
        private const val NEXT_CHECK_DELAY_HOURS = 1L
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "开始离线请求同步")

            // 1. 检查网络连通性
            val isConnected = networkMonitor.isCurrentlyConnected()

            if (isConnected) {
                Log.d(TAG, "网络可用，处理离线请求队列")

                // 2. 有网络 —— 处理所有待处理的离线请求
                offlineRequestManager.processPendingRequests()

                Log.d(TAG, "离线请求处理完成，${NEXT_CHECK_DELAY_HOURS}小时后再次检查")

                // 3. 调度下一次轮询检查（1小时后）
                scheduleNextCheck(delayHours = NEXT_CHECK_DELAY_HOURS)

                Result.success()

            } else {
                Log.d(TAG, "网络不可用，调度带网络约束的重试")

                // 4. 无网络 —— 调度一个仅在 NetworkType.CONNECTED 时触发的新实例
                scheduleNetworkConstrainedRetry()

                // 当前执行成功结束（重试已委托给新 WorkRequest）
                Result.success()
            }

        } catch (e: Exception) {
            Log.e(TAG, "离线请求同步失败", e)
            Result.retry()
        }
    }

    /**
     * 调度一次延迟执行的 [OfflineSyncWorker]
     */
    private fun scheduleNextCheck(delayHours: Long) {
        val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setInitialDelay(delayHours, TimeUnit.HOURS)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(request)
    }

    /**
     * 调度一个带有 [NetworkType.CONNECTED] 约束的 [OfflineSyncWorker]，
     * 网络恢复时自动执行。
     */
    private fun scheduleNetworkConstrainedRetry() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(constraints)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(applicationContext).enqueue(request)
    }
}
