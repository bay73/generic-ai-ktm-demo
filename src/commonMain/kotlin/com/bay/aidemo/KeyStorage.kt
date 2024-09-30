package com.bay.aidemo

import com.bay.aiclient.AiClient

expect fun readLocalStorage(path: String): Map<String, String>

expect fun writeLocalStorage(
    path: String,
    settings: Map<String, String>,
)

object KeyStorage {
    fun getKeys(): Map<AiClient.Type, String> {
        val settings = readLocalStorage(".api_keys")
        return settings.mapKeys { (key, _) -> AiClient.Type.valueOf(key) }
    }

    fun saveKeys(keys: Map<AiClient.Type, String>) {
        writeLocalStorage(".api_keys", keys.mapKeys { (provider, _) -> provider.name })
    }
}
