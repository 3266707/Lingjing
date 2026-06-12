package com.lingjing.service.rag

import android.util.Log
import com.lingjing.core.common.constant.AppConstants
import com.lingjing.core.database.dao.SystemDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RAG 检索助手
 * 使用 TF-IDF 关键词提取 + 余弦相似度进行本地检索
 * Phase 5 将升级为 ONNX Embedding 模型
 */
@Singleton
class RAGHelper @Inject constructor(
    private val systemDao: SystemDao
) {

    /**
     * 检索相似历史任务
     * @param query 用户输入文本
     * @param topK 返回数量
     * @return 相似任务上下文文本
     */
    suspend fun retrieveSimilarHistory(query: String, topK: Int = AppConstants.RAG_TOP_K): String {
        if (query.isBlank()) return ""

        try {
            val queryKeywords = extractKeywords(query)
            if (queryKeywords.isEmpty()) return ""

            val allEmbeddings = systemDao.getAllTaskEmbeddings(500)
            if (allEmbeddings.isEmpty()) return ""

            // 计算关键词匹配分数（简化版 TF-IDF 相似度）
            val scored = allEmbeddings.map { embedding ->
                val taskKeywords = extractKeywords(embedding.keywords)
                val score = calculateKeywordSimilarity(queryKeywords, taskKeywords)
                embedding to score
            }.filter { it.second > AppConstants.RAG_SIMILARITY_THRESHOLD }
             .sortedByDescending { it.second }
             .take(topK)

            if (scored.isEmpty()) return ""

            // 构建上下文
            val contextBuilder = StringBuilder()
            scored.forEach { (embedding, score) ->
                contextBuilder.appendLine("- ${embedding.taskName} (相似度: ${"%.0f".format(score * 100)}%)")
            }

            return contextBuilder.toString()
        } catch (e: Exception) {
            Log.w("RAGHelper", "Failed to retrieve similar history", e)
            return ""
        }
    }

    /**
     * 提取关键词（简化版 TF-IDF）
     */
    fun extractKeywords(text: String): List<String> {
        // 移除 JSON 标记和标点，提取中文词
        val cleaned = text
            .replace(Regex("[\\[\\]\"{}\\,:0-9]"), " ")
            .trim()

        // 简单分词：按空格和常见分隔符拆分
        return cleaned
            .split(Regex("[\\s，。！？、；：\"\"''（）《》\\[\\]【】\\-]+"))
            .filter { it.length >= 2 }
            .distinct()
            .take(20)
    }

    /**
     * 计算关键词相似度（Jaccard 系数 + TF 加权）
     */
    fun calculateKeywordSimilarity(queryWords: List<String>, targetWords: List<String>): Float {
        if (queryWords.isEmpty() || targetWords.isEmpty()) return 0f

        val querySet = queryWords.toSet()
        val targetSet = targetWords.toSet()

        val intersection = querySet.intersect(targetSet).size
        val union = querySet.union(targetSet).size

        if (union == 0) return 0f

        // Jaccard 相似度
        val jaccard = intersection.toFloat() / union

        // TF 加权：命中词在目标中的占比
        val tfWeight = if (targetSet.isNotEmpty()) {
            intersection.toFloat() / targetSet.size
        } else 0f

        return (jaccard * 0.6f + tfWeight * 0.4f)
    }

    /**
     * 保存任务关键词嵌入
     */
    suspend fun saveTaskKeywords(taskId: Long, taskName: String) {
        val keywords = extractKeywords(taskName)
        if (keywords.isNotEmpty()) {
            systemDao.insertTaskEmbedding(
                com.lingjing.data.local.entity.TaskEmbeddingEntity(
                    taskId = taskId,
                    taskName = taskName,
                    keywords = keywords.joinToString(",")
                )
            )
        }
    }
}
