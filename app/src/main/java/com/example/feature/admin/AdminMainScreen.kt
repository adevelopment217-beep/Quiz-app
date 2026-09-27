package com.example.feature.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.core.ai.AiManager
import com.example.core.model.*
import com.example.data.repository.AdminRepository
import com.example.data.repository.DashboardStats
import com.example.data.repository.QuizRepository
import com.example.ui.components.AppHeader
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.StatCard
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch

enum class AdminTab(val label: String) {
    DASHBOARD("ড্যাশবোর্ড"),
    CONTENT("কনটেন্ট"),
    JSON_IMPORT("JSON ইম্পোর্ট"),
    AI_CORRECTIONS("AI সংশোধন"),
    AI_SETTINGS("AI সেটিংস"),
    AUDIT_LOGS("অডিট লগ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    adminRepository: AdminRepository,
    quizRepository: QuizRepository,
    adminUid: String,
    onBackToUserPanel: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(AdminTab.DASHBOARD) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "অ্যাডমিন কন্ট্রোল সেন্টার",
                subtitle = "MedhaQuiz Management System",
                onBack = onBackToUserPanel,
                actions = {
                    IconButton(
                        onClick = onBackToUserPanel,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("admin_exit_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Exit to User Panel")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(tonalElevation = 8.dp) {
                NavigationBarItem(
                    selected = selectedTab == AdminTab.DASHBOARD,
                    onClick = { selectedTab = AdminTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("ড্যাশবোর্ড", fontSize = 11.sp) },
                    modifier = Modifier.testTag("admin_tab_dashboard")
                )
                NavigationBarItem(
                    selected = selectedTab == AdminTab.CONTENT,
                    onClick = { selectedTab = AdminTab.CONTENT },
                    icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                    label = { Text("কনটেন্ট", fontSize = 11.sp) },
                    modifier = Modifier.testTag("admin_tab_content")
                )
                NavigationBarItem(
                    selected = selectedTab == AdminTab.JSON_IMPORT,
                    onClick = { selectedTab = AdminTab.JSON_IMPORT },
                    icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    label = { Text("JSON", fontSize = 11.sp) },
                    modifier = Modifier.testTag("admin_tab_json")
                )
                NavigationBarItem(
                    selected = selectedTab == AdminTab.AI_CORRECTIONS,
                    onClick = { selectedTab = AdminTab.AI_CORRECTIONS },
                    icon = { Icon(Icons.Default.FactCheck, contentDescription = null) },
                    label = { Text("AI রিপোর্ট", fontSize = 11.sp) },
                    modifier = Modifier.testTag("admin_tab_corrections")
                )
                NavigationBarItem(
                    selected = selectedTab == AdminTab.AI_SETTINGS,
                    onClick = { selectedTab = AdminTab.AI_SETTINGS },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                    label = { Text("AI সেটিংস", fontSize = 11.sp) },
                    modifier = Modifier.testTag("admin_tab_ai_settings")
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                AdminTab.DASHBOARD -> AdminDashboardView(
                    adminRepository = adminRepository,
                    onNavigate = { selectedTab = it }
                )
                AdminTab.CONTENT -> AdminContentManagementView(
                    adminRepository = adminRepository,
                    quizRepository = quizRepository,
                    adminUid = adminUid
                )
                AdminTab.JSON_IMPORT -> AdminJsonImportView(
                    adminRepository = adminRepository,
                    quizRepository = quizRepository,
                    adminUid = adminUid
                )
                AdminTab.AI_CORRECTIONS -> AdminCorrectionReportsView(
                    adminRepository = adminRepository,
                    adminUid = adminUid
                )
                AdminTab.AI_SETTINGS -> AdminAiSettingsView()
                AdminTab.AUDIT_LOGS -> AdminAuditLogsView(adminRepository = adminRepository)
            }
        }
    }
}

// -------------------------------------------------------------
// ADMIN TAB 1: DASHBOARD
// -------------------------------------------------------------
@Composable
fun AdminDashboardView(
    adminRepository: AdminRepository,
    onNavigate: (AdminTab) -> Unit
) {
    var stats by remember { mutableStateOf(DashboardStats()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        stats = adminRepository.getDashboardStats()
        isLoading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "সিস্টেম মেট্রিক্স ও পরিসংখ্যান",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "মোট শ্রেণি",
                    value = "${stats.totalClasses}",
                    icon = Icons.Default.School,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "মোট বিষয়",
                    value = "${stats.totalSubjects}",
                    icon = Icons.Default.Book,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "মোট অধ্যায়",
                    value = "${stats.totalChapters}",
                    icon = Icons.Default.Layers,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "প্রকাশিত কুইজ",
                    value = "${stats.publishedQuizzes}/${stats.totalQuizzes}",
                    icon = Icons.Default.Quiz,
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "AI পেন্ডিং রিপোর্ট",
                    value = "${stats.pendingCorrections}",
                    icon = Icons.Default.Warning,
                    accentColor = if (stats.pendingCorrections > 0) WarningOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "গড় শিক্ষার্থী স্কোর",
                    value = "${stats.averageScore}%",
                    icon = Icons.Default.AutoGraph,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Admin Actions
        item {
            Text(
                text = "দ্রুত কার্যক্রম (Quick Actions):",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            Card(
                onClick = { onNavigate(AdminTab.CONTENT) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("নতুন কুইজ বা প্রশ্ন যোগ করুন", fontWeight = FontWeight.Bold)
                        Text("শ্রেণি, বিষয় ও অধ্যায়ভিত্তিক কনটেন্ট ম্যানেজমেন্ট", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        item {
            Card(
                onClick = { onNavigate(AdminTab.JSON_IMPORT) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("JSON থেকে এক ক্লিকে কুইজ ইম্পোর্ট", fontWeight = FontWeight.Bold)
                        Text("বাল্ক প্রশ্ন আপলোড ও স্বয়ংক্রিয় এআই কোয়ালিটি চেক", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        item {
            Card(
                onClick = { onNavigate(AdminTab.AI_CORRECTIONS) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FactCheck, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("এআই সংশোধন রিপোর্ট পর্যালোচনা", fontWeight = FontWeight.Bold)
                        Text("ভুল উত্তর, অস্পষ্ট প্রশ্ন ও ডুপ্লিকেট অপশন সমাধান", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ADMIN TAB 2: CONTENT MANAGEMENT (Classes, Subjects, Chapters, Quizzes)
// -------------------------------------------------------------
@Composable
fun AdminContentManagementView(
    adminRepository: AdminRepository,
    quizRepository: QuizRepository,
    adminUid: String
) {
    val classes by quizRepository.getClasses().collectAsState(initial = emptyList())
    var selectedClassId by remember { mutableStateOf("class_9") }
    val subjects by quizRepository.getSubjects(selectedClassId).collectAsState(initial = emptyList())
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(subjects) {
        if (selectedSubjectId == null || subjects.none { it.id == selectedSubjectId }) {
            selectedSubjectId = subjects.firstOrNull()?.id
        }
    }

    val chapters by quizRepository.getChapters(selectedSubjectId ?: "").collectAsState(initial = emptyList())
    var selectedChapterId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chapters) {
        if (selectedChapterId == null || chapters.none { it.id == selectedChapterId }) {
            selectedChapterId = chapters.firstOrNull()?.id
        }
    }

    val quizzes by quizRepository.getQuizzes(selectedChapterId ?: "").collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Dialog state for adding a new quiz
    var showCreateQuizDialog by remember { mutableStateOf(false) }
    var newQuizTitle by remember { mutableStateOf("") }
    var newQuizDesc by remember { mutableStateOf("") }
    var newQuizDifficulty by remember { mutableStateOf("medium") }
    var newQuizTimeLimit by remember { mutableStateOf("300") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "শিক্ষা পাঠ্যক্রম ব্যবস্থাপনা",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        // 1. Select Class
        item {
            Column {
                Text("১. শ্রেণি নির্বাচন করুন:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(classes) { eduClass ->
                        FilterChip(
                            selected = eduClass.id == selectedClassId,
                            onClick = { selectedClassId = eduClass.id },
                            label = { Text(eduClass.bengaliName) },
                            modifier = Modifier.testTag("admin_class_chip_${eduClass.id}")
                        )
                    }
                }
            }
        }

        // 2. Select Subject
        item {
            Column {
                Text("২. বিষয় নির্বাচন করুন:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                if (subjects.isEmpty()) {
                    Text("কোনো বিষয় পাওয়া যায়নি।", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(subjects) { sub ->
                            FilterChip(
                                selected = sub.id == selectedSubjectId,
                                onClick = { selectedSubjectId = sub.id },
                                label = { Text(sub.bengaliName) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Select Chapter
        item {
            Column {
                Text("৩. অধ্যায় নির্বাচন করুন:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                if (chapters.isEmpty()) {
                    Text("এই বিষয়ে কোনো অধ্যায় নেই।", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chapters) { chap ->
                            FilterChip(
                                selected = chap.id == selectedChapterId,
                                onClick = { selectedChapterId = chap.id },
                                label = { Text(chap.bengaliTitle) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Quizzes Header & Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "অধ্যায়ের কুইজসমূহ:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Button(
                    onClick = { showCreateQuizDialog = true },
                    modifier = Modifier.testTag("admin_add_quiz_button"),
                    enabled = selectedChapterId != null
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("কুইজ তৈরি")
                }
            }
        }

        // Quizzes List
        if (quizzes.isEmpty()) {
            item {
                Text(
                    text = "এই অধ্যায়ে এখনও কোনো কুইজ নেই। নতুন কুইজ তৈরি করতে উপরের বাটনে চাপ দিন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(quizzes) { quiz ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = quiz.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            DifficultyBadge(difficulty = quiz.difficulty)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "প্রশ্ন: ${quiz.questionsCount}টি • সময়: ${quiz.timeLimitSeconds / 60} মি. • স্থিতি: ${if (quiz.isPublished) "প্রকাশিত" else "খসড়া"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Publish / Unpublish Toggle
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        adminRepository.togglePublishQuiz(quiz.id, !quiz.isPublished, adminUid)
                                    }
                                }
                            ) {
                                Text(if (quiz.isPublished) "অপ্রকাশিত করুন" else "প্রকাশ করুন")
                            }

                            // Delete Quiz Button
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        adminRepository.deleteQuiz(quiz.id, adminUid)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Quiz Dialog
    if (showCreateQuizDialog) {
        AlertDialog(
            onDismissRequest = { showCreateQuizDialog = false },
            title = { Text("নতুন কুইজ তৈরি করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newQuizTitle,
                        onValueChange = { newQuizTitle = it },
                        label = { Text("কুইজের শিরোনাম (Title)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newQuizDesc,
                        onValueChange = { newQuizDesc = it },
                        label = { Text("বিবরণ (Description)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newQuizTimeLimit,
                        onValueChange = { newQuizTimeLimit = it },
                        label = { Text("সময়সীমা (সেকেন্ড)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newQuizTitle.isNotBlank() && selectedChapterId != null && selectedSubjectId != null) {
                            scope.launch {
                                val quiz = Quiz(
                                    id = "quiz_${System.currentTimeMillis()}",
                                    chapterId = selectedChapterId ?: "",
                                    subjectId = selectedSubjectId ?: "",
                                    classId = selectedClassId,
                                    title = newQuizTitle,
                                    description = newQuizDesc,
                                    difficulty = newQuizDifficulty,
                                    timeLimitSeconds = newQuizTimeLimit.toIntOrNull() ?: 300,
                                    isPublished = true,
                                    questionsCount = 0,
                                    totalPoints = 0
                                )
                                adminRepository.saveQuiz(quiz, adminUid)
                                showCreateQuizDialog = false
                                newQuizTitle = ""
                                newQuizDesc = ""
                            }
                        }
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateQuizDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// ADMIN TAB 3: JSON QUIZ IMPORT & AI QUALITY INSPECTION
// -------------------------------------------------------------
@Composable
fun AdminJsonImportView(
    adminRepository: AdminRepository,
    quizRepository: QuizRepository,
    adminUid: String
) {
    var jsonText by remember { mutableStateOf("") }
    var parsedSchema by remember { mutableStateOf<QuizImportSchema?>(null) }
    var detectedReports by remember { mutableStateOf<List<AiCorrectionReport>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingAi by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val classes by quizRepository.getClasses().collectAsState(initial = emptyList())
    var selectedClassId by remember { mutableStateOf("class_9") }
    val subjects by quizRepository.getSubjects(selectedClassId).collectAsState(initial = emptyList())
    var selectedSubjectId by remember { mutableStateOf<String?>("sub_class_9_math") }
    val chapters by quizRepository.getChapters(selectedSubjectId ?: "").collectAsState(initial = emptyList())
    var selectedChapterId by remember { mutableStateOf<String?>("chap_c9_m1") }

    val scope = rememberCoroutineScope()

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
      "acceptedAnswers": ["ঢাকা", "Dhaka"],
      "explanation": "বাংলাদেশের রাজধানী ও বৃহত্তম শহর হলো ঢাকা।",
      "points": 1
    }
  ]
}
    """.trimIndent()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "JSON কুইজ ইম্পোর্ট ও এআই কোয়ালিটি চেক",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "JSON পেস্ট করুন, ভ্যালিডেট করুন এবং এআই এর মাধ্যমে ভুল উত্তর ও অপশন পরীক্ষা করুন।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Target Curriculum Location
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ইম্পোর্ট লক্ষ্য নির্ধারণ (Target Hierarchy):", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text("শ্রেণি: $selectedClassId") }
                        )
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text("বিষয়: ${selectedSubjectId?.substringAfterLast("_") ?: ""}") }
                        )
                    }
                }
            }
        }

        // Paste JSON Box
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("JSON কোড পেস্ট করুন:", fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { jsonText = sampleTemplate }) {
                        Text("টেমপ্লেট লোড করুন")
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

        // Validate Button
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        statusMessage = null
                        val res = adminRepository.parseJsonImport(jsonText)
                        res.onSuccess {
                            parsedSchema = it
                            statusMessage = "✅ JSON সফলভাবে ভ্যালিডেট হয়েছে! (${it.questions.size}টি প্রশ্ন পাওয়া গেছে)"
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
                    Text("ভ্যালিডেট (Validate)")
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
                                        explanation = q.explanation,
                                        points = q.points
                                    )
                                }
                                val reports = AiManager.instance.inspectQuizQuality(schema.title, dummyQuestions)
                                detectedReports = reports
                                if (reports.isNotEmpty()) {
                                    adminRepository.addCorrectionReports(reports)
                                }
                                isCheckingAi = false
                            }
                        }
                    },
                    enabled = parsedSchema != null && !isCheckingAi,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_quality_check_button")
                ) {
                    if (isCheckingAi) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("যাচাই চলছে...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI কোয়ালিটি চেক")
                    }
                }
            }
        }

        if (statusMessage != null) {
            item {
                Text(
                    text = statusMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (statusMessage?.startsWith("✅") == true) SuccessGreen else MaterialTheme.colorScheme.error
                )
            }
        }

        // Detected Quality Issues by AI
        if (detectedReports.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
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
                Button(
                    onClick = {
                        val schema = parsedSchema ?: return@Button
                        isSaving = true
                        scope.launch {
                            val res = adminRepository.saveImportedQuiz(
                                schema = schema,
                                classId = selectedClassId,
                                subjectId = selectedSubjectId ?: "sub_class_9_math",
                                chapterId = selectedChapterId ?: "chap_c9_m1",
                                adminUid = adminUid
                            )
                            isSaving = false
                            res.onSuccess {
                                statusMessage = "🎉 কুইজ সফলভাবে সংরক্ষিত হয়েছে! আইডি: $it"
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
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("অনুমোদন দিন ও ডাটাবেজে সংরক্ষণ করুন (Approve & Save)")
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ADMIN TAB 4: AI CORRECTION REPORTS WORKFLOW
// -------------------------------------------------------------
@Composable
fun AdminCorrectionReportsView(
    adminRepository: AdminRepository,
    adminUid: String
) {
    val reports by adminRepository.correctionReports.collectAsState()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "এআই সংশোধন ও মান নিয়ন্ত্রণ রিপোর্ট",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "এআই যেসকল প্রশ্নের উত্তর বা বিকল্পে অসঙ্গতি খুঁজে পেয়েছে তা পর্যালোচনা করুন। অনুমোদন দিলে ডাটাবেজে মূল উত্তর স্বয়ংক্রিয়ভাবে আপডেট হবে।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (reports.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("কোনো অমীমাংসিত সংশোধন রিপোর্ট নেই।", fontWeight = FontWeight.Bold)
                        Text("সকল কুইজ ও প্রশ্ন নির্ভরযোগ্য অবস্থায় রয়েছে।", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(reports) { rep ->
                val isPending = rep.status == CorrectionStatus.PENDING.name
                val statusColor = when (rep.status) {
                    CorrectionStatus.APPROVED.name -> SuccessGreen
                    CorrectionStatus.REJECTED.name -> ErrorRed
                    else -> WarningOrange
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rep.issueType,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = statusColor
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = rep.status,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "কুইজ: ${rep.quizTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "প্রশ্ন: ${rep.questionText}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "বর্তমান মান: '${rep.currentValue}'", style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                        Text(text = "প্রস্তাবিত মান: '${rep.proposedValue}'", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = SuccessGreen)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "কারণ: ${rep.reason}", style = MaterialTheme.typography.bodySmall)

                        if (isPending) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            adminRepository.rejectCorrection(rep.id, adminUid)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reject_correction_${rep.id}")
                                ) {
                                    Text("প্রত্যাখ্যান (Reject)")
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            adminRepository.approveCorrection(rep, adminUid)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("approve_correction_${rep.id}")
                                ) {
                                    Text("অনুমোদন (Approve)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// ADMIN TAB 5: AI PROVIDER SETTINGS & TUNING
// -------------------------------------------------------------
@Composable
fun AdminAiSettingsView() {
    val aiManager = AiManager.instance
    var provider by remember { mutableStateOf(aiManager.config.provider) }
    var model by remember { mutableStateOf(aiManager.config.model) }
    var baseUrl by remember { mutableStateOf(aiManager.config.baseUrl) }
    var temperature by remember { mutableFloatStateOf(aiManager.config.temperature) }
    var maxTokens by remember { mutableIntStateOf(aiManager.config.maxTokens) }
    var systemPrompt by remember { mutableStateOf(aiManager.config.systemPrompt) }
    var isEnabled by remember { mutableStateOf(aiManager.config.isEnabled) }
    var isSemanticEvalEnabled by remember { mutableStateOf(aiManager.config.isSemanticEvalEnabled) }
    var isQualityCheckEnabled by remember { mutableStateOf(aiManager.config.isQualityCheckEnabled) }

    var testStatusMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val providerOptions = listOf("gemini", "openai", "openrouter", "custom")
    val modelOptions = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview", "gpt-4o-mini", "custom-model")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "এআই ইঞ্জিন ও মডেল কনফিগারেশন",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "এআই প্রোভাইডার, মডেল এবং ফিচারসমূহ ডায়নামিক্যালি নিয়ন্ত্রণ করুন।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Toggles
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("এআই ইঞ্জিন চালু রাখুন (Master Toggle)")
                        Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
                    }
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("সেমেন্টিক উত্তর মূল্যায়ন (Semantic Evaluation)")
                        Switch(checked = isSemanticEvalEnabled, onCheckedChange = { isSemanticEvalEnabled = it })
                    }
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("কুইজ মান নিয়ন্ত্রণ (Quality Inspection)")
                        Switch(checked = isQualityCheckEnabled, onCheckedChange = { isQualityCheckEnabled = it })
                    }
                }
            }
        }

        // Provider & Model
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("মডেল সেটিংস:", fontWeight = FontWeight.Bold)

                    // Provider Chips
                    Text("প্রোভাইডার:", style = MaterialTheme.typography.bodySmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(providerOptions) { prov ->
                            FilterChip(
                                selected = provider == prov,
                                onClick = { provider = prov },
                                label = { Text(prov.uppercase()) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("মডেল নাম (Model Name)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_model_input")
                    )

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("বেস ইউআরএল (Base URL)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Temperature Slider
                    Column {
                        Text("টেম্পারেচার (Temperature): ${String.format("%.1f", temperature)}")
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            valueRange = 0.0f..1.0f,
                            steps = 9
                        )
                    }

                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text("সিস্টেম নির্দেশাবলী (System Prompt)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                }
            }
        }

        // Test Connection & Save
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        testStatusMessage = null
                        scope.launch {
                            val reply = aiManager.getTutorResponse("Admin System Check", "হ্যালো, এআই কানেকশন পরীক্ষা করছি।")
                            isTesting = false
                            testStatusMessage = if (reply.isNotBlank() && !reply.contains("নিষ্ক্রিয়")) {
                                "✅ কানেকশন সফল! এআই উত্তর: ${reply.take(50)}..."
                            } else {
                                "⚠️ নোটিস: $reply"
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
                        Text("টেস্ট কানেকশন")
                    }
                }

                Button(
                    onClick = {
                        val newConfig = AiProviderConfig(
                            provider = provider,
                            model = model,
                            baseUrl = baseUrl,
                            temperature = temperature,
                            maxTokens = maxTokens,
                            systemPrompt = systemPrompt,
                            isEnabled = isEnabled,
                            isAssistantEnabled = isEnabled,
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
                    Text("সংরক্ষণ করুন (Save)")
                }
            }
        }

        if (testStatusMessage != null) {
            item {
                Text(
                    text = testStatusMessage ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (testStatusMessage?.startsWith("✅") == true) SuccessGreen else WarningOrange
                )
            }
        }
    }
}

// -------------------------------------------------------------
// ADMIN TAB 6: AUDIT LOGS
// -------------------------------------------------------------
@Composable
fun AdminAuditLogsView(adminRepository: AdminRepository) {
    val logs by adminRepository.auditLogs.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "নিরাপত্তা ও অডিট লগ (Security Audit Logs)",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "প্রশাসনিক সকল পরিবর্তন এবং অনুমোদনের অপরিবর্তনীয় ট্র্যাকিং রেকর্ড।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(logs) { log ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.action,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = log.collection,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = log.details, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Admin: ${log.adminUid}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
