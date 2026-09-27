package com.example.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.model.Question
import com.example.core.model.QuestionType
import com.example.core.model.Quiz
import com.example.data.repository.AdminRepository
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQuestionsDialog(
    quiz: Quiz,
    adminRepository: AdminRepository,
    adminUid: String,
    onDismiss: () -> Unit
) {
    val questions by adminRepository.getQuestions(quiz.id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<Question?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "প্রশ্ন ব্যবস্থাপনা (Question Manager)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "কুইজ: ${quiz.title} • মোট প্রশ্ন: ${questions.size}টি",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_questions_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Add Question Button
                Button(
                    onClick = {
                        editingQuestion = null
                        showEditDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_question_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("নতুন প্রশ্ন যোগ করুন (+ Add Question)")
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Questions List
                if (questions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "এই কুইজে এখনও কোনো প্রশ্ন যোগ করা হয়নি।",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "উপরের বাটনে ক্লিক করে MCQ অথবা Fill in the blank প্রশ্ন যুক্ত করুন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(questions) { q ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (q.type == "mcq") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = if (q.type == "mcq") "MCQ (বহুনির্বাচনী)" else "শূন্যস্থান পূরণ (Fill blank)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                color = if (q.type == "mcq") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingQuestion = q
                                                    showEditDialog = true
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit Question",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    scope.launch {
                                                        adminRepository.deleteQuestion(q.id, quiz.id, adminUid)
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Question",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${q.order}. ${q.questionText}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    if (q.type == "mcq" && q.options.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            q.options.forEachIndexed { idx, opt ->
                                                val isCorrect = opt.trim().equals(q.answer.trim(), ignoreCase = true)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isCorrect) SuccessGreen.copy(alpha = 0.15f)
                                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "${('ক' + idx)}.",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (isCorrect) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = opt,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isCorrect) SuccessGreen else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (isCorrect) {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                        Icon(
                                                            Icons.Default.CheckCircle,
                                                            contentDescription = "Correct",
                                                            tint = SuccessGreen,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "সঠিক উত্তর: ${q.answer}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = SuccessGreen
                                        )
                                        if (q.acceptedAnswers.isNotEmpty()) {
                                            Text(
                                                text = "গ্রহণযোগ্য রূপভেদ: ${q.acceptedAnswers.joinToString(", ")}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    if (q.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "ব্যাখ্যা: ${q.explanation}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

    // Add / Edit Question Sub-Dialog
    if (showEditDialog) {
        QuestionEditorDialog(
            quizId = quiz.id,
            existingQuestion = editingQuestion,
            nextOrder = questions.size + 1,
            onDismiss = { showEditDialog = false },
            onSave = { questionToSave ->
                scope.launch {
                    adminRepository.saveQuestion(questionToSave, adminUid)
                    showEditDialog = false
                }
            }
        )
    }
}

@Composable
fun QuestionEditorDialog(
    quizId: String,
    existingQuestion: Question?,
    nextOrder: Int,
    onDismiss: () -> Unit,
    onSave: (Question) -> Unit
) {
    var type by remember { mutableStateOf(existingQuestion?.type ?: "mcq") }
    var questionText by remember { mutableStateOf(existingQuestion?.questionText ?: "") }
    var opt1 by remember { mutableStateOf(existingQuestion?.options?.getOrNull(0) ?: "") }
    var opt2 by remember { mutableStateOf(existingQuestion?.options?.getOrNull(1) ?: "") }
    var opt3 by remember { mutableStateOf(existingQuestion?.options?.getOrNull(2) ?: "") }
    var opt4 by remember { mutableStateOf(existingQuestion?.options?.getOrNull(3) ?: "") }
    var correctIndex by remember {
        val curAnswer = existingQuestion?.answer ?: ""
        val idx = existingQuestion?.options?.indexOfFirst { it.trim().equals(curAnswer.trim(), ignoreCase = true) } ?: 0
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }
    var blankAnswer by remember { mutableStateOf(existingQuestion?.answer ?: "") }
    var acceptedAnswersText by remember { mutableStateOf(existingQuestion?.acceptedAnswers?.joinToString(", ") ?: "") }
    var explanation by remember { mutableStateOf(existingQuestion?.explanation ?: "") }
    var points by remember { mutableStateOf((existingQuestion?.points ?: 1).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existingQuestion == null) "নতুন প্রশ্ন যোগ করুন" else "প্রশ্ন সম্পাদনা করুন")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "mcq",
                        onClick = { type = "mcq" },
                        label = { Text("MCQ (বহুনির্বাচনী)") }
                    )
                    FilterChip(
                        selected = type == "fill_blank",
                        onClick = { type = "fill_blank" },
                        label = { Text("শূন্যস্থান পূরণ") }
                    )
                }

                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("প্রশ্ন (Question Text)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                if (type == "mcq") {
                    Text("বিকল্পসমূহ এবং সঠিক উত্তর নির্বাচন করুন:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                    val optionsList = listOf(opt1, opt2, opt3, opt4)
                    val setters = listOf(
                        { v: String -> opt1 = v },
                        { v: String -> opt2 = v },
                        { v: String -> opt3 = v },
                        { v: String -> opt4 = v }
                    )

                    optionsList.forEachIndexed { index, optVal ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = correctIndex == index,
                                onClick = { correctIndex = index }
                            )
                            OutlinedTextField(
                                value = optVal,
                                onValueChange = setters[index],
                                label = { Text("অপশন ${('ক' + index)}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = blankAnswer,
                        onValueChange = { blankAnswer = it },
                        label = { Text("মূল সঠিক উত্তর (Canonical Answer)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = acceptedAnswersText,
                        onValueChange = { acceptedAnswersText = it },
                        label = { Text("গ্রহণযোগ্য বিকল্পসমূহ (কমা দিয়ে লিখুন)") },
                        placeholder = { Text("যেমন: ঢাকা, Dhaka, dhaka") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = explanation,
                    onValueChange = { explanation = it },
                    label = { Text("ব্যাখ্যা (Explanation)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = points,
                    onValueChange = { points = it },
                    label = { Text("পয়েন্ট (Points)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (questionText.isNotBlank()) {
                        val finalAnswer: String
                        val finalOptions: List<String>
                        val finalAccepted: List<String>

                        if (type == "mcq") {
                            finalOptions = listOf(opt1.trim(), opt2.trim(), opt3.trim(), opt4.trim())
                            finalAnswer = finalOptions.getOrElse(correctIndex) { finalOptions.firstOrNull() ?: "" }
                            finalAccepted = listOf(finalAnswer)
                        } else {
                            finalOptions = emptyList()
                            finalAnswer = blankAnswer.trim()
                            finalAccepted = acceptedAnswersText
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .let { if (it.isEmpty()) listOf(finalAnswer) else it }
                        }

                        val q = Question(
                            id = existingQuestion?.id ?: "q_${System.currentTimeMillis()}",
                            quizId = quizId,
                            type = type,
                            questionText = questionText.trim(),
                            options = finalOptions,
                            answer = finalAnswer,
                            acceptedAnswers = finalAccepted,
                            explanation = explanation.trim(),
                            points = points.toIntOrNull() ?: 1,
                            order = existingQuestion?.order ?: nextOrder
                        )
                        onSave(q)
                    }
                }
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
