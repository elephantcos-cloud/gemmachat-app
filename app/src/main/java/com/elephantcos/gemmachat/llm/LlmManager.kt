package com.elephantcos.gemmachat.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LlmManager private constructor() {

    private lateinit var llm: LlmInference
    private val responseBuffer = StringBuilder()

    @Volatile private var tokenCallback: ((String) -> Unit)? = null
    @Volatile private var doneCallback: ((String) -> Unit)? = null

    fun initialize(context: Context, modelPath: String) {
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(1024)
            .setTopK(40)
            .setTemperature(0.8f)
            .setRandomSeed(42)
            .setResultListener { partial, done ->
                if (partial != null) {
                    responseBuffer.append(partial)
                    tokenCallback?.invoke(partial)
                }
                if (done) {
                    doneCallback?.invoke(responseBuffer.toString())
                    tokenCallback = null
                    doneCallback = null
                }
            }
            .build()
        llm = LlmInference.createFromOptions(context, options)
    }

    fun buildPrompt(history: List<Pair<String, String>>): String = buildString {
        history.forEach { (role, text) ->
            append("<start_of_turn>$role\n$text<end_of_turn>\n")
        }
        append("<start_of_turn>model\n")
    }

    suspend fun generate(prompt: String, onToken: (String) -> Unit): String =
        suspendCancellableCoroutine { cont ->
            responseBuffer.clear()
            tokenCallback = onToken
            doneCallback = { result ->
                if (cont.isActive) cont.resume(result)
            }
            cont.invokeOnCancellation {
                tokenCallback = null
                doneCallback = null
            }
            try {
                llm.generateResponseAsync(prompt)
            } catch (e: Exception) {
                if (cont.isActive) cont.resumeWithException(e)
            }
        }

    fun close() {
        if (::llm.isInitialized) llm.close()
    }

    companion object {
        @Volatile private var INSTANCE: LlmManager? = null

        fun getInstance(context: Context, modelPath: String): LlmManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LlmManager().also {
                    it.initialize(context, modelPath)
                    INSTANCE = it
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
