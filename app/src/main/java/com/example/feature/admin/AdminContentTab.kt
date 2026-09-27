package com.example.feature.admin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.ui.components.DifficultyBadge
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch

sealed class ContentNavState {
    object Classes : ContentNavState()
    data class Subjects(val eduClass: EducationClass) : ContentNavState()
    data class Chapters(val eduClass: EducationClass, val subject: Subject) : ContentNavState()
    data class Quizzes(val eduClass: EducationClass, val subject: Subject, val chapter: Chapter) : ContentNavState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContentTab(
    adminRepository: AdminRepository,
    adminUid: String
) {
    var navState by remember { mutableStateOf<ContentNavState>(ContentNavState.Classes) }
    val scope = rememberCoroutineScope()

    // Handle back button hierarchically: Quizzes -> Chapters -> Subjects -> Classes
    BackHandler(enabled = navState !is ContentNavState.Classes) {
        when (val s = navState) {
            is ContentNavState.Quizzes -> navState = ContentNavState.Chapters(s.eduClass, s.subject)
            is ContentNavState.Chapters -> navState = ContentNavState.Subjects(s.eduClass)
            is ContentNavState.Subjects -> navState = ContentNavState.Classes
            ContentNavState.Classes -> {}
        }
    }

    // Dialog States
    var showClassDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<EducationClass?>(null) }

