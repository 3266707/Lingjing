package com.lingjing.data.remote.api

import com.lingjing.data.remote.dto.ChatCompletionRequest
import com.lingjing.data.remote.dto.ChatCompletionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * DeepSeek Chat API 接口
 */
interface DeepSeekApiService {

    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}
