package com.bay.aidemo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bay.aiclient.AiClient
import com.bay.aidemo.clients.MultiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun App() {
    val storedKeys = KeyStorage.getKeys()
    MultiClient.setKeys(storedKeys)
    MaterialTheme {
        val scope = rememberCoroutineScope()
        var userPrompt by remember { mutableStateOf("") }
        var systemPrompt by remember { mutableStateOf("") }
        var selectedTabIndex by remember { mutableStateOf(0) }
        var responses by remember { mutableStateOf(mapOf<AiClient.Type, String>()) }
        var statistics by remember { mutableStateOf(mapOf<AiClient.Type, String>()) }
        var models by remember { mutableStateOf(mapOf<AiClient.Type, List<String>>()) }
        var keys by remember { mutableStateOf(storedKeys) }
        var modelsExpanded by remember { mutableStateOf(keys.mapValues { (_, _) -> false }) }

        fun setModelsExpanded(
            provider: AiClient.Type,
            expanded: Boolean,
        ) {
            modelsExpanded = modelsExpanded.toMutableMap().apply { this[provider] = expanded }
        }

        var currentModels by remember { mutableStateOf(MultiClient.getCurrentModels()) }

        fun setCurrentModel(
            provider: AiClient.Type,
            model: String,
        ) {
            MultiClient.setCurrentModel(provider, model)
            currentModels = MultiClient.getCurrentModels()
        }

        var loading by remember { mutableStateOf(false) }
        var showSystemPrompt by remember { mutableStateOf(false) }
        var showSettings by remember { mutableStateOf(false) }
        scope.launch { models = MultiClient.getModels(false) }
        Column(
            Modifier.fillMaxSize().padding(all = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                ScrollableTabRow(modifier = Modifier.fillMaxWidth(), selectedTabIndex = selectedTabIndex) {
                    MultiClient.getClientTypes().forEachIndexed { index, aiProvider ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text("${aiProvider.name}\n${currentModels[aiProvider]}") },
                        )
                    }
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Keys setup")
                }
            }
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 8.dp, end = 8.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(64.dp),
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                                Text(
                                    text = statistics[AiClient.Type.entries[selectedTabIndex]] ?: "",
                                    color = Color.Blue,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 0.85.em,
                                )
                            }
                            Text(
                                text = responses[AiClient.Type.entries[selectedTabIndex]] ?: "",
                                overflow = TextOverflow.Visible,
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.selectableGroup().width(200.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = (!showSystemPrompt), onClick = { showSystemPrompt = false })
                        Text(text = "User prompt")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = (showSystemPrompt), onClick = { showSystemPrompt = true })
                        Text(text = "System prompt")
                    }
                }
                if (showSystemPrompt) {
                    TextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(end = 8.dp)
                                .heightIn(min = 100.dp),
                        placeholder = { Text("Put your system prompt here...") },
                    )
                } else {
                    TextField(
                        value = userPrompt,
                        onValueChange = { userPrompt = it },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(end = 8.dp)
                                .heightIn(min = 100.dp),
                        placeholder = { Text("Put your user prompt here...") },
                    )
                }
                Button(
                    onClick = {
                        loading = true
                        scope.launch {
                            val results = MultiClient.chatResponses(userPrompt, systemPrompt)
                            responses =
                                results.mapValues { (_, result) ->
                                    result.errorMessage ?: (result.response ?: "")
                                }
                            statistics = results.mapValues { (_, result) -> "Token count:${result.tokenCount}" }
                            loading = false
                            userPrompt = ""
                        }
                    },
                    modifier = Modifier.width(70.dp),
                ) {
                    Text("Run")
                }
            }
        }
        if (showSettings) {
            Dialog(
                onDismissRequest = { showSettings = false },
                properties =
                    DialogProperties(
                        usePlatformDefaultWidth = false,
                    ),
            ) {
                Card(
                    modifier = Modifier.height(900.dp).width(900.dp).padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        Modifier.width(2000.dp).padding(all = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "AI providers settings",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 1.2.em,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Provider", modifier = Modifier.width(130.dp))
                            Text(text = "API Key", modifier = Modifier.width(250.dp))
                            Text(text = "Model", modifier = Modifier.width(450.dp))
                        }
                        AiClient.Type.entries.forEach { provider ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = provider.name, modifier = Modifier.width(130.dp))
                                TextField(
                                    value = keys[provider] ?: "",
                                    onValueChange = { newValue ->
                                        keys = keys.toMutableMap().apply { this[provider] = newValue }
                                        MultiClient.setKeys(keys)
                                        KeyStorage.saveKeys(keys)
                                        scope.launch { models = MultiClient.getModels(true) }
                                    },
                                    modifier = Modifier.width(250.dp),
                                    placeholder = { Text("Put API key for ${provider.name}") },
                                    singleLine = true,
                                )
                                ExposedDropdownMenuBox(
                                    expanded = modelsExpanded[provider] ?: false,
                                    onExpandedChange = { setModelsExpanded(provider, true) },
                                    modifier =
                                        Modifier.height(55.dp).width(450.dp).padding(start = 8.dp, end = 8.dp),
                                ) {
                                    TextField(
                                        value = currentModels[provider] ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = modelsExpanded[provider] ?: false,
                                            )
                                        },
                                        modifier = Modifier.width(450.dp),
                                    )

                                    ExposedDropdownMenu(
                                        expanded = modelsExpanded[provider] ?: false,
                                        onDismissRequest = { setModelsExpanded(provider, false) },
                                    ) {
                                        models[provider]?.forEach { item ->
                                            DropdownMenuItem(
                                                onClick = {
                                                    setModelsExpanded(provider, false)
                                                    setCurrentModel(provider, item)
                                                },
                                            ) {
                                                Text(text = item)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