    var showSubjectDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }

    var showChapterDialog by remember { mutableStateOf(false) }
    var editingChapter by remember { mutableStateOf<Chapter?>(null) }

    var showQuizDialog by remember { mutableStateOf(false) }
    var editingQuiz by remember { mutableStateOf<Quiz?>(null) }

    var showJsonImportDialog by remember { mutableStateOf(false) }
    var activeQuizForQuestions by remember { mutableStateOf<Quiz?>(null) }

    var itemToDelete by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ==========================================
        // 1. HIERARCHICAL BREADCRUMB HEADER
        // ==========================================
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button if in nested view
                if (navState !is ContentNavState.Classes) {
                    IconButton(
                        onClick = {
                            when (val s = navState) {
                                is ContentNavState.Quizzes -> navState = ContentNavState.Chapters(s.eduClass, s.subject)
                                is ContentNavState.Chapters -> navState = ContentNavState.Subjects(s.eduClass)
                                is ContentNavState.Subjects -> navState = ContentNavState.Classes
                                ContentNavState.Classes -> {}
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Breadcrumb trail
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "কনটেন্ট",
                        fontWeight = if (navState is ContentNavState.Classes) FontWeight.Bold else FontWeight.Normal,
                        color = if (navState is ContentNavState.Classes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { navState = ContentNavState.Classes }
                    )

                    when (val s = navState) {
                        is ContentNavState.Subjects -> {
                            Text(" › ", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = s.eduClass.bengaliName,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                        is ContentNavState.Chapters -> {
                            Text(" › ", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = s.eduClass.bengaliName,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { navState = ContentNavState.Subjects(s.eduClass) }
                            )
                            Text(" › ", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = s.subject.bengaliName,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                        is ContentNavState.Quizzes -> {
                            Text(" › ", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = s.eduClass.bengaliName,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { navState = ContentNavState.Subjects(s.eduClass) }
                            )
                            Text(" › ", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = s.subject.bengaliName,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable { navState = ContentNavState.Chapters(s.eduClass, s.subject) }
                            )
                            Text(" › ", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = s.chapter.bengaliTitle,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )
                        }
                        ContentNavState.Classes -> {}
                    }
                }
            }
        }

        // ==========================================
        // 2. MAIN HIERARCHICAL CONTENT PANELS
        // ==========================================
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (val s = navState) {
                // LEVEL 1: CLASS LIST
                is ContentNavState.Classes -> {
                    val classes by adminRepository.getAllClasses().collectAsState(initial = emptyList())

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "সকল শ্রেণি (Class Hierarchy)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "যেকোনো শ্রেণিতে ট্যাপ করে এর বিষয়সমূহ ব্রাউজ করুন।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        editingClass = null
                                        showClassDialog = true
                                    },
                                    modifier = Modifier.testTag("admin_add_class_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("শ্রেণি যোগ")
                                }
                            }
                        }

                        items(classes) { cls ->
                            Card(
                                onClick = { navState = ContentNavState.Subjects(cls) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (cls.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "#${cls.order}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cls.bengaliName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "${cls.name} • ট্যাপ করে বিষয় দেখুন",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Switch
                                    Switch(
                                        checked = cls.isActive,
                                        onCheckedChange = { isEnabled ->
                                            scope.launch { adminRepository.toggleEnableClass(cls.id, isEnabled, adminUid) }
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Edit
                                    IconButton(
                                        onClick = {
                                            editingClass = cls
                                            showClassDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = {
                                            itemToDelete = Triple("Class", cls.bengaliName) {
                                                scope.launch { adminRepository.deleteClass(cls.id, adminUid) }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }

                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View Subjects", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // LEVEL 2: SUBJECT LIST FOR SELECTED CLASS
                is ContentNavState.Subjects -> {
                    val subjects by adminRepository.getSubjects(s.eduClass.id).collectAsState(initial = emptyList())

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${s.eduClass.bengaliName}-এর বিষয়সমূহ",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "যেকোনো বিষয়ে ট্যাপ করে এর অধ্যায়সমূহ দেখুন।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        editingSubject = null
                                        showSubjectDialog = true
                                    },
                                    modifier = Modifier.testTag("admin_add_subject_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("বিষয় যোগ")
                                }
                            }
                        }

                        if (subjects.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("${s.eduClass.bengaliName}-এ কোনো বিষয় নেই।", fontWeight = FontWeight.Bold)
                                        Text("নতুন বিষয় যোগ করতে উপরের বাটনে চাপ দিন।", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        } else {
                            items(subjects) { sub ->
                                Card(
                                    onClick = { navState = ContentNavState.Chapters(s.eduClass, sub) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (sub.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = try { Color(android.graphics.Color.parseColor(sub.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sub.bengaliName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "${sub.name} • অধ্যায়সমূহ দেখতে ট্যাপ করুন",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Switch(
                                            checked = sub.isActive,
                                            onCheckedChange = { isEnabled ->
                                                scope.launch { adminRepository.toggleEnableSubject(sub.id, isEnabled, adminUid) }
                                            }
                                        )

                                        IconButton(
                                            onClick = {
                                                editingSubject = sub
                                                showSubjectDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                itemToDelete = Triple("Subject", sub.bengaliName) {
                                                    scope.launch { adminRepository.deleteSubject(sub.id, adminUid) }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }

                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View Chapters", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // LEVEL 3: CHAPTER LIST FOR SELECTED SUBJECT
                is ContentNavState.Chapters -> {
                    val chapters by adminRepository.getChapters(s.subject.id).collectAsState(initial = emptyList())

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${s.subject.bengaliName}-এর অধ্যায়সমূহ",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "যেকোনো অধ্যায়ে ট্যাপ করে এর কুইজ ম্যানেজ করুন।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        editingChapter = null
                                        showChapterDialog = true
                                    },
                                    modifier = Modifier.testTag("admin_add_chapter_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("অধ্যায় যোগ")
                                }
                            }
                        }

                        if (chapters.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("${s.subject.bengaliName}-এ কোনো অধ্যায় নেই।", fontWeight = FontWeight.Bold)
                                        Text("নতুন অধ্যায় যোগ করতে উপরের বাটনে চাপ দিন।", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        } else {
                            items(chapters) { chap ->
                                Card(
                                    onClick = { navState = ContentNavState.Quizzes(s.eduClass, s.subject, chap) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (chap.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${chap.chapterNumber}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = chap.bengaliTitle,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "অধ্যায় ${chap.chapterNumber} • কুইজ দেখতে ট্যাপ করুন",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Switch(
                                            checked = chap.isActive,
                                            onCheckedChange = { isEnabled ->
                                                scope.launch { adminRepository.toggleEnableChapter(chap.id, isEnabled, adminUid) }
                                            }
                                        )

                                        IconButton(
                                            onClick = {
                                                editingChapter = chap
                                                showChapterDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                itemToDelete = Triple("Chapter", chap.bengaliTitle) {
                                                    scope.launch { adminRepository.deleteChapter(chap.id, adminUid) }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }

                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View Quizzes", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // LEVEL 4: QUIZ LIST FOR SELECTED CHAPTER
                is ContentNavState.Quizzes -> {
                    val quizzes by adminRepository.getQuizzes(s.chapter.id).collectAsState(initial = emptyList())

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${s.chapter.bengaliTitle}-এর কুইজসমূহ",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            editingQuiz = null
                                            showQuizDialog = true
                                        },
                                        modifier = Modifier.weight(1f).testTag("admin_add_quiz_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("কুইজ তৈরি")
                                    }

                                    OutlinedButton(
                                        onClick = { showJsonImportDialog = true },
                                        modifier = Modifier.weight(1f).testTag("admin_import_json_context_button"),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("JSON ইম্পোর্ট")
                                    }
                                }
                            }
                        }

                        if (quizzes.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("এই অধ্যায়ে কোনো কুইজ নেই।", fontWeight = FontWeight.Bold)
                                        Text("ম্যানুয়ালি কুইজ তৈরি করতে পারেন অথবা এক ক্লিকে JSON ইম্পোর্ট করতে পারেন।", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        } else {
                            items(quizzes) { quiz ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = quiz.title,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.weight(1f)
                                            )
                                            DifficultyBadge(difficulty = quiz.difficulty)
                                        }

                                        if (quiz.description.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = quiz.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("⏱️ ${quiz.timeLimitSeconds / 60} মিনিট", style = MaterialTheme.typography.labelSmall)
                                            Text("❓ ${quiz.questionsCount}টি প্রশ্ন", style = MaterialTheme.typography.labelSmall)
                                            Text("🏆 ${quiz.totalPoints} পয়েন্ট", style = MaterialTheme.typography.labelSmall)
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Switch(
                                                    checked = quiz.isPublished,
                                                    onCheckedChange = { isPub ->
                                                        scope.launch { adminRepository.togglePublishQuiz(quiz.id, isPub, adminUid) }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (quiz.isPublished) "প্রকাশিত" else "খসড়া",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (quiz.isPublished) SuccessGreen else MaterialTheme.colorScheme.outline
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { activeQuizForQuestions = quiz },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("প্রশ্নসমূহ (${quiz.questionsCount})", fontSize = 11.sp)
                                                }

                                                IconButton(
                                                    onClick = {
                                                        editingQuiz = quiz
                                                        showQuizDialog = true
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Quiz", modifier = Modifier.size(18.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        itemToDelete = Triple("Quiz", quiz.title) {
                                                            scope.launch { adminRepository.deleteQuiz(quiz.id, adminUid) }
                                                        }
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
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

    // ==========================================
    // DIALOGS SECTION
    // ==========================================

    // Class Dialog
    if (showClassDialog) {
        ClassEditorDialog(
            existingClass = editingClass,
            nextOrder = 10,
            onDismiss = { showClassDialog = false },
            onSave = { clsToSave ->
                scope.launch {
                    adminRepository.saveClass(clsToSave, adminUid)
                    showClassDialog = false
                }
            }
        )
    }

    // Subject Dialog
    if (showSubjectDialog) {
        val currentClass = (navState as? ContentNavState.Subjects)?.eduClass
            ?: (navState as? ContentNavState.Chapters)?.eduClass
            ?: (navState as? ContentNavState.Quizzes)?.eduClass
        if (currentClass != null) {
            SubjectEditorDialog(
                classId = currentClass.id,
                existingSubject = editingSubject,
                nextOrder = 5,
                onDismiss = { showSubjectDialog = false },
                onSave = { subToSave ->
                    scope.launch {
                        adminRepository.saveSubject(subToSave, adminUid)
                        showSubjectDialog = false
                    }
                }
            )
        }
    }

    // Chapter Dialog
    if (showChapterDialog) {
        val currentClass = (navState as? ContentNavState.Chapters)?.eduClass
            ?: (navState as? ContentNavState.Quizzes)?.eduClass
        val currentSubject = (navState as? ContentNavState.Chapters)?.subject
            ?: (navState as? ContentNavState.Quizzes)?.subject
        if (currentClass != null && currentSubject != null) {
            ChapterEditorDialog(
                classId = currentClass.id,
                subjectId = currentSubject.id,
                existingChapter = editingChapter,
                nextOrder = 3,
                onDismiss = { showChapterDialog = false },
                onSave = { chapToSave ->
                    scope.launch {
                        adminRepository.saveChapter(chapToSave, adminUid)
                        showChapterDialog = false
                    }
                }
            )
        }
    }

    // Quiz Dialog
    if (showQuizDialog) {
        val qState = navState as? ContentNavState.Quizzes
        if (qState != null) {
            QuizEditorDialog(
                classId = qState.eduClass.id,
                subjectId = qState.subject.id,
                chapterId = qState.chapter.id,
                existingQuiz = editingQuiz,
                onDismiss = { showQuizDialog = false },
                onSave = { quizToSave ->
                    scope.launch {
                        adminRepository.saveQuiz(quizToSave, adminUid)
                        showQuizDialog = false
                    }
                }
            )
        }
    }

    // Contextual JSON Import Dialog (automatically inherits Class, Subject, Chapter without re-asking!)
    if (showJsonImportDialog && navState is ContentNavState.Quizzes) {
        val qState = navState as ContentNavState.Quizzes
        ContextualJsonImportDialog(
            eduClass = qState.eduClass,
            subject = qState.subject,
            chapter = qState.chapter,
            adminRepository = adminRepository,
            adminUid = adminUid,
            onDismiss = { showJsonImportDialog = false }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { (type, name, action) ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("$type মুছে ফেলতে চান?") },
            text = { Text("'$name' মুছে ফেলা হলে এর সাথে সংশ্লিষ্ট সকল ডেটা ডাটাবেজ থেকে মুছে যাবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        action()
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Question Management Modal
    activeQuizForQuestions?.let { qz ->
        AdminQuestionsDialog(
            quiz = qz,
            adminRepository = adminRepository,
            adminUid = adminUid,
            onDismiss = { activeQuizForQuestions = null }
        )
    }
}

// ---------------------------------------------------------------------------
// CONTEXTUAL JSON IMPORT DIALOG (NO RE-SELECTION NEEDED!)
// ---------------------------------------------------------------------------
@Composable
fun ContextualJsonImportDialog(
    eduClass: EducationClass,
    subject: Subject,
    chapter: Chapter,
    adminRepository: AdminRepository,
    adminUid: String,
    onDismiss: () -> Unit
) {
    var jsonText by remember { mutableStateOf("") }
    var parsedSchema by remember { mutableStateOf<QuizImportSchema?>(null) }
    var detectedReports by remember { mutableStateOf<List<AiCorrectionReport>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingAi by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val sampleTemplate = """
{
  "version": 1,
  "title": "${chapter.bengaliTitle} মডেল কুইজ",
  "description": "${subject.bengaliName} বিষয়ের ${chapter.bengaliTitle} অধ্যায়ের কুইজ",
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
      "explanation": "বাংলাদেশের রাজধানী ঢাকা।",
      "points": 1
    }
  ]
}
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("JSON কুইজ ইম্পোর্ট", fontWeight = FontWeight.Bold)
                Text(
                    text = "লক্ষ্য: ${eduClass.bengaliName} › ${subject.bengaliName} › ${chapter.bengaliTitle}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("JSON কোড পেস্ট করুন:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { jsonText = sampleTemplate }) {
                        Text("টেমপ্লেট লোড", fontSize = 11.sp)
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
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("Paste JSON here...") },
                    shape = RoundedCornerShape(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val res = adminRepository.parseJsonImport(jsonText)
                            res.onSuccess {
                                parsedSchema = it
                                statusMessage = "✅ ভ্যালিডেশন সফল! (${it.questions.size}টি প্রশ্ন)"
                            }.onFailure {
                                statusMessage = "❌ ত্রুটি: ${it.localizedMessage}"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ভ্যালিডেট", fontSize = 12.sp)
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
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = parsedSchema != null && !isCheckingAi
                    ) {
                        if (isCheckingAi) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Text("AI অডিট", fontSize = 12.sp)
                        }
                    }
                }

                if (statusMessage != null) {
                    Text(
                        text = statusMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (statusMessage?.startsWith("✅") == true) SuccessGreen else ErrorRed
                    )
                }

                if (detectedReports.isNotEmpty()) {
                    Text(
                        text = "AI সতর্কতা: ${detectedReports.size}টি সমস্যা সনাক্ত হয়েছে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningOrange
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val schema = parsedSchema ?: return@Button
                    isSaving = true
                    scope.launch {
                        val res = adminRepository.saveImportedQuiz(
                            schema = schema,
                            classId = eduClass.id,
                            subjectId = subject.id,
                            chapterId = chapter.id,
                            adminUid = adminUid
                        )
                        isSaving = false
                        res.onSuccess {
                            onDismiss()
                        }.onFailure {
                            statusMessage = "ইম্পোর্ট ব্যর্থ: ${it.localizedMessage}"
                        }
                    }
                },
                enabled = parsedSchema != null && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("অনুমোদন ও ইম্পোর্ট")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

// ---------------------------------------------------------------------------
// SUB-DIALOGS: CLASS, SUBJECT, CHAPTER, QUIZ
// ---------------------------------------------------------------------------

@Composable
fun ClassEditorDialog(
    existingClass: EducationClass?,
    nextOrder: Int,
    onDismiss: () -> Unit,
    onSave: (EducationClass) -> Unit
) {
    var name by remember { mutableStateOf(existingClass?.name ?: "") }
    var bengaliName by remember { mutableStateOf(existingClass?.bengaliName ?: "") }
    var order by remember { mutableStateOf((existingClass?.order ?: nextOrder).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingClass == null) "নতুন শ্রেণি তৈরি করুন" else "শ্রেণি সম্পাদনা করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = bengaliName,
                    onValueChange = { bengaliName = it },
                    label = { Text("শ্রেণির বাংলা নাম (যেমন: নবম শ্রেণি)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("শ্রেণির ইংরেজি নাম (যেমন: Class 9)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = order,
                    onValueChange = { order = it },
                    label = { Text("ক্রমিক নম্বর (Order)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bengaliName.isNotBlank() && name.isNotBlank()) {
                        val parsedOrder = order.toIntOrNull() ?: nextOrder
                        val cls = EducationClass(
                            id = existingClass?.id ?: "class_${System.currentTimeMillis()}",
                            name = name.trim(),
                            bengaliName = bengaliName.trim(),
                            order = parsedOrder,
                            iconName = existingClass?.iconName ?: "school",
                            isActive = existingClass?.isActive ?: true
                        )
                        onSave(cls)
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

@Composable
fun SubjectEditorDialog(
    classId: String,
    existingSubject: Subject?,
    nextOrder: Int,
    onDismiss: () -> Unit,
    onSave: (Subject) -> Unit
) {
    var name by remember { mutableStateOf(existingSubject?.name ?: "") }
    var bengaliName by remember { mutableStateOf(existingSubject?.bengaliName ?: "") }
    var colorHex by remember { mutableStateOf(existingSubject?.colorHex ?: "#2563EB") }
    var order by remember { mutableStateOf((existingSubject?.order ?: nextOrder).toString()) }

    val presetColors = listOf("#2563EB", "#7C3AED", "#059669", "#D97706", "#DC2626", "#0891B2", "#4F46E5")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingSubject == null) "নতুন বিষয় তৈরি করুন" else "বিষয় সম্পাদনা করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = bengaliName,
                    onValueChange = { bengaliName = it },
                    label = { Text("বিষয়ের বাংলা নাম (যেমন: গণিত, বাংলা)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("বিষয়ের ইংরেজি নাম (যেমন: Mathematics)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = order,
                    onValueChange = { order = it },
                    label = { Text("ক্রমিক নম্বর (Order)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("কালার নির্বাচন করুন:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetColors) { hex ->
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bengaliName.isNotBlank() && name.isNotBlank()) {
                        val parsedOrder = order.toIntOrNull() ?: nextOrder
                        val sub = Subject(
                            id = existingSubject?.id ?: "sub_${classId}_${System.currentTimeMillis()}",
                            classId = classId,
                            name = name.trim(),
                            bengaliName = bengaliName.trim(),
                            iconName = existingSubject?.iconName ?: "book",
                            colorHex = colorHex,
                            order = parsedOrder,
                            isActive = existingSubject?.isActive ?: true
                        )
                        onSave(sub)
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

@Composable
fun ChapterEditorDialog(
    classId: String,
    subjectId: String,
    existingChapter: Chapter?,
    nextOrder: Int,
    onDismiss: () -> Unit,
    onSave: (Chapter) -> Unit
) {
    var title by remember { mutableStateOf(existingChapter?.title ?: "") }
    var bengaliTitle by remember { mutableStateOf(existingChapter?.bengaliTitle ?: "") }
    var chapterNumber by remember { mutableStateOf((existingChapter?.chapterNumber ?: nextOrder).toString()) }
    var description by remember { mutableStateOf(existingChapter?.description ?: "") }
    var order by remember { mutableStateOf((existingChapter?.order ?: nextOrder).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingChapter == null) "নতুন অধ্যায় তৈরি করুন" else "অধ্যায় সম্পাদনা করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = bengaliTitle,
                    onValueChange = { bengaliTitle = it },
                    label = { Text("অধ্যায়ের বাংলা নাম (যেমন: বাস্তব সংখ্যা)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("অধ্যায়ের ইংরেজি নাম (যেমন: Real Numbers)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chapterNumber,
                        onValueChange = { chapterNumber = it },
                        label = { Text("অধ্যায় নং") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = order,
                        onValueChange = { order = it },
                        label = { Text("ক্রম") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("সংক্ষিপ্ত বিবরণ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bengaliTitle.isNotBlank()) {
                        val chap = Chapter(
                            id = existingChapter?.id ?: "chap_${System.currentTimeMillis()}",
                            subjectId = subjectId,
                            classId = classId,
                            title = if (title.isNotBlank()) title.trim() else bengaliTitle.trim(),
                            bengaliTitle = bengaliTitle.trim(),
                            chapterNumber = chapterNumber.toIntOrNull() ?: 1,
                            description = description.trim(),
                            order = order.toIntOrNull() ?: nextOrder,
                            isActive = existingChapter?.isActive ?: true
                        )
                        onSave(chap)
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

@Composable
fun QuizEditorDialog(
    classId: String,
    subjectId: String,
    chapterId: String,
    existingQuiz: Quiz?,
    onDismiss: () -> Unit,
    onSave: (Quiz) -> Unit
) {
    var title by remember { mutableStateOf(existingQuiz?.title ?: "") }
    var description by remember { mutableStateOf(existingQuiz?.description ?: "") }
    var difficulty by remember { mutableStateOf(existingQuiz?.difficulty ?: "medium") }
    var timeLimitMinutes by remember { mutableStateOf(((existingQuiz?.timeLimitSeconds ?: 300) / 60).toString()) }

    val difficulties = listOf("easy" to "সহজ (Easy)", "medium" to "মাঝারি (Medium)", "hard" to "কঠিন (Hard)")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingQuiz == null) "নতুন কুইজ তৈরি করুন" else "কুইজ সম্পাদনা করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("কুইজের শিরোনাম (Title)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("সংক্ষিপ্ত বিবরণ (Description)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("কঠিনতার মাত্রা:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    difficulties.forEach { (diffKey, diffLabel) ->
                        FilterChip(
                            selected = difficulty == diffKey,
                            onClick = { difficulty = diffKey },
                            label = { Text(diffLabel, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = timeLimitMinutes,
                    onValueChange = { timeLimitMinutes = it },
                    label = { Text("সময়সীমা (মিনিট)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val minutes = timeLimitMinutes.toIntOrNull() ?: 5
                        val qz = Quiz(
                            id = existingQuiz?.id ?: "quiz_${System.currentTimeMillis()}",
                            chapterId = chapterId,
                            subjectId = subjectId,
                            classId = classId,
                            title = title.trim(),
                            description = description.trim(),
                            difficulty = difficulty,
                            timeLimitSeconds = minutes * 60,
                            shuffleQuestions = existingQuiz?.shuffleQuestions ?: true,
                            shuffleOptions = existingQuiz?.shuffleOptions ?: true,
                            isPublished = existingQuiz?.isPublished ?: true,
                            questionsCount = existingQuiz?.questionsCount ?: 0,
                            totalPoints = existingQuiz?.totalPoints ?: 0,
                            createdAt = existingQuiz?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(qz)
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

