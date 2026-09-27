package com.example.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.AiManager
import com.example.core.model.*
import com.example.data.repository.AdminRepository
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminJsonImportTab(
    adminRepository: AdminRepository,
    adminUid: String
) {
    val scope = rememberCoroutineScope()

    // 1. Dynamic Classes from Firestore / Room
    val classes by adminRepository.getAllClasses().collectAsState(initial = emptyList())
    var selectedClassId by remember { mutableStateOf<String>("class_9") }

    // Synchronize default selected class when classes load
    LaunchedEffect(classes) {
        if (classes.isNotEmpty() && classes.none { it.id == selectedClassId }) {
            selectedClassId = classes.first().id
        }
    }

    // 2. Dynamic Subjects strictly filtered by selectedClassId
    val subjects by adminRepository.getSubjects(selectedClassId).collectAsState(initial = emptyList())
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(subjects) {
        if (selectedSubjectId == null || subjects.none { it.id == selectedSubjectId }) {
            selectedSubjectId = subjects.firstOrNull()?.id
        }
    }

    // 3. Dynamic Chapters strictly filtered by selectedSubjectId
    val chapters by adminRepository.getChapters(selectedSubjectId ?: "").collectAsState(initial = emptyList())
    var selectedChapterId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chapters) {
        if (selectedChapterId == null || chapters.none { it.id == selectedChapterId }) {
            selectedChapterId = chapters.firstOrNull()?.id
        }
    }

    // Dropdown expanded states
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var chapterDropdownExpanded by remember { mutableStateOf(false) }

    // Quick add chapter dialog
    var showQuickAddChapterDialog by remember { mutableStateOf(false) }
    var newChapterTitle by remember { mutableStateOf("") }

    // Import Form States
    var jsonText by remember { mutableStateOf("") }
    var parsedSchema by remember { mutableStateOf<QuizImportSchema?>(null) }
    var detectedReports by remember { mutableStateOf<List<AiCorrectionReport>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingAi by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val sampleTemplate = """
{
  "version": 1,
  "title": "বীজগণিতীয় অনুশীলন কুইজ",
  "description": "বীজগণিতের মৌলিক সূত্রাবলীর মডেল টেস্ট",
  "difficulty": "medium",
  "timeLimit": 300,
  "shuffleQuestions": true,
  "shuffleOptions": true,
  "questions": [
    {
      "type": "mcq",
      "question": "What is 2 + 2?",
      "options": ["2", "3", "4", "5"],
      "answer": "4",
      "acceptedAnswers": ["4", "four", "৪", "চার"],
      "explanation": "2 + 2 = 4.",
      "points": 1
    },
    {
      "type": "fill_blank",
      "question": "বাংলাদেশের রাজধানী কোনটি?",
      "options": [],
      "answer": "ঢাকা",
      "acceptedAnswers": ["ঢাকা", "Dhaka", "dhaka"],
      "explanation": "বাংলাদেশের রাজধানী ও বৃহত্তম শহর হলো ঢাকা।",
      "points": 1
    }
  ]
}
    """.trimIndent()

    val selectedClass = classes.find { it.id == selectedClassId }
    val selectedSubject = subjects.find { it.id == selectedSubjectId }
    val selectedChapter = chapters.find { it.id == selectedChapterId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "JSON কুইজ ইম্পোর্ট ও এআই কোয়ালিটি অডিট",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "প্রথমে টার্গেট শ্রেণি, বিষয় ও অধ্যায় নির্বাচন করুন। এরপর JSON ভ্যালিডেট ও এআই কোয়ালিটি চেক করে ডাটাবেজে সংরক্ষণ করুন।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ==========================================
        // DYNAMIC TARGET HIERARCHY SELECTOR
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ইম্পোর্ট লক্ষ্য নির্ধারণ (Dynamic Target Hierarchy)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    // 1. Target Class Dynamic Dropdown
                    Column {
                        Text(
                            text = "১. টার্গেট শ্রেণি (Class):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = classDropdownExpanded,
                            onExpandedChange = { classDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedClass?.bengaliName ?: "শ্রেণি নির্বাচন করুন",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("admin_target_class_dropdown"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = classDropdownExpanded,
                                onDismissRequest = { classDropdownExpanded = false }
                            ) {
                                classes.forEach { cls ->
                                    DropdownMenuItem(
                                        text = { Text("${cls.bengaliName} (${cls.name})") },
                                        onClick = {
                                            selectedClassId = cls.id
                                            classDropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            if (cls.id == selectedClassId) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Target Subject Dynamic Dropdown (Filtered by selected class)
                    Column {
                        Text(
                            text = "২. টার্গেট বিষয় (Subject - ${selectedClass?.bengaliName ?: ""}):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (subjects.isEmpty()) {
                            Text(
                                "এই শ্রেণিতে কোনো বিষয় নেই। কনটেন্ট ট্যাব থেকে বিষয় যোগ করুন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = ErrorRed
                            )
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = subjectDropdownExpanded,
                                onExpandedChange = { subjectDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedSubject?.bengaliName ?: "বিষয় নির্বাচন করুন",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("admin_target_subject_dropdown"),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = subjectDropdownExpanded,
                                    onDismissRequest = { subjectDropdownExpanded = false }
                                ) {
                                    subjects.forEach { sub ->
                                        DropdownMenuItem(
                                            text = { Text("${sub.bengaliName} (${sub.name})") },
                                            onClick = {
                                                selectedSubjectId = sub.id
                                                subjectDropdownExpanded = false
                                            },
                                            leadingIcon = {
                                                if (sub.id == selectedSubjectId) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Target Chapter Dynamic Dropdown (Filtered by selected subject)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "৩. টার্গেট অধ্যায় (Chapter - ${selectedSubject?.bengaliName ?: ""}):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            if (selectedSubjectId != null) {
                                TextButton(
                                    onClick = { showQuickAddChapterDialog = true },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ দ্রুত অধ্যায় তৈরি", fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (chapters.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "এই বিষয়ে কোনো অধ্যায় নেই।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WarningOrange
                                )
                                OutlinedButton(
                                    onClick = { showQuickAddChapterDialog = true },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("অধ্যায় তৈরি করুন", fontSize = 11.sp)
                                }
                            }
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = chapterDropdownExpanded,
                                onExpandedChange = { chapterDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedChapter?.bengaliTitle ?: "অধ্যায় নির্বাচন করুন",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("admin_target_chapter_dropdown"),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = chapterDropdownExpanded,
                                    onDismissRequest = { chapterDropdownExpanded = false }
                                ) {
                                    chapters.forEach { chap ->
                                        DropdownMenuItem(
                                            text = { Text("অধ্যায় ${chap.chapterNumber}: ${chap.bengaliTitle}") },
                                            onClick = {
                                                selectedChapterId = chap.id
                                                chapterDropdownExpanded = false
                                            },
                                            leadingIcon = {
                                                if (chap.id == selectedChapterId) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
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
        // JSON INPUT BOX & TEMPLATE
        // ==========================================
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("JSON কোড পেস্ট করুন:", fontWeight = FontWeight.SemiBold)
                    Row {
                        TextButton(onClick = { jsonText = sampleTemplate }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("টেমপ্লেট লোড")
                        }
                        if (jsonText.isNotBlank()) {
                            TextButton(onClick = {
                                jsonText = ""
                                parsedSchema = null
                                detectedReports = emptyList()
                                statusMessage = null
                            }) {
                                Text("পরিষ্কার", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = {
                        jsonText = it
                        parsedSchema = null
                        detectedReports = emptyList()
                        statusMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("admin_json_input"),
                    placeholder = { Text("Paste JSON quiz schema here...") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // ==========================================
        // ACTION BUTTONS (VALIDATE & AI AUDIT)
        // ==========================================
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        statusMessage = null
                        val res = adminRepository.parseJsonImport(jsonText)
                        res.onSuccess {
                            parsedSchema = it
                            statusMessage = "✅ JSON ভ্যালিডেশন সফল! (${it.questions.size}টি প্রশ্ন পাওয়া গেছে)"
                        }.onFailure {
                            statusMessage = "❌ ত্রুটি: ${it.localizedMessage}"
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("validate_json_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ভ্যালিডেট")
                }

                Button(
                    onClick = {
                        val schema = parsedSchema
                        if (schema != null) {
                            isCheckingAi = true
                            scope.launch {
                                val dummyQuestions = schema.questions.mapIndexed { idx, q ->
                                    Question(
                                        id = "temp_$idx",
                                        quizId = "temp_quiz",
                                        type = q.type,
                                        questionText = q.question,
                                        options = q.options,
                                        answer = q.answer,
                                        acceptedAnswers = q.acceptedAnswers,
                                        explanation = q.explanation,
                                        points = q.points,
                                        order = idx + 1
                                    )
                                }
                                val reports = AiManager.instance.inspectQuizQuality(
                                    quizTitle = schema.title,
                                    questions = dummyQuestions
                                )
                                detectedReports = reports
                                isCheckingAi = false
                                if (reports.isNotEmpty()) {
                                    adminRepository.addCorrectionReports(reports)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_quality_check_button"),
                    enabled = parsedSchema != null && !isCheckingAi
                ) {
                    if (isCheckingAi) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI অডিট")
                    }
                }
            }
        }

        // Status Feedback Banner
        if (statusMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (statusMessage?.startsWith("✅") == true) SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (statusMessage?.startsWith("✅") == true) SuccessGreen else ErrorRed
                    )
                }
            }
        }

        // AI Detected Issues
        if (detectedReports.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.1f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = WarningOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI কর্তৃক সনাক্তকৃত সমস্যাসমূহ (${detectedReports.size}টি):",
                                fontWeight = FontWeight.Bold,
                                color = WarningOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        detectedReports.forEach { rep ->
                            Text(
                                text = "• [${rep.issueType}] ${rep.reason} (প্রস্তাবিত: ${rep.proposedValue})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Final Save to Database
        if (parsedSchema != null) {
            item {
                val canSave = selectedChapterId != null && selectedSubjectId != null
                Button(
                    onClick = {
                        val schema = parsedSchema ?: return@Button
                        val chapId = selectedChapterId ?: return@Button
                        val subId = selectedSubjectId ?: return@Button

                        isSaving = true
                        scope.launch {
                            val res = adminRepository.saveImportedQuiz(
                                schema = schema,
                                classId = selectedClassId,
                                subjectId = subId,
                                chapterId = chapId,
                                adminUid = adminUid
                            )
                            isSaving = false
                            res.onSuccess {
                                statusMessage = "🎉 কুইজ সফলভাবে সংরক্ষিত হয়েছে! আইডি: $it\n(লক্ষ্য: ${selectedClass?.bengaliName} ➔ ${selectedSubject?.bengaliName} ➔ ${selectedChapter?.bengaliTitle})"
                                jsonText = ""
                                parsedSchema = null
                            }.onFailure {
                                statusMessage = "সংরক্ষণ ব্যর্থ: ${it.localizedMessage}"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_imported_quiz_button"),
                    enabled = !isSaving && canSave,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (canSave) "অনুমোদন দিন ও ডাটাবেজে সংরক্ষণ করুন (Approve & Save)"
                            else "প্রথমে অধ্যায় নির্বাচন করুন"
                        )
                    }
                }
            }
        }
    }

    // Quick Add Chapter Dialog
    if (showQuickAddChapterDialog) {
        AlertDialog(
            onDismissRequest = { showQuickAddChapterDialog = false },
            title = { Text("নতুন অধ্যায় তৈরি করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "শ্রেণি: ${selectedClass?.bengaliName} • বিষয়: ${selectedSubject?.bengaliName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newChapterTitle,
                        onValueChange = { newChapterTitle = it },
                        label = { Text("অধ্যায়ের শিরোনাম (যেমন: বাস্তব সংখ্যা)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChapterTitle.isNotBlank() && selectedSubjectId != null) {
                            scope.launch {
                                val chapId = "chap_${System.currentTimeMillis()}"
                                val newChap = Chapter(
                                    id = chapId,
                                    subjectId = selectedSubjectId ?: "",
                                    classId = selectedClassId,
                                    title = newChapterTitle.trim(),
                                    bengaliTitle = newChapterTitle.trim(),
                                    chapterNumber = chapters.size + 1,
                                    order = chapters.size + 1,
                                    isActive = true
                                )
                                adminRepository.saveChapter(newChap, adminUid)
                                selectedChapterId = chapId
                                showQuickAddChapterDialog = false
                                newChapterTitle = ""
                            }
                        }
                    }
                ) {
                    Text("তৈরি করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showQuickAddChapterDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
