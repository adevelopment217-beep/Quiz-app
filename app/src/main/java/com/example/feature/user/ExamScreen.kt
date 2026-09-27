package com.example.feature.user

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.DeterministicEvaluator
import com.example.core.model.Question
import com.example.core.model.Quiz
import com.example.core.model.QuizAttempt
import com.example.core.model.UserAnswerRecord
import com.example.data.repository.QuizRepository
import com.example.ui.components.TimerBadge
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    quizId: String,
    quizRepository: QuizRepository,
    currentUserId: String,
    onBack: () -> Unit,
    onSubmitComplete: (attemptId: String) -> Unit
) {
    var quiz by remember { mutableStateOf<Quiz?>(null) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    val userAnswers = remember { mutableStateMapOf<String, String>() }
    val markedForReview = remember { mutableStateMapOf<String, Boolean>() }
    var timeRemainingSeconds by remember { mutableIntStateOf(300) }
    var totalTimeSeconds by remember { mutableIntStateOf(300) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Handle back button interception to prevent accidental exam quit
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    BackHandler {
        showExitConfirmDialog = true
    }

    LaunchedEffect(quizId) {
        val q = quizRepository.getQuiz(quizId)
        val qList = quizRepository.getQuestionsDirect(quizId)
        quiz = q
        questions = if (q?.shuffleQuestions == true) qList.shuffled() else qList
        val initialTime = if ((q?.timeLimitSeconds ?: 0) > 0) q?.timeLimitSeconds ?: 300 else 300
        timeRemainingSeconds = initialTime
        totalTimeSeconds = initialTime
        isTimerRunning = true
        isLoading = false
    }

    // Timer countdown loop
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && timeRemainingSeconds > 0) {
            delay(1000L)
            timeRemainingSeconds--
            if (timeRemainingSeconds <= 0) {
                isTimerRunning = false
                // Auto submit on time expired
                submitExam(
                    quiz = quiz,
                    questions = questions,
                    userAnswers = userAnswers,
                    currentUserId = currentUserId,
                    quizRepository = quizRepository,
                    timeTaken = totalTimeSeconds,
                    onSubmitComplete = onSubmitComplete
                )
            }
        }
    }

    val currentQuestion = questions.getOrNull(currentIndex)
    val totalQuestions = questions.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = quiz?.title ?: "মডেল টেস্ট",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showExitConfirmDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("exam_quit_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Exam")
                    }
                },
                actions = {
                    TimerBadge(
                        secondsRemaining = timeRemainingSeconds,
                        totalSeconds = totalTimeSeconds,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0,
                        modifier = Modifier.testTag("exam_prev_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পূর্ববর্তী")
                    }

                    Button(
                        onClick = { showSubmitDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.testTag("exam_submit_button")
                    ) {
                        Text("জমা দিন (Submit)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (currentIndex < totalQuestions - 1) {
                                currentIndex++
                            } else {
                                showSubmitDialog = true
                            }
                        },
                        modifier = Modifier.testTag("exam_next_button")
                    ) {
                        Text(if (currentIndex < totalQuestions - 1) "পরবর্তী" else "রিভিউ")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
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
                Text("পরীক্ষায় কোনো প্রশ্ন পাওয়া যায়নি।")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Question Palette Grid (Jump to any question)
                Text(
                    text = "প্রশ্ন প্যালেট (Question Palette):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 40.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(questions) { idx, q ->
                        val isCurrent = idx == currentIndex
                        val isAnswered = userAnswers.containsKey(q.id) && userAnswers[q.id]?.isNotBlank() == true
                        val isMarked = markedForReview[q.id] == true

                        val (bgColor, textColor) = when {
                            isCurrent -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
                            isMarked -> WarningOrange to Color.White
                            isAnswered -> SuccessGreen to Color.White
                            else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable { currentIndex = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                        }
                    }
                }

                // Question Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "প্রশ্ন ${currentIndex + 1} / $totalQuestions",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isMarked = markedForReview[currentQuestion.id] == true
                                FilterChip(
                                    selected = isMarked,
                                    onClick = { markedForReview[currentQuestion.id] = !isMarked },
                                    label = { Text("রিভিউ", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("mark_for_review_chip")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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
                        currentQuestion.options.forEachIndexed { optIndex, optionText ->
                            val isSelected = userAnswers[currentQuestion.id] == optionText
                            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                                    .clickable {
                                        userAnswers[currentQuestion.id] = optionText
                                    }
                                    .testTag("exam_option_$optIndex"),
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
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { userAnswers[currentQuestion.id] = optionText }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Fill in the blank
                    OutlinedTextField(
                        value = userAnswers[currentQuestion.id] ?: "",
                        onValueChange = { userAnswers[currentQuestion.id] = it },
                        label = { Text("আপনার উত্তর লিখুন") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exam_fill_blank_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Submit Confirmation Dialog
    if (showSubmitDialog) {
        val answeredCount = userAnswers.values.count { it.isNotBlank() }
        val markedCount = markedForReview.values.count { it }
        val unansweredCount = totalQuestions - answeredCount

        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("পরীক্ষা জমা দিন (Submit Exam)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("আপনি কি নিশ্চিতভাবে পরীক্ষা জমা দিতে চান?")
                    Divider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("মোট প্রশ্ন:")
                        Text("$totalQuestions", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("উত্তর প্রদান করেছেন:")
                        Text("$answeredCount", fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("অনাবৃত (Unanswered):")
                        Text("$unansweredCount", fontWeight = FontWeight.Bold, color = if (unansweredCount > 0) WarningOrange else MaterialTheme.colorScheme.onSurface)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("রিভিউ এর জন্য চিহ্নিত:")
                        Text("$markedCount", fontWeight = FontWeight.Bold, color = WarningOrange)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        isTimerRunning = false
                        isSubmitting = true
                        val timeSpent = totalTimeSeconds - timeRemainingSeconds
                        submitExam(
                            quiz = quiz,
                            questions = questions,
                            userAnswers = userAnswers,
                            currentUserId = currentUserId,
                            quizRepository = quizRepository,
                            timeTaken = timeSpent,
                            onSubmitComplete = onSubmitComplete
                        )
                    },
                    modifier = Modifier.testTag("confirm_submit_exam_button")
                ) {
                    Text("হ্যাঁ, জমা দিন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSubmitDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Exit Confirmation Dialog
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("পরীক্ষা পরিত্যাগ করবেন?") },
            text = { Text("এখন বের হয়ে গেলে বর্তমান পরীক্ষার উত্তর ও সময় হারিয়ে যাবে। আপনি কি নিশ্চিত?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        isTimerRunning = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("বেরিয়ে যান")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitConfirmDialog = false }) {
                    Text("পরীক্ষা চালিয়ে যান")
                }
            }
        )
    }
}

private fun submitExam(
    quiz: Quiz?,
    questions: List<Question>,
    userAnswers: Map<String, String>,
    currentUserId: String,
    quizRepository: QuizRepository,
    timeTaken: Int,
    onSubmitComplete: (attemptId: String) -> Unit
) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        var score = 0
        var correctCount = 0
        var incorrectCount = 0
        var unansweredCount = 0
        val recordsMap = mutableMapOf<String, UserAnswerRecord>()

        for (q in questions) {
            val submitted = userAnswers[q.id]
            if (submitted.isNullOrBlank()) {
                unansweredCount++
                recordsMap[q.id] = UserAnswerRecord(
                    questionId = q.id,
                    selectedAnswer = "",
                    isCorrect = false,
                    pointsEarned = 0,
                    evaluationStatus = "UNANSWERED",
                    explanation = q.explanation
                )
            } else {
                val eval = DeterministicEvaluator.evaluate(q, submitted)
                val isCorrect = eval?.status == "CORRECT"
                if (isCorrect) {
                    score += q.points
                    correctCount++
                } else {
                    incorrectCount++
                }
                recordsMap[q.id] = UserAnswerRecord(
                    questionId = q.id,
                    selectedAnswer = submitted,
                    isCorrect = isCorrect,
                    pointsEarned = if (isCorrect) q.points else 0,
                    evaluationStatus = eval?.status ?: "INCORRECT",
                    explanation = q.explanation
                )
            }
        }

        val totalPoints = questions.sumOf { it.points }
        val accuracy = if (questions.isNotEmpty()) (correctCount.toDouble() / questions.size.toDouble()) * 100.0 else 0.0
        val attemptId = "att_exam_${System.currentTimeMillis()}"

        // Serialize answers json
        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(Map::class.java, String::class.java, UserAnswerRecord::class.java)
        val adapter = moshi.adapter<Map<String, UserAnswerRecord>>(type)
        val answersJson = adapter.toJson(recordsMap)

        val attempt = QuizAttempt(
            id = attemptId,
            quizId = quiz?.id ?: "",
            quizTitle = quiz?.title ?: "Exam",
            userId = currentUserId,
            mode = "EXAM",
            score = score,
            totalPoints = totalPoints,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            unansweredCount = unansweredCount,
            accuracyPercentage = accuracy,
            timeTakenSeconds = timeTaken,
            answersJson = answersJson
        )

        quizRepository.recordAttempt(attempt)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            onSubmitComplete(attemptId)
        }
    }
}
