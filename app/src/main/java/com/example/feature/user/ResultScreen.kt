package com.example.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.AiManager
import com.example.core.model.Question
import com.example.core.model.QuizAttempt
import com.example.core.model.UserAnswerRecord
import com.example.data.repository.QuizRepository
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    attemptId: String,
    quizRepository: QuizRepository,
    currentUserId: String,
    onRetry: (String) -> Unit,
    onDone: () -> Unit
) {
    var attempt by remember { mutableStateOf<QuizAttempt?>(null) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var answersMap by remember { mutableStateOf<Map<String, UserAnswerRecord>>(emptyList<Pair<String, UserAnswerRecord>>().toMap()) }
    var aiAdviceText by remember { mutableStateOf<String?>(null) }
    var isAiLoading by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(attemptId) {
        val attempts = quizRepository.getUserAttempts(currentUserId)
        attempts.collect { list ->
            val match = list.find { it.id == attemptId } ?: list.firstOrNull()
            attempt = match
            if (match != null) {
                questions = quizRepository.getQuestionsDirect(match.quizId)
                // Deserialize answersJson
                try {
                    val moshi = Moshi.Builder().build()
                    val type = Types.newParameterizedType(Map::class.java, String::class.java, UserAnswerRecord::class.java)
                    val adapter = moshi.adapter<Map<String, UserAnswerRecord>>(type)
                    answersMap = adapter.fromJson(match.answersJson) ?: emptyMap()
                } catch (e: Exception) {
                    answersMap = emptyMap()
                }
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ফলাফল ও মূল্যায়ন (Results)") },
                actions = {
                    IconButton(
                        onClick = onDone,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("result_done_top_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val curAttempt = attempt
            if (curAttempt == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ফলাফল পাওয়া যায়নি।")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Banner Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = curAttempt.quizTitle,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Score Circle
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${curAttempt.score}/${curAttempt.totalPoints}",
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${String.format("%.1f", curAttempt.accuracyPercentage)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Stat Breakdown Row
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${curAttempt.correctCount}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = SuccessGreen
                                )
                                Text("সঠিক", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${curAttempt.incorrectCount}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = ErrorRed
                                )
                                Text("ভুল", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${curAttempt.unansweredCount}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = WarningOrange
                                )
                                Text("অনাবৃত", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val mins = curAttempt.timeTakenSeconds / 60
                                val secs = curAttempt.timeTakenSeconds % 60
                                Text(
                                    text = String.format("%02d:%02d", mins, secs),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("সময় ব্যয়", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // AI Performance Analysis Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "এআই পারফরম্যান্স বিশ্লেষণ (AI Tutor Analysis)",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            if (aiAdviceText == null) {
                                Text(
                                    text = "আপনার প্রাপ্ত স্কোর ও ভুল প্রশ্নগুলো বিশ্লেষণ করে পড়ার পরামর্শ নিন।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        isAiLoading = true
                                        scope.launch {
                                            val prompt = "কুইজ: ${curAttempt.quizTitle}, স্কোর: ${curAttempt.score}/${curAttempt.totalPoints}, নির্ভুলতা: ${curAttempt.accuracyPercentage}%, সঠিক: ${curAttempt.correctCount}, ভুল: ${curAttempt.incorrectCount}."
                                            val advice = AiManager.instance.getTutorResponse(
                                                contextPrompt = prompt,
                                                userQuery = "আমার এই ফলাফলের ওপর ভিত্তি করে কোন অংশে আরও পড়া উচিত এবং উন্নতি করার কৌশল সংক্ষেপে বাংলায় বলো।"
                                            )
                                            aiAdviceText = advice
                                            isAiLoading = false
                                        }
                                    },
                                    enabled = !isAiLoading,
                                    modifier = Modifier.testTag("generate_ai_analysis_button")
                                ) {
                                    if (isAiLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("বিশ্লেষণ চলছে...")
                                    } else {
                                        Text("বিশ্লেষণ তৈরি করুন")
                                    }
                                }
                            } else {
                                Text(
                                    text = aiAdviceText ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }

                    // Detailed Question by Question Review
                    Text(
                        text = "প্রশ্নোত্তর পর্যালোচনা (Question Review):",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    questions.forEachIndexed { index, question ->
                        val record = answersMap[question.id]
                        val isCorrect = record?.isCorrect == true
                        val isUnanswered = record?.evaluationStatus == "UNANSWERED" || record?.selectedAnswer.isNullOrBlank()

                        val statusColor = when {
                            isCorrect -> SuccessGreen
                            isUnanswered -> WarningOrange
                            else -> ErrorRed
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "প্রশ্ন ${index + 1}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = when {
                                            isCorrect -> "সঠিক (+${question.points})"
                                            isUnanswered -> "অনাবৃত (০)"
                                            else -> "ভুল (০)"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = statusColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = question.questionText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "আপনার উত্তর: ${if (record?.selectedAnswer.isNullOrBlank()) "উত্তর দেওয়া হয়নি" else record?.selectedAnswer}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCorrect) SuccessGreen else ErrorRed
                                )
                                Text(
                                    text = "সঠিক উত্তর: ${question.answer}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = SuccessGreen
                                )

                                if (question.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "ব্যাখ্যা: ${question.explanation}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRetry(curAttempt.quizId) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("retry_quiz_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("আবার চেষ্টা করুন")
                        }

                        Button(
                            onClick = onDone,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("finish_quiz_results_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("সম্পন্ন (Done)")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
