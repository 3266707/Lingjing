package com.lingjing.service.network

import com.lingjing.core.database.dao.SystemDao
import com.lingjing.data.local.entity.OfflineRequestEntity
import com.lingjing.data.remote.api.PlanApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 离线请求管理器
 * 无网络时将 AI 请求存入本地队列，网络恢复后自动重试
 */
@Singleton
class OfflineRequestManager @Inject constructor(
    private val systemDao: SystemDao,
    private val planApiService: PlanApiService,
    private val networkMonitor: NetworkMonitor
) {

    companion object {
        const val TYPE_PLAN_GENERATION = "plan_generation"
        const val TYPE_REVIEW_ANALYSIS = "review_analysis"
        const val MAX_RETRIES = 3

        const val STATUS_PENDING = 0
        const val STATUS_PROCESSING = 1
        const val STATUS_DONE = 2
        const val STATUS_FAILED = 3
    }

    /**
     * 入队离线请求
     */
    suspend fun enqueue(requestType: String, requestBody: String): Long {
        return systemDao.insertOfflineRequest(
            OfflineRequestEntity(
                requestType = requestType,
                requestBody = requestBody,
                retryCount = 0,
                maxRetries = MAX_RETRIES,
                status = STATUS_PENDING
            )
        )
    }

    /**
     * 处理所有待处理的离线请求
     */
    suspend fun processPendingRequests() {
        if (!networkMonitor.isCurrentlyConnected()) return

        val pending = systemDao.getPendingOfflineRequests()
        for (request in pending) {
            if (request.retryCount >= request.maxRetries) {
                systemDao.updateOfflineRequestStatus(request.id, STATUS_FAILED)
                continue
            }

            try {
                // 标记为处理中（不增加重试计数）
                if (request.status == STATUS_PENDING) {
                    // 首次处理
                }
                systemDao.updateOfflineRequestStatus(request.id, STATUS_PROCESSING)

                when (request.requestType) {
                    TYPE_PLAN_GENERATION -> {
                        val result = planApiService.generateDailyPlan(request.requestBody)
                        if (result.isSuccess) {
                            systemDao.updateOfflineRequestStatus(request.id, STATUS_DONE)
                        } else {
                            systemDao.updateOfflineRequestStatus(request.id, STATUS_PENDING)
                        }
                    }
                    // 其他类型暂不处理
                    else -> {
                        systemDao.updateOfflineRequestStatus(request.id, STATUS_DONE)
                    }
                }
            } catch (e: Exception) {
                // 重试计数由 updateOfflineRequestStatus 中的 retry_count + 1 处理
                systemDao.updateOfflineRequestStatus(request.id, STATUS_PENDING)
            }
        }

        // 清理已完成
        systemDao.deleteCompletedOfflineRequests()
    }
}
