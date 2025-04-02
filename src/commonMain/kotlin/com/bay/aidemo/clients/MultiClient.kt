package com.bay.aidemo.clients

import com.bay.aiclient.AiClient
import com.bay.aiclient.api.azureopenai.AzureOpenAiClient
import com.bay.aiclient.api.bedrock.BedrockClient
import com.bay.aiclient.api.yandex.YandexClient
import io.ktor.client.plugins.logging.LogLevel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object MultiClient {
    private val providers: MutableMap<AiClient.Type, AiClient> = mutableMapOf()
    private val currentModels: MutableMap<AiClient.Type, String> =
        mutableMapOf(
            AiClient.Type.AI21 to "jamba-large",
            AiClient.Type.ANTHROPIC to "claude-3-5-sonnet-20241022",
            AiClient.Type.AZURE_OPENAI to "gpt-4o-mini",
            AiClient.Type.BEDROCK to "anthropic.claude-3-5-sonnet-20240620-v1:0",
            AiClient.Type.CEREBRAS to "llama-3.3-70b",
            AiClient.Type.COHERE to "command-r",
            AiClient.Type.DEEP_SEEK to "deepseek-chat",
            AiClient.Type.GOOGLE to "models/gemini-2.0-pro-exp",
            AiClient.Type.GROK to "grok-2-1212",
            AiClient.Type.MISTRAL to "mistral-large-latest",
            AiClient.Type.OPEN_AI to "o1-mini",
            AiClient.Type.SAMBA_NOVA to "Meta-Llama-3.3-70B-Instruct",
            AiClient.Type.TOGETHER_AI to "meta-llama/Llama-3.3-70B-Instruct-Turbo-Free",
            AiClient.Type.YANDEX to "yandexgpt",
        )

    private var models: Map<AiClient.Type, List<String>> = emptyMap()

    fun getCurrentModels(): Map<AiClient.Type, String> = currentModels.toMap()

    suspend fun getModels(refreshState: Boolean): Map<AiClient.Type, List<String>> {
        if (models.isEmpty() || refreshState) {
            models =
                providers
                    .mapValues { it.value.models() }
                    .mapValues {
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
        return jobs
            .mapValues { it.value.await() }
            .mapValues {
                if (it.value.isFailure) {
                    AiResponse(errorMessage = it.value.exceptionOrNull().toString())
                } else {
                    val response = it.value.getOrNull()
                    AiResponse(
                        response = response?.response,
                        tokenCount = response?.usage?.totalTokens ?: 0,
                    )
                }
            }
    }

    fun setKeys(keys: Map<AiClient.Type, String>) {
        keys.forEach { (provider, key) ->
            when (provider) {
                AiClient.Type.BEDROCK -> {
                    providers[provider] =
                        AiClient.get<_, BedrockClient.Builder>(BedrockClient::class) {
                            val parts = key.split("%")
                            check(parts.size == 4)
                            credentials =
                                BedrockClient.Credentials(parts[0], false, parts[1], parts[2], parts[3])
                            defaultModel = currentModels[provider]
                        }
                }
                AiClient.Type.AZURE_OPENAI -> {
                    providers[provider] =
                        AiClient.get<_, AzureOpenAiClient.Builder>(AzureOpenAiClient::class) {
                            val parts = key.split("%")
                            check(parts.size == 2)
                            resourceName = parts[0]
                            apiKey = parts[1]
                            defaultModel = currentModels[provider]
                            httpLogLevel = LogLevel.ALL
                        }
                }
                AiClient.Type.YANDEX -> {
                    providers[provider] =
                        AiClient.get<_, YandexClient.Builder>(YandexClient::class) {
                            val parts = key.split("%")
                            check(parts.size == 2)
                            resourceFolder = parts[0]
                            apiKey = parts[1]
                            defaultModel = currentModels[provider]
                            httpLogLevel = LogLevel.ALL
                        }
                }
                else -> {
                    providers[provider] =
                        AiClient.get(provider) {
                            apiKey = key
                            defaultModel = currentModels[provider]
                            httpLogLevel = LogLevel.ALL
                        }
                }
            }
        }
    }
}
