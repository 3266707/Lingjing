package com.lingjing.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DeepSeek Chat Completion 请求
 */
data class ChatCompletionRequest(
    @SerializedName("model") val model: String = "deepseek-chat",
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("temperature") val temperature: Double = 0.3,
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    @SerializedName("response_format") val responseFormat: ResponseFormat? = null
)

data class ChatMessage(
    @SerializedName("role") val role: String,  // system, user, assistant
    @SerializedName("content") val content: String? = null  // nullable: API may return null on content-filter
)

data class ResponseFormat(
    @SerializedName("type") val type: String = "json_object"
)

/**
 * DeepSeek Chat Completion 响应
 */
data class ChatCompletionResponse(
    @SerializedName("id") val id: String?,
    @SerializedName("choices") val choices: List<ChatChoice>?,
    @SerializedName("usage") val usage: UsageInfo?,
    @SerializedName("error") val error: ApiError?
)

data class ChatChoice(
    @SerializedName("index") val index: Int?,
    @SerializedName("message") val message: ChatMessage? = null,
    @SerializedName("finish_reason") val finishReason: String? = null
)

data class UsageInfo(
    @SerializedName("prompt_tokens") val promptTokens: Int?,
    @SerializedName("completion_tokens") val completionTokens: Int?,
    @SerializedName("total_tokens") val totalTokens: Int?
)

data class ApiError(
    @SerializedName("message") val message: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("code") val code: String?
)

/**
 * AI 生成计划响应 JSON 结构
 */
data class PlanGenerationResponse(
    @SerializedName("title") val title: String = "",
    @SerializedName("summary") val summary: String = "",
    @SerializedName("tasks") val tasks: List<GeneratedTask> = emptyList(),
    @SerializedName("qi_concentration") val qiConcentration: Int = 10
)

data class GeneratedTask(
    @SerializedName("name") val name: String,
    @SerializedName("attribute") val attribute: String = "wisdom",
    @SerializedName("estimated_minutes") val estimatedMinutes: Int = 30,
    @SerializedName("difficulty") val difficulty: Int = 3,
    @SerializedName("base_exp") val baseExp: Int = 20,
    @SerializedName("is_rest") val isRest: Boolean = false,
    @SerializedName("repeat_rule") val repeatRule: String? = null
)

/**
 * AI 复盘分析响应
 */
data class ReviewAnalysisResponse(
    @SerializedName("emotion") val emotion: String = "neutral",
    @SerializedName("emotion_detail") val emotionDetail: String = "",
    @SerializedName("unfinished_reason") val unfinishedReason: String = "",
    @SerializedName("exp_balance_comment") val expBalanceComment: String = "",
    @SerializedName("attribute_balance_comment") val attributeBalanceComment: String = "",
    @SerializedName("difficulty_shift") val difficultyShift: Float = 0f,
    @SerializedName("tomorrow_suggestion") val tomorrowSuggestion: String = "",
    @SerializedName("strategy_advice") val strategyAdvice: String = ""
)
