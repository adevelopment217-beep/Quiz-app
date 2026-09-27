package com.example.feature.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.AiConfigRepository
import com.example.core.ai.AiManager
import com.example.core.ai.AiProviderResult
import com.example.core.model.AiProviderConfig
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAiSettingsTab() {
    val context = LocalContext.current
    val aiConfigRepo = remember { AiConfigRepository.getInstance(context) }
    val aiManager = AiManager.instance

    // Load initial config from persistent storage
    var currentConfig by remember { mutableStateOf(aiConfigRepo.loadConfig()) }

    var provider by remember { mutableStateOf(currentConfig.provider) }
    var model by remember { mutableStateOf(currentConfig.model) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var baseUrl by remember { mutableStateOf(currentConfig.baseUrl) }
    var temperature by remember { mutableFloatStateOf(currentConfig.temperature) }
    var maxTokens by remember { mutableIntStateOf(currentConfig.maxTokens) }
    var systemPrompt by remember { mutableStateOf(currentConfig.systemPrompt) }

    var isEnabled by remember { mutableStateOf(currentConfig.isEnabled) }
    var isAssistantEnabled by remember { mutableStateOf(currentConfig.isAssistantEnabled) }
    var isSemanticEvalEnabled by remember { mutableStateOf(currentConfig.isSemanticEvalEnabled) }
    var isQualityCheckEnabled by remember { mutableStateOf(currentConfig.isQualityCheckEnabled) }

    var showAdvanced by remember { mutableStateOf(false) }
    var showRequestHealth by remember { mutableStateOf(false) }

    var testResult by remember { mutableStateOf<AiProviderResult?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var saveStatusMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val logs by aiManager.requestLogs.collectAsState()

    val providerOptions = listOf("gemini", "openai", "openrouter", "anthropic", "custom")
    val modelOptions = when (provider.lowercase()) {
        "gemini" -> listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-1.5-flash")
        "openai" -> listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo")
        "openrouter" -> listOf("meta-llama/llama-3.3-70b-instruct", "google/gemini-2.5-flash", "anthropic/claude-3.5-haiku")
        "anthropic" -> listOf("claude-3-5-sonnet-20241022", "claude-3-5-haiku-20241022")
        else -> listOf("openai/gpt-oss-120b:groq", "meta-llama/llama-3-8b-instruct", "custom-model")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "এআই সেটিংস ও ইঞ্জিন কনফিগারেশন",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "প্রোভাইডার, মডেল এবং ক্রেডেনশিয়ালস সংরক্ষণ করুন। সংরক্ষিত কনফিগারেশন অনুযায়ী সকল এআই ফিচার পরিচালিত হবে।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ==========================================
        // 1. AI STATUS (Master Engine)
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("মাস্টার এআই ইঞ্জিন (Master Engine)", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isEnabled) "এআই সেবা সম্পূর্ণ সক্রিয়" else "এআই সেবা সাময়িকভাবে বন্ধ",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) SuccessGreen else MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        modifier = Modifier.testTag("ai_master_switch")
                    )
                }
            }
        }

        // ==========================================
        // 2. AI FEATURES TOGGLES
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("এআই ফিচারসমূহ (AI Features):", fontWeight = FontWeight.Bold)
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("সেমেন্টিক মূল্যায়ন (Semantic Evaluation)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("বিকল্প বা রূপভেদ উত্তরের অর্থগত সঠিকতা যাচাই", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = isSemanticEvalEnabled, onCheckedChange = { isSemanticEvalEnabled = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("এআই গৃহশিক্ষক (AI Tutor Assistant)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("শিক্ষার্থীদের পাঠ্যক্রম বিষয়ক প্রশ্নোত্তরের তাৎক্ষণিক ব্যাখ্যা", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = isAssistantEnabled, onCheckedChange = { isAssistantEnabled = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("কুইজ মান নিয়ন্ত্রণ (Quality Inspection)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("ইম্পোর্ট করা কুইজের উত্তর বা অপশন ত্রুটি স্বয়ংক্রিয় সনাক্তকরণ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(checked = isQualityCheckEnabled, onCheckedChange = { isQualityCheckEnabled = it })
                    }
                }
            }
        }

        // ==========================================
        // 3. PROVIDER SELECTION
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("প্রোভাইডার নির্বাচন (Provider):", fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(providerOptions) { prov ->
                            val isSelected = provider.equals(prov, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    provider = prov
                                    when (prov) {
                                        "gemini" -> {
                                            model = "gemini-2.5-flash"
                                            baseUrl = "https://generativelanguage.googleapis.com"
                                        }
                                        "openai" -> {
                                            model = "gpt-4o-mini"
                                            baseUrl = "https://api.openai.com/v1"
                                        }
                                        "openrouter" -> {
                                            model = "meta-llama/llama-3.3-70b-instruct"
                                            baseUrl = "https://openrouter.ai/api/v1"
                                        }
                                        "anthropic" -> {
                                            model = "claude-3-5-sonnet-20241022"
                                            baseUrl = "https://api.anthropic.com/v1"
                                        }
                                        "custom" -> {
                                            if (baseUrl.contains("generativelanguage.googleapis.com") || baseUrl.isBlank()) {
                                                baseUrl = "https://router.huggingface.co/v1"
                                            }
                                        }
                                    }
                                },
                                label = { Text(prov.uppercase(), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("provider_chip_$prov")
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. CREDENTIALS (API KEY)
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ক্রেডেনশিয়ালস (Credentials):", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text("Enter your API Key...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_api_key_input"),
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API Key Visibility"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = "নিরাপত্তা নিশ্চিতকরণ: গোপন কি ডিভাইসের নিরাপদ স্যান্ডবক্সে সংরক্ষিত থাকে এবং কোথাও ফাঁস হয় না।",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // ==========================================
        // 5. MODEL SELECTION
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("মডেল নাম (Model Name):", fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(modelOptions) { mOpt ->
                            SuggestionChip(
                                onClick = { model = mOpt },
                                label = { Text(mOpt, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("মডেল আইডি / পাথ") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_model_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // ==========================================
        // 6. CONNECTION & TEST
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("কানেকশন পরীক্ষা (Live Pipeline Test):", fontWeight = FontWeight.Bold)
                    Text(
                        text = "এটি অ্যাপ্লিকেশনের মূল এআই ইঞ্জিন ব্যবহার করে একটি সরাসরি টেস্ট রিকুয়েস্ট পাঠাবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            isTesting = true
                            testResult = null
                            scope.launch {
                                // Temporarily apply user's inputs to test them live
                                val testConfig = AiProviderConfig(
                                    provider = provider,
                                    model = model,
                                    apiKey = apiKey,
                                    baseUrl = baseUrl,
                                    temperature = temperature,
                                    maxTokens = maxTokens,
                                    systemPrompt = systemPrompt,
                                    isEnabled = true,
                                    isAssistantEnabled = isAssistantEnabled,
                                    isSemanticEvalEnabled = isSemanticEvalEnabled,
                                    isQualityCheckEnabled = isQualityCheckEnabled
                                )
                                aiManager.updateConfig(testConfig)
                                val res = aiManager.testConnection()
                                isTesting = false
                                testResult = res
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_ai_connection_button"),
                        enabled = !isTesting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("পরীক্ষা চলছে (Connecting)...")
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("টেস্ট কানেকশন (Test Connection)")
                        }
                    }

                    // Test Result Diagnostics
                    testResult?.let { res ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (res.success) SuccessGreen.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (res.success) SuccessGreen.copy(alpha = 0.4f) else ErrorRed.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (res.success) "✅ কানেকশন সফল (Success)" else "❌ কানেকশন ব্যর্থ (Failed)",
                                        fontWeight = FontWeight.Bold,
                                        color = if (res.success) SuccessGreen else ErrorRed
                                    )
                                    Text(
                                        text = "${res.latencyMs} ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("প্রোভাইডার: ${res.providerName} • মডেল: ${res.modelName}", style = MaterialTheme.typography.labelSmall)
                                Text("হোস্ট: ${res.endpointHost} • কোড: HTTP ${res.httpStatus}", style = MaterialTheme.typography.labelSmall)

                                if (res.success) {
                                    Text(
                                        text = "এআই উত্তর: \"${res.content.take(120)}\"",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = SuccessGreen
                                    )
                                } else {
                                    Text(
                                        text = "ত্রুটি বিবরণ: ${res.errorMessage ?: "অজানা ব্যর্থতা"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ErrorRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 7. ADVANCED SETTINGS (Collapsible Accordion)
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvanced = !showAdvanced },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("উন্নত সেটিংস (Advanced Settings)", fontWeight = FontWeight.Bold)
                            Text("Base URL, Temperature, Max Tokens", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Icon(
                            imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Advanced Settings"
                        )
                    }

                    AnimatedVisibility(visible = showAdvanced) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OutlinedTextField(
                                value = baseUrl,
                                onValueChange = { baseUrl = it },
                                label = { Text("বেস ইউআরএল (Base URL)") },
                                placeholder = { Text("https://router.huggingface.co/v1") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("সর্বোচ্চ টোকেন (Max Tokens):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(512, 1024, 2048, 4096).forEach { tok ->
                                        FilterChip(
                                            selected = maxTokens == tok,
                                            onClick = { maxTokens = tok },
                                            label = { Text("$tok", fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 8. SYSTEM PROMPT INSTRUCTIONS
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("সিস্টেম নির্দেশিকা (System Instructions):", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text("এআই আচরণ ও শিক্ষাদানের নীতি") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // ==========================================
        // 9. SAVE CONFIGURATION BUTTON
        // ==========================================
        item {
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
                    aiConfigRepo.saveConfig(newConfig)
                    currentConfig = newConfig
                    saveStatusMessage = "✅ এআই কনফিগারেশন সফলভাবে সংরক্ষিত হয়েছে এবং কার্যকর করা হয়েছে!"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_ai_settings_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("কনফিগারেশন সংরক্ষণ করুন (Save Configuration)")
            }
        }

        if (saveStatusMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SuccessGreen.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = saveStatusMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = SuccessGreen
                    )
                }
            }
        }

        // ==========================================
        // 10. AI REQUEST HEALTH / DIAGNOSTIC LOGS
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRequestHealth = !showRequestHealth },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("এআই রিকুয়েস্ট ডায়াগনস্টিক লগ (${logs.size})", fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            imageVector = if (showRequestHealth) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Diagnostics"
                        )
                    }

                    AnimatedVisibility(visible = showRequestHealth) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (logs.isEmpty()) {
                                Text(
                                    "এখনও কোনো এআই রিকুয়েস্ট পাঠানো হয়নি। টেস্ট কানেকশন বাটনে ক্লিক করে পরীক্ষা করুন।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            } else {
                                logs.take(10).forEach { log ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "[${log.feature}] ${log.provider}/${log.model}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    text = "${log.latencyMs}ms • ${log.status}",
                                                    color = if (log.status == "SUCCESS") SuccessGreen else ErrorRed,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                text = "Host: ${log.endpointHost} • Code: HTTP ${log.httpStatus}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            if (log.sanitizedError != null) {
                                                Text(
                                                    text = "Error: ${log.sanitizedError}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ErrorRed
                                                )
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
