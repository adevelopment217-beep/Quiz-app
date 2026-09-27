package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.auth.AuthManager
import com.example.data.repository.AdminRepository
import com.example.data.repository.QuizRepository
import com.example.feature.admin.AdminMainScreen
import com.example.feature.auth.AuthScreen
import com.example.feature.user.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authManager = AuthManager.getInstance(this)
        val quizRepository = QuizRepository.getInstance(this)
        val adminRepository = AdminRepository.getInstance(this)
        com.example.core.ai.AiConfigRepository.getInstance(this)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val currentUser by authManager.currentUser.collectAsState()
                    val isAuthLoading by authManager.isLoading.collectAsState()

                    if (isAuthLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val startDestination = when {
                            currentUser == null -> "auth"
                            currentUser?.isAdmin == true -> "admin_main"
                            else -> "user_main"
                        }

                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {
                            // Authentication Screen
                            composable("auth") {
                                AuthScreen(
                                    authManager = authManager,
                                    onAuthSuccess = {
                                        val dest = if (authManager.currentUser.value?.isAdmin == true) "admin_main" else "user_main"
                                        navController.navigate(dest) {
                                            popUpTo("auth") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // User Panel Main
                            composable("user_main") {
                                UserMainScreen(
                                    authManager = authManager,
                                    quizRepository = quizRepository,
                                    onQuizSelected = { quizId ->
                                        navController.navigate("quiz_detail/$quizId")
                                    },
                                    onOpenAdminPanel = {
                                        navController.navigate("admin_main")
                                    },
                                    onLogout = {
                                        navController.navigate("auth") {
                                            popUpTo("user_main") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // Quiz Details
                            composable(
                                route = "quiz_detail/{quizId}",
                                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
                                QuizDetailScreen(
                                    quizId = quizId,
                                    quizRepository = quizRepository,
                                    currentUserId = currentUser?.uid ?: "guest",
                                    onBack = { navController.popBackStack() },
                                    onStartPractice = { qId ->
                                        navController.navigate("practice/$qId")
                                    },
                                    onStartExam = { qId ->
                                        navController.navigate("exam/$qId")
                                    },
                                    onOpenAiWithContext = {
                                        navController.navigate("user_main")
                                    }
                                )
                            }

                            // Practice Mode
                            composable(
                                route = "practice/{quizId}",
                                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
                                PracticeScreen(
                                    quizId = quizId,
                                    quizRepository = quizRepository,
                                    currentUserId = currentUser?.uid ?: "guest",
                                    onBack = { navController.popBackStack() },
                                    onFinish = { attemptId, _, _ ->
                                        navController.navigate("result/$attemptId") {
                                            popUpTo("quiz_detail/$quizId") { inclusive = false }
                                        }
                                    }
                                )
                            }

                            // Exam Mode
                            composable(
                                route = "exam/{quizId}",
                                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
                                ExamScreen(
                                    quizId = quizId,
                                    quizRepository = quizRepository,
                                    currentUserId = currentUser?.uid ?: "guest",
                                    onBack = { navController.popBackStack() },
                                    onSubmitComplete = { attemptId ->
                                        navController.navigate("result/$attemptId") {
                                            popUpTo("quiz_detail/$quizId") { inclusive = false }
                                        }
                                    }
                                )
                            }

                            // Results
                            composable(
                                route = "result/{attemptId}",
                                arguments = listOf(navArgument("attemptId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val attemptId = backStackEntry.arguments?.getString("attemptId") ?: ""
                                ResultScreen(
                                    attemptId = attemptId,
                                    quizRepository = quizRepository,
                                    currentUserId = currentUser?.uid ?: "guest",
                                    onRetry = { quizId ->
                                        navController.navigate("exam/$quizId") {
                                            popUpTo("result/$attemptId") { inclusive = true }
                                        }
                                    },
                                    onDone = {
                                        navController.navigate("user_main") {
                                            popUpTo("user_main") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // Admin Panel
                            composable("admin_main") {
                                if (currentUser?.isAdmin != true) {
                                    LaunchedEffect(Unit) {
                                        navController.navigate("user_main") {
                                            popUpTo("admin_main") { inclusive = true }
                                        }
                                    }
                                } else {
                                    AdminMainScreen(
                                        adminRepository = adminRepository,
                                        quizRepository = quizRepository,
                                        adminUid = currentUser?.uid ?: "admin_root",
                                        onBackToUserPanel = {
                                            navController.popBackStack()
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
}
