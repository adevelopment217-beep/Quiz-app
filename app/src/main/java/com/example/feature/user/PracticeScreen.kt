package com.example.feature.user

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.core.model.AiEvaluationResult
import com.example.core.model.Question
import com.example.core.model.Quiz
import com.example.core.model.QuizAttempt
import com.example.data.repository.QuizRepository
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    quizId: String,
    quizRepository: QuizRepository,
    currentUserId: String,
    onBack: () -> Unit,
    onFinish: (attemptId: String, score: Int, total: Int) -> Unit
) {
    var quiz by remember { mutableStateOf<Quiz?>(null) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf("") }
    var fillAnswerText by remember { mutableStateOf("") }
    var isAnswerSubmitted by remember { mutableStateOf(false) }
    var evaluationResult by remember { mutableStateOf<AiEvaluationResult?>(null) }
    var aiExplanationText by remember { mutableStateOf<String?>(null) }
    var isAiLoading by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    LaunchedEffect(quizId) {
        val q = quizRepository.getQuiz(quizId)
        val qList = quizRepository.getQuestionsDirect(quizId)
        quiz = q
        questions = if (q?.shuffleQuestions == true) qList.shuffled() else qList
        isLoading = false
    }

    val currentQuestion = questions.getOrNull(currentIndex)
    val totalQuestions = questions.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "অনুশীলন: ${quiz?.title ?: ""}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("practice_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = "স্কোর: $score",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(end = 16.dp)
                    )
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
        } else if (currentQuestion == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("এই কুইজে কোনো প্রশ্ন পাওয়া যায়নি।")
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
                // Progress Bar & Question Counter
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "প্রশ্ন ${currentIndex + 1} / $totalQuestions",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (currentQuestion.type == "mcq") "MCQ" else "শূন্যস্থান পূরণ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / totalQuestions },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                // Question Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = currentQuestion.questionText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 24.sp
                            )
                        )
                    }
                }

                // Options or Input
                if (currentQuestion.type == "mcq") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val options = currentQuestion.options
                        options.forEachIndexed { optIndex, optionText ->
                            val isSelected = selectedOption == optionText
                            val isCorrectAnswer = optionText == currentQuestion.answer
                            val borderColor = when {
                                !isAnswerSubmitted -> if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                isCorrectAnswer -> SuccessGreen
                                isSelected -> ErrorRed
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                            val bgColor = when {
                                !isAnswerSubmitted -> if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                isCorrectAnswer -> SuccessGreen.copy(alpha = 0.12f)
                                isSelected -> ErrorRed.copy(alpha = 0.12f)
                                else -> MaterialTheme.colorScheme.surface
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isAnswerSubmitted) {
                                        selectedOption = optionText
                                    }
                                    .testTag("practice_option_$optIndex"),
                                color = bgColor
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(borderColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ('A' + optIndex).toString(),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = borderColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isAnswerSubmitted) {
                                        if (isCorrectAnswer) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = SuccessGreen)
                                        } else if (isSelected) {
                                            Icon(Icons.Default.Cancel, contentDescription = "Incorrect", tint = ErrorRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Fill in the blank
                    OutlinedTextField(
                        value = fillAnswerText,
                        onValueChange = { if (!isAnswerSubmitted) fillAnswerText = it },
                        label = { Text("আপনার উত্তর লিখুন (Type answer)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("practice_fill_blank_input"),
                        enabled = !isAnswerSubmitted,
                        singleLine = true
                    )
                }

                // Check Answer Button (if not submitted)
                if (!isAnswerSubmitted) {
                    Button(
                        onClick = {
                            val answer = if (currentQuestion.type == "mcq") selectedOption else fillAnswerText
                            if (answer.isNotBlank()) {
                                isAnswerSubmitted = true
                                scope.launch {
                                    val eval = AiManager.instance.evaluateAnswer(currentQuestion, answer)
                                    evaluationResult = eval
                                    if (eval.status == "CORRECT") {
                                        score += currentQuestion.points
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_practice_answer_button"),
                        enabled = if (currentQuestion.type == "mcq") selectedOption.isNotBlank() else fillAnswerText.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("উত্তর যাচাই করুন (Check Answer)", fontWeight = FontWeight.Bold)
                    }
                }

                // Evaluation & Explanation Result Card (if submitted)
                AnimatedVisibility(
                    visible = isAnswerSubmitted && evaluationResult != null,
                    enter = fadeIn() + expandVertically()
                ) {
                    val result = evaluationResult
                    if (result != null) {
                        val isCorrect = result.status == "CORRECT"
                        val statusColor = if (isCorrect) SuccessGreen else ErrorRed
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.08f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isCorrect) "অভিনন্দন! আপনার উত্তর সঠিক হয়েছে।" else "দুঃখিত, উত্তরটি সঠিক নয়।",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = statusColor
                                    )
                                }

                                if (!isCorrect) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "সঠিক উত্তর: ${currentQuestion.answer}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (result.reason.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "মূল্যায়ন: ${result.reason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (currentQuestion.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "ব্যাখ্যা: ${currentQuestion.explanation}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // AI Explanation Expand Button
                                if (aiExplanationText == null) {
                                    OutlinedButton(
                                        onClick = {
                                            isAiLoading = true
                                            scope.launch {
                                                val explanation = AiManager.instance.getTutorResponse(
                                                    contextPrompt = "Quiz: ${quiz?.title}, Question: ${currentQuestion.questionText}, Correct: ${currentQuestion.answer}",
                                                    userQuery = "এই প্রশ্নটা বিস্তারিত বুঝিয়ে দাও এবং শিক্ষার্থীকে মূল ধারণাটি পরিষ্কার করো।"
                                                )
                                                aiExplanationText = explanation
                                                isAiLoading = false
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ai_explanation_button"),
                                        enabled = !isAiLoading,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isAiLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("এআই ব্যাখ্যা তৈরি করছে...")
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("এআই থেকে গভীর ব্যাখ্যা জানুন")
                                        }
                                    }
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp)),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("এআই গৃহশিক্ষকের পরামর্শ:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = aiExplanationText ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Next or Finish Button (when submitted)
                if (isAnswerSubmitted) {
                    Button(
                        onClick = {
                            if (currentIndex < totalQuestions - 1) {
                                currentIndex++
                                selectedOption = ""
                                fillAnswerText = ""
                                isAnswerSubmitted = false
                                evaluationResult = null
                                aiExplanationText = null
                            } else {
                                // Save practice attempt
                                scope.launch {
                                    val attemptId = "att_prac_${System.currentTimeMillis()}"
                                    val attempt = QuizAttempt(
                                        id = attemptId,
                                        quizId = quizId,
                                        quizTitle = quiz?.title ?: "Practice Quiz",
                                        userId = currentUserId,
                                        mode = "PRACTICE",
                                        score = score,
                                        totalPoints = questions.sumOf { it.points },
                                        correctCount = score,
                                        incorrectCount = totalQuestions - score,
                                        unansweredCount = 0,
                                        accuracyPercentage = (score.toDouble() / totalQuestions.toDouble()) * 100.0,
                                        timeTakenSeconds = 120
                                    )
                                    quizRepository.recordAttempt(attempt)
                                    onFinish(attemptId, score, questions.sumOf { it.points })
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("practice_next_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (currentIndex < totalQuestions - 1) "পরবর্তী প্রশ্ন (Next)" else "অনুশীলন সম্পন্ন করুন (Finish)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
