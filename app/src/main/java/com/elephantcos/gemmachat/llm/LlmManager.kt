package com.elephantcos.gemmachat.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LlmManager private constructor(private val llm: LlmInference) {

    fun buildPrompt(history: List<Pair<String, String>>): String = buildString {
        history.forEach { (role, text) ->
            append("<start_of_turn>$role\n$text<end_of_turn>\n")
        }
        append("<start_of_turn>model\n")
    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        llm.generateResponse(prompt)
    }

    fun close() = llm.close()

    companion object {
        @Volatile private var INSTANCE: LlmManager? = null

        fun getInstance(context: Context, modelPath: String): LlmManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelPath)
                        .setMaxTokens(1024)
                        .setMaxTopK(40)
                        .build()
                    LlmManager(LlmInference.createFromOptions(context, options))
                        .also { INSTANCE = it }
                }
            }

        fun get(): LlmManager? = INSTANCE
        fun isLoaded(): Boolean = INSTANCE != null

        fun release() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
