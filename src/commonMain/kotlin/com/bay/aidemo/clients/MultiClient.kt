package com.bay.aidemo.clients

import com.bay.aiclient.AiClient
import com.bay.aiclient.api.bedrock.BedrockClient
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object MultiClient {
    private val providers: MutableMap<AiClient.Type, AiClient> = mutableMapOf()
    private val currentModels: MutableMap<AiClient.Type, String> =
        mutableMapOf(
            AiClient.Type.BEDROCK to "anthropic.claude-3-5-sonnet-20240620-v1:0",
            AiClient.Type.COHERE to "command-r",
            AiClient.Type.GOOGLE to "models/gemini-1.5-pro-001",
            AiClient.Type.OPEN_AI to "gpt-4o",
            AiClient.Type.AI21 to "jamba-1.5-large",
            AiClient.Type.MISTRAL to "mistral-large-latest",
        )

    private var models: Map<AiClient.Type, List<String>> = emptyMap()

    fun getCurrentModels(): Map<AiClient.Type, String> = currentModels.toMap()

    suspend fun getModels(refreshState: Boolean): Map<AiClient.Type, List<String>> {
        if (models.isEmpty() || refreshState) {
            models =
                providers.mapValues { it.value.models() }.mapValues {
                    if (it.value.isFailure) {
                        emptyList()
                    } else {
                        it.value
                            .getOrNull()
                            ?.models
                            ?.map { model -> model.id } ?: emptyList()
                    }
                }
        }
        return models
    }

    fun setCurrentModel(
        provider: AiClient.Type,
        model: String,
    ) {
        currentModels[provider] = model
        providers[provider]?.defaultModel = model
    }

    suspend fun chatResponses(
        prompt: String,
        systemPrompt: String,
    ): Map<AiClient.Type, AiResponse<String>> {
        val jobs =
            coroutineScope {
                providers.mapValues {
                    async {
                        it.value.generateText {
                            this.prompt = prompt
                            this.systemInstructions = systemPrompt
                        }
                    }
                }
            }
        return jobs.mapValues { it.value.await() }.mapValues {
            if (it.value.isFailure) {
                AiResponse(errorMessage = it.value.exceptionOrNull().toString())
            } else {
                val response = it.value.getOrNull()
                AiResponse(response = response?.response, tokenCount = response?.usage?.totalToken ?: 0)
            }
        }
    }

    fun setKeys(keys: Map<AiClient.Type, String>) {
        keys.forEach { (provider, key) ->
            if (provider == AiClient.Type.BEDROCK) {
                providers[provider] =
                    AiClient.get<_, BedrockClient.Builder>(BedrockClient::class) {
                        val parts = key.split("%")
                        check(parts.size == 4)
                        credentials = BedrockClient.Credentials(parts[0], false, parts[1], parts[2], parts[3])
                        defaultModel = currentModels[provider]
                    }
            } else {
                providers[provider] =
                    AiClient.get(provider) {
                        apiAky = key
                        defaultModel = currentModels[provider]
                        httpLogLevel = LogLevel.ALL
                    }
            }
        }
    }
}
