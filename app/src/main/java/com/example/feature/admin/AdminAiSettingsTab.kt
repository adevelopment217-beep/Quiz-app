package com.example.feature.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.AiManager
import com.example.core.model.AiProviderConfig
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch

@Composable
fun AdminAiSettingsTab() {
    val aiManager = AiManager.instance
    var provider by remember { mutableStateOf(aiManager.config.provider) }
    var model by remember { mutableStateOf(aiManager.config.model) }
    var apiKey by remember { mutableStateOf(aiManager.config.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var baseUrl by remember { mutableStateOf(aiManager.config.baseUrl) }
    var temperature by remember { mutableFloatStateOf(aiManager.config.temperature) }
    var maxTokens by remember { mutableIntStateOf(aiManager.config.maxTokens) }
    var systemPrompt by remember { mutableStateOf(aiManager.config.systemPrompt) }
    var isEnabled by remember { mutableStateOf(aiManager.config.isEnabled) }
    var isAssistantEnabled by remember { mutableStateOf(aiManager.config.isAssistantEnabled) }
    var isSemanticEvalEnabled by remember { mutableStateOf(aiManager.config.isSemanticEvalEnabled) }
    var isQualityCheckEnabled by remember { mutableStateOf(aiManager.config.isQualityCheckEnabled) }

    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val providerOptions = listOf("gemini", "openai", "openrouter", "anthropic", "custom")
    val modelOptions = when (provider.lowercase()) {
        "gemini" -> listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash")
        "openai" -> listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo")
        "anthropic" -> listOf("claude-3-5-sonnet-20241022", "claude-3-haiku-20240307")
        else -> listOf("mistralai/mistral-7b-instruct", "meta-llama/llama-3-8b-instruct", "custom-model")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "এআই ইঞ্জিন ও মডেল কন্ট্রোল সিস্টেম",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "অ্যাপের সকল এআই ফিচার, প্রোভাইডার এবং মডেল সেটিংস এখান থেকে রিয়েল-টাইমে নিয়ন্ত্রণ করা যায়।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ==========================================
        // FEATURE TOGGLES
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ফিচার সক্রিয়করণ (Feature Toggles):", fontWeight = FontWeight.Bold)
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("এআই মাস্টার ইঞ্জিন (Master Engine)", fontWeight = FontWeight.SemiBold)
                            Text("সকল এআই সাব-সার্ভিসের কেন্দ্রীয় সুইচ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                    }
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("সেমেন্টিক উত্তর মূল্যায়ন (Semantic Evaluation)", fontWeight = FontWeight.SemiBold)
                            Text("শূন্যস্থান ও বর্ণনামূলক উত্তরে আংশিক/অর্থগত সঠিকতা যাচাই", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = isSemanticEvalEnabled, onCheckedChange = { isSemanticEvalEnabled = it })
                    }
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("এআই গৃহশিক্ষক চ্যাট (AI Tutor)", fontWeight = FontWeight.SemiBold)
                            Text("শিক্ষার্থীদের প্রতিটি বিষয়ে তাৎক্ষণিক প্রশ্নোত্তরের সুবিধা", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = isAssistantEnabled, onCheckedChange = { isAssistantEnabled = it })
                    }
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("কুইজ মান নিয়ন্ত্রণ (Quality Inspection)", fontWeight = FontWeight.SemiBold)
                            Text("ইম্পোর্ট করা কুইজের অপশন এবং উত্তরের ত্রুটি পরীক্ষণ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = isQualityCheckEnabled, onCheckedChange = { isQualityCheckEnabled = it })
                    }
                }
            }
        }

        // ==========================================
        // PROVIDER & MODEL CONFIGURATION
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("মডেল ও প্রোভাইডার কনফিগারেশন:", fontWeight = FontWeight.Bold)

                    // Provider Chips
                    Column {
                        Text("প্রোভাইডার নির্বাচন করুন:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(providerOptions) { prov ->
                                FilterChip(
                                    selected = provider.equals(prov, ignoreCase = true),
                                    onClick = {
                                        provider = prov
                                        if (prov == "openai") model = "gpt-4o-mini"
                                        else if (prov == "gemini") model = "gemini-2.5-flash"
                                        else if (prov == "anthropic") model = "claude-3-5-sonnet-20241022"
                                    },
                                    label = { Text(prov.uppercase(), fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    // Model Name with Quick Presets
                    Column {
                        Text("মডেল নাম (Model Name):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(modelOptions) { mOpt ->
                                SuggestionChip(
                                    onClick = { model = mOpt },
                                    label = { Text(mOpt, fontSize = 11.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("মডেল আইডি বা নাম") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_model_input")
                        )
                    }

                    // API Key Field (with Show/Hide Toggle)
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key (ঐচ্ছিক - ডিফল্ট কি ব্যবহৃত হবে)") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API Key Visibility"
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("বেস ইউআরএল (Base URL)") },
                        placeholder = { Text("https://generativelanguage.googleapis.com") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Temperature Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("টেম্পারেচার (Temperature):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text("${String.format("%.2f", temperature)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            valueRange = 0.0f..1.0f,
                            steps = 19
                        )
                    }

                    // Max Tokens
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("সর্বোচ্চ টোকেন (Max Tokens):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(512, 1024, 2048).forEach { tok ->
                                FilterChip(
                                    selected = maxTokens == tok,
                                    onClick = { maxTokens = tok },
                                    label = { Text("$tok", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text("সিস্টেম নির্দেশনা (System Prompt)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                }
            }
        }

        // ==========================================
        // ACTION BUTTONS (TEST & SAVE)
        // ==========================================
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        testStatusMessage = null
                        scope.launch {
                            val start = System.currentTimeMillis()
                            val reply = aiManager.getTutorResponse("Admin System Check", "হ্যালো, এআই কানেকশন পরীক্ষা করছি।")
                            val elapsed = System.currentTimeMillis() - start
                            isTesting = false
                            testStatusMessage = if (reply.isNotBlank() && !reply.contains("নিষ্ক্রিয়")) {
                                "✅ কানেকশন সফল! ($elapsed ms)\nপ্রোভাইডার: ${provider.uppercase()} ($model)\nএআই উত্তর: ${reply.take(70)}..."
                            } else {
                                "⚠️ সতর্কবার্তা: $reply"
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_ai_connection_button"),
                    enabled = !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("টেস্ট কানেকশন")
                    }
                }

                Button(
                    onClick = {
                        val newConfig = AiProviderConfig(
                            provider = provider,
                            model = model,
                            apiKey = apiKey,
                            baseUrl = baseUrl,
                            temperature = temperature,
                            maxTokens = maxTokens,
                            systemPrompt = systemPrompt,
                            isEnabled = isEnabled,
                            isAssistantEnabled = isAssistantEnabled,
                            isSemanticEvalEnabled = isSemanticEvalEnabled,
                            isQualityCheckEnabled = isQualityCheckEnabled
                        )
                        aiManager.updateConfig(newConfig)
                        testStatusMessage = "✅ সেটিংস সফলভাবে আপডেট ও কার্যকর করা হয়েছে!"
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_ai_settings_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সংরক্ষণ করুন")
                }
            }
        }

        if (testStatusMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (testStatusMessage?.startsWith("✅") == true) SuccessGreen.copy(alpha = 0.15f) else WarningOrange.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = testStatusMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (testStatusMessage?.startsWith("✅") == true) SuccessGreen else WarningOrange
                    )
                }
            }
        }
    }
}
