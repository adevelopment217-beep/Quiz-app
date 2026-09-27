package com.example.feature.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.ai.AiManager
import com.example.core.auth.AuthManager
import com.example.core.model.*
import com.example.data.repository.QuizRepository
import com.example.ui.components.DifficultyBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.StatCard
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch

enum class UserTab(val label: String) {
    HOME("হোম"),
    EXPLORE("পাঠ্যক্রম"),
    PROGRESS("অগ্রগতি"),
    AI("এআই গৃহশিক্ষক"),
    PROFILE("প্রোফাইল")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMainScreen(
    authManager: AuthManager,
    quizRepository: QuizRepository,
    onQuizSelected: (String) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(UserTab.HOME) }
    val currentUser by authManager.currentUser.collectAsState()
    val scope = rememberCoroutineScope()

    // Education Data
    val classes by quizRepository.getClasses().collectAsState(initial = emptyList())
    var activeClassId by remember { mutableStateOf("class_9") }

    LaunchedEffect(currentUser) {
        currentUser?.selectedClassId?.let {
            if (it.isNotBlank()) activeClassId = it
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == UserTab.HOME,
                    onClick = { selectedTab = UserTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("হোম") },
                    modifier = Modifier.testTag("tab_home")
                )
                NavigationBarItem(
                    selected = selectedTab == UserTab.EXPLORE,
                    onClick = { selectedTab = UserTab.EXPLORE },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Curriculum") },
                    label = { Text("পাঠ্যক্রম") },
                    modifier = Modifier.testTag("tab_explore")
                )
                NavigationBarItem(
                    selected = selectedTab == UserTab.PROGRESS,
                    onClick = { selectedTab = UserTab.PROGRESS },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Progress") },
                    label = { Text("অগ্রগতি") },
                    modifier = Modifier.testTag("tab_progress")
                )
                NavigationBarItem(
                    selected = selectedTab == UserTab.AI,
                    onClick = { selectedTab = UserTab.AI },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI") },
                    label = { Text("এআই") },
                    modifier = Modifier.testTag("tab_ai")
                )
                NavigationBarItem(
                    selected = selectedTab == UserTab.PROFILE,
                    onClick = { selectedTab = UserTab.PROFILE },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("প্রোফাইল") },
                    modifier = Modifier.testTag("tab_profile")
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
                UserTab.HOME -> HomeContent(
                    currentUser = currentUser,
                    activeClassId = activeClassId,
                    classes = classes,
                    quizRepository = quizRepository,
                    onClassSelect = {
                        activeClassId = it
                        scope.launch { authManager.updateSelectedClass(it) }
                    },
                    onQuizSelected = onQuizSelected,
                    onOpenAi = { selectedTab = UserTab.AI }
                )
                UserTab.EXPLORE -> ExploreContent(
                    activeClassId = activeClassId,
                    classes = classes,
                    quizRepository = quizRepository,
                    onClassSelect = { activeClassId = it },
                    onQuizSelected = onQuizSelected
                )
                UserTab.PROGRESS -> ProgressContent(
                    currentUser = currentUser,
                    quizRepository = quizRepository,
                    onQuizSelected = onQuizSelected
                )
                UserTab.AI -> AiAssistantContent(
                    currentUser = currentUser,
                    activeClassId = activeClassId,
                    classes = classes
                )
                UserTab.PROFILE -> ProfileContent(
                    currentUser = currentUser,
                    classes = classes,
                    onClassChanged = {
                        activeClassId = it
                        scope.launch { authManager.updateSelectedClass(it) }
                    },
                    onOpenAdminPanel = onOpenAdminPanel,
                    onLogout = {
                        authManager.logout()
                        onLogout()
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: HOME
// -------------------------------------------------------------
@Composable
fun HomeContent(
    currentUser: UserProfile?,
    activeClassId: String,
    classes: List<EducationClass>,
    quizRepository: QuizRepository,
    onClassSelect: (String) -> Unit,
    onQuizSelected: (String) -> Unit,
    onOpenAi: () -> Unit
) {
    val quizzes by quizRepository.getQuizzesByClass(activeClassId).collectAsState(initial = emptyList())
    val announcements by quizRepository.getAnnouncements().collectAsState(initial = emptyList())
    val attempts by quizRepository.getUserAttempts(currentUser?.uid ?: "").collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Greeting & Current Class Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "স্বাগতম, ${currentUser?.name ?: "শিক্ষার্থী"}! 👋",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "আজ নতুন কিছু শিখি এবং অনুশীলনে অংশ নিই",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    val currentClassName = classes.find { it.id == activeClassId }?.bengaliName ?: "শ্রেণি ৯"
                    Text(
                        text = currentClassName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Class Switcher Row
        item {
            Column {
                Text(
                    text = "শ্রেণি পরিবর্তন করুন (Select Class):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(classes) { eduClass ->
                        val isSelected = eduClass.id == activeClassId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onClassSelect(eduClass.id) },
                            label = { Text(eduClass.bengaliName) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.testTag("class_chip_${eduClass.id}")
                        )
                    }
                }
            }
        }

        // Hero Banner Illustration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.banner_hero_quiz_1790500485793),
                        contentDescription = "MedhaQuiz Hero Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.CenterStart)
                    ) {
                        Text(
                            text = "স্মার্ট এআই কুইজ প্ল্যাটফর্ম",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "অধ্যায়ভিত্তিক কুইজ এবং স্বয়ংক্রিয় এআই মূল্যায়ন",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Quick Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val totalQuizzesCompleted = attempts.size
                val avgAccuracy = if (attempts.isNotEmpty()) attempts.map { it.accuracyPercentage }.average() else 0.0
                val totalScore = attempts.sumOf { it.score }

                StatCard(
                    title = "কুইজ সম্পন্ন",
                    value = "$totalQuizzesCompleted",
                    icon = Icons.Default.CheckCircle,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "গড় নির্ভুলতা",
                    value = "${String.format("%.0f", avgAccuracy)}%",
                    icon = Icons.Default.TrendingUp,
                    accentColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Announcements
        if (announcements.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    val ann = announcements.first()
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ann.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = ann.message,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // AI Assistant Quick Card
        item {
            Card(
                onClick = onOpenAi,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_ask_ai_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "এআই গৃহশিক্ষক আপনার সেবায় প্রস্তুত!",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "কোনো প্রশ্ন না বুঝলে বা ব্যাখ্যা চাইলে জিজ্ঞেস করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        // Available Quizzes for Selected Class
        item {
            Text(
                text = "প্রস্তুতিমূলক কুইজ তালিকা (Featured Quizzes):",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (quizzes.isEmpty()) {
            item {
                Text(
                    text = "এই শ্রেণির জন্য বর্তমানে কোনো প্রকাশিত কুইজ নেই। অ্যাডমিন প্যানেল থেকে কুইজ যোগ করা যাবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(quizzes) { quiz ->
                GlassCard(
                    onClick = { onQuizSelected(quiz.id) },
                    modifier = Modifier.testTag("home_quiz_item_${quiz.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DifficultyBadge(difficulty = quiz.difficulty)
                        Text(
                            text = "${quiz.questionsCount} প্রশ্ন • ${quiz.timeLimitSeconds / 60} মিনিট",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = quiz.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (quiz.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = quiz.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: EXPLORE (Class -> Subject -> Chapter -> Quizzes)
// -------------------------------------------------------------
@Composable
fun ExploreContent(
    activeClassId: String,
    classes: List<EducationClass>,
    quizRepository: QuizRepository,
    onClassSelect: (String) -> Unit,
    onQuizSelected: (String) -> Unit
) {
    val subjects by quizRepository.getSubjects(activeClassId).collectAsState(initial = emptyList())
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(subjects) {
        if (selectedSubjectId == null || subjects.none { it.id == selectedSubjectId }) {
            selectedSubjectId = subjects.firstOrNull()?.id
        }
    }

    val chapters by quizRepository.getChapters(selectedSubjectId ?: "").collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Class Selector
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "শ্রেণি ও বিষয় নির্বাচন (Curriculum Explorer)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(classes) { eduClass ->
                        val isSelected = eduClass.id == activeClassId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onClassSelect(eduClass.id) },
                            label = { Text(eduClass.bengaliName) }
                        )
                    }
                }
            }
        }

        // Subject Tabs
        if (subjects.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = subjects.indexOfFirst { it.id == selectedSubjectId }.coerceAtLeast(0),
                edgePadding = 16.dp
            ) {
                subjects.forEach { subject ->
                    Tab(
                        selected = subject.id == selectedSubjectId,
                        onClick = { selectedSubjectId = subject.id },
                        text = { Text(subject.bengaliName) }
                    )
                }
            }
        }

        // Chapters & Quizzes List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (chapters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "এই বিষয়ের জন্য কোনো অধ্যায় প্রস্তুত করা হয়নি।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(chapters) { chapter ->
                    val quizzes by quizRepository.getQuizzes(chapter.id).collectAsState(initial = emptyList())
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
                                    text = "অধ্যায় ${chapter.chapterNumber}: ${chapter.bengaliTitle}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            if (chapter.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = chapter.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (quizzes.isEmpty()) {
                                Text(
                                    text = "অধ্যায়ের কুইজ শীঘ্রই যুক্ত হবে।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    quizzes.forEach { q ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onQuizSelected(q.id) },
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = q.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                                    Text(
                                                        text = "${q.questionsCount} প্রশ্ন • ${q.timeLimitSeconds / 60} মিনিট",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                DifficultyBadge(difficulty = q.difficulty)
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

// -------------------------------------------------------------
// TAB 3: PROGRESS
// -------------------------------------------------------------
@Composable
fun ProgressContent(
    currentUser: UserProfile?,
    quizRepository: QuizRepository,
    onQuizSelected: (String) -> Unit
) {
    val attempts by quizRepository.getUserAttempts(currentUser?.uid ?: "").collectAsState(initial = emptyList())
    val favorites by quizRepository.getFavorites(currentUser?.uid ?: "").collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "আপনার সামগ্রিক অগ্রগতি (Learning Progress)",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Summary Stats Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                val totalAttempts = attempts.size
                val totalScore = attempts.sumOf { it.score }
                val avgAccuracy = if (attempts.isNotEmpty()) attempts.map { it.accuracyPercentage }.average() else 0.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$totalAttempts", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Text(text = "মোট কুইজ", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$totalScore", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.secondary)
                        Text(text = "অর্জিত পয়েন্ট", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${String.format("%.0f", avgAccuracy)}%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = SuccessGreen)
                        Text(text = "গড় দক্ষতা", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Favorites Section
        if (favorites.isNotEmpty()) {
            item {
                Text(
                    text = "প্রিয় কুইজসমূহ (Favorites):",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            items(favorites) { fav ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onQuizSelected(fav.targetId) },
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = fav.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                            if (fav.subtitle.isNotBlank()) {
                                Text(text = fav.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }

        // Quiz Attempt History List
        item {
            Text(
                text = "কুইজ অনুশীলনের ইতিহাস (Recent Quiz Attempts):",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (attempts.isEmpty()) {
            item {
                Text(
                    text = "এখনও কোনো কুইজ সম্পন্ন করা হয়নি। পাঠ্যক্রম থেকে পছন্দের কুইজে অংশগ্রহণ করুন!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(attempts) { attempt ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = attempt.quizTitle, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "মোড: ${if (attempt.mode == "EXAM") "পরীক্ষা" else "অনুশীলন"} • সময়: ${attempt.timeTakenSeconds / 60} মি. ${attempt.timeTakenSeconds % 60} সে.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${attempt.score}/${attempt.totalPoints}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${String.format("%.0f", attempt.accuracyPercentage)}% সঠিক",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (attempt.accuracyPercentage >= 70.0) SuccessGreen else WarningOrange
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: AI ASSISTANT (Tutor Chat & Question Clarifications)
// -------------------------------------------------------------
data class ChatMessage(val sender: String, val text: String, val timestamp: Long = System.currentTimeMillis())

@Composable
fun AiAssistantContent(
    currentUser: UserProfile?,
    activeClassId: String,
    classes: List<EducationClass>
) {
    val currentClassName = classes.find { it.id == activeClassId }?.bengaliName ?: "শ্রেণি ৯"
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "ai",
                text = "নমস্কার/সালাম! আমি মেধা-এআই (Medha AI), তোমার সার্বক্ষণিক গৃহশিক্ষক।\n\n$currentClassName-এর যেকোনো বিষয়, অধ্যায়, কঠিন অংক বা সাধারণ বিজ্ঞানের ধারণা বুঝতে আমাকে প্রশ্ন করতে পারো।"
            )
        )
    }

    val suggestionChips = listOf(
        "এই অধ্যায় সহজে বুঝিয়ে দাও",
        "আমার জন্য ৫টি গণিত প্র্যাকটিস প্রশ্ন তৈরি করো",
        "কেন আমার বিজ্ঞানের উত্তর ভুল হয়েছিল?",
        "পরীক্ষায় ভালো করার কৌশল কী?"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Header
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "মেধাকুইজ এআই গৃহশিক্ষক",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "প্রসঙ্গ: $currentClassName • বাংলা ও ইংরেজি সমর্থিত",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Suggestions
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestionChips) { chip ->
                SuggestionChip(
                    onClick = {
                        inputText = chip
                    },
                    label = { Text(chip, fontSize = 12.sp) }
                )
            }
        }

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            if (isSending) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "মেধা-এআই ভাবছে...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Input Field
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("প্রশ্ন বা সহায়তা চান...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val query = inputText.trim()
                        if (query.isNotBlank() && !isSending) {
                            messages.add(ChatMessage("user", query))
                            inputText = ""
                            isSending = true
                            scope.launch {
                                val reply = AiManager.instance.getTutorResponse(
                                    contextPrompt = "User: ${currentUser?.name}, Selected Class: $currentClassName",
                                    userQuery = query
                                )
                                messages.add(ChatMessage("ai", reply))
                                isSending = false
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_ai_chat_button"),
                    enabled = inputText.isNotBlank() && !isSending
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 5: PROFILE
// -------------------------------------------------------------
@Composable
fun ProfileContent(
    currentUser: UserProfile?,
    classes: List<EducationClass>,
    onClassChanged: (String) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.name ?: "Student",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentUser?.isAdmin == true) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (currentUser?.isAdmin == true) "🛡️ ADMINISTRATOR" else "🎓 STUDENT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (currentUser?.isAdmin == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Switch to Admin Panel (if admin)
        if (currentUser?.isAdmin == true) {
            item {
                Card(
                    onClick = onOpenAdminPanel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_admin_panel_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "অ্যাডমিন প্যানেলে প্রবেশ করুন",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "শ্রেণি, বিষয়, অধ্যায়, কুইজ ও এআই ম্যানেজমেন্ট পরিচালনা করুন",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }

        // Class Switcher Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ডিফল্ট শ্রেণি নির্ধারণ (Default Class)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(classes) { eduClass ->
                            val isSelected = eduClass.id == currentUser?.selectedClassId
                            FilterChip(
                                selected = isSelected,
                                onClick = { onClassChanged(eduClass.id) },
                                label = { Text(eduClass.bengaliName) }
                            )
                        }
                    }
                }
            }
        }

        // App Info & Sync Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("অ্যাপ সংস্করণ (App Version):")
                        Text("1.0.0 (2026 Build)", fontWeight = FontWeight.Bold)
                    }
                    Divider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ডাটাবেজ স্ট্যাটাস:")
                        Text("Room Cache + Firestore", fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                    Divider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("এআই ইঞ্জিন:")
                        Text(AiManager.instance.config.model, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Logout Button
        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("লগআউট করুন (Log Out)")
            }
        }
    }
}
