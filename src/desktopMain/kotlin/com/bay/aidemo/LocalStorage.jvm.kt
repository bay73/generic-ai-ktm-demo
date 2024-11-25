package com.bay.aidemo

import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter

actual fun readLocalStorage(path: String): Map<String, String> =
    try {
        val lines = File(path).bufferedReader().use { it.lineSequence().toList() }
        lines.associate { it.split(":")[0].trim() to it.split(":")[1].trim() }
    } catch (e: FileNotFoundException) {
        emptyMap()
    }

actual fun writeLocalStorage(
    path: String,
    settings: Map<String, String>,
) {
    val bufferedWriter = BufferedWriter(FileWriter(File(path)))
    bufferedWriter.use { it.write(settings.map { (key, value) -> "$key:$value" }.joinToString("\n")) }
}
