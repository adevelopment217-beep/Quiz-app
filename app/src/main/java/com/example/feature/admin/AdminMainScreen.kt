package com.example.feature.admin

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.*
import com.example.data.repository.AdminRepository
import com.example.data.repository.DashboardStats
import com.example.data.repository.QuizRepository
import com.example.ui.components.AppHeader
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
                        onClick = { selectedTab = AdminTab.AUDIT_LOGS },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("admin_audit_logs_top_button")
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Audit Logs",
                            tint = if (selectedTab == AdminTab.AUDIT_LOGS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

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
                AdminTab.CONTENT -> AdminContentTab(
                    adminRepository = adminRepository,
                    adminUid = adminUid
                )
                AdminTab.JSON_IMPORT -> AdminJsonImportTab(
                    adminRepository = adminRepository,
                    adminUid = adminUid
                )
                AdminTab.AI_CORRECTIONS -> AdminCorrectionReportsView(
                    adminRepository = adminRepository,
                    adminUid = adminUid
                )
                AdminTab.AI_SETTINGS -> AdminAiSettingsTab()
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
                        Text("শ্রেণি, বিষয় ও অধ্যায় ব্যবস্থাপনা", fontWeight = FontWeight.Bold)
                        Text("ডায়নামিক কারিকুলাম, কুইজ ও প্রশ্ন কন্ট্রোল সেন্টার", style = MaterialTheme.typography.bodySmall)
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
                        Text("টার্গেট শ্রেণি, বিষয় ও অধ্যায় নির্বাচন এবং এআই কোয়ালিটি অডিট", style = MaterialTheme.typography.bodySmall)
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
                    Icon(Icons.Default.FactCheck, contentDescription = null, tint = WarningOrange)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AI সংশোধন ও মান নিয়ন্ত্রণ রিপোর্ট", fontWeight = FontWeight.Bold)
                        Text("সনাক্তকৃত ত্রুটি পর্যালোচনা ও অনুমোদনের মাধ্যমে ডাটাবেজ আপডেট", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }

        item {
            Card(
                onClick = { onNavigate(AdminTab.AUDIT_LOGS) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("নিরাপত্তা ও অডিট লগ পর্যালোচনা", fontWeight = FontWeight.Bold)
                        Text("প্রশাসনিক সকল পরিবর্তন এবং অনুমোদনের অপরিবর্তনীয় ট্র্যাকিং রেকর্ড", style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
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

        if (logs.isEmpty()) {
            item {
                Text("কোনো লগ রেকর্ড পাওয়া যায়নি।", style = MaterialTheme.typography.bodySmall)
            }
        } else {
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
}
