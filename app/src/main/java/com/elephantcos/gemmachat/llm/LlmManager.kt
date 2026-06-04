package com.elephantcos.gemmachat.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LlmManager private constructor(private val llm: LlmInference) {

    companion object {
        @Volatile private var INSTANCE: LlmManager? = null

        fun getInstance(context: Context, modelPath: String): LlmManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelPath)
                        .setMaxTokens(1024)
                        .setTopK(40)
                        .setTemperature(0.8f)
                        .setRandomSeed(42)
                        .build()
                    LlmManager(LlmInference.createFromOptions(context, options))
                        .also { INSTANCE = it }
                }
            }

        fun get(): LlmManager? = INSTANCE
        fun isLoaded(): Boolean = INSTANCE != null

        fun release() {
            INSTANCE?.llm?.close()
            INSTANCE = null
        }
    }

    fun buildPrompt(history: List<Pair<String, String>>): String = buildString {
        history.forEach { (role, text) ->
            append("<start_of_turn>$role\n$text<end_of_turn>\n")
        }
        append("<start_of_turn>model\n")
    }

    suspend fun generate(prompt: String, onToken: (String) -> Unit): String =
        suspendCancellableCoroutine { cont ->
            val response = StringBuilder()
            try {
                llm.generateResponseAsync(prompt) { partial, done ->
                    if (!cont.isActive) return@generateResponseAsync
                    if (partial != null) {
                        response.append(partial)
                        onToken(partial)
                    }
                    if (done && cont.isActive) {
                        cont.resume(response.toString())
                    }
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resumeWithException(e)
            }
        }
}
