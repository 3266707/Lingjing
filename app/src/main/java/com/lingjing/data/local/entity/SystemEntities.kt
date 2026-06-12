package com.lingjing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingjing.core.common.constant.DbConstants

/**
 * 任务文本向量实体 - 用于RAG检索
 */
@Entity(
    tableName = DbConstants.TABLE_TASK_EMBEDDINGS,
    indices = [androidx.room.Index(value = ["task_id"])]
)
data class TaskEmbeddingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,

    @ColumnInfo(name = "task_id") val taskId: Long,
    @ColumnInfo(name = "task_name") val taskName: String,
    @ColumnInfo(name = "keywords") val keywords: String,

    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB)
    val embedding: ByteArray? = null,

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TaskEmbeddingEntity) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}

/**
 * 离线请求队列
 */
@Entity(tableName = DbConstants.TABLE_OFFLINE_REQUESTS)
data class OfflineRequestEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,

    @ColumnInfo(name = "request_type") val requestType: String,
    @ColumnInfo(name = "request_body") val requestBody: String,
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
    @ColumnInfo(name = "max_retries") val maxRetries: Int = 3,

    @ColumnInfo(name = "status") val status: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * 记忆条目 - 用于RAG长期记忆
 */
@Entity(tableName = DbConstants.TABLE_MEMORY_ENTRIES)
data class MemoryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,

    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "source_id") val sourceId: Long,

    @ColumnInfo(name = "keywords") val keywords: String,
    @ColumnInfo(name = "embedding", typeAffinity = ColumnInfo.BLOB)
    val embedding: ByteArray? = null,

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemoryEntryEntity) return false
        return id == other.id
    }
    override fun hashCode(): Int = id.hashCode()
}

/**
 * 真元交易记录
 */
@Entity(tableName = DbConstants.TABLE_ENERGY_TRANSACTIONS)
data class EnergyTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,

    @ColumnInfo(name = "delta") val delta: Int,
    @ColumnInfo(name = "reason_category") val reasonCategory: String,
    @ColumnInfo(name = "reason_id") val reasonId: Long? = null,
    @ColumnInfo(name = "balance_after") val balanceAfter: Int,

    @ColumnInfo(name = "description") val description: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * 休息任务实例
 */
@Entity(tableName = DbConstants.TABLE_REST_TASK_INSTANCES)
data class RestTaskInstanceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,

    @ColumnInfo(name = "task_id") val taskId: Long,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,

    @ColumnInfo(name = "started_at") val startedAt: Long? = null,
    @ColumnInfo(name = "scheduled_end") val scheduledEnd: Long? = null,
    @ColumnInfo(name = "status") val status: Int = 0,
    @ColumnInfo(name = "energy_restored") val energyRestored: Int = 0,

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
