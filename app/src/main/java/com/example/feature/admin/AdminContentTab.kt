package com.example.feature.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.core.model.*
import com.example.data.repository.AdminRepository
import com.example.ui.components.DifficultyBadge
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

enum class ContentSection(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CLASSES("শ্রেণি (Classes)", Icons.Default.School),
    SUBJECTS("বিষয় (Subjects)", Icons.Default.Book),
    CHAPTERS("অধ্যায় (Chapters)", Icons.Default.Layers),
    QUIZZES("কুইজ ও প্রশ্ন (Quizzes)", Icons.Default.Quiz)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContentTab(
    adminRepository: AdminRepository,
    adminUid: String
) {
    var currentSection by remember { mutableStateOf(ContentSection.CLASSES) }
    val scope = rememberCoroutineScope()

    // Classes Flow
    val classes by adminRepository.getAllClasses().collectAsState(initial = emptyList())
    var selectedClassId by remember { mutableStateOf<String>("class_9") }

    LaunchedEffect(classes) {
        if (classes.isNotEmpty() && classes.none { it.id == selectedClassId }) {
            selectedClassId = classes.first().id
        }
    }

    // Subjects Flow
    val subjects by adminRepository.getSubjects(selectedClassId).collectAsState(initial = emptyList())
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(subjects) {
        if (selectedSubjectId == null || subjects.none { it.id == selectedSubjectId }) {
            selectedSubjectId = subjects.firstOrNull()?.id
        }
    }

    // Chapters Flow
    val chapters by adminRepository.getChapters(selectedSubjectId ?: "").collectAsState(initial = emptyList())
    var selectedChapterId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chapters) {
        if (selectedChapterId == null || chapters.none { it.id == selectedChapterId }) {
            selectedChapterId = chapters.firstOrNull()?.id
        }
    }

    // Quizzes Flow
    val quizzes by adminRepository.getQuizzes(selectedChapterId ?: "").collectAsState(initial = emptyList())

    // Active Quiz for Questions Dialog
    var activeQuizForQuestions by remember { mutableStateOf<Quiz?>(null) }

    // Dialog States
    var showClassDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<EducationClass?>(null) }

    var showSubjectDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }

    var showChapterDialog by remember { mutableStateOf(false) }
    var editingChapter by remember { mutableStateOf<Chapter?>(null) }

    var showQuizDialog by remember { mutableStateOf(false) }
    var editingQuiz by remember { mutableStateOf<Quiz?>(null) }

    var itemToDelete by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) } // type, name, deleteAction

    Column(modifier = Modifier.fillMaxSize()) {
        // Section Segmented Selector
        ScrollableTabRow(
            selectedTabIndex = currentSection.ordinal,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            ContentSection.entries.forEach { section ->
                Tab(
                    selected = currentSection == section,
                    onClick = { currentSection = section },
                    text = { Text(section.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(section.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_content_tab_${section.name.lowercase()}")
                )
            }
        }

        // Section Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (currentSection) {
                // ==========================================
                // 1. CLASSES MANAGEMENT
                // ==========================================
                ContentSection.CLASSES -> {
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
                                        text = "শ্রেণি ব্যবস্থাপনা (Class Management)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "মোট শ্রেণি: ${classes.size}টি (ডায়নামিক ক্লাউড ডাটাবেজ)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        editingClass = null
                                        showClassDialog = true
                                    },
                                    modifier = Modifier.testTag("admin_add_class_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("শ্রেণি যোগ")
                                }
                            }
                        }

                        if (classes.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("কোনো শ্রেণি পাওয়া যায়নি।", fontWeight = FontWeight.Bold)
                                        Text("নতুন শ্রেণি তৈরি করতে উপরের বাটনে চাপ দিন।", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        } else {
                            items(classes) { cls ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (cls.isActive) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (cls.id == selectedClassId) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "#${cls.order}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = cls.bengaliName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Text(
                                                        text = "${cls.name} • ID: ${cls.id}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Enable/Disable Switch
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Switch(
                                                    checked = cls.isActive,
                                                    onCheckedChange = { isEnabled ->
                                                        scope.launch {
                                                            adminRepository.toggleEnableClass(cls.id, isEnabled, adminUid)
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Reorder buttons
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        if (cls.order > 1) {
                                                            scope.launch {
                                                                adminRepository.reorderClass(cls.id, cls.order - 1, adminUid)
                                                            }
                                                        }
                                                    },
                                                    enabled = cls.order > 1,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            adminRepository.reorderClass(cls.id, cls.order + 1, adminUid)
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                // View Subjects Button
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedClassId = cls.id
                                                        currentSection = ContentSection.SUBJECTS
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("বিষয়সমূহ (${cls.bengaliName})", fontSize = 11.sp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }

                                                // Edit Button
                                                IconButton(
                                                    onClick = {
                                                        editingClass = cls
                                                        showClassDialog = true
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Class", modifier = Modifier.size(18.dp))
                                                }

                                                // Delete Button
                                                IconButton(
                                                    onClick = {
                                                        itemToDelete = Triple("Class", cls.bengaliName) {
                                                            scope.launch {
                                                                adminRepository.deleteClass(cls.id, adminUid)
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete Class",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
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

                // ==========================================
                // 2. SUBJECTS MANAGEMENT
                // ==========================================
                ContentSection.SUBJECTS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Class Selector Header
                        item {
                            Column {
                                Text(
                                    text = "শ্রেণি অনুযায়ী ফিল্টার করুন:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(classes) { cls ->
                                        FilterChip(
                                            selected = cls.id == selectedClassId,
                                            onClick = { selectedClassId = cls.id },
                                            label = { Text(cls.bengaliName) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentClass = classes.find { it.id == selectedClassId }
                                Column {
                                    Text(
                                        text = "${currentClass?.bengaliName ?: "নির্বাচিত শ্রেণি"}-এর বিষয়সমূহ",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "মোট বিষয়: ${subjects.size}টি",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        editingSubject = null
                                        showSubjectDialog = true
                                    },
                                    modifier = Modifier.testTag("admin_add_subject_button")
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("এই শ্রেণিতে কোনো বিষয় নেই।", fontWeight = FontWeight.Bold)
                                        Text("নতুন বিষয় যোগ করতে উপরের বাটনে চাপ দিন।", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        } else {
                            items(subjects) { sub ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (sub.isActive) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (sub.id == selectedSubjectId) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = try {
                                                        Color(android.graphics.Color.parseColor(sub.colorHex))
                                                    } catch (e: Exception) {
                                                        MaterialTheme.colorScheme.primary
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Default.MenuBook,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = sub.bengaliName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Text(
                                                        text = "${sub.name} • ক্রম: #${sub.order}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Switch(
                                                checked = sub.isActive,
                                                onCheckedChange = { isEnabled ->
                                                    scope.launch {
                                                        adminRepository.toggleEnableSubject(sub.id, isEnabled, adminUid)
                                                    }
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Reorder buttons
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        if (sub.order > 1) {
                                                            scope.launch {
                                                                adminRepository.reorderSubject(sub.id, sub.order - 1, adminUid)
                                                            }
                                                        }
                                                    },
                                                    enabled = sub.order > 1,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            adminRepository.reorderSubject(sub.id, sub.order + 1, adminUid)
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedSubjectId = sub.id
                                                        currentSection = ContentSection.CHAPTERS
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("অধ্যায়সমূহ (${sub.bengaliName})", fontSize = 11.sp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        editingSubject = sub
                                                        showSubjectDialog = true
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Subject", modifier = Modifier.size(18.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        itemToDelete = Triple("Subject", sub.bengaliName) {
                                                            scope.launch {
                                                                adminRepository.deleteSubject(sub.id, adminUid)
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete Subject",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
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

                // ==========================================
                // 3. CHAPTERS MANAGEMENT
                // ==========================================
                ContentSection.CHAPTERS -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Class & Subject filters
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("১. শ্রেণি নির্বাচন:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(classes) { cls ->
                                        FilterChip(
                                            selected = cls.id == selectedClassId,
                                            onClick = { selectedClassId = cls.id },
                                            label = { Text(cls.bengaliName, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                Text("২. বিষয় নির্বাচন:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(subjects) { sub ->
                                        FilterChip(
                                            selected = sub.id == selectedSubjectId,
                                            onClick = { selectedSubjectId = sub.id },
                                            label = { Text(sub.bengaliName, fontSize = 12.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentSub = subjects.find { it.id == selectedSubjectId }
                                Column {
                                    Text(
                                        text = "${currentSub?.bengaliName ?: "নির্বাচিত বিষয়"}-এর অধ্যায়সমূহ",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "মোট অধ্যায়: ${chapters.size}টি",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        editingChapter = null
                                        showChapterDialog = true
                                    },
                                    enabled = selectedSubjectId != null,
                                    modifier = Modifier.testTag("admin_add_chapter_button")
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("এই বিষয়ে কোনো অধ্যায় নেই।", fontWeight = FontWeight.Bold)
                                        Text("নতুন অধ্যায় তৈরি করতে উপরের বাটনে চাপ দিন।", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        } else {
                            items(chapters) { chap ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (chap.isActive) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (chap.id == selectedChapterId) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = chap.bengaliTitle,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = "অধ্যায় #${chap.chapterNumber} • ${chap.title}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                if (chap.description.isNotBlank()) {
                                                    Text(
                                                        text = chap.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Switch(
                                                checked = chap.isActive,
                                                onCheckedChange = { isEnabled ->
                                                    scope.launch {
                                                        adminRepository.toggleEnableChapter(chap.id, isEnabled, adminUid)
                                                    }
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Reorder buttons
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        if (chap.order > 1) {
                                                            scope.launch {
                                                                adminRepository.reorderChapter(chap.id, chap.order - 1, adminUid)
                                                            }
                                                        }
                                                    },
                                                    enabled = chap.order > 1,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        scope.launch {
                                                            adminRepository.reorderChapter(chap.id, chap.order + 1, adminUid)
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedChapterId = chap.id
                                                        currentSection = ContentSection.QUIZZES
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("কুইজসমূহ", fontSize = 11.sp)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        editingChapter = chap
                                                        showChapterDialog = true
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Chapter", modifier = Modifier.size(18.dp))
                                                }

                                                IconButton(
                                                    onClick = {
                                                        itemToDelete = Triple("Chapter", chap.bengaliTitle) {
                                                            scope.launch {
                                                                adminRepository.deleteChapter(chap.id, adminUid)
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete Chapter",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
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

                // ==========================================
                // 4. QUIZZES & QUESTIONS MANAGEMENT
                // ==========================================
                ContentSection.QUIZZES -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Filters
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("১. শ্রেণি:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(classes) { cls ->
                                        FilterChip(
                                            selected = cls.id == selectedClassId,
                                            onClick = { selectedClassId = cls.id },
                                            label = { Text(cls.bengaliName, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                Text("২. বিষয়:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(subjects) { sub ->
                                        FilterChip(
                                            selected = sub.id == selectedSubjectId,
                                            onClick = { selectedSubjectId = sub.id },
                                            label = { Text(sub.bengaliName, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                Text("৩. অধ্যায়:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(chapters) { chap ->
                                        FilterChip(
                                            selected = chap.id == selectedChapterId,
                                            onClick = { selectedChapterId = chap.id },
                                            label = { Text(chap.bengaliTitle, fontSize = 12.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentChap = chapters.find { it.id == selectedChapterId }
                                Column {
                                    Text(
                                        text = "${currentChap?.bengaliTitle ?: "নির্বাচিত অধ্যায়"}-এর কুইজসমূহ",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "মোট কুইজ: ${quizzes.size}টি",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = {
                                        editingQuiz = null
                                        showQuizDialog = true
                                    },
                                    enabled = selectedChapterId != null,
                                    modifier = Modifier.testTag("admin_add_quiz_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("কুইজ তৈরি")
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("এই অধ্যায়ে এখনও কোনো কুইজ নেই।", fontWeight = FontWeight.Bold)
                                        Text("ম্যানুয়ালি কুইজ তৈরি করতে উপরের বাটন চাপুন অথবা JSON ইম্পোর্ট ট্যাব ব্যবহার করুন।", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
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
                                            Text(
                                                text = "⏱️ ${quiz.timeLimitSeconds / 60} মিনিট",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Text(
                                                text = "❓ ${quiz.questionsCount}টি প্রশ্ন",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Text(
                                                text = "🏆 মোট পয়েন্ট: ${quiz.totalPoints}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Publish / Unpublish Toggle
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Switch(
                                                    checked = quiz.isPublished,
                                                    onCheckedChange = { isPub ->
                                                        scope.launch {
                                                            adminRepository.togglePublishQuiz(quiz.id, isPub, adminUid)
                                                        }
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (quiz.isPublished) "প্রকাশিত" else "খসড়া (Draft)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (quiz.isPublished) SuccessGreen else MaterialTheme.colorScheme.outline
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                // Manage Questions Button
                                                Button(
                                                    onClick = { activeQuizForQuestions = quiz },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("প্রশ্নসমূহ (${quiz.questionsCount})", fontSize = 11.sp)
                                                }

                                                // Edit Quiz
                                                IconButton(
                                                    onClick = {
                                                        editingQuiz = quiz
                                                        showQuizDialog = true
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Quiz", modifier = Modifier.size(18.dp))
                                                }

                                                // Delete Quiz
                                                IconButton(
                                                    onClick = {
                                                        itemToDelete = Triple("Quiz", quiz.title) {
                                                            scope.launch {
                                                                adminRepository.deleteQuiz(quiz.id, adminUid)
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete Quiz",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
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

    // ==========================================
    // DIALOGS SECTION
    // ==========================================

    // Class Dialog (Create / Edit)
    if (showClassDialog) {
        ClassEditorDialog(
            existingClass = editingClass,
            nextOrder = classes.size + 1,
            onDismiss = { showClassDialog = false },
            onSave = { clsToSave ->
                scope.launch {
                    adminRepository.saveClass(clsToSave, adminUid)
                    showClassDialog = false
                }
            }
        )
    }

    // Subject Dialog (Create / Edit)
    if (showSubjectDialog) {
        SubjectEditorDialog(
            classId = selectedClassId,
            existingSubject = editingSubject,
            nextOrder = subjects.size + 1,
            onDismiss = { showSubjectDialog = false },
            onSave = { subToSave ->
                scope.launch {
                    adminRepository.saveSubject(subToSave, adminUid)
                    showSubjectDialog = false
                }
            }
        )
    }

    // Chapter Dialog (Create / Edit)
    if (showChapterDialog) {
        ChapterEditorDialog(
            classId = selectedClassId,
            subjectId = selectedSubjectId ?: "",
            existingChapter = editingChapter,
            nextOrder = chapters.size + 1,
            onDismiss = { showChapterDialog = false },
            onSave = { chapToSave ->
                scope.launch {
                    adminRepository.saveChapter(chapToSave, adminUid)
                    showChapterDialog = false
                }
            }
        )
    }

    // Quiz Dialog (Create / Edit)
    if (showQuizDialog) {
        QuizEditorDialog(
            classId = selectedClassId,
            subjectId = selectedSubjectId ?: "",
            chapterId = selectedChapterId ?: "",
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

    // Delete Confirmation Dialog
    itemToDelete?.let { (type, name, action) ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("$type মুছে ফেলতে চান?") },
            text = { Text("'$name' মুছে ফেলা হলে এর সাথে সংশ্লিষ্ট সকল ডেটা ডাটাবেজ থেকে বিলুপ্ত হবে। আপনি কি নিশ্চিত?") },
            confirmButton = {
                Button(
                    onClick = {
                        action()
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন (Delete)")
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
                    label = { Text("বিষয়ের বাংলা নাম (যেমন: বাংলা, গণিত)") },
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
