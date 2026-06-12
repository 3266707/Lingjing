package com.lingjing.data.remote.api

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lingjing.core.common.constant.AppConstants
import com.lingjing.core.common.constant.DbConstants
import com.lingjing.data.remote.dto.*
import com.lingjing.domain.model.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom AI persona defined by the user.
 */
data class CustomPersona(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val prompt: String
)

/**
 * 计划生成 API 服务
 * 封装 DeepSeek API 调用，构建 Prompt，解析响应
 */
@Singleton
class PlanApiService @Inject constructor(
    private val deepSeekApiService: DeepSeekApiService,
    private val securePrefs: SharedPreferences,
    private val gson: Gson,
    private val okHttpClient: OkHttpClient
) {

    companion object {
        private const val PREF_API_KEY = "deepseek_api_key"
        private const val PREF_PERSONA = "current_persona"
        private const val PREF_PERSONA_AUTO_SWITCH = "persona_auto_switch"
        private const val PREF_CUSTOM_PERSONAS = "custom_personas"
        private const val CUSTOM_PERSONA_PREFIX = "custom_"
        private const val PREF_AI_PROVIDER = "ai_provider"
        private const val PREF_AI_BASE_URL = "ai_base_url"
        private const val PREF_AI_MODEL = "ai_model"

        /** AI 提供商预设配置 */
        data class ProviderConfig(val baseUrl: String, val model: String)

        val PROVIDER_DEFAULTS = mapOf(
            "deepseek" to ProviderConfig(AppConstants.DEEPSEEK_BASE_URL, "deepseek-chat"),
            "openai" to ProviderConfig("https://api.openai.com/v1", "gpt-4o-mini"),
            "custom" to ProviderConfig("", "")
        )

        private val PROVIDER_LABELS = mapOf(
            "deepseek" to "DeepSeek (默认)",
            "openai" to "OpenAI 兼容",
            "custom" to "自定义"
        )
        fun getProviderLabel(provider: String): String =
            PROVIDER_LABELS[provider] ?: provider

        // Prompt 模板
        private val PERSONA_PROMPTS = mapOf(
            DbConstants.PERSONA_RATIONAL to "你是一位冷静理性的修仙导师，用数据和逻辑引导修士成长。语气平和、精准。",
            DbConstants.PERSONA_PASSIONATE to "你是一位热血洋溢的修仙师兄，充满元气和中二气息。用燃爆的方式鼓励修士！",
            DbConstants.PERSONA_TSUNDERE to "你是一位毒舌但心软的修仙师姐，嘴上不饶人但实际很关心修士。用幽默激将法激励。",
            DbConstants.PERSONA_GENTLE to "你是一位温柔包容的修仙前辈，像春风一样温暖。用治愈的语气陪伴修士。"
        )

        private val PLAN_GENERATION_PROMPT = """
%s

【任务】根据用户的自然语言输入，将目标拆解为具体的可执行任务列表。

【输出格式】必须返回严格JSON，不要有额外说明：
{
  "title": "计划标题（简短）",
  "summary": "一句话总结本日计划要点",
  "tasks": [
    {
      "name": "任务名称",
      "attribute": "wisdom|physique|perception|energy|will",
      "estimated_minutes": 30,
      "difficulty": 3,
      "base_exp": 20,
      "is_rest": false,
      "repeat_rule": null
    }
  ],
  "qi_concentration": 10
}

【五维属性说明】
- wisdom（灵根·悟性）：学习、思考、阅读、写作、研究类任务
- physique（道体·体魄）：运动、健身、跑步、体能训练类任务
- perception（神识·感知）：冥想、瑜伽、情绪管理、觉察类任务
- energy（真元·精力）：消耗真元的执行类任务（默认）
- will（丹心·意志）：坚持、习惯养成、自律类任务

【规则】
1. difficulty 范围 1-5（1=轻松，5=地狱）
2. estimated_minutes 合理估计（10-180分钟）
3. base_exp = estimated_minutes / 2 + difficulty × 5（10-50之间）
4. is_rest=true 的任务不消耗真元，完成后恢复真元
5. 每个任务最多关联一个属性
6. qi_concentration 范围 0-50，默认10
7. 任务数量不超过8个
8. 按推荐执行顺序排列任务
        """.trimIndent()
    }

    fun getApiKey(): String? {
        return securePrefs.getString(PREF_API_KEY, null)
    }

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    fun setApiKey(key: String) {
        securePrefs.edit().putString(PREF_API_KEY, key).apply()
    }

    fun getActivePersona(): String {
        return securePrefs.getString(PREF_PERSONA, DbConstants.PERSONA_RATIONAL)
            ?: DbConstants.PERSONA_RATIONAL
    }

    fun setPersona(persona: String) {
        securePrefs.edit().putString(PREF_PERSONA, persona).apply()
    }

    // ---- Auto-switch ----

    fun getAutoSwitchEnabled(): Boolean {
        return securePrefs.getBoolean(PREF_PERSONA_AUTO_SWITCH, false)
    }

    fun setAutoSwitchEnabled(enabled: Boolean) {
        securePrefs.edit().putBoolean(PREF_PERSONA_AUTO_SWITCH, enabled).apply()
    }

    /**
     * Automatically select persona based on detected emotional state.
     * Only takes effect when auto-switch is enabled.
     */
    fun autoSelectPersona(emotion: String) {
        if (!getAutoSwitchEnabled()) return
        val newPersona = when (emotion) {
            DbConstants.EMOTION_NEGATIVE -> DbConstants.PERSONA_GENTLE
            DbConstants.EMOTION_POSITIVE -> DbConstants.PERSONA_PASSIONATE
            else -> DbConstants.PERSONA_RATIONAL
        }
        setPersona(newPersona)
    }

    // ---- Custom Personas ----

    fun getCustomPersonas(): List<CustomPersona> {
        val json = securePrefs.getString(PREF_CUSTOM_PERSONAS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CustomPersona>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addCustomPersona(name: String, prompt: String) {
        val personas = getCustomPersonas().toMutableList()
        personas.add(CustomPersona(name = name, prompt = prompt))
        securePrefs.edit().putString(PREF_CUSTOM_PERSONAS, gson.toJson(personas)).apply()
    }

    fun deleteCustomPersona(id: String) {
        val personas = getCustomPersonas().toMutableList()
        val removed = personas.filter { it.id != id }
        securePrefs.edit().putString(PREF_CUSTOM_PERSONAS, gson.toJson(removed)).apply()
        // If the deleted persona was the active one, reset to default
        val activePersona = getActivePersona()
        if (activePersona == "$CUSTOM_PERSONA_PREFIX$id") {
            setPersona(DbConstants.PERSONA_RATIONAL)
        }
    }

    /**
     * Resolve the active persona prompt. For built-in personas uses the preset map;
     * for custom personas looks up by ID suffix.
     */
    private fun resolvePersonaPrompt(persona: String): String {
        if (persona.startsWith(CUSTOM_PERSONA_PREFIX)) {
            val id = persona.removePrefix(CUSTOM_PERSONA_PREFIX)
            return getCustomPersonas().find { it.id == id }?.prompt
                ?: PERSONA_PROMPTS[DbConstants.PERSONA_RATIONAL]!!
        }
        return PERSONA_PROMPTS[persona] ?: PERSONA_PROMPTS[DbConstants.PERSONA_RATIONAL]!!
    }

    // ---- AI Provider Config ----

    fun getAiProvider(): String =
        securePrefs.getString(PREF_AI_PROVIDER, "deepseek") ?: "deepseek"

    fun setAiProvider(provider: String) {
        securePrefs.edit().putString(PREF_AI_PROVIDER, provider).apply()
        invalidateApiServiceCache()
    }

    /** Returns the saved base URL, or null if never set. */
    fun getAiBaseUrl(): String? = securePrefs.getString(PREF_AI_BASE_URL, null)

    /** Returns the effective base URL: saved value, or provider default, or DeepSeek fallback. */
    fun getEffectiveBaseUrl(): String {
        val saved = getAiBaseUrl()
        if (!saved.isNullOrBlank()) return saved
        return PROVIDER_DEFAULTS[getAiProvider()]?.baseUrl ?: AppConstants.DEEPSEEK_BASE_URL
    }

    fun setAiBaseUrl(url: String) {
        securePrefs.edit().putString(PREF_AI_BASE_URL, url).apply()
        invalidateApiServiceCache()
    }

    /** Returns the saved model, or null if never set. */
    fun getAiModel(): String? = securePrefs.getString(PREF_AI_MODEL, null)

    /** Returns the effective model: saved value, or provider default, or "deepseek-chat". */
    fun getEffectiveModel(): String {
        val saved = getAiModel()
        if (!saved.isNullOrBlank()) return saved
        return PROVIDER_DEFAULTS[getAiProvider()]?.model ?: "deepseek-chat"
    }

    fun setAiModel(model: String) {
        securePrefs.edit().putString(PREF_AI_MODEL, model).apply()
    }

    // ---- Dynamic API Service ----

    @Volatile
    private var customApiService: DeepSeekApiService? = null
    @Volatile
    private var customApiServiceBaseUrl: String? = null

    private fun invalidateApiServiceCache() {
        customApiService = null
        customApiServiceBaseUrl = null
    }

    /**
     * Returns the appropriate [DeepSeekApiService] based on the configured base URL.
     * Uses the injected service for the default DeepSeek URL; builds a new Retrofit
     * instance lazily for any other URL.
     */
    private fun getActiveApiService(): DeepSeekApiService {
        val baseUrl = getEffectiveBaseUrl()
        if (baseUrl == AppConstants.DEEPSEEK_BASE_URL) {
            return deepSeekApiService
        }
        if (customApiService != null && customApiServiceBaseUrl == baseUrl) {
            return customApiService!!
        }
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val service = retrofit.create(DeepSeekApiService::class.java)
        customApiService = service
        customApiServiceBaseUrl = baseUrl
        return service
    }

    /**
     * 生成单日计划
     */
    suspend fun generateDailyPlan(
        userInput: String,
        ragContext: String = "",
        historyContext: String = ""
    ): Result<PlanGenerationResponse> {
        val apiKey = getApiKey()
            ?: return Result.failure(Exception("请先在设置中配置 DeepSeek API Key"))

        val persona = getActivePersona()
        val personaPrompt = resolvePersonaPrompt(persona)

        val systemPrompt = PLAN_GENERATION_PROMPT.format(personaPrompt)

        // 构建上下文
        val contextParts = mutableListOf<String>()
        if (ragContext.isNotBlank()) {
            contextParts.add("【历史相似任务参考】\n$ragContext")
        }
        if (historyContext.isNotBlank()) {
            contextParts.add("【近期完成情况】\n$historyContext")
        }

        val fullUserPrompt = buildString {
            if (contextParts.isNotEmpty()) {
                appendLine(contextParts.joinToString("\n\n"))
                appendLine()
            }
            appendLine("<user_input>")
            append(userInput)
            appendLine("</user_input>")
        }

        return try {
            val service = getActiveApiService()
            val model = getEffectiveModel()
            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", fullUserPrompt)
                ),
                temperature = AppConstants.TEMPERATURE,
                maxTokens = AppConstants.MAX_TOKENS,
                responseFormat = ResponseFormat("json_object")
            )

            val response = service.createChatCompletion(
                apiKey = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                val error = body?.error
                if (error != null) {
                    Result.failure(Exception(error.message ?: "API 返回错误"))
                } else {
                    val content = body?.choices?.firstOrNull()?.message?.content
                        ?: return Result.failure(Exception("API 返回空内容"))

                    val planResponse = parsePlanResponse(content)
                    Result.success(planResponse)
                }
            } else {
                Result.failure(Exception("API 请求失败: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    /**
     * 解析 AI 返回的计划 JSON
     */
    private fun parsePlanResponse(jsonStr: String): PlanGenerationResponse {
        return try {
            val cleanJson = extractJson(jsonStr)
            gson.fromJson(cleanJson, PlanGenerationResponse::class.java)
        } catch (e: Exception) {
            // Fallback: try regex parsing instead of returning empty
            try {
                fallbackParse(jsonStr)
            } catch (e2: Exception) {
                throw Exception("AI返回格式无法解析，请重试")
            }
        }
    }

    private fun extractJson(text: String): String {
        // 大小写不敏感的代码块标记移除
        var cleaned = text.trim()
        val codeBlockRegex = Regex("""```[jJ][sS][oO][nN]?\s*""")
        cleaned = codeBlockRegex.replaceFirst(cleaned, "")
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trim()
        }
        return cleaned.trim()
    }

    private fun fallbackParse(text: String): PlanGenerationResponse {
        val title = Regex("\"title\"\\s*:\\s*\"([^\"]*)\"").find(text)?.groupValues?.get(1)
            ?: "今日修炼计划"

        val qiMatch = Regex("\"qi_concentration\"\\s*:\\s*(\\d+)").find(text)
        val qi = qiMatch?.groupValues?.get(1)?.toIntOrNull() ?: 10

        // Attempt to extract individual task objects from the raw text
        val tasks = mutableListOf<GeneratedTask>()
        val taskBlockRegex = Regex("""\{[^{}]*"name"\s*:\s*"([^"]*)"[^{}]*"attribute"\s*:\s*"([^"]*)"[^{}]*\}""")
        for (match in taskBlockRegex.findAll(text)) {
            val name = match.groupValues[1]
            val attr = match.groupValues[2]
            val estMin = Regex("\"estimated_minutes\"\\s*:\\s*(\\d+)").find(match.value)?.groupValues?.get(1)?.toIntOrNull() ?: 30
            val diff = Regex("\"difficulty\"\\s*:\\s*(\\d+)").find(match.value)?.groupValues?.get(1)?.toIntOrNull() ?: 3
            val exp = Regex("\"base_exp\"\\s*:\\s*(\\d+)").find(match.value)?.groupValues?.get(1)?.toIntOrNull() ?: 20
            tasks.add(GeneratedTask(name = name, attribute = attr, estimatedMinutes = estMin, difficulty = diff, baseExp = exp))
        }

        return PlanGenerationResponse(
            title = title,
            summary = "",
            tasks = tasks,
            qiConcentration = qi
        )
    }

    /**
     * AI 复盘分析
     */
    suspend fun analyzeReview(
        todaySummary: String,
        reviewText: String,
        emotion: String? = null
    ): Result<ReviewAnalysisResponse> {
        val apiKey = getApiKey()
            ?: return Result.failure(Exception("请先配置 API Key"))

        val systemPrompt = """
你是修仙世界的复盘导师。根据修士当天完成情况和札记，分析情绪、找出问题、给出调整建议。

返回严格JSON：
{
  "emotion": "positive|neutral|negative",
  "emotion_detail": "情绪分析详情",
  "unfinished_reason": "未完成任务的主要原因",
  "exp_balance_comment": "经验值分配合理性评价",
  "attribute_balance_comment": "属性成长均衡性分析",
  "difficulty_shift": 0.0,
  "tomorrow_suggestion": "明日计划调整建议",
  "strategy_advice": "具体的策略建议"
}
difficulty_shift 范围 -0.5 ~ 0.5，负数降低难度，正数增加难度。
        """.trimIndent()

        return try {
            val service = getActiveApiService()
            val model = getEffectiveModel()
            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    ChatMessage("user", buildString {
                        append("今日总结：$todaySummary\n\n修炼札记：$reviewText")
                        if (!emotion.isNullOrBlank()) {
                            append("\n\n情绪状态：$emotion")
                        }
                    })
                ),
                temperature = 0.5,
                maxTokens = 2048,
                responseFormat = ResponseFormat("json_object")
            )

            val response = service.createChatCompletion("Bearer $apiKey", request)
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                    ?: return Result.failure(Exception("API 返回空内容"))
                val analysis = gson.fromJson(extractJson(content), ReviewAnalysisResponse::class.java)
                Result.success(analysis)
            } else {
                Result.failure(Exception("API 请求失败"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }
}
