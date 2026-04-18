@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.fitnessapp

import android.Manifest
import android.content.Context
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fitnessapp.data.MealEntity
import com.example.fitnessapp.data.ProfileEntity
import com.example.fitnessapp.data.BodyMetricsEntity
import com.example.fitnessapp.data.WorkoutEntity
import com.example.fitnessapp.ui.theme.FitnessTheme
import com.example.fitnessapp.vm.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitnessTheme {
                App()
            }
        }
    }
}

/** Rotas */
private sealed class Route(val value: String) {
    data object Splash : Route("splash")
    data object Login : Route("login")
    data object Register : Route("register")
    data object Home : Route("home")
    data object Daily : Route("daily")
    data object Nutrition : Route("nutrition")
    data object Training : Route("training")
    data object Profile : Route("profile")
    data object CompleteProfile : Route("complete_profile")
    data object History : Route("history")
    data object Goals : Route("goals")
    data object ExerciseLibrary : Route("exercise_library")
    data object Calendar : Route("calendar")
    data object BodyMetrics : Route("body_metrics")
}

/** Bottom tabs */
private data class BottomTab(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
private fun App() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val scope = rememberCoroutineScope()
    var drawerCanOpen by remember { mutableStateOf(false) }
    val drawerState = remember {
        DrawerState(
            initialValue = DrawerValue.Closed,
            confirmStateChange = { target ->
                target == DrawerValue.Closed || drawerCanOpen
            }
        )
    }
    val context = LocalContext.current

    val bottomTabs = listOf(
        BottomTab(Route.Home.value, "Home", { Icon(Icons.Default.Home, contentDescription = null) }),
        BottomTab(Route.Nutrition.value, "Nutrição", { Icon(Icons.Default.Restaurant, contentDescription = null) }),
        BottomTab(Route.Training.value, "Treinos", { Icon(Icons.Default.FitnessCenter, contentDescription = null) }),
        BottomTab(Route.Profile.value, "Perfil", { Icon(Icons.Default.Person, contentDescription = null) })
    )

    val showNav = currentRoute in setOf(
        Route.Home.value,
        Route.Nutrition.value,
        Route.Training.value,
        Route.Profile.value,
        Route.Daily.value,
        Route.History.value,
        Route.Goals.value,
        Route.ExerciseLibrary.value,
        Route.Calendar.value,
        Route.BodyMetrics.value
    )

    val userVM: UserViewModel = viewModel()
    val profileVM: ProfileViewModel = viewModel()
    val reportVM: ReportViewModel = viewModel()

    val currentUser by userVM.user.collectAsState(initial = null)
    val currentProfile by profileVM.profile.collectAsState(initial = null)

    val userName = currentUser?.name ?: "Utilizador"
    val userEmail = currentUser?.email ?: ""
    val userWeight = currentProfile?.weightKg?.toInt() ?: 0
    val userGoal = currentProfile?.goal ?: "Manter Físico"

    val sessionUserId by Session.userId.collectAsState()
    var showAppTour by remember(sessionUserId) { mutableStateOf(false) }

    LaunchedEffect(showNav) {
        if (!showNav) {
            drawerCanOpen = false
            drawerState.close()
        }
    }

    LaunchedEffect(sessionUserId, currentRoute) {
        val shouldOpenTour = sessionUserId != null && currentRoute == Route.Home.value
        if (shouldOpenTour && shouldShowAppTour(context, sessionUserId!!)) {
            showAppTour = true
        }
        MotivationWidgetProvider.requestRefresh(context)
    }

    val appScaffold: @Composable () -> Unit = {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showNav) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 6.dp
                    ) {
                        bottomTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = tab.icon,
                                label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = Route.Splash.value,
                modifier = Modifier.padding(padding)
            ) {
                composable(Route.Splash.value) {
                    SplashScreen(onTimeout = {
                        val destination = if (sessionUserId != null) Route.Home.value else Route.Login.value
                        nav.navigate(destination) {
                            popUpTo(Route.Splash.value) { inclusive = true }
                        }
                    })
                }
                composable(Route.Login.value) {
                    val vm: AuthViewModel = viewModel()
                    LoginScreen(
                        authState = vm.state.collectAsState().value,
                        onLogin = { email, pass ->
                            vm.login(email, pass) {
                                nav.navigate(Route.Home.value) {
                                    popUpTo(Route.Login.value) { inclusive = true }
                                }
                            }
                        },
                        onGoRegister = { nav.navigate(Route.Register.value) }
                    )
                }
                composable(Route.Register.value) {
                    val vm: AuthViewModel = viewModel()
                    RegisterScreen(
                        authState = vm.state.collectAsState().value,
                        onRegister = { name, email, pass ->
                            vm.register(name, email, pass) {
                                nav.navigate(Route.CompleteProfile.value) {
                                    popUpTo(Route.Register.value) { inclusive = true }
                                }
                            }
                        },
                        onBack = { nav.popBackStack() }
                    )
                }
                composable(Route.CompleteProfile.value) {
                    val vm: ProfileViewModel = viewModel()
                    CompleteProfileScreen(
                        onComplete = { age, goal, height, weight ->
                            vm.save(age, goal, height, weight, true)
                            nav.navigate(Route.Home.value) {
                                popUpTo(Route.CompleteProfile.value) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Route.Home.value) {
                    HomeOverviewScreen(
                        userName = userName,
                        userWeight = userWeight,
                        userGoal = userGoal,
                        onOpenDaily = { nav.navigate(Route.Daily.value) },
                        onOpenDrawer = {
                            drawerCanOpen = true
                            scope.launch { drawerState.open() }
                        }
                    )
                }
                composable(Route.Daily.value) {
                    DailyOverviewScreen(onOpenDrawer = {
                        drawerCanOpen = true
                        scope.launch { drawerState.open() }
                    })
                }
                composable(Route.Nutrition.value) {
                    NutritionScreen(onOpenDrawer = {
                        drawerCanOpen = true
                        scope.launch { drawerState.open() }
                    })
                }
                composable(Route.Training.value) {
                    TrainingScreen(
                        onOpenDrawer = {
                            drawerCanOpen = true
                            scope.launch { drawerState.open() }
                        },
                        onOpenLibrary = { nav.navigate(Route.ExerciseLibrary.value) }
                    )
                }
                composable(Route.Profile.value) {
                    ProfileScreen(
                        userName = userName,
                        userEmail = userEmail,
                        onLogout = {
                            Session.clear()
                            nav.navigate(Route.Login.value) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onOpenDrawer = {
                            drawerCanOpen = true
                            scope.launch { drawerState.open() }
                        }
                    )
                }
                composable(Route.History.value) {
                    HistoryScreen(onOpenDrawer = {
                        drawerCanOpen = true
                        scope.launch { drawerState.open() }
                    })
                }
                composable(Route.Goals.value) {
                    GoalsProgressScreen(onOpenDrawer = {
                        drawerCanOpen = true
                        scope.launch { drawerState.open() }
                    })
                }
                composable(Route.ExerciseLibrary.value) {
                    EnhancedExerciseLibraryScreen(onOpenDrawer = {
                        drawerCanOpen = true
                        scope.launch { drawerState.open() }
                    })
                }
                composable(Route.Calendar.value) {
                    FitnessCalendarScreen(onOpenDrawer = {
                        drawerCanOpen = true
                        scope.launch { drawerState.open() }
                    })
                }
                composable(Route.BodyMetrics.value) {
                    BodyMetricsPremiumScreen(onOpenDrawer = {
                        drawerCanOpen = true
                        scope.launch { drawerState.open() }
                    })
                }
            }
        }
    }

    if (showNav) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "UcandoIt",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        label = { Text("Home") },
                        selected = currentRoute == Route.Home.value,
                        onClick = {
                            scope.launch { drawerState.close() }
                            nav.navigate(Route.Home.value) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Nutrição") },
                        selected = currentRoute == Route.Nutrition.value,
                        onClick = {
                            scope.launch { drawerState.close() }
                            nav.navigate(Route.Nutrition.value) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Treinos") },
                        selected = currentRoute == Route.Training.value,
                        onClick = {
                            scope.launch { drawerState.close() }
                            nav.navigate(Route.Training.value) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Histórico de Refeições") },
                        selected = currentRoute == Route.History.value,
                        onClick = {
                            scope.launch { drawerState.close() }
                            nav.navigate(Route.History.value)
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    NavigationDrawerItem(
                        label = { Text("Gerar Relatório PDF") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            reportVM.generateReport(context)
                        },
                        icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Gráfico de Metas") },
                        selected = currentRoute == Route.Goals.value,
                        onClick = {
                            scope.launch { drawerState.close() }
                            nav.navigate(Route.Goals.value)
                        },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Biblioteca de Exercícios") },
                        selected = currentRoute == Route.ExerciseLibrary.value,
                        onClick = {
                            scope.launch { drawerState.close() }
                            nav.navigate(Route.ExerciseLibrary.value)
                        },
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Calendario Fitness") },
                        selected = currentRoute == Route.Calendar.value,
                        onClick = {
                            scope.launch { drawerState.close() }
                            nav.navigate(Route.Calendar.value)
                        },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Peso e Medidas") },
                        selected = currentRoute == Route.BodyMetrics.value,
                        onClick = {
                            scope.launch { drawerState.close() }
                            nav.navigate(Route.BodyMetrics.value)
                        },
                        icon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        ) {
            appScaffold()
        }
    } else {
        appScaffold()
    }

    if (showAppTour && sessionUserId != null) {
        AppWorldTourDialog(
            userName = userName,
            onDismiss = {
                markAppTourSeen(context, sessionUserId!!)
                showAppTour = false
            }
        )
    }
}
/* -------------------- SCREEN SPLASH -------------------- */

@Composable
private fun SplashScreen(onTimeout: () -> Unit) {
    val scale = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(120.dp).scale(scale.value)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "UCANDOIT",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                modifier = Modifier.alpha(scale.value)
            )
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

/* -------------------- SCREEN 0: COMPLETE PROFILE -------------------- */

@Composable
private fun GoalDropdown(
    selectedGoal: String,
    onGoalSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Perder Gordura", "Perder Peso", "Ganhar massa muscular", "Manter Físico")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedGoal,
            onValueChange = {},
            readOnly = true,
            label = { Text("Objetivo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onGoalSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CompleteProfileScreen(onComplete: (Int, String, Int, Float) -> Unit) {
    var age by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("Manter Físico") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var isPreparing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alphaAnim.animateTo(1f, tween(800)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .alpha(alphaAnim.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Complete o seu Perfil", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        
        OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Idade") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        
        GoalDropdown(selectedGoal = goal, onGoalSelected = { goal = it })
        
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Altura (cm)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth())
        
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val ageInt = age.toIntOrNull() ?: 0
                val heightInt = height.toIntOrNull() ?: 0
                val weightFloat = weight.toFloatOrNull() ?: 0f
                isPreparing = true
                scope.launch {
                    delay(2200)
                    onComplete(ageInt, goal, heightInt, weightFloat)
                    isPreparing = false
                }
            },
            enabled = !isPreparing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("OK")
        }
    }

    if (isPreparing) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Estamos preparando suas informacoes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Vamos te ajudar a conquistar seus objetivos.",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private data class AppTourStep(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val accent: Color
)

@Composable
private fun AppWorldTourDialog(userName: String, onDismiss: () -> Unit) {
    val steps = remember {
        listOf(
            AppTourStep(
                icon = Icons.Default.EmojiPeople,
                title = "Bem-vindo ao UcandoIt",
                description = "Este tour rápido vai mostrar como usar o app e onde fica cada funcionalidade principal.",
                accent = Color(0xFFF97316)
            ),
            AppTourStep(
                icon = Icons.Default.Home,
                title = "Home",
                description = "Aqui tens o resumo do teu dia, progresso, água, calorias e mensagens inteligentes para te orientar.",
                accent = Color(0xFF2563EB)
            ),
            AppTourStep(
                icon = Icons.Default.Restaurant,
                title = "Nutricao",
                description = "Nesta aba podes registar refeições, acompanhar calorias, água e usar o assistente fitness para sugestões.",
                accent = Color(0xFF059669)
            ),
            AppTourStep(
                icon = Icons.Default.FitnessCenter,
                title = "Treinos",
                description = "Aqui fica o teu plano semanal, vídeos de exercício, conclusão de treino e histórico dos últimos treinos.",
                accent = Color(0xFFDC2626)
            ),
            AppTourStep(
                icon = Icons.Default.Person,
                title = "Perfil",
                description = "No perfil podes ajustar os teus dados, objetivo, notificações e também definir a tua foto.",
                accent = Color(0xFF7C3AED)
            ),
            AppTourStep(
                icon = Icons.Default.AutoAwesome,
                title = "Dica final",
                description = "Usa o app todos os dias. Quanto mais registares treino, refeições e água, mais útil fica o teu acompanhamento.",
                accent = Color(0xFFEA580C)
            )
        )
    }
    var currentStep by remember { mutableStateOf(0) }
    val step = steps[currentStep]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x990F172A))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    step.accent.copy(alpha = 0.16f),
                                    Color.White
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(step.accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                step.icon,
                                contentDescription = null,
                                tint = step.accent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (currentStep == 0) "Ola, $userName" else "Passo ${currentStep + 1} de ${steps.size}",
                                color = Color(0xFF64748B),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                step.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.92f)
                    ) {
                        Text(
                            text = step.description,
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFF334155),
                            lineHeight = 22.sp,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        steps.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (index == currentStep) step.accent else Color(0xFFE2E8F0)
                                    )
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (currentStep > 0) {
                            OutlinedButton(
                                onClick = { currentStep -= 1 },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Voltar")
                            }
                        }

                        Button(
                            onClick = {
                                if (currentStep == steps.lastIndex) onDismiss()
                                else currentStep += 1
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (currentStep == steps.lastIndex) "Comecar" else "Continuar")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Pular tour")
                    }
                }
            }
        }
    }
}

/* -------------------- SCREEN 1: LOGIN -------------------- */

@Composable
private fun LoginScreen(
    authState: AuthState,
    onLogin: (String, String) -> Unit,
    onGoRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    val alphaAnim = remember { Animatable(0f) }
    val yOffsetAnim = remember { Animatable(50f) }

    LaunchedEffect(Unit) {
        launch { alphaAnim.animateTo(1f, tween(1000)) }
        launch { yOffsetAnim.animateTo(0f, tween(1000)) }
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top blue block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(headerGradient),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("WELCOME!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }

        // White card with inputs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .offset(y = (-34).dp + yOffsetAnim.value.dp)
                .alpha(alphaAnim.value),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Email", fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Email") }
                )

                Spacer(Modifier.height(14.dp))
                Text("Senha", fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("••••••") },
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onLogin(email, pass) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (authState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text("ENTRAR", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))
                if (authState.error != null) {
                    Text(authState.error, color = Color(0xFFB91C1C), style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { /* TODO */ }, contentPadding = PaddingValues(0.dp)) {
                    Text("Esqueceu a senha?", color = Color(0xFF2563EB))
                }
                Spacer(Modifier.height(2.dp))
                TextButton(onClick = onGoRegister, contentPadding = PaddingValues(0.dp)) {
                    Text("Não tem uma conta? Inscreva-se", color = Color(0xFF2563EB))
                }

            }
        }
    }
}


/* -------------------- SCREEN 1B: REGISTER -------------------- */

@Composable
private fun RegisterScreen(
    authState: AuthState,
    onRegister: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alphaAnim.animateTo(1f, tween(800)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
            .alpha(alphaAnim.value)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            Spacer(Modifier.width(6.dp))
            Text("Criar conta", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (authState.error != null) {
            Spacer(Modifier.height(10.dp))
            Text(authState.error, color = Color(0xFFB91C1C), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { onRegister(name, email, pass) },
            enabled = !authState.isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (authState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text("REGISTAR", fontWeight = FontWeight.Bold)
        }
    }
}

/* -------------------- SCREEN 2: HOME / DASHBOARD -------------------- */

@Composable
private fun HomeScreen(userName: String, userWeight: Int, userGoal: String, onOpenDaily: () -> Unit, onOpenDrawer: () -> Unit) {
    val nutritionViewModel: NutritionViewModel = viewModel()
    val kcalConsumedTarget = nutritionViewModel.caloriesToday.collectAsState().value

    // Default values based on goal
    val (stepsGoal, trainingGoal, hydrationGoalUnit) = when (userGoal) {
        "Perder Gordura" -> Triple(12000, 600, "cl")
        "Perder Peso" -> Triple(10000, 500, "cl")
        "Ganhar massa muscular" -> Triple(8000, 800, "cl")
        else -> Triple(10000, 400, "cl") // Manter Físico
    }
    
    val hydrationGoal = 300 // cl (3 Litros)
    
    // Mock current values
    val stepsTarget = 7500
    val trainingKcalTarget = 350
    val hydrationTarget = 120 // cl

    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { contentAlpha.animateTo(1f, tween(1000)) }

    Column(Modifier.fillMaxSize().alpha(contentAlpha.value)) {
        DashboardHeader(
            userName = userName,
            weightKg = userWeight,
            kcal = kcalConsumedTarget,
            avatarPath = "",
            onOpenDrawer = onOpenDrawer
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Atividades Diárias", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenDaily) { Text("Ver tudo") }
                }
            }

            item {
                MetricRowCard(
                    icon = Icons.Default.DirectionsWalk,
                    title = "Passos",
                    currentValue = stepsTarget,
                    goalValue = stepsGoal,
                    unitPrefix = "/",
                    unitSuffix = "",
                    progress = stepsTarget / stepsGoal.toFloat()
                )
            }
            item {
                MetricRowCard(
                    icon = Icons.Default.FitnessCenter,
                    title = "Treino",
                    currentValue = trainingKcalTarget,
                    goalValue = trainingGoal,
                    unitPrefix = "/",
                    unitSuffix = " kcal",
                    progress = trainingKcalTarget / trainingGoal.toFloat()
                )
            }
            item {
                MetricRowCard(
                    icon = Icons.Default.WaterDrop,
                    title = "Hidratação",
                    currentValue = hydrationTarget,
                    goalValue = hydrationGoal,
                    unitPrefix = "/",
                    unitSuffix = " $hydrationGoalUnit",
                    progress = hydrationTarget / hydrationGoal.toFloat()
                )
            }

            item {
                Text("Progresso Semanal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            item { WeeklyProgressCard() }
        }
    }
}

@Composable
private fun AppAvatar(
    avatarPath: String,
    size: androidx.compose.ui.unit.Dp,
    placeholderIconSize: androidx.compose.ui.unit.Dp,
    placeholderTint: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarBitmap = remember(avatarPath) { loadAvatarBitmap(context, avatarPath) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = placeholderTint,
                modifier = Modifier.size(placeholderIconSize)
            )
        }
    }
}

private fun loadAvatarBitmap(context: Context, avatarPath: String): Bitmap? {
    if (avatarPath.isBlank()) return null
    return runCatching { BitmapFactory.decodeFile(File(avatarPath).absolutePath) }.getOrNull()
}

private fun saveAvatarBitmap(context: Context, bitmap: Bitmap): String {
    val file = File(context.filesDir, "profile_avatar.jpg")
    FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
    }
    return file.absolutePath
}

private fun saveAvatarFromUri(context: Context, sourceUri: Uri): String? {
    val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
        BitmapFactory.decodeStream(input)
    } ?: return null
    return saveAvatarBitmap(context, bitmap)
}

@Composable
private fun DashboardHeader(
    userName: String,
    weightKg: Int,
    kcal: Int,
    avatarPath: String,
    onOpenDrawer: () -> Unit
) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
    val headerGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(headerGradient)
            .padding(14.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Text("$greeting, $userName!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                NotificationCenterActionButton(iconTint = Color.White)
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderPill(label = "Peso", value = "${weightKg} kg")
                HeaderPill(label = "Calorias consumidas", value = "${kcal} kcal")
            }
        }

        AppAvatar(
            avatarPath = avatarPath,
            size = 54.dp,
            placeholderIconSize = 28.dp,
            placeholderTint = Color.White,
            modifier = Modifier
                .align(Alignment.CenterEnd)
        )
    }

}

@Composable
private fun HeaderPill(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MetricRowCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    currentValue: Int,
    goalValue: Int,
    unitPrefix: String,
    unitSuffix: String,
    progress: Float
) {
    val animatedProgress = remember { Animatable(0f) }
    val animatedValue = remember { Animatable(0f) }

    LaunchedEffect(currentValue, progress) {
        launch {
            animatedProgress.animateTo(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 1500)
            )
        }
        launch {
            animatedValue.animateTo(
                targetValue = currentValue.toFloat(),
                animationSpec = tween(durationMillis = 1500)
            )
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(animatedValue.value.toInt().toString(), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text("$unitPrefix $goalValue$unitSuffix", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(99.dp)),
                )
            }
        }
    }
}

@Composable
private fun WeeklyProgressCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Treinos", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(" ", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))

            val bars = listOf(0.15f, 0.35f, 0.25f, 0.5f, 0.2f, 0.65f, 0.42f)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { p ->
                    val animatedBarHeight = remember { Animatable(0f) }
                    LaunchedEffect(p) {
                        animatedBarHeight.animateTo(
                            targetValue = p,
                            animationSpec = tween(durationMillis = 1000, delayMillis = 200)
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height((70 * animatedBarHeight.value + 12).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { d ->
                    Text(d, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
            }
        }
    }
}

/* -------------------- SCREEN 3: ATIVIDADES DIÁRIAS -------------------- */

@Composable
private fun DailyActivitiesScreen(onOpenDrawer: () -> Unit) {
    val items = remember<List<DailyItem>> {
        listOf(
            DailyItem(Icons.Default.DirectionsWalk, "Passos", "8,200", "com / 10.000"),
            DailyItem(Icons.Default.WaterDrop, "Hidratação", "2", "litros de água"),
            DailyItem(Icons.Default.Restaurant, "Refeição", "Almoço", "550 kcal  •  12:30"),
            DailyItem(Icons.Default.FitnessCenter, "Treino", "Peito e Tríceps", "45 min  •  14:30"),
            DailyItem(Icons.Default.Scale, "Peso", "Peso Atual", "79,5 kg  •  19:00"),
        )
    }

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Atividades Diárias", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = null) }
            },
            actions = {
                IconButton(onClick = {}) { Icon(Icons.Default.Notifications, contentDescription = null) }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item: DailyItem ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }

                        Text(item.trailing, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}


/* -------------------- NUTRIÇÃO -------------------- */

@Composable
private fun HomeOverviewScreen(
    userName: String,
    userWeight: Int,
    userGoal: String,
    onOpenDaily: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val nutritionViewModel: NutritionViewModel = viewModel()
    val trainingViewModel: TrainingViewModel = viewModel()
    val profileVM: ProfileViewModel = viewModel()
    val historyVM: HistoryViewModel = viewModel()
    val caloriesToday = nutritionViewModel.caloriesToday.collectAsState().value
    val meals = nutritionViewModel.meals.collectAsState().value
    val mealHistory = historyVM.mealHistory.collectAsState().value
    val hydration = nutritionViewModel.hydration.collectAsState().value
    val workouts = trainingViewModel.workouts.collectAsState().value
    val profile = profileVM.profile.collectAsState().value

    val effectiveGoal = profile?.goal ?: userGoal
    val effectiveWeight = profile?.weightKg?.takeIf { it > 0f } ?: userWeight.toFloat()
    val caloriesTarget = nutritionCaloriesTargetFor(effectiveGoal)
    val hydrationGoalMl = hydrationGoalMlFor(effectiveWeight)
    val hydrationNowMl = hydration.consumedMl.coerceAtLeast(0)
    val workoutsThisWeek = workouts.filter { isInCurrentWeek(it.createdAtEpochMs) }
    val workoutsToday = workouts.filter { isToday(it.createdAtEpochMs) }
    val weeklyWorkoutGoal = recommendedWorkoutSessionsFor(effectiveGoal)
    val weeklyMinutes = workoutsThisWeek.sumOf { it.durationMin }
    val weeklyBurnedKcal = workoutsThisWeek.sumOf { it.caloriesBurned }
    val latestMeal = meals.maxByOrNull { it.createdAtEpochMs }
    val latestWorkout = workoutsToday.maxByOrNull { it.createdAtEpochMs }
    val weekMeals = mealHistory.filterKeys { isDateKeyInCurrentWeek(it) }.values.flatten()
    val weeklySummary = remember(workoutsThisWeek, weekMeals, weeklyWorkoutGoal, weeklyMinutes, weeklyBurnedKcal) {
        weeklyAutoSummary(
            workouts = workoutsThisWeek,
            meals = weekMeals,
            weeklyWorkoutGoal = weeklyWorkoutGoal,
            weeklyMinutes = weeklyMinutes,
            weeklyBurnedKcal = weeklyBurnedKcal
        )
    }
    val trainingStreak = workoutStreakDays(workouts)
    val nutritionStreak = mealLoggingStreakDays(meals)
    val hydrationTodayGoalHit = hydrationNowMl >= hydrationGoalMl
    val unlockedAchievements = remember(workouts, meals, hydrationNowMl, hydrationGoalMl, weeklyWorkoutGoal) {
        homeAchievements(
            workouts = workouts,
            meals = meals,
            hydrationGoalHitToday = hydrationTodayGoalHit,
            weeklyWorkoutGoal = weeklyWorkoutGoal
        )
    }

    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { contentAlpha.animateTo(1f, tween(1000)) }

    Column(Modifier.fillMaxSize().alpha(contentAlpha.value)) {
        DashboardHeader(
            userName = userName,
            weightKg = userWeight,
            kcal = caloriesToday,
            avatarPath = profile?.avatarPath.orEmpty(),
            onOpenDrawer = onOpenDrawer
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Resumo de hoje", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenDaily) { Text("Ver tudo") }
                }
            }

            item {
                DashboardMotivationCard(
                    goal = effectiveGoal,
                    mealCount = meals.size,
                    waterProgress = if (hydrationGoalMl > 0) hydrationNowMl / hydrationGoalMl.toFloat() else 0f,
                    hasWorkoutToday = latestWorkout != null,
                    caloriesProgress = if (caloriesTarget > 0) caloriesToday / caloriesTarget.toFloat() else 0f
                )
            }

            item {
                ProgressHighlightsCard(
                    trainingStreak = trainingStreak,
                    nutritionStreak = nutritionStreak,
                    hydrationGoalHitToday = hydrationTodayGoalHit,
                    achievements = unlockedAchievements
                )
            }

            item {
                HomeMetricCard(
                    icon = Icons.Default.LocalFireDepartment,
                    title = "Calorias consumidas",
                    valueText = "$caloriesToday kcal",
                    goalText = "$caloriesTarget kcal",
                    supportingText = if (meals.isEmpty()) {
                        "Ainda não existem refeições registadas hoje."
                    } else {
                        "${meals.size} refeições hoje • última: ${latestMeal?.title ?: "-"}"
                    },
                    progress = if (caloriesTarget > 0) caloriesToday / caloriesTarget.toFloat() else 0f
                )
            }

            item {
                HomeMetricCard(
                    icon = Icons.Default.WaterDrop,
                    title = "Hidratação",
                    valueText = formatWaterLiters(hydrationNowMl),
                    goalText = formatWaterLiters(hydrationGoalMl),
                    supportingText = if (hydrationNowMl >= hydrationGoalMl) {
                        "Meta de água concluída hoje."
                    } else {
                        "Faltam ${formatWaterLiters((hydrationGoalMl - hydrationNowMl).coerceAtLeast(0))} para bater a meta."
                    },
                    progress = if (hydrationGoalMl > 0) hydrationNowMl / hydrationGoalMl.toFloat() else 0f
                )
            }

            item {
                HomeMetricCard(
                    icon = Icons.Default.FitnessCenter,
                    title = "Treino da semana",
                    valueText = "${workoutsThisWeek.size} sessoes",
                    goalText = "$weeklyWorkoutGoal planeadas",
                    supportingText = if (latestWorkout != null) {
                        "Hoje: ${latestWorkout.title} • ${latestWorkout.durationMin} min"
                    } else {
                        "$weeklyMinutes min acumulados • $weeklyBurnedKcal kcal queimadas"
                    },
                    progress = if (weeklyWorkoutGoal > 0) workoutsThisWeek.size / weeklyWorkoutGoal.toFloat() else 0f
                )
            }

            item {
                Text("Progresso semanal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            item {
                WeeklyTrainingOverviewCard(
                    workouts = workoutsThisWeek,
                    weeklyGoal = weeklyWorkoutGoal,
                    weeklyMinutes = weeklyMinutes,
                    weeklyBurnedKcal = weeklyBurnedKcal
                )
            }

            item {
                WeeklyAutoSummaryCard(summary = weeklySummary)
            }
        }
    }
}

@Composable
private fun ProgressHighlightsCard(
    trainingStreak: Int,
    nutritionStreak: Int,
    hydrationGoalHitToday: Boolean,
    achievements: List<HomeAchievement>
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFFBEB), Color(0xFFEFF6FF))
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Conquistas e sequência", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.9f)
                ) {
                    Text(
                        "${achievements.size} desbloqueadas",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AchievementMiniStat("Treino", "${trainingStreak}d", Icons.Default.LocalFireDepartment, Color(0xFF2563EB), Modifier.weight(1f))
                AchievementMiniStat("Dieta", "${nutritionStreak}d", Icons.Default.Restaurant, Color(0xFF16A34A), Modifier.weight(1f))
                AchievementMiniStat("Água", if (hydrationGoalHitToday) "Meta" else "Hoje", Icons.Default.WaterDrop, Color(0xFF0EA5E9), Modifier.weight(1f))
            }

            achievements.take(3).forEach { achievement ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.94f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(achievement.accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(achievement.icon, contentDescription = null, tint = achievement.accent)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(achievement.title, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                            Text(achievement.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                    }
                }
            }
        }
    }
}

private data class WeeklyAutoSummary(
    val headline: String,
    val supporting: String,
    val trainingValue: String,
    val nutritionValue: String,
    val activeDaysValue: String
)

@Composable
private fun WeeklyAutoSummaryCard(summary: WeeklyAutoSummary) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFF8FAFC), Color(0xFFEEF2FF))
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Insights, contentDescription = null, tint = Color(0xFF4F46E5))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Resumo automático da semana", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(summary.headline, color = Color(0xFF475569), style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistantMiniStat("Treino", summary.trainingValue, Modifier.weight(1f))
                AssistantMiniStat("Dieta", summary.nutritionValue, Modifier.weight(1f))
                AssistantMiniStat("Ativos", summary.activeDaysValue, Modifier.weight(1f))
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.92f)
            ) {
                Text(
                    summary.supporting,
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFF334155),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun AchievementMiniStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent)
            Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
        }
    }
}

private data class HomeAchievement(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color
)

private fun homeAchievements(
    workouts: List<WorkoutEntity>,
    meals: List<MealEntity>,
    hydrationGoalHitToday: Boolean,
    weeklyWorkoutGoal: Int
): List<HomeAchievement> {
    val achievements = mutableListOf<HomeAchievement>()
    val workoutDays = workouts.map { dayKey(it.createdAtEpochMs) }.distinct().size
    val weekWorkouts = workouts.filter { isInCurrentWeek(it.createdAtEpochMs) }.map { dayKey(it.createdAtEpochMs) }.distinct().size

    if (hydrationGoalHitToday) {
        achievements += HomeAchievement(
            title = "Meta de água concluída",
            description = "Hoje já bateste a meta de hidratação.",
            icon = Icons.Default.WaterDrop,
            accent = Color(0xFF0EA5E9)
        )
    }
    if (meals.size >= 3) {
        achievements += HomeAchievement(
            title = "Dieta consistente",
            description = "Já registaste pelo menos 3 refeições hoje.",
            icon = Icons.Default.Restaurant,
            accent = Color(0xFF16A34A)
        )
    }
    if (workoutDays >= 1) {
        achievements += HomeAchievement(
            title = "Primeiros treinos feitos",
            description = "O teu histórico já mostra progresso real.",
            icon = Icons.Default.EmojiEvents,
            accent = Color(0xFFF59E0B)
        )
    }
    if (weekWorkouts >= weeklyWorkoutGoal && weeklyWorkoutGoal > 0) {
        achievements += HomeAchievement(
            title = "Meta semanal batida",
            description = "Concluíste a meta de treinos da semana.",
            icon = Icons.Default.MilitaryTech,
            accent = Color(0xFF7C3AED)
        )
    }

    return if (achievements.isEmpty()) {
        listOf(
            HomeAchievement(
                title = "Tudo pronto para evoluir",
                description = "Regista treino, refeições e água para começares a desbloquear conquistas.",
                icon = Icons.Default.AutoAwesome,
                accent = Color(0xFF2563EB)
            )
        )
    } else {
        achievements
    }
}

private fun workoutStreakDays(workouts: List<WorkoutEntity>): Int {
    val workoutDates = workouts.map { dayKey(it.createdAtEpochMs) }.distinct().toSet()
    var streak = 0
    var cursor = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    while (workoutDates.contains(dayKey(cursor.timeInMillis))) {
        streak += 1
        cursor.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

private fun mealLoggingStreakDays(meals: List<MealEntity>): Int {
    if (meals.isEmpty()) return 0
    val mealDates = meals.map { dayKey(it.createdAtEpochMs) }.distinct().toSet()
    var streak = 0
    var cursor = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    while (mealDates.contains(dayKey(cursor.timeInMillis))) {
        streak += 1
        cursor.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

@Composable
private fun NotificationCenterDialog(
    notifications: List<AppNotificationItem>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 18.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Notificações", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                if (notifications.isEmpty()) {
                    Text(
                        "Ainda não tens notificações por ver.",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(notifications.take(10)) { item ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (item.read) Color(0xFFF8FAFC) else Color(0xFFE0F2FE)
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            item.title,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            notificationTimeLabel(item.createdAtEpochMs),
                                            color = Color(0xFF64748B),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        item.message,
                                        color = Color(0xFF334155),
                                        style = MaterialTheme.typography.bodySmall
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

@Composable
private fun NotificationCenterActionButton(iconTint: Color = LocalContentColor.current) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showNotifications by remember { mutableStateOf(false) }
    var unreadCount by remember { mutableIntStateOf(NotificationCenterManager.unreadCount(context)) }
    var notifications by remember { mutableStateOf(NotificationCenterManager.latestNotifications(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                unreadCount = NotificationCenterManager.unreadCount(context)
                notifications = NotificationCenterManager.latestNotifications(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BadgedBox(
        badge = {
            if (unreadCount > 0) {
                Badge(containerColor = Color(0xFFDC2626)) {
                    Text(unreadCount.coerceAtMost(99).toString())
                }
            }
        }
    ) {
        IconButton(
            onClick = {
                notifications = NotificationCenterManager.latestNotifications(context)
                unreadCount = NotificationCenterManager.unreadCount(context)
                showNotifications = true
            }
        ) {
            Icon(Icons.Default.Notifications, contentDescription = "Central de notificações", tint = iconTint)
        }
    }

    if (showNotifications) {
        NotificationCenterDialog(
            notifications = notifications.take(10),
            onDismiss = {
                NotificationCenterManager.markAllAsRead(context)
                unreadCount = 0
                notifications = NotificationCenterManager.latestNotifications(context)
                showNotifications = false
            }
        )
    }
}

@Composable
private fun DailyOverviewScreen(onOpenDrawer: () -> Unit) {
    val nutritionViewModel: NutritionViewModel = viewModel()
    val trainingViewModel: TrainingViewModel = viewModel()
    val profileVM: ProfileViewModel = viewModel()
    val meals = nutritionViewModel.meals.collectAsState().value
    val hydration = nutritionViewModel.hydration.collectAsState().value
    val workouts = trainingViewModel.workouts.collectAsState().value
    val profile = profileVM.profile.collectAsState().value
    val caloriesToday = nutritionViewModel.caloriesToday.collectAsState().value
    val caloriesTarget = nutritionCaloriesTargetFor(profile?.goal)
    val waterGoalMl = hydrationGoalMlFor(profile?.weightKg)
    val dailyItems = remember(meals, hydration, workouts, profile, caloriesToday, caloriesTarget, waterGoalMl) {
        buildDailyItems(
            meals = meals,
            workouts = workouts.filter { isToday(it.createdAtEpochMs) },
            hydration = hydration,
            profile = profile,
            caloriesToday = caloriesToday,
            caloriesTarget = caloriesTarget,
            waterGoalMl = waterGoalMl
        )
    }

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Atividades do dia", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = null) }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Visao geral", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (dailyItems.isEmpty()) "Ainda não existe atividade registada hoje. Comece pela água, uma refeição ou um treino."
                            else "Resumo real do seu dia com alimentação, hidratação, treino e perfil atual.",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            items(dailyItems) { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }

                        Text(item.trailing, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    valueText: String,
    goalText: String,
    supportingText: String,
    progress: Float
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(progress.coerceIn(0f, 1f), tween(1400))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text("Meta: $goalText", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                }
                Text(valueText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text(supportingText, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun DashboardMotivationCard(
    goal: String,
    mealCount: Int,
    waterProgress: Float,
    hasWorkoutToday: Boolean,
    caloriesProgress: Float
) {
    val hasMeals = mealCount > 0
    val hitWaterGoal = waterProgress >= 1f
    val caloriesOnTrack = caloriesProgress in 0.45f..0.95f
    val celebrationCount = listOf(hasWorkoutToday, hitWaterGoal, hasMeals, caloriesOnTrack).count { it }
    val mascotMood = when {
        celebrationCount >= 3 -> "happy"
        !hasWorkoutToday && waterProgress < 0.35f && !hasMeals -> "worried"
        else -> "encourage"
    }
    val bounce by rememberInfiniteTransition(label = "mascot_bounce").animateFloat(
        initialValue = if (mascotMood == "worried") 0.98f else 1f,
        targetValue = if (mascotMood == "happy") 1.08f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_scale"
    )
    val mascotEmoji = when (mascotMood) {
        "happy" -> "😄"
        "worried" -> "🥺"
        else -> "🙂"
    }
    val mascotAccent = when (mascotMood) {
        "happy" -> Color(0xFF16A34A)
        "worried" -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }
    val mascotLabel = when (mascotMood) {
        "happy" -> "Mandou bem"
        "worried" -> "Vamos focar"
        else -> "Estou contigo"
    }

    val currentMessage = remember(goal, mealCount, waterProgress, hasWorkoutToday, caloriesProgress) {
        when {
            celebrationCount >= 3 ->
                "Dia forte. Já estás a acertar em várias frentes e isso vai aparecer no resultado."
            hitWaterGoal && hasWorkoutToday ->
                "Boa! Água em dia e treino concluído. Hoje foi um dia muito bem jogado."
            !hasWorkoutToday && !hasMeals ->
                "Vamos organizar o dia: faz uma boa refeição e encaixa o treino para ganhar ritmo."
            waterProgress < 0.35f ->
                "Bebe mais água agora. Isso já melhora energia, foco e disposição."
            !hasWorkoutToday ->
                "O treino de hoje ainda está à tua espera. Faz mesmo que seja uma sessão curta."
            !hasMeals ->
                "Vamos manter a dieta. Uma refeição bem feita agora muda o resto do dia."
            hasMeals && !caloriesOnTrack ->
                "Ajusta as proximas escolhas para manter o dia mais alinhado com a tua meta."
            hasMeals ->
                "Boa. Já tens $mealCount refeição${if (mealCount > 1) "es" else ""} registada${if (mealCount > 1) "s" else ""} hoje."
            goal == "Ganhar massa muscular" ->
                "Forca no plano. Comer bem e treinar forte vao construir resultado."
            goal == "Perder Gordura" || goal == "Perder Peso" ->
                "Consistência ganha ao impulso. Hoje vale muito."
            else ->
                "Vamos lá. Hoje é mais um passo para a tua melhor versão."
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFF7ED),
                            Color(0xFFFEF3C7),
                            Color(0xFFE0F2FE)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.88f),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                AnimatedContent(
                                    targetState = currentMessage,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                                    },
                                    label = "dashboard_motivation"
                                ) { message ->
                                    Text(
                                        message,
                                        color = Color(0xFF0F172A),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 18.sp,
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .padding(start = 26.dp, top = 2.dp)
                                .size(12.dp)
                                .graphicsLayer { rotationZ = 45f }
                                .background(Color.White.copy(alpha = 0.88f))
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .graphicsLayer {
                                        scaleX = bounce
                                        scaleY = bounce
                                    }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.78f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(mascotEmoji, fontSize = 26.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.72f)
                            ) {
                                Text(
                                    mascotLabel,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = mascotAccent,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    MotivationStatusChip(
                        label = if (hasWorkoutToday) "Treino ok" else "Treino",
                        positive = hasWorkoutToday
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MotivationStatusChip(
                        label = if (hitWaterGoal) "Água ok" else "Água",
                        positive = hitWaterGoal,
                        modifier = Modifier.weight(1f)
                    )
                    MotivationStatusChip(
                        label = if (hasMeals) "Dieta ok" else "Dieta",
                        positive = hasMeals,
                        modifier = Modifier.weight(1f)
                    )
                    MotivationStatusChip(
                        label = if (caloriesOnTrack) "Meta ok" else "Meta",
                        positive = caloriesOnTrack,
                        modifier = Modifier.weight(1f)
                    )
                }

            }
        }
    }
}

@Composable
private fun MotivationStatusChip(label: String, positive: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (positive) Color.White.copy(alpha = 0.78f) else Color(0xFFF8FAFC).copy(alpha = 0.92f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (positive) Icons.Default.CheckCircle else Icons.Default.Schedule,
                contentDescription = null,
                tint = if (positive) Color(0xFF16A34A) else Color(0xFF64748B),
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                color = if (positive) Color(0xFF166534) else Color(0xFF475569),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WeeklyTrainingOverviewCard(
    workouts: List<WorkoutEntity>,
    weeklyGoal: Int,
    weeklyMinutes: Int,
    weeklyBurnedKcal: Int
) {
    val completedSessions = workouts.size

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Treinos da semana", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("$completedSessions/$weeklyGoal", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistantMiniStat("Minutos", "$weeklyMinutes min", Modifier.weight(1f))
                AssistantMiniStat("Kcal", "$weeklyBurnedKcal", Modifier.weight(1f))
                AssistantMiniStat("Restam", "${(weeklyGoal - completedSessions).coerceAtLeast(0)}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weekDayEntries().forEach { day ->
                    val hasWorkout = workouts.any { workoutDayLabel(it.createdAtEpochMs) == day }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = if (hasWorkout) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color(0xFFF1F5F9)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(day, fontWeight = FontWeight.Bold, color = if (hasWorkout) MaterialTheme.colorScheme.primary else Color(0xFF64748B))
                            Spacer(Modifier.height(6.dp))
                            Icon(
                                if (hasWorkout) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (hasWorkout) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutritionScreen(onOpenDrawer: () -> Unit) {
    val vm: NutritionViewModel = viewModel()
    val profileVM: ProfileViewModel = viewModel()
    val meals = vm.meals.collectAsState().value
    val caloriesToday = vm.caloriesToday.collectAsState().value
    val hydration = vm.hydration.collectAsState().value
    val profile = profileVM.profile.collectAsState().value
    var showAssistant by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showAssistantHint by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            HydrationReminderManager.scheduleNextReminder(context)
        }
    }

    val caloriesTarget = nutritionCaloriesTargetFor(profile?.goal)
    val waterGoalMl = hydrationGoalMlFor(profile?.weightKg)
    val waterNowMl = hydration.consumedMl.coerceAtLeast(0)
    val waterGoalL = waterGoalMl / 1000f

    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alphaAnim.animateTo(1f, tween(1000)) }

    LaunchedEffect(Unit) {
        if (shouldShowNutritionAssistantHint(context)) {
            showAssistantHint = true
            markNutritionAssistantHintSeen(context)
            delay(3600)
            showAssistantHint = false
        }
    }

    LaunchedEffect(profile?.notificationsEnabled, profile?.hydrationReminderMinutes, waterNowMl, waterGoalMl) {
        val notificationsEnabled = profile?.notificationsEnabled == true
        HydrationReminderManager.syncState(
            context = context,
            consumedMl = waterNowMl,
            goalMl = waterGoalMl,
            notificationsEnabled = notificationsEnabled,
            intervalMinutes = profile?.hydrationReminderMinutes ?: 0
        )

        if (
            notificationsEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(Modifier.fillMaxSize().alpha(alphaAnim.value)) {
        Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Nutrição", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = null) } },
            actions = { NotificationCenterActionButton() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    val animatedCalories = remember { Animatable(0f) }
                    val animatedProgress = remember { Animatable(0f) }
                    val targetProgress = if (caloriesTarget > 0) (caloriesToday / caloriesTarget.toFloat()) else 0f

                    LaunchedEffect(caloriesToday) {
                        launch { animatedCalories.animateTo(caloriesToday.toFloat(), tween(1500)) }
                        launch { animatedProgress.animateTo(targetProgress.coerceIn(0f, 1f), tween(1500)) }
                    }

                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Calorias", fontWeight = FontWeight.SemiBold)
                                Text("${animatedCalories.value.toInt()} / $caloriesTarget kcal", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${(animatedProgress.value * 100).toInt()}%", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress.value },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(99.dp)),
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    val animatedWater = remember { Animatable(0f) }
                    val animatedProgress = remember { Animatable(0f) }
                    val targetProgress = if (waterGoalMl > 0) (waterNowMl / waterGoalMl.toFloat()) else 0f

                    LaunchedEffect(waterNowMl, waterGoalMl) {
                        launch { animatedWater.animateTo(waterNowMl.toFloat(), tween(1500)) }
                        launch { animatedProgress.animateTo(targetProgress.coerceIn(0f, 1f), tween(1500)) }
                    }

                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Água", fontWeight = FontWeight.SemiBold)
                                Text("${String.format("%.1f", animatedWater.value)}L / ${waterGoalL}L", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${(animatedProgress.value * 100).toInt()}%", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress.value },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(99.dp)),
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { vm.addWater(250, waterGoalMl) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+250 ml")
                            }
                            OutlinedButton(
                                onClick = { vm.resetWater(waterGoalMl) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reset")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (waterNowMl >= waterGoalMl) {
                                "Meta de água batida hoje. Os lembretes voltam amanhã."
                            } else {
                                "Lembretes ativos enquanto a meta não for atingida e as notificações estiverem ligadas."
                            },
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Text("Refeições de Hoje", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }


            items(meals) { m ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.title, fontWeight = FontWeight.SemiBold)
                            Text(m.time, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                            if (!m.note.isNullOrBlank()) {
                                Text(m.note, style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${m.calories} kcal", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { vm.deleteMeal(m) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFB91C1C), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Sugestão rápida", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Inclui proteína + fibra em 2 refeições hoje para manter saciedade.", color = Color(0xFF64748B))
                        Spacer(Modifier.height(10.dp))
                        var showAddMeal by remember { mutableStateOf(false) }

                        Button(
                            onClick = { showAddMeal = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Adicionar refeição")
                        }

                        if (showAddMeal) {
                            AddMealDialog(
                                onDismiss = { showAddMeal = false },
                                onSave = { title, kcal, time, note ->
                                    vm.addMeal(title, kcal, time, note)
                                    showAddMeal = false
                                }
                            )
                        }
                    }
                }
            }
        }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(
                visible = showAssistantHint,
                enter = fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.92f),
                exit = fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.92f)
            ) {
                NutritionAssistantHintBalloon(
                    text = "Toque aqui para receber sugestões inteligentes de refeições."
                )
            }

            NutritionAssistantBubble(
                onClick = {
                    showAssistantHint = false
                    showAssistant = true
                }
            )
        }

        if (showAssistant) {
            NutritionAssistantDialog(
                goal = profile?.goal ?: "Manter Físico",
                caloriesToday = caloriesToday,
                caloriesTarget = caloriesTarget,
                onSuggestionClick = { mealType, suggestion ->
                    vm.addMeal(
                        title = mealType,
                        calories = suggestion.calories,
                        time = getCurrentTimeText(),
                        note = "Ingerido: ${suggestion.name}"
                    )
                    showAssistant = false
                },
                onDismiss = { showAssistant = false }
            )
        }
    }
}

@Composable
private fun NutritionAssistantCard(
    goal: String,
    caloriesToday: Int,
    caloriesTarget: Int,
    onSuggestionClick: (String, NutritionSuggestion) -> Unit = { _, _ -> }
) {
    val mealTypes = remember {
        listOf(
            "Cafe da manha",
            "Lanche da manha",
            "Almoco",
            "Lanche da tarde",
            "Janta",
            "Lanche da noite"
        )
    }
    val currentMealType = remember { mealTypeForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMealType by remember { mutableStateOf(currentMealType) }
    val remainingCalories = (caloriesTarget - caloriesToday).coerceAtLeast(0)
    val mealBudget = mealBudgetFor(selectedMealType, remainingCalories, caloriesTarget, goal)
    val suggestions = remember(selectedMealType, remainingCalories, caloriesTarget, goal) {
        nutritionSuggestionsFor(
            mealType = selectedMealType,
            goal = goal,
            remainingCalories = remainingCalories,
            mealBudget = mealBudget
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Assistente Fitness", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        assistantSummaryText(
                            mealType = selectedMealType,
                            currentMealType = currentMealType,
                            goal = goal,
                            remainingCalories = remainingCalories,
                            mealBudget = mealBudget
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistantMiniStat(
                    label = "Hoje",
                    value = "$caloriesToday kcal",
                    modifier = Modifier.weight(1f)
                )
                AssistantMiniStat(
                    label = "Meta",
                    value = "$caloriesTarget kcal",
                    modifier = Modifier.weight(1f)
                )
                AssistantMiniStat(
                    label = "Restam",
                    value = "$remainingCalories kcal",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mealTypes) { mealType ->
                    FilterChip(
                        selected = selectedMealType == mealType,
                        onClick = { selectedMealType = mealType },
                        label = { Text(mealType) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            suggestions.forEach { suggestion ->
                val visual = nutritionVisualFor(selectedMealType, suggestion.name)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(selectedMealType, suggestion) }
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(Brush.linearGradient(visual.colors))
                                .padding(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color.White.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            selectedMealType,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(Modifier.weight(1f))
                                    AssistCalorieBadge(calories = suggestion.calories)
                                }
                                Text(
                                    visual.emoji,
                                    fontSize = 44.sp
                                )
                                Column {
                                    Text(
                                        suggestion.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 18.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        visual.caption,
                                        color = Color.White.copy(alpha = 0.92f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    visual.tags.joinToString(" • "),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                suggestion.description,
                                color = Color(0xFF64748B),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Toque para adicionar esta refeição ao seu dia.",
                                color = Color(0xFF0F172A),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun NutritionAssistantBubble(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "assistant_bubble").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "assistant_bubble_scale"
    )

    Box(
        modifier = modifier
            .size(62.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            .padding(6.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Abrir assistente fitness", tint = Color.White)
            }
        }
    }
}

@Composable
private fun NutritionAssistantDialog(
    goal: String,
    caloriesToday: Int,
    caloriesTarget: Int,
    onSuggestionClick: (String, NutritionSuggestion) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Assistente Fitness", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar assistente")
                    }
                }
                NutritionAssistantCard(
                    goal = goal,
                    caloriesToday = caloriesToday,
                    caloriesTarget = caloriesTarget,
                    onSuggestionClick = onSuggestionClick
                )
            }
        }
    }
}

@Composable
private fun AssistCalorieBadge(calories: Int) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            "$calories kcal",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun nutritionSuggestionsFor(
    mealType: String,
    goal: String,
    remainingCalories: Int,
    mealBudget: Int
): List<NutritionSuggestion> {
    val profile = when (goal) {
        "Perder Gordura", "Perder Peso" -> "light"
        "Ganhar massa muscular" -> "build"
        else -> "balance"
    }

    val baseSuggestions = when (mealType) {
        "Cafe da manha" -> listOf(
            NutritionSuggestion("Cafe da manha reforcado", 430, "Ovos, pao integral, queijo fresco e fruta para acordar com energia."),
            NutritionSuggestion("Iogurte com aveia e banana", 280, "Boa base de carboidrato e proteina para a manha."),
            NutritionSuggestion("Panqueca de banana fit", 340, "Aveia, banana e ovo com boa saciedade.")
        )
        "Lanche da manha" -> listOf(
            NutritionSuggestion("Iogurte proteico com fruta", 170, "Prático e com boa saciedade sem pesar."),
            NutritionSuggestion("Maca com pasta de amendoim", 190, "Combina fibra com gordura boa."),
            NutritionSuggestion("Castanhas com queijo fresco", 220, "Lanche funcional e fácil de transportar.")
        )
        "Almoco" -> listOf(
            NutritionSuggestion("Frango, arroz e salada", 480, "Prato base fit com proteína magra e carboidrato controlado."),
            NutritionSuggestion("Peixe com batata doce", 420, "Opção leve e consistente para o meio do dia."),
            NutritionSuggestion("Carne magra com legumes", 530, "Melhor escolha quando ainda há bastante margem calórica.")
        )
        "Lanche da tarde" -> listOf(
            NutritionSuggestion("Sandes integral de peru", 230, "Ajuda a segurar a fome até à janta."),
            NutritionSuggestion("Banana com iogurte", 180, "Lanche simples e funcional para energia estável."),
            NutritionSuggestion("Tapioca com cottage", 260, "Boa opção quando o treino ou a janta ainda vão demorar.")
        )
        "Janta" -> listOf(
            NutritionSuggestion("Peixe com legumes", 320, "Janta fit com proteína e volume sem exagero."),
            NutritionSuggestion("Omelete com salada", 280, "Leve e boa para fechar o dia."),
            NutritionSuggestion("Frango desfiado com sopa de legumes", 350, "Mais conforto sem sair do plano.")
        )
        else -> listOf(
            NutritionSuggestion("Iogurte natural", 110, "Boa opção leve para não dormir pesado."),
            NutritionSuggestion("Kiwi com chia", 95, "Baixa caloria com fibra."),
            NutritionSuggestion("Leite magro com canela", 90, "Ajuda a controlar a fome noturna.")
        )
    }

    val adjusted = baseSuggestions.map { suggestion ->
        val adjustedCalories = adjustCaloriesToGoal(suggestion.calories, profile).coerceAtMost(mealBudget)
        val finalCalories = adjustedCalories.coerceAtLeast(minSuggestionCaloriesFor(mealType))
        val fitDescription = when (profile) {
        "light" -> "${suggestion.description} Mantém controlo calórico para encaixar melhor na dieta."
            "build" -> "${suggestion.description} Traz mais energia e proteina para sustentar ganho muscular."
            else -> "${suggestion.description} Mantém equilíbrio entre saciedade e controlo calórico."
        }
        suggestion.copy(calories = finalCalories, description = fitDescription)
    }

    return if (remainingCalories < 120) {
        listOf(
            NutritionSuggestion(
                "Ceia muito leve",
                remainingCalories.coerceAtLeast(70),
                "O teu saldo calórico está curto. Prioriza algo pequeno, proteico e fácil de digerir."
            )
        )
    } else {
        adjusted.sortedBy { kotlin.math.abs(it.calories - mealBudget) }.take(3)
    }
}

private fun nutritionCaloriesTargetFor(goal: String?): Int {
    return when (goal) {
        "Perder Gordura" -> 1900
        "Perder Peso" -> 1800
        "Ganhar massa muscular" -> 2600
        else -> 2200
    }
}

private fun hydrationGoalMlFor(weightKg: Float?): Int {
    val calculated = ((weightKg ?: 70f) * 35f).toInt()
    return calculated.coerceIn(2000, 4000)
}

private fun formatWaterLiters(ml: Int): String {
    return String.format(Locale.getDefault(), "%.1fL", ml / 1000f)
}

private fun mealTypeForHour(hour: Int): String {
    return when (hour) {
        in 5..8 -> "Cafe da manha"
        in 9..11 -> "Lanche da manha"
        in 12..14 -> "Almoco"
        in 15..17 -> "Lanche da tarde"
        in 18..21 -> "Janta"
        else -> "Lanche da noite"
    }
}

private fun mealBudgetFor(mealType: String, remainingCalories: Int, caloriesTarget: Int, goal: String): Int {
    val ratio = when (mealType) {
        "Cafe da manha" -> if (goal == "Ganhar massa muscular") 0.24f else 0.20f
        "Lanche da manha" -> 0.10f
        "Almoco" -> 0.28f
        "Lanche da tarde" -> 0.12f
        "Janta" -> 0.22f
        else -> 0.08f
    }
    val plannedBudget = (caloriesTarget * ratio).toInt()
    return plannedBudget.coerceAtMost(remainingCalories.coerceAtLeast(90))
}

private fun adjustCaloriesToGoal(baseCalories: Int, profile: String): Int {
    return when (profile) {
        "light" -> (baseCalories * 0.85f).toInt()
        "build" -> (baseCalories * 1.12f).toInt()
        else -> baseCalories
    }
}

private fun minSuggestionCaloriesFor(mealType: String): Int {
    return when (mealType) {
        "Cafe da manha" -> 180
        "Almoco", "Janta" -> 220
        else -> 90
    }
}

private fun assistantSummaryText(
    mealType: String,
    currentMealType: String,
    goal: String,
    remainingCalories: Int,
    mealBudget: Int
): String {
    val timingText =
        if (mealType == currentMealType) "Pelo horário atual, esta é a melhor janela para $mealType."
        else "Você pode planear $mealType desde já para não sair da dieta."
    return "$timingText Objetivo: $goal. Restam $remainingCalories kcal no dia, então a sugestão aponta para cerca de $mealBudget kcal."
}

private fun recommendedWorkoutSessionsFor(goal: String): Int {
    return when (goal) {
        "Perder Gordura", "Perder Peso" -> 5
        "Ganhar massa muscular" -> 4
        else -> 4
    }
}

@Composable
private fun NutritionAssistantHintBalloon(text: String) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                color = Color(0xFF0F172A),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .padding(end = 22.dp, top = 2.dp)
                .size(10.dp)
                .graphicsLayer { rotationZ = 45f }
                .background(Color.White)
        )
    }
}

private fun isToday(epochMs: Long): Boolean {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { timeInMillis = epochMs }
    return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}

private fun isInCurrentWeek(epochMs: Long): Boolean {
    val startOfWeek = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val endOfWeek = Calendar.getInstance().apply {
        timeInMillis = startOfWeek.timeInMillis
        add(Calendar.DAY_OF_YEAR, 7)
    }
    return epochMs in startOfWeek.timeInMillis until endOfWeek.timeInMillis
}

private fun weekDayEntries(): List<String> = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sab", "Dom")

private fun workoutDayLabel(epochMs: Long): String {
    return when (Calendar.getInstance().apply { timeInMillis = epochMs }.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Seg"
        Calendar.TUESDAY -> "Ter"
        Calendar.WEDNESDAY -> "Qua"
        Calendar.THURSDAY -> "Qui"
        Calendar.FRIDAY -> "Sex"
        Calendar.SATURDAY -> "Sab"
        else -> "Dom"
    }
}

private fun buildDailyItems(
    meals: List<MealEntity>,
    workouts: List<WorkoutEntity>,
    hydration: com.example.fitnessapp.data.HydrationEntity,
    profile: ProfileEntity?,
    caloriesToday: Int,
    caloriesTarget: Int,
    waterGoalMl: Int
): List<DailyItem> {
    val items = mutableListOf<DailyItem>()
    items += DailyItem(
        icon = Icons.Default.LocalFireDepartment,
        title = "Calorias do dia",
        trailing = "$caloriesToday kcal",
        subtitle = "Meta atual: $caloriesTarget kcal"
    )
    items += DailyItem(
        icon = Icons.Default.WaterDrop,
        title = "Hidratação",
        trailing = formatWaterLiters(hydration.consumedMl.coerceAtLeast(0)),
        subtitle = "Meta atual: ${formatWaterLiters(waterGoalMl)}"
    )
    meals.firstOrNull()?.let { meal ->
        items += DailyItem(
            icon = Icons.Default.Restaurant,
            title = "Última refeição",
            trailing = "${meal.calories} kcal",
            subtitle = "${meal.title} • ${meal.time}"
        )
    }
    workouts.firstOrNull()?.let { workout ->
        items += DailyItem(
            icon = Icons.Default.FitnessCenter,
            title = "Último treino",
            trailing = "${workout.durationMin} min",
            subtitle = "${workout.title} • ${workout.caloriesBurned} kcal"
        )
    }
    profile?.weightKg?.takeIf { it > 0f }?.let { weight ->
        items += DailyItem(
            icon = Icons.Default.Scale,
            title = "Perfil atual",
            trailing = "${weight.toInt()} kg",
            subtitle = profile.goal
        )
    }
    return items
}

private data class NutritionVisualInfo(
    val emoji: String,
    val caption: String,
    val tags: List<String>,
    val colors: List<Color>
)

private fun nutritionVisualFor(mealType: String, suggestionName: String): NutritionVisualInfo {
    return when {
        suggestionName.contains("Iogurte", ignoreCase = true) -> NutritionVisualInfo(
            emoji = "🥣",
            caption = "Textura leve com boa saciedade",
            tags = listOf("Proteína", "Prático", "Leve"),
            colors = listOf(Color(0xFF0F766E), Color(0xFF2DD4BF))
        )
        suggestionName.contains("Panqueca", ignoreCase = true) || suggestionName.contains("Tapioca", ignoreCase = true) -> NutritionVisualInfo(
            emoji = "🥞",
            caption = "Opção funcional para energia estável",
            tags = listOf("Energia", "Caseiro", "Fit"),
            colors = listOf(Color(0xFFB45309), Color(0xFFF59E0B))
        )
        suggestionName.contains("Frango", ignoreCase = true) || suggestionName.contains("Peru", ignoreCase = true) -> NutritionVisualInfo(
            emoji = "🍗",
            caption = "Proteina magra para sustentar o plano",
            tags = listOf("Proteina", "Saciedade", "Base fit"),
            colors = listOf(Color(0xFF9A3412), Color(0xFFFB923C))
        )
        suggestionName.contains("Peixe", ignoreCase = true) -> NutritionVisualInfo(
            emoji = "🐟",
            caption = "Refeição leve e eficiente",
            tags = listOf("Leve", "Proteína", "Janta"),
            colors = listOf(Color(0xFF1D4ED8), Color(0xFF60A5FA))
        )
        suggestionName.contains("Banana", ignoreCase = true) || suggestionName.contains("Fruta", ignoreCase = true) || suggestionName.contains("Kiwi", ignoreCase = true) || suggestionName.contains("Maca", ignoreCase = true) -> NutritionVisualInfo(
            emoji = "🍓",
            caption = "Boa escolha para micronutrientes e fibras",
            tags = listOf("Fruta", "Fibra", "Leve"),
            colors = listOf(Color(0xFFBE123C), Color(0xFFFB7185))
        )
        suggestionName.contains("Omelete", ignoreCase = true) || suggestionName.contains("Ovos", ignoreCase = true) -> NutritionVisualInfo(
            emoji = "🍳",
            caption = "Proteina simples para qualquer horario",
            tags = listOf("Proteína", "Rápido", "Sustenta"),
            colors = listOf(Color(0xFF92400E), Color(0xFFFBBF24))
        )
        else -> {
            val byMeal = when (mealType) {
                "Cafe da manha" -> NutritionVisualInfo(
                    emoji = "🥑",
                    caption = "Comeco de dia com energia controlada",
                    tags = listOf("Manha", "Equilibrio", "Energia"),
                    colors = listOf(Color(0xFF166534), Color(0xFF4ADE80))
                )
                "Almoco" -> NutritionVisualInfo(
                    emoji = "🍽️",
            caption = "Prato principal com foco no objetivo",
                    tags = listOf("Principal", "Completo", "Sacia"),
                    colors = listOf(Color(0xFF7C2D12), Color(0xFFF97316))
                )
                "Janta" -> NutritionVisualInfo(
                    emoji = "🥗",
                    caption = "Fechamento do dia com leveza",
                    tags = listOf("Noite", "Leve", "Controle"),
                    colors = listOf(Color(0xFF14532D), Color(0xFF22C55E))
                )
                else -> NutritionVisualInfo(
                    emoji = "🥪",
            caption = "Lanche inteligente para não sair da rota",
                    tags = listOf("Snack", "Prático", "Fit"),
                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF38BDF8))
                )
            }
            byMeal
        }
    }
}

private enum class GoalsFilter(val label: String) {
    WEEK("Semana"),
    MONTH("Mês"),
    YEAR("Ano")
}

private data class GoalProgressSummary(
    val title: String,
    val value: String,
    val subtitle: String,
    val progress: Float,
    val accent: Color
)

private data class ProgressChartBar(
    val label: String,
    val value: Float
)

private data class FitnessCalendarMonthState(
    val title: String,
    val monthKey: String,
    val weekLabels: List<String>,
    val days: List<FitnessCalendarDayUi>
)

private data class FitnessCalendarDayUi(
    val dateKey: String,
    val dayNumberLabel: String,
    val prettyLabel: String,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val hasWorkout: Boolean,
    val mealCount: Int,
    val hitHydrationGoal: Boolean,
    val hydrationPercent: Int,
    val summaryText: String,
    val workoutTitles: List<String>,
    val mealTitles: List<String>,
    val hydrationText: String
)

private enum class BodyMetricType(
    val label: String,
    val startColor: Color,
    val endColor: Color
) {
    Weight("Peso", Color(0xFF4F46E5), Color(0xFF818CF8)),
    Waist("Cintura", Color(0xFF0EA5E9), Color(0xFF38BDF8)),
    Chest("Peito", Color(0xFF22C55E), Color(0xFF86EFAC)),
    Arm("Braço", Color(0xFFF59E0B), Color(0xFFFCD34D));

    fun valueOf(entry: BodyMetricsEntity): Float {
        return when (this) {
            Weight -> entry.weightKg
            Waist -> entry.waistCm
            Chest -> entry.chestCm
            Arm -> entry.armCm
        }
    }
}

@Composable
private fun GoalsProgressScreen(onOpenDrawer: () -> Unit) {
    val historyVM: HistoryViewModel = viewModel()
    val trainingVM: TrainingViewModel = viewModel()
    val profileVM: ProfileViewModel = viewModel()
    val mealHistory = historyVM.mealHistory.collectAsState().value
    val workouts = trainingVM.workouts.collectAsState().value
    val profile = profileVM.profile.collectAsState().value
    var filter by remember { mutableStateOf(GoalsFilter.WEEK) }

    val caloriesTarget = nutritionCaloriesTargetFor(profile?.goal)
    val workoutGoalPerWeek = recommendedWorkoutSessionsFor(profile?.goal ?: "Manter Físico")
    val periodDays = remember(filter) { progressPeriodDays(filter) }
    val chartBars = remember(filter, workouts, mealHistory) { buildProgressChartBars(filter, workouts, mealHistory) }
    val summaries = remember(filter, workouts, mealHistory, caloriesTarget, workoutGoalPerWeek) {
        buildGoalSummaries(
            filter = filter,
            workouts = workouts,
            mealHistory = mealHistory,
            caloriesTarget = caloriesTarget,
            workoutGoalPerWeek = workoutGoalPerWeek
        )
    }

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Gráfico de Metas", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = null) } },
            actions = { NotificationCenterActionButton() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Análise de progresso", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Filtra por semana, mês ou ano para ver se estás a bater as metas e como evoluem as tuas atividades.",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoalsFilter.values().forEach { option ->
                                FilterChip(
                                    selected = filter == option,
                                    onClick = { filter = option },
                                    label = { Text(option.label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ProgressChartCard(
                    title = "Atividades por período",
                    subtitle = "Últimos $periodDays dias",
                    bars = chartBars
                )
            }

            items(summaries) { summary ->
                GoalSummaryCard(summary = summary)
            }
        }
    }
}

@Composable
private fun ProgressChartCard(title: String, subtitle: String, bars: List<ProgressChartBar>) {
    val maxValue = bars.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { bar ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(bar.value.toInt().toString(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(((bar.value / maxValue) * 110f).coerceAtLeast(10f).dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF0EA5E9), Color(0xFF2563EB))
                                    )
                                )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(bar.label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalSummaryCard(summary: GoalProgressSummary) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(summary.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(summary.value, fontWeight = FontWeight.Bold, color = summary.accent)
            }
            LinearProgressIndicator(
                progress = { summary.progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = summary.accent
            )
            Text(summary.subtitle, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun FitnessCalendarScreen(onOpenDrawer: () -> Unit) {
    val historyVM: HistoryViewModel = viewModel()
    val trainingVM: TrainingViewModel = viewModel()
    val mealHistory = historyVM.mealHistory.collectAsState().value
    val hydrationHistory = historyVM.hydrationHistory.collectAsState().value
    val workouts = trainingVM.workouts.collectAsState().value
    var monthOffset by remember { mutableStateOf(0) }
    val monthState = remember(monthOffset, mealHistory, hydrationHistory, workouts) {
        buildFitnessCalendarMonthState(monthOffset, mealHistory, hydrationHistory, workouts)
    }
    var selectedDateKey by remember(monthState.monthKey) {
        mutableStateOf(monthState.days.firstOrNull { it.isCurrentMonth }?.dateKey)
    }
    val selectedDay = monthState.days.firstOrNull { it.dateKey == selectedDateKey && it.isCurrentMonth }

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Calendario Fitness", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = null) } },
            actions = { NotificationCenterActionButton() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { monthOffset -= 1 }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior", tint = Color.White)
                            }
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(monthState.title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                                Text(
                                    "Treinos, refeicoes e hidratacao no mesmo calendario.",
                                    color = Color.White.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { monthOffset += 1 }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Proximo mes", tint = Color.White)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CalendarHeroStat("Dias ativos", monthState.days.count { it.hasWorkout || it.mealCount > 0 }.toString(), Modifier.weight(1f))
                            CalendarHeroStat("Água OK", monthState.days.count { it.hitHydrationGoal }.toString(), Modifier.weight(1f))
                            CalendarHeroStat("Treinos", monthState.days.count { it.hasWorkout }.toString(), Modifier.weight(1f))
                        }

                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            monthState.weekLabels.forEach { label ->
                                Text(
                                    label,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.68f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        monthState.days.chunked(7).forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                week.forEach { day ->
                                    FitnessCalendarDayCell(
                                        day = day,
                                        selected = day.dateKey == selectedDateKey,
                                        onClick = {
                                            if (day.isCurrentMonth) selectedDateKey = day.dateKey
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CalendarLegendItem("Treino", Color(0xFF22C55E), Modifier.weight(1f))
                        CalendarLegendItem("Dieta", Color(0xFFF59E0B), Modifier.weight(1f))
                        CalendarLegendItem("Água OK", Color(0xFF0EA5E9), Modifier.weight(1f))
                    }
                }
            }

            if (selectedDay != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFEEF2FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.EventNote, contentDescription = null, tint = Color(0xFF4F46E5))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(selectedDay.prettyLabel, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(
                                        selectedDay.summaryText,
                                        color = Color(0xFF475569),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CalendarStatCard("Treino", if (selectedDay.hasWorkout) "Feito" else "Não", Color(0xFF22C55E), Modifier.weight(1f))
                                CalendarStatCard("Refeicoes", selectedDay.mealCount.toString(), Color(0xFFF59E0B), Modifier.weight(1f))
                                CalendarStatCard("Água", "${selectedDay.hydrationPercent}%", Color(0xFF0EA5E9), Modifier.weight(1f))
                            }
                            CalendarTimelineSection(
                                title = "Treinos do dia",
                                emptyText = "Nenhum treino registado.",
                                items = selectedDay.workoutTitles,
                                icon = Icons.Default.FitnessCenter,
                                accent = Color(0xFF22C55E)
                            )
                            CalendarTimelineSection(
                                title = "Refeicoes registadas",
                                emptyText = "Nenhuma refeicao registada.",
                                items = selectedDay.mealTitles,
                                icon = Icons.Default.Restaurant,
                                accent = Color(0xFFF59E0B)
                            )
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFFE0F2FE)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF0284C7))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Hidratacao do dia", fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                        Text(selectedDay.hydrationText, color = Color(0xFF0369A1), style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun FitnessCalendarDayCell(
    day: FitnessCalendarDayUi,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary
        day.isToday -> Color(0xFFCBD5E1)
        else -> Color.Transparent
    }
    Surface(
        modifier = Modifier
            .size(width = 40.dp, height = 56.dp)
            .clickable(enabled = day.isCurrentMonth, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = when {
            selected -> Color(0xFFEFF6FF)
            day.hasWorkout && day.hitHydrationGoal -> Color(0xFFF0FDF4)
            day.isCurrentMonth -> Color.White
            else -> Color(0xFFF8FAFC)
        },
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                day.dayNumberLabel,
                color = if (day.isCurrentMonth) Color(0xFF0F172A) else Color(0xFF94A3B8),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (day.hasWorkout) CalendarMarker(Color(0xFF22C55E))
                if (day.mealCount > 0) CalendarMarker(Color(0xFFF59E0B))
                if (day.hitHydrationGoal) CalendarMarker(Color(0xFF0EA5E9))
            }
        }
    }
}

@Composable
private fun CalendarHeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun CalendarMarker(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun CalendarLegendItem(label: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CalendarMarker(color)
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color(0xFF475569), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CalendarStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(label, color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(value, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BodyMetricsScreen(onOpenDrawer: () -> Unit) {
    val vm: BodyMetricsViewModel = viewModel()
    val profileVM: ProfileViewModel = viewModel()
    val context = LocalContext.current
    val history = vm.history.collectAsState().value
    val profile = profileVM.profile.collectAsState().value
    var weightText by remember(profile, history) { mutableStateOf((history.firstOrNull()?.weightKg ?: profile?.weightKg ?: 0f).toString()) }
    var waistText by remember(history) { mutableStateOf((history.firstOrNull()?.waistCm ?: 0f).toString()) }
    var chestText by remember(history) { mutableStateOf((history.firstOrNull()?.chestCm ?: 0f).toString()) }
    var armText by remember(history) { mutableStateOf((history.firstOrNull()?.armCm ?: 0f).toString()) }
    var noteText by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf("") }
    var selectedMetric by remember { mutableStateOf(BodyMetricType.Weight) }
    var editingEntry by remember { mutableStateOf<BodyMetricsEntity?>(null) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            saveAvatarFromUri(context, uri)?.let { savedPath ->
                photoPath = savedPath
            }
        }
    }

    val latest = history.firstOrNull()
    val previous = history.getOrNull(1)
    val chartEntries = history.take(6).reversed()

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Peso e Medidas", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = null) } },
            actions = { NotificationCenterActionButton() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Evolucao corporal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text(
                                    "Acompanha peso, cintura, peito e braço com um histórico visual claro.",
                                    color = Color.White.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricsHeroStat("Peso atual", latest?.weightKg?.let { "${formatMetric(it)} kg" } ?: "--", Modifier.weight(1f))
                            MetricsHeroStat("Registos", history.size.toString(), Modifier.weight(1f))
                            MetricsHeroStat("Tendencia", metricsTrendLabel(latest, previous), Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Tendencia recente", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Leitura rápida das últimas medições corporais.",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(BodyMetricType.values().toList()) { metric ->
                                FilterChip(
                                    selected = selectedMetric == metric,
                                    onClick = { selectedMetric = metric },
                                    label = { Text(metric.label) }
                                )
                            }
                        }
                        BodyMetricsMetricChart(entries = chartEntries, metric = selectedMetric)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BodyMetricDeltaCard("Cintura", latest?.waistCm, previous?.waistCm, Color(0xFF0EA5E9), Modifier.weight(1f))
                            BodyMetricDeltaCard("Peito", latest?.chestCm, previous?.chestCm, Color(0xFF22C55E), Modifier.weight(1f))
                            BodyMetricDeltaCard("Braço", latest?.armCm, previous?.armCm, Color(0xFFF59E0B), Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Novo registo corporal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Guarda um novo ponto da tua evolucao para comparar ao longo do tempo.",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(value = weightText, onValueChange = { weightText = it }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = waistText, onValueChange = { waistText = it }, label = { Text("Cintura (cm)") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = chestText, onValueChange = { chestText = it }, label = { Text("Peito (cm)") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        OutlinedTextField(value = armText, onValueChange = { armText = it }, label = { Text("Braço (cm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text("Observacao (opcional)") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { photoLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (photoPath.isBlank()) "Adicionar foto" else "Trocar foto")
                            }
                            if (photoPath.isNotBlank()) {
                                Text("Foto pronta", color = Color(0xFF15803D), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Button(
                            onClick = {
                                vm.addEntry(
                                    weightKg = weightText.replace(",", ".").toFloatOrNull() ?: 0f,
                                    waistCm = waistText.replace(",", ".").toFloatOrNull() ?: 0f,
                                    chestCm = chestText.replace(",", ".").toFloatOrNull() ?: 0f,
                                    armCm = armText.replace(",", ".").toFloatOrNull() ?: 0f,
                                    note = noteText.trim(),
                                    photoPath = photoPath
                                )
                                noteText = ""
                                photoPath = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Guardar medicao")
                        }
                    }
                }
            }

            item {
                        Text("Histórico corporal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            items(history.take(12)) { entry ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.clickable { editingEntry = entry }
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatMetricsDate(entry.createdAtEpochMs), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("${formatMetric(entry.weightKg)} kg", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BodyMetricChip("Cintura", "${formatMetric(entry.waistCm)} cm", Modifier.weight(1f))
                            BodyMetricChip("Peito", "${formatMetric(entry.chestCm)} cm", Modifier.weight(1f))
                            BodyMetricChip("Braço", "${formatMetric(entry.armCm)} cm", Modifier.weight(1f))
                        }
                        if (entry.note.isNotBlank()) {
                            Text(entry.note, color = Color(0xFF475569), style = MaterialTheme.typography.bodySmall)
                        }
                        if (entry.photoPath.isNotBlank()) {
                            BodyMetricsPhotoPreview(photoPath = entry.photoPath)
                        }
                    }
                }
            }
        }
    }

    if (editingEntry != null) {
        EditBodyMetricsDialog(
            entry = editingEntry!!,
            onDismiss = { editingEntry = null },
            onSave = {
                vm.updateEntry(it)
                editingEntry = null
            },
            onDelete = {
                vm.deleteEntry(editingEntry!!.id)
                editingEntry = null
            }
        )
    }
}

@Composable
private fun MetricsHeroStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.08f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BodyMetricsMetricChart(entries: List<BodyMetricsEntity>, metric: BodyMetricType) {
    val values = entries.map { metric.valueOf(it) }
    val maxValue = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (entries.isEmpty()) {
            Text("Ainda sem dados suficientes para o grafico.", color = Color(0xFF64748B))
        } else {
            entries.forEach { entry ->
                val value = metric.valueOf(entry)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(formatMetric(value), style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(((value / maxValue) * 95f).coerceAtLeast(16f).dp)
                            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                            .background(Brush.verticalGradient(listOf(metric.startColor, metric.endColor)))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(entry.createdAtEpochMs)), style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                }
            }
        }
    }
}

@Composable
private fun BodyMetricDeltaCard(label: String, current: Float?, previous: Float?, accent: Color, modifier: Modifier = Modifier) {
    val delta = if (current != null && previous != null) current - previous else null
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = accent.copy(alpha = 0.10f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(label, color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                delta?.let { "${if (it >= 0) "+" else ""}${formatMetric(it)} cm" } ?: "--",
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BodyMetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color(0xFFF8FAFC)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BodyMetricsPhotoPreview(photoPath: String) {
    val bitmap = remember(photoPath) {
        runCatching { BitmapFactory.decodeFile(photoPath)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun EditBodyMetricsDialog(
    entry: BodyMetricsEntity,
    onDismiss: () -> Unit,
    onSave: (BodyMetricsEntity) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var weightText by remember(entry) { mutableStateOf(formatMetric(entry.weightKg)) }
    var waistText by remember(entry) { mutableStateOf(formatMetric(entry.waistCm)) }
    var chestText by remember(entry) { mutableStateOf(formatMetric(entry.chestCm)) }
    var armText by remember(entry) { mutableStateOf(formatMetric(entry.armCm)) }
    var noteText by remember(entry) { mutableStateOf(entry.note) }
    var photoPath by remember(entry) { mutableStateOf(entry.photoPath) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            saveAvatarFromUri(context, uri)?.let { savedPath ->
                photoPath = savedPath
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Editar medicao", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                OutlinedTextField(value = weightText, onValueChange = { weightText = it }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = waistText, onValueChange = { waistText = it }, label = { Text("Cintura (cm)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = chestText, onValueChange = { chestText = it }, label = { Text("Peito (cm)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = armText, onValueChange = { armText = it }, label = { Text("Braço (cm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text("Observacao") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { photoLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (photoPath.isBlank()) "Adicionar foto de progresso" else "Trocar foto de progresso")
                }
                if (photoPath.isNotBlank()) {
                    BodyMetricsPhotoPreview(photoPath = photoPath)
                }
                Button(
                    onClick = {
                        onSave(
                            entry.copy(
                                weightKg = weightText.replace(",", ".").toFloatOrNull() ?: 0f,
                                waistCm = waistText.replace(",", ".").toFloatOrNull() ?: 0f,
                                chestCm = chestText.replace(",", ".").toFloatOrNull() ?: 0f,
                                armCm = armText.replace(",", ".").toFloatOrNull() ?: 0f,
                                note = noteText.trim(),
                                photoPath = photoPath
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Guardar alteracoes")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C))
                ) {
                    Text("Eliminar medicao")
                }
            }
        }
    }
}

@Composable
private fun BodyMetricsPremiumScreen(onOpenDrawer: () -> Unit) {
    val vm: BodyMetricsViewModel = viewModel()
    val profileVM: ProfileViewModel = viewModel()
    val context = LocalContext.current
    val history = vm.history.collectAsState().value
    val profile = profileVM.profile.collectAsState().value
    val latest = history.firstOrNull()
    val previous = history.getOrNull(1)
    val firstEntry = history.lastOrNull()
    val chartEntries = history.take(6).reversed()

    var weightText by remember(profile, history) {
        mutableStateOf(formatMetric(history.firstOrNull()?.weightKg ?: profile?.weightKg ?: 0f))
    }
    var waistText by remember(history) { mutableStateOf(formatMetric(history.firstOrNull()?.waistCm ?: 0f)) }
    var chestText by remember(history) { mutableStateOf(formatMetric(history.firstOrNull()?.chestCm ?: 0f)) }
    var armText by remember(history) { mutableStateOf(formatMetric(history.firstOrNull()?.armCm ?: 0f)) }
    var noteText by remember { mutableStateOf("") }
    var photoPath by remember { mutableStateOf("") }
    var selectedMetric by remember { mutableStateOf(BodyMetricType.Weight) }
    var editingEntry by remember { mutableStateOf<BodyMetricsEntity?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            saveAvatarFromUri(context, uri)?.let { savedPath ->
                photoPath = savedPath
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Peso e Medidas", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = null)
                }
            },
            actions = { NotificationCenterActionButton() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BodyMetricsPremiumHeroCard(
                    history = history,
                    latest = latest,
                    previous = previous,
                    firstEntry = firstEntry,
                    totalCount = history.size
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Tendencia recente", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Leitura rápida das últimas medições corporais.",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(BodyMetricType.values().toList()) { metric ->
                                FilterChip(
                                    selected = selectedMetric == metric,
                                    onClick = { selectedMetric = metric },
                                    label = { Text(metric.label) }
                                )
                            }
                        }
                        BodyMetricsMetricChart(entries = chartEntries, metric = selectedMetric)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BodyMetricDeltaCard("Cintura", latest?.waistCm, previous?.waistCm, Color(0xFF0EA5E9), Modifier.weight(1f))
                            BodyMetricDeltaCard("Peito", latest?.chestCm, previous?.chestCm, Color(0xFF22C55E), Modifier.weight(1f))
                            BodyMetricDeltaCard("Braço", latest?.armCm, previous?.armCm, Color(0xFFF59E0B), Modifier.weight(1f))
                        }
                        BodyMetricsSummaryRow(latest = latest, firstEntry = firstEntry)
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Novo registo corporal", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Guarda peso, medidas e uma foto de progresso para comparar a tua evolucao.",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Peso (kg)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = waistText,
                                onValueChange = { waistText = it },
                                label = { Text("Cintura (cm)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = chestText,
                                onValueChange = { chestText = it },
                                label = { Text("Peito (cm)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = armText,
                            onValueChange = { armText = it },
                            label = { Text("Braço (cm)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Observacao (opcional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { photoLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (photoPath.isBlank()) "Adicionar foto" else "Trocar foto")
                            }
                            if (photoPath.isNotBlank()) {
                                Text("Foto pronta", color = Color(0xFF15803D), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (photoPath.isNotBlank()) {
                            BodyMetricsPhotoPreview(photoPath = photoPath)
                        }
                        Button(
                            onClick = {
                                vm.addEntry(
                                    weightKg = weightText.replace(",", ".").toFloatOrNull() ?: 0f,
                                    waistCm = waistText.replace(",", ".").toFloatOrNull() ?: 0f,
                                    chestCm = chestText.replace(",", ".").toFloatOrNull() ?: 0f,
                                    armCm = armText.replace(",", ".").toFloatOrNull() ?: 0f,
                                    note = noteText.trim(),
                                    photoPath = photoPath
                                )
                                weightText = weightText.replace(",", ".").toFloatOrNull()?.let(::formatMetric) ?: weightText
                                waistText = waistText.replace(",", ".").toFloatOrNull()?.let(::formatMetric) ?: waistText
                                chestText = chestText.replace(",", ".").toFloatOrNull()?.let(::formatMetric) ?: chestText
                                armText = armText.replace(",", ".").toFloatOrNull()?.let(::formatMetric) ?: armText
                                noteText = ""
                                photoPath = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Guardar medicao")
                        }
                    }
                }
            }

            item {
                Text("Histórico corporal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            if (history.isEmpty()) {
                item {
                    BodyMetricsEmptyStateCard()
                }
            } else {
                items(history.take(12)) { entry ->
                    BodyMetricsHistoryCard(
                        entry = entry,
                        latest = latest,
                        onClick = { editingEntry = entry }
                    )
                }
            }
        }
    }

    if (editingEntry != null) {
        EditBodyMetricsPremiumDialog(
            entry = editingEntry!!,
            onDismiss = { editingEntry = null },
            onSave = {
                vm.updateEntry(it)
                editingEntry = null
            },
            onDelete = {
                vm.deleteEntry(editingEntry!!.id)
                editingEntry = null
            }
        )
    }
}

@Composable
private fun BodyMetricsPremiumHeroCard(
    history: List<BodyMetricsEntity>,
    latest: BodyMetricsEntity?,
    previous: BodyMetricsEntity?,
    firstEntry: BodyMetricsEntity?,
    totalCount: Int
) {
    val totalWeightDelta = if (latest != null && firstEntry != null && totalCount > 1) latest.weightKg - firstEntry.weightKg else null
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Evolucao corporal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        "Acompanha peso, cintura, peito e braço num histórico visual mais claro.",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricsHeroStat("Peso atual", latest?.weightKg?.let { "${formatMetric(it)} kg" } ?: "--", Modifier.weight(1f))
                MetricsHeroStat("Registos", totalCount.toString(), Modifier.weight(1f))
                MetricsHeroStat("Tendencia", metricsTrendLabel(latest, previous), Modifier.weight(1f))
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Resumo rapido", color = Color.White.copy(alpha = 0.70f), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(bodyMetricsInsightText(history), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    if (totalWeightDelta != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Variacao total: ${if (totalWeightDelta >= 0f) "+" else ""}${formatMetric(totalWeightDelta)} kg",
                            color = Color(0xFFFCD34D),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyMetricsSummaryRow(latest: BodyMetricsEntity?, firstEntry: BodyMetricsEntity?) {
    val totalDelta = if (latest != null && firstEntry != null) latest.weightKg - firstEntry.weightKg else null
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CalendarStatCard(
            label = "Peso inicial",
            value = firstEntry?.let { "${formatMetric(it.weightKg)} kg" } ?: "--",
            color = Color(0xFF4F46E5),
            modifier = Modifier.weight(1f)
        )
        CalendarStatCard(
            label = "Variacao",
            value = totalDelta?.let { "${if (it >= 0f) "+" else ""}${formatMetric(it)} kg" } ?: "--",
            color = if ((totalDelta ?: 0f) <= 0f) Color(0xFF16A34A) else Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
        CalendarStatCard(
            label = "Seguimento",
            value = bodyMetricsTrackingLabel(latest, firstEntry),
            color = Color(0xFF0EA5E9),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BodyMetricsHistoryCard(
    entry: BodyMetricsEntity,
    latest: BodyMetricsEntity?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(formatMetricsDate(entry.createdAtEpochMs), fontWeight = FontWeight.Bold)
                    Text(
                        bodyMetricsEntrySubtitle(entry, latest),
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Text("${formatMetric(entry.weightKg)} kg", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BodyMetricChip("Cintura", "${formatMetric(entry.waistCm)} cm", Modifier.weight(1f))
                BodyMetricChip("Peito", "${formatMetric(entry.chestCm)} cm", Modifier.weight(1f))
                BodyMetricChip("Braço", "${formatMetric(entry.armCm)} cm", Modifier.weight(1f))
            }
            if (entry.note.isNotBlank()) {
                Text(entry.note, color = Color(0xFF475569), style = MaterialTheme.typography.bodySmall)
            }
            if (entry.photoPath.isNotBlank()) {
                BodyMetricsPhotoPreview(photoPath = entry.photoPath)
            }
        }
    }
}

@Composable
private fun BodyMetricsEmptyStateCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = Color(0xFF4F46E5))
            }
            Text("Ainda nao tens registos", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Adiciona a tua primeira medicao para comecar a acompanhar a evolucao corporal.",
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun EditBodyMetricsPremiumDialog(
    entry: BodyMetricsEntity,
    onDismiss: () -> Unit,
    onSave: (BodyMetricsEntity) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var weightText by remember(entry) { mutableStateOf(formatMetric(entry.weightKg)) }
    var waistText by remember(entry) { mutableStateOf(formatMetric(entry.waistCm)) }
    var chestText by remember(entry) { mutableStateOf(formatMetric(entry.chestCm)) }
    var armText by remember(entry) { mutableStateOf(formatMetric(entry.armCm)) }
    var noteText by remember(entry) { mutableStateOf(entry.note) }
    var photoPath by remember(entry) { mutableStateOf(entry.photoPath) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            saveAvatarFromUri(context, uri)?.let { savedPath ->
                photoPath = savedPath
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Editar medicao", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                OutlinedTextField(value = weightText, onValueChange = { weightText = it }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = waistText, onValueChange = { waistText = it }, label = { Text("Cintura (cm)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = chestText, onValueChange = { chestText = it }, label = { Text("Peito (cm)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = armText, onValueChange = { armText = it }, label = { Text("Braço (cm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text("Observacao") }, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { photoLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (photoPath.isBlank()) "Adicionar foto de progresso" else "Trocar foto de progresso")
                }
                if (photoPath.isNotBlank()) {
                    BodyMetricsPhotoPreview(photoPath = photoPath)
                    TextButton(onClick = { photoPath = "" }, modifier = Modifier.align(Alignment.End)) {
                        Text("Remover foto")
                    }
                }
                Button(
                    onClick = {
                        onSave(
                            entry.copy(
                                weightKg = weightText.replace(",", ".").toFloatOrNull() ?: 0f,
                                waistCm = waistText.replace(",", ".").toFloatOrNull() ?: 0f,
                                chestCm = chestText.replace(",", ".").toFloatOrNull() ?: 0f,
                                armCm = armText.replace(",", ".").toFloatOrNull() ?: 0f,
                                note = noteText.trim(),
                                photoPath = photoPath
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Guardar alteracoes")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C))
                ) {
                    Text("Eliminar medicao")
                }
            }
        }
    }
}

private fun bodyMetricsTrackingLabel(latest: BodyMetricsEntity?, firstEntry: BodyMetricsEntity?): String {
    if (latest == null || firstEntry == null) return "--"
    val days = ((latest.createdAtEpochMs - firstEntry.createdAtEpochMs) / (24L * 60L * 60L * 1000L)).coerceAtLeast(0L)
    return when {
        days == 0L -> "Hoje"
        days < 7L -> "${days + 1} dias"
        else -> "${(days / 7L) + 1} sem"
    }
}

private fun bodyMetricsInsightText(history: List<BodyMetricsEntity>): String {
    val latest = history.firstOrNull() ?: return "Regista o teu primeiro peso para desbloquear a leitura da tua evolucao."
    val first = history.lastOrNull() ?: latest
    if (history.size == 1) {
        return "Primeiro registo guardado. Continua a medir nas proximas semanas para ver a tendencia real."
    }
    val delta = latest.weightKg - first.weightKg
    return when {
        delta <= -1f -> "Boa consistencia. O peso baixou ${formatMetric(kotlin.math.abs(delta))} kg desde o primeiro registo."
        delta >= 1f -> "O peso subiu ${formatMetric(delta)} kg desde o primeiro registo. Usa cintura e peito para perceber a qualidade dessa evolucao."
        else -> "O peso está estável. Compara agora cintura, peito e braço para ler melhor o teu progresso."
    }
}

private fun bodyMetricsEntrySubtitle(entry: BodyMetricsEntity, latest: BodyMetricsEntity?): String {
    if (latest == null || latest.id == entry.id) return "Registo mais recente"
    val delta = entry.weightKg - latest.weightKg
    return if (delta == 0f) {
        "Mesmo peso do registo atual"
    } else {
        "${if (delta > 0f) "+" else ""}${formatMetric(delta)} kg vs. atual"
    }
}

@Composable
private fun CalendarTimelineSection(
    title: String,
    emptyText: String,
    items: List<String>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.08f)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            }
            if (items.isEmpty()) {
                Text(emptyText, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
            } else {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(item, modifier = Modifier.weight(1f), color = Color(0xFF334155), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLibraryScreen(onOpenDrawer: () -> Unit) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var selectedCategory by remember { mutableStateOf(ExerciseLibraryCategory.All) }
    var selectedExercise by remember { mutableStateOf<ExerciseLibraryItem?>(null) }
    val exercises = remember { exerciseLibraryItems() }
    val filteredExercises = remember(selectedCategory, exercises) {
        exercises.filter { selectedCategory == ExerciseLibraryCategory.All || it.category == selectedCategory }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Biblioteca de Exercícios", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = null) } },
            actions = { NotificationCenterActionButton() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Aprende a executar melhor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Escolhe uma categoria, abre o exercício e segue instruções claras com apoio visual em vídeo.",
                                    color = Color.White.copy(alpha = 0.76f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ExerciseLibraryHeaderStat("Categorias", (ExerciseLibraryCategory.values().size - 1).toString(), Modifier.weight(1f))
                            ExerciseLibraryHeaderStat("Exercícios", exercises.size.toString(), Modifier.weight(1f))
                            ExerciseLibraryHeaderStat("Com vídeo", "100%", Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Categorias", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "Filtra por grupo muscular ou tipo de trabalho para encontrar mais rápido o que queres aprender hoje.",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ExerciseLibraryCategory.values().toList()) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(repairPtText(category.label)) },
                                    leadingIcon = {
                                        Icon(
                                            category.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (selectedCategory == ExerciseLibraryCategory.All) "Todos os exercícios" else selectedCategory.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${filteredExercises.size} opções",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            items(filteredExercises) { exercise ->
                ExerciseLibraryCard(
                    item = exercise,
                    onOpen = { selectedExercise = exercise },
                    onOpenVideo = { uriHandler.openUri(exercise.videoUrl) }
                )
            }
        }
    }

    if (selectedExercise != null) {
        ExerciseLibraryDetailsDialog(
            item = selectedExercise!!,
            onDismiss = { selectedExercise = null },
            onOpenVideo = { uriHandler.openUri(selectedExercise!!.videoUrl) }
        )
    }
}

@Composable
private fun ExerciseLibraryHeaderStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun ExerciseLibraryCard(
    item: ExerciseLibraryItem,
    onOpen: () -> Unit,
    onOpenVideo: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
        modifier = Modifier.clickable { onOpen() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(item.category.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.category.icon,
                        contentDescription = null,
                        tint = item.category.accent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(item.focus, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                }
                FilledTonalButton(
                    onClick = onOpenVideo,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Vídeo")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExerciseTag(item.level, item.category.accent)
                ExerciseTag(repairPtText(item.equipment), Color(0xFF0F172A))
                ExerciseTag(item.category.label, Color(0xFF475569))
            }

            Text(
                item.summary,
                color = Color(0xFF334155),
                style = MaterialTheme.typography.bodyMedium
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.steps.take(3).forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(item.category.accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${index + 1}",
                                color = item.category.accent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            step,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF475569),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Text(
                "Toque no card para ver instruções completas, dicas e erros a evitar.",
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun ExerciseTag(label: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = accent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ExerciseLibraryDetailsDialog(
    item: ExerciseLibraryItem,
    onDismiss: () -> Unit,
    onOpenVideo: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(item.category.accent.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.category.icon, contentDescription = null, tint = item.category.accent, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(repairPtText(item.name), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(repairPtText(item.focus), color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExerciseTag(repairPtText(item.level), item.category.accent)
                    ExerciseTag(repairPtText(item.equipment), Color(0xFF0F172A))
                    ExerciseTag(repairPtText(item.category.label), Color(0xFF475569))
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Como executar", fontWeight = FontWeight.Bold)
                        Text(repairPtText(item.summary), color = Color(0xFF475569), style = MaterialTheme.typography.bodySmall)
                        item.steps.forEachIndexed { index, step ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(item.category.accent.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${index + 1}",
                                        color = item.category.accent,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(repairPtText(step), modifier = Modifier.weight(1f), color = Color(0xFF334155))
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = item.category.accent.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Dicas de execução", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        item.tips.forEach { tip ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = item.category.accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tip, modifier = Modifier.weight(1f), color = Color(0xFF475569), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFF7ED)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Erros a evitar", fontWeight = FontWeight.Bold, color = Color(0xFF9A3412))
                        item.commonMistakes.forEach { mistake ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(mistake, modifier = Modifier.weight(1f), color = Color(0xFF7C2D12), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Button(
                    onClick = onOpenVideo,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PlayCircleFilled, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir vídeo demonstrativo")
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
private fun EnhancedExerciseLibraryScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var selectedCategory by remember { mutableStateOf(ExerciseLibraryCategory.All) }
    var selectedLevel by remember { mutableStateOf("Todos") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedExercise by remember { mutableStateOf<ExerciseLibraryItem?>(null) }
    val exercises = remember { exerciseLibraryItems() }
    var favoriteIds by remember { mutableStateOf(loadExerciseFavorites(context)) }
    val favoriteExercises = remember(exercises, favoriteIds) {
        exercises.filter { it.id in favoriteIds }
    }
    val filteredExercises = remember(selectedCategory, selectedLevel, searchQuery, exercises) {
        exercises.filter { item ->
            val matchesCategory = selectedCategory == ExerciseLibraryCategory.All || item.category == selectedCategory
            val matchesLevel = selectedLevel == "Todos" || item.level == selectedLevel
            val normalizedQuery = searchQuery.trim()
            val matchesSearch = normalizedQuery.isBlank() ||
                repairPtText(item.name).contains(normalizedQuery, ignoreCase = true) ||
                repairPtText(item.focus).contains(normalizedQuery, ignoreCase = true) ||
                repairPtText(item.category.label).contains(normalizedQuery, ignoreCase = true)
            matchesCategory && matchesLevel && matchesSearch
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Biblioteca de Exercícios", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = null) } },
            actions = { NotificationCenterActionButton() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Aprende a executar melhor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Pesquisa, filtra por nível e guarda favoritos para criares uma biblioteca pessoal de exercícios.",
                                    color = Color.White.copy(alpha = 0.76f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ExerciseLibraryHeaderStat("Categorias", (ExerciseLibraryCategory.values().size - 1).toString(), Modifier.weight(1f))
                            ExerciseLibraryHeaderStat("Exercícios", exercises.size.toString(), Modifier.weight(1f))
                            ExerciseLibraryHeaderStat("Favoritos", favoriteExercises.size.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Explorar biblioteca", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "Encontra por nome, grupo muscular ou dificuldade e abre instruções claras com vídeo.",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Pesquisar exercício") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpar pesquisa")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ExerciseLibraryCategory.values().toList()) { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = { selectedCategory = category },
                                    label = { Text(repairPtText(category.label)) },
                                    leadingIcon = {
                                        Icon(category.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(exerciseLibraryLevels()) { level ->
                                FilterChip(
                                    selected = selectedLevel == level,
                                    onClick = { selectedLevel = level },
                                    label = { Text(repairPtText(level)) }
                                )
                            }
                        }
                    }
                }
            }

            if (favoriteExercises.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE11D48))
                                Spacer(Modifier.width(8.dp))
                                Text("Os teus favoritos", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("${favoriteExercises.size}", color = Color(0xFF92400E), style = MaterialTheme.typography.labelSmall)
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(favoriteExercises.take(6)) { exercise ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        modifier = Modifier.clickable { selectedExercise = exercise }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(exercise.category.icon, contentDescription = null, tint = exercise.category.accent, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(repairPtText(exercise.name), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (selectedCategory == ExerciseLibraryCategory.All) "Todos os exercícios" else repairPtText(selectedCategory.label),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${filteredExercises.size} opcoes", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                }
            }

            items(filteredExercises) { exercise ->
                EnhancedExerciseLibraryCard(
                    item = exercise,
                    isFavorite = exercise.id in favoriteIds,
                    onOpen = { selectedExercise = exercise },
                    onOpenVideo = { uriHandler.openUri(exercise.videoUrl) },
                    onToggleFavorite = {
                        favoriteIds = toggleExerciseFavorite(context, favoriteIds, exercise.id)
                    }
                )
            }
        }
    }

    if (selectedExercise != null) {
        ExerciseLibraryDetailsDialog(
            item = selectedExercise!!,
            onDismiss = { selectedExercise = null },
            onOpenVideo = { uriHandler.openUri(selectedExercise!!.videoUrl) }
        )
    }
}

@Composable
private fun EnhancedExerciseLibraryCard(
    item: ExerciseLibraryItem,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onOpenVideo: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
        modifier = Modifier.clickable { onOpen() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(item.category.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.category.icon, contentDescription = null, tint = item.category.accent, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(repairPtText(item.name), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(repairPtText(item.focus), color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favoritar exercício",
                            tint = if (isFavorite) Color(0xFFE11D48) else Color(0xFF94A3B8)
                        )
                    }
                    FilledTonalButton(
                        onClick = onOpenVideo,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Vídeo")
                    }
                }
            }

            ExerciseLibraryPreviewBanner(item = item)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExerciseTag(repairPtText(item.level), item.category.accent)
                ExerciseTag(repairPtText(item.equipment), Color(0xFF0F172A))
                ExerciseTag(repairPtText(item.category.label), Color(0xFF475569))
            }

            Text(repairPtText(item.summary), color = Color(0xFF334155), style = MaterialTheme.typography.bodyMedium)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.steps.take(3).forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(item.category.accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = item.category.accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(repairPtText(step), modifier = Modifier.weight(1f), color = Color(0xFF475569), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Text(
                "Toque no card para ver instruções completas, dicas e erros a evitar.",
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun ExerciseLibraryPreviewBanner(item: ExerciseLibraryItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(94.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        item.category.accent.copy(alpha = 0.95f),
                        item.category.accent.copy(alpha = 0.55f),
                        Color.White
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Foco principal", color = Color.White.copy(alpha = 0.86f), style = MaterialTheme.typography.labelSmall)
            Text(repairPtText(item.focus), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Execução guiada com vídeo de apoio", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
        }
        Icon(
            item.category.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.22f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .size(54.dp)
        )
    }
}

/* -------------------- TREINOS -------------------- */

@Composable
private fun TrainingScreen(onOpenDrawer: () -> Unit, onOpenLibrary: () -> Unit) {
    val vm: TrainingViewModel = viewModel()
    val profileVM: ProfileViewModel = viewModel()
    val workouts = vm.workouts.collectAsState().value
    val aiSuggestion by vm.aiSuggestion.collectAsState()
    val profile = profileVM.profile.collectAsState().value

    val today = remember { Calendar.getInstance() }
    val trainingGoal = profile?.goal ?: "Manter Físico"
    val planRotation = remember { currentPlanRotationInfo() }
    val baseWeeklyPlan = remember(trainingGoal, planRotation.planIndex) {
        buildGoalBasedWorkoutPlan(trainingGoal, planRotation.planIndex)
    }
    val weeklyPlan = remember(baseWeeklyPlan, workouts) {
        buildSmartWeeklyWorkoutPlan(baseWeeklyPlan, workouts, today)
    }
    val weekGoal = baseWeeklyPlan.count { !it.isRestDay }
    val weekDone = workoutsDoneThisWeek(workouts)
    val todayPlan = weeklyPlan.firstOrNull { it.dayOfWeek == today.get(Calendar.DAY_OF_WEEK) }
    val didWorkoutToday = hasWorkoutOnDate(workouts, today)
    val primaryWorkout = when {
        todayPlan != null && !todayPlan.isRestDay && !didWorkoutToday -> todayPlan
        else -> nextPlannedWorkout(weeklyPlan, today.get(Calendar.DAY_OF_WEEK))
    }
    val upcomingWorkout = when {
        todayPlan != null && !todayPlan.isRestDay && !didWorkoutToday ->
            nextPlanEntryAfter(weeklyPlan, todayPlan.dayOfWeek)
        primaryWorkout != null ->
            nextPlanEntryAfter(weeklyPlan, primaryWorkout.dayOfWeek)
        else -> null
    }
    val highlightLabel = when {
        todayPlan != null && todayPlan.isRestDay -> "Hoje é recuperação"
        todayPlan != null && !didWorkoutToday -> "Treino de hoje"
        else -> "Próximo treino"
    }
    val highlightWorkoutName = when {
        todayPlan != null && todayPlan.isRestDay -> "Descanso ativo"
        primaryWorkout != null -> primaryWorkout.workoutTitle
        else -> "Sem treino planeado"
    }
    val highlightWhen = when {
        todayPlan != null && todayPlan.isRestDay -> "Hoje"
        todayPlan != null && !didWorkoutToday -> "Hoje"
        primaryWorkout != null -> primaryWorkout.dayLabel
        else -> "-"
    }
    val highlightSupportingText = when {
        todayPlan != null && todayPlan.isRestDay ->
            "Hoje o plano pede recuperação. Próximo: ${upcomingWorkout?.dayLabel ?: "-"} - ${upcomingWorkout?.workoutTitle ?: "A definir"}"
        todayPlan != null && !didWorkoutToday ->
            "Depois: ${upcomingWorkout?.dayLabel ?: "-"} - ${upcomingWorkout?.workoutTitle ?: "A definir"}"
        primaryWorkout != null ->
            "Planeado para ${primaryWorkout.dayLabel.lowercase(Locale.getDefault())} - ${primaryWorkout.durationText}"
        else -> "Atualiza o plano para ver os próximos treinos."
    }

    val smartHighlightLabel = if (primaryWorkout?.isAutoRescheduled == true) "Treino reajustado" else highlightLabel
    val smartHighlightSupportingText = if (primaryWorkout?.isAutoRescheduled == true) {
        "Este treino vinha de ${primaryWorkout.rescheduledFromDayLabel ?: "-"} e foi movido para ${primaryWorkout.dayLabel.lowercase(Locale.getDefault())} para manter a consistência da semana."
    } else {
        highlightSupportingText
    }

    var selectedWorkoutDay by remember { mutableStateOf<WorkoutPlanEntry?>(null) }
    var editingWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }
    val missedCount = remember(baseWeeklyPlan, workouts) {
        baseWeeklyPlan.count { entry ->
            !entry.isRestDay &&
                !hasWorkoutOnDate(workouts, calendarForCurrentWeekDay(entry.dayOfWeek)) &&
                isPastPlannedWorkout(calendarForCurrentWeekDay(entry.dayOfWeek))
        }
    }

    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alphaAnim.animateTo(1f, tween(1000)) }


    Column(Modifier.fillMaxSize().alpha(alphaAnim.value)) {
        CenterAlignedTopAppBar(
            title = { Text("Treinos", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = null) } },
            actions = { NotificationCenterActionButton() }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // AI Suggestion Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Sugestão da IA", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                aiSuggestion ?: "A analisar o teu perfil...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Biblioteca de exercícios", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Explora categorias, aprende a execução e abre vídeos guiados para cada exercício.",
                                color = Color.White.copy(alpha = 0.74f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(onClick = onOpenLibrary) {
                            Text("Abrir", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    val animatedDone = remember { Animatable(0f) }
                    val animatedProgress = remember { Animatable(0f) }
                    val targetProgress = if (weekGoal > 0) (weekDone / weekGoal.toFloat()) else 0f

                    LaunchedEffect(weekDone) {
                        launch { animatedDone.animateTo(weekDone.toFloat(), tween(1500)) }
                        launch { animatedProgress.animateTo(targetProgress.coerceIn(0f, 1f), tween(1500)) }
                    }

                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Meta semanal", fontWeight = FontWeight.SemiBold)
                                Text("${animatedDone.value.toInt()} / $weekGoal treinos", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                            }
                            Text("${(animatedProgress.value * 100).toInt()}%", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress.value },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(99.dp)),
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(smartHighlightLabel, fontWeight = FontWeight.SemiBold)
                            Text(highlightWorkoutName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(smartHighlightSupportingText, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                        Text(highlightWhen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Text("Plano da Semana", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

            if (missedCount > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFDBEAFE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF2563EB))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Planeamento inteligente ativo", fontWeight = FontWeight.SemiBold, color = Color(0xFF1D4ED8))
                                Text(
                                    "Reorganizámos $missedCount treino(s) em falta para os próximos dias livres da semana.",
                                    color = Color(0xFF475569),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Ciclo do plano: mensal", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                                "Faltam ${planRotation.daysRemaining} dias para o próximo bloco. A troca acontece em ${planRotation.nextChangeLabel}.",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            items(weeklyPlan) { p ->
                val plannedDate = remember(p.dayOfWeek) { calendarForCurrentWeekDay(p.dayOfWeek) }
                val isCompleted = hasWorkoutOnDate(workouts, plannedDate)
                val isTodayPlan = p.dayOfWeek == today.get(Calendar.DAY_OF_WEEK)
                val isPastPending = !isCompleted && isPastPlannedWorkout(plannedDate) && !p.isAutoRescheduled
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCompleted -> Color(0xFFF0FDF4)
                            p.isAutoRescheduled -> Color(0xFFF0F9FF)
                            isPastPending -> Color(0xFFFFFBEB)
                            else -> Color.White
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.clickable { selectedWorkoutDay = p }
                ) {
                    val mediaInfo = workoutMediaInfoFor(p.workoutTitle)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isCompleted -> Color(0xFFDCFCE7)
                                        p.isAutoRescheduled -> Color(0xFFE0F2FE)
                                        isPastPending -> Color(0xFFFEF3C7)
                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when {
                                    isCompleted -> Icons.Default.EmojiEvents
                                    p.isAutoRescheduled -> Icons.Default.AutoAwesome
                                    isPastPending -> Icons.Default.Warning
                                    else -> mediaInfo.icon
                                },
                                contentDescription = null,
                                tint = when {
                                    isCompleted -> Color(0xFF16A34A)
                                    p.isAutoRescheduled -> Color(0xFF0284C7)
                                    isPastPending -> Color(0xFFD97706)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.workoutTitle, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = when {
                                        isCompleted -> Color(0xFFDCFCE7)
                                        p.isAutoRescheduled -> Color(0xFFE0F2FE)
                                        isPastPending -> Color(0xFFFEF3C7)
                                        p.isRestDay -> Color(0xFFE2E8F0)
                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                    }
                                ) {
                                    Text(
                                        when {
                                            isCompleted -> "Treino feito"
                                            p.isAutoRescheduled -> "Reagendado de ${p.rescheduledFromDayLabel ?: "-"}"
                                            isPastPending -> "Pendente"
                                            p.isRestDay -> "Recuperação"
                                            else -> "Vídeo"
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = when {
                                            isCompleted -> Color(0xFF15803D)
                                            p.isAutoRescheduled -> Color(0xFF0369A1)
                                            isPastPending -> Color(0xFFB45309)
                                            p.isRestDay -> Color(0xFF475569)
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (p.isRestDay) "${p.durationText} • toque para ver mobilidade" else "${p.durationText} • toque para ver exercícios e vídeo",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(p.dayLabel, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            if (isCompleted) {
                                Icon(
                                    Icons.Default.TaskAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A)
                                )
                            } else if (isTodayPlan) {
                                FilledIconButton(
                                    onClick = {
                                        vm.addWorkout(
                                            title = p.workoutTitle,
                                            durationMin = durationMinutesFromPlan(p.durationText),
                                            kcal = estimatedWorkoutCalories(p, trainingGoal),
                                            createdAtEpochMs = completionEpochForPlannedDate(plannedDate),
                                            dateTime = "Concluído ${p.dayLabel} • ${getCurrentTimeText()}"
                                        )
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (p.isRestDay) Color(0xFF0EA5E9) else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        if (p.isRestDay) Icons.Default.SelfImprovement else Icons.Default.Check,
                                        contentDescription = if (p.isRestDay) "Marcar recuperação de hoje como feita" else "Marcar treino como feito",
                                        tint = Color.White
                                    )
                                }
                            } else if (isPastPending) {
                                FilledIconButton(
                                    onClick = {
                                        vm.addWorkout(
                                            title = p.workoutTitle,
                                            durationMin = durationMinutesFromPlan(p.durationText),
                                            kcal = estimatedWorkoutCalories(p, trainingGoal),
                                            createdAtEpochMs = completionEpochForPlannedDate(plannedDate),
                                            dateTime = "Registado depois de ${p.dayLabel} • ${getCurrentTimeText()}"
                                        )
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color(0xFFF59E0B)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = "Marcar treino pendente deste dia",
                                        tint = Color.White
                                    )
                                }
                            } else if (p.isRestDay) {
                                Icon(
                                    Icons.Default.SelfImprovement,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B)
                                )
                            } else {
                                Icon(
                                    Icons.Default.EventAvailable,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }

            item { Text("Últimos treinos", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

            items(workouts.take(10)) { w ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.clickable { editingWorkout = w }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${w.durationMin} min • ${w.caloriesBurned} kcal", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                        
                        Row(
                            modifier = Modifier.widthIn(max = 112.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                compactWorkoutDateLabel(w.dateTime),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color(0xFF475569)
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { vm.deleteWorkout(w) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFB91C1C), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Ação rápida", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Regista o teu treino para manter o progresso da semana.", color = Color(0xFF64748B))
                        Spacer(Modifier.height(10.dp))
                        var showAddWorkout by remember { mutableStateOf(false) }

                        Button(
                            onClick = { showAddWorkout = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Registar treino")
                        }

                        if (showAddWorkout) {
                            AddWorkoutDialog(
                                onDismiss = { showAddWorkout = false },
                                onSave = { title, durationMin, kcal, dateTime ->
                                    vm.addWorkout(title, durationMin, kcal, dateTime)
                                    showAddWorkout = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedWorkoutDay != null) {
        WorkoutDetailsDialog(day = selectedWorkoutDay!!, goal = trainingGoal, onDismiss = { selectedWorkoutDay = null })
    }

    if (editingWorkout != null) {
        AddWorkoutDialog(
            workout = editingWorkout,
            onDismiss = { editingWorkout = null },
            onSave = { title, durationMin, kcal, dateTime ->
                vm.updateWorkout(editingWorkout!!.id, title, durationMin, kcal, dateTime)
                editingWorkout = null
            }
        )
    }
}

@Composable
private fun WorkoutDetailsDialog(day: WorkoutPlanEntry, goal: String, onDismiss: () -> Unit) {
    val exercises = workoutDetailsFor(day.workoutTitle, goal)
    val mediaInfo = workoutMediaInfoFor(day.workoutTitle)
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            mediaInfo.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(day.workoutTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            "${day.dayLabel} • ${day.durationText}",
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayCircleFilled,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Exemplo em vídeo", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            if (day.isRestDay) {
                                "Abrimos uma demonstração de mobilidade e recuperação para orientar este dia."
                            } else {
                                "Vê um exemplo visual do treino antes de executar, para melhorares a técnica e a confiança."
                            },
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { uriHandler.openUri(mediaInfo.videoUrl) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Abrir vídeo de exemplo")
                        }
                    }
                }

                Text(
                    trainingGoalSummary(goal),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    exercises.forEach { ex ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { uriHandler.openUri(videoSearchUrl(ex)) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ex, color = Color(0xFF0F172A))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Toque para ver a execução",
                                    color = Color(0xFF64748B),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
private fun AssistantMiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.72f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(label, color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}


@Composable
private fun ProfileScreen(userName: String, userEmail: String, onLogout: () -> Unit, onOpenDrawer: () -> Unit) {
    val vm: ProfileViewModel = viewModel()
    val profile = vm.profile.collectAsState().value
    val context = LocalContext.current

    var ageText by remember(profile) { mutableStateOf((profile?.age ?: 0).toString()) }
    var goal by remember(profile) { mutableStateOf(profile?.goal ?: "Manter Físico") }
    var heightText by remember(profile) { mutableStateOf((profile?.heightCm ?: 170).toString()) }
    var weightText by remember(profile) { mutableStateOf((profile?.weightKg ?: 75f).toString()) }
    var notifications by remember(profile) { mutableStateOf(profile?.notificationsEnabled ?: true) }
    var avatarPath by remember(profile) { mutableStateOf(profile?.avatarPath.orEmpty()) }
    var hydrationReminderMinutes by remember(profile) { mutableStateOf(profile?.hydrationReminderMinutes ?: 0) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            avatarPath = saveAvatarBitmap(context, bitmap)
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            saveAvatarFromUri(context, uri)?.let { savedPath ->
                avatarPath = savedPath
            }
        }
    }

    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alphaAnim.animateTo(1f, tween(1000)) }

    Column(Modifier.fillMaxSize().alpha(alphaAnim.value)) {
        // Top header like dashboard
        val headerGradient = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primaryContainer
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(headerGradient)
                .padding(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Text("Perfil", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppAvatar(
                        avatarPath = avatarPath,
                        size = 64.dp,
                        placeholderIconSize = 34.dp,
                        placeholderTint = Color.White
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(userEmail, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Conta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Foto de perfil", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { cameraLauncher.launch(null) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Tirar foto")
                            }
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Collections, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Escolher")
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("Idade", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))

                        Text("Objetivo", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        
                        GoalDropdown(selectedGoal = goal, onGoalSelected = { goal = it })

                        Spacer(Modifier.height(10.dp))

                        Text("Altura (cm)", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it.filter { c -> c.isDigit() }.take(3) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))

                        Text("Peso (kg)", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it.take(6) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Notificações", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Switch(checked = notifications, onCheckedChange = { notifications = it })
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Intervalo do alerta de água", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            hydrationReminderOptions().forEach { option ->
                                FilterChip(
                                    selected = hydrationReminderMinutes == option.minutes,
                                    onClick = { hydrationReminderMinutes = option.minutes },
                                    label = { Text(option.label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (hydrationReminderMinutes == 0) {
                                "Sem seleção, o app mantém o intervalo padrão atual."
                            } else {
                                "Os lembretes passam a seguir ${hydrationReminderLabel(hydrationReminderMinutes)}."
                            },
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val a = ageText.toIntOrNull() ?: 0
                                val h = heightText.toIntOrNull() ?: 170
                                val w = weightText.replace(",", ".").toFloatOrNull() ?: 75f
                                vm.save(a, goal, h, w, notifications, avatarPath, hydrationReminderMinutes)
                                val (consumedMl, goalMl) = HydrationReminderManager.currentHydration(context)
                                HydrationReminderManager.syncState(
                                    context = context,
                                    consumedMl = consumedMl,
                                    goalMl = goalMl,
                                    notificationsEnabled = notifications,
                                    intervalMinutes = hydrationReminderMinutes
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Guardar alterações")
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Sessão", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = onLogout,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Terminar sessão")
                        }
                    }
                }
            }
        }
    }
}

/* -------------------- HISTORY SCREEN -------------------- */
@Composable
fun HistoryScreen(onOpenDrawer: () -> Unit) {
    val vm: HistoryViewModel = viewModel()
    val history = vm.mealHistory.collectAsState().value
    val sortedDates = history.keys.sortedDescending()
    var expandedDate by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        CenterAlignedTopAppBar(
            title = { Text("Histórico de Refeições", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = null) } }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sortedDates.isEmpty()) {
                item {
                    Text(
                        "Nenhum histórico de refeições encontrado.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            items(sortedDates) { date ->
                val meals = history[date] ?: emptyList()
                val totalCalories = meals.sumOf { it.calories }
                val isExpanded = expandedDate == date

                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedDate = if (isExpanded) null else date
                        }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(date, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text("$totalCalories kcal", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                meals.forEach { meal ->
                                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(meal.title, fontWeight = FontWeight.SemiBold)
                                            Text(meal.time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            if (!meal.note.isNullOrBlank()) {
                                                Text(
                                                    meal.note,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF475569)
                                                )
                                            }
                                        }
                                        Text("${meal.calories} kcal", color = Color.Gray)
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

private data class WorkoutPlanEntry(
    val dayLabel: String,
    val dayOfWeek: Int,
    val workoutTitle: String,
    val durationText: String,
    val isRestDay: Boolean = false,
    val isAutoRescheduled: Boolean = false,
    val rescheduledFromDayLabel: String? = null
)

private data class PlanRotationInfo(
    val daysRemaining: Int,
    val nextChangeLabel: String,
    val planIndex: Int
)

private fun buildGoalBasedWorkoutPlan(goal: String, planIndex: Int): List<WorkoutPlanEntry> {
    val normalizedGoal = normalizeTrainingGoal(goal)
    val safePlanIndex = planIndex % 4
    return when (normalizedGoal) {
        "lose" -> listOf(
            WorkoutPlanEntry("Seg", Calendar.MONDAY, if (safePlanIndex % 2 == 0) "HIIT + Corrida" else "HIIT + Bike", "40 min"),
            WorkoutPlanEntry("Ter", Calendar.TUESDAY, "Circuito metabólico", "45 min"),
            WorkoutPlanEntry("Qua", Calendar.WEDNESDAY, if (safePlanIndex < 2) "Corrida progressiva" else "Escada + Core", "35 min"),
            WorkoutPlanEntry("Qui", Calendar.THURSDAY, "Mobilidade e caminhada", "30 min", isRestDay = true),
            WorkoutPlanEntry("Sex", Calendar.FRIDAY, if (safePlanIndex % 2 == 0) "Full body dinâmico" else "HIIT + Core", "45 min")
        )
        "build" -> listOf(
            WorkoutPlanEntry("Seg", Calendar.MONDAY, if (safePlanIndex % 2 == 0) "Peito + Tríceps" else "Peito + Ombro", "60 min"),
            WorkoutPlanEntry("Ter", Calendar.TUESDAY, if (safePlanIndex < 2) "Costas + Bíceps" else "Costas pesadas", "60 min"),
            WorkoutPlanEntry("Qua", Calendar.WEDNESDAY, "Descanso ativo", "Mobilidade", isRestDay = true),
            WorkoutPlanEntry("Qui", Calendar.THURSDAY, if (safePlanIndex % 2 == 0) "Perna pesada" else "Perna + Core", "65 min"),
            WorkoutPlanEntry("Sex", Calendar.FRIDAY, if (safePlanIndex < 2) "Ombro + Braços" else "Upper body com carga", "55 min")
        )
        else -> listOf(
            WorkoutPlanEntry("Seg", Calendar.MONDAY, if (safePlanIndex % 2 == 0) "Peito + Core" else "Full body moderado", "50 min"),
            WorkoutPlanEntry("Ter", Calendar.TUESDAY, "Cardio moderado", "30 min"),
            WorkoutPlanEntry("Qua", Calendar.WEDNESDAY, if (safePlanIndex < 2) "Costas + Bíceps" else "Pernas moderadas", "50 min"),
            WorkoutPlanEntry("Qui", Calendar.THURSDAY, "Descanso ativo", "Mobilidade", isRestDay = true),
            WorkoutPlanEntry("Sex", Calendar.FRIDAY, if (safePlanIndex % 2 == 0) "Pernas + Ombro" else "Full body técnico", "50 min")
        )
    }
}

private fun normalizeTrainingGoal(goal: String): String {
    return when (goal) {
        "Perder Peso", "Perder Gordura" -> "lose"
        "Ganhar massa muscular" -> "build"
        else -> "maintain"
    }
}

private fun nextPlannedWorkout(plan: List<WorkoutPlanEntry>, currentDayOfWeek: Int): WorkoutPlanEntry? {
    if (plan.isEmpty()) return null
    val todayIndex = plan.indexOfFirst { it.dayOfWeek == currentDayOfWeek }
    if (todayIndex == -1) return plan.firstOrNull()
    for (offset in 1..plan.size) {
        val candidate = plan[(todayIndex + offset) % plan.size]
        if (!candidate.isRestDay) return candidate
    }
    return plan.firstOrNull()
}

private fun nextPlanEntryAfter(plan: List<WorkoutPlanEntry>, currentDayOfWeek: Int): WorkoutPlanEntry? {
    if (plan.isEmpty()) return null
    val currentIndex = plan.indexOfFirst { it.dayOfWeek == currentDayOfWeek }
    if (currentIndex == -1) return plan.firstOrNull()
    return plan[(currentIndex + 1) % plan.size]
}

private fun calendarForCurrentWeekDay(dayOfWeek: Int): Calendar {
    return Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, dayOfWeek)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private fun buildSmartWeeklyWorkoutPlan(
    basePlan: List<WorkoutPlanEntry>,
    workouts: List<WorkoutEntity>,
    today: Calendar
): List<WorkoutPlanEntry> {
    if (basePlan.isEmpty()) return basePlan

    val displayPlan = basePlan.toMutableList()
    val missedWorkouts = basePlan.filter { entry ->
        if (entry.isRestDay) return@filter false
        val plannedDate = calendarForCurrentWeekDay(entry.dayOfWeek)
        !hasWorkoutOnDate(workouts, plannedDate) && isPastPlannedWorkout(plannedDate)
    }

    if (missedWorkouts.isEmpty()) return displayPlan

    val availableRestSlots = displayPlan.withIndex()
        .filter { (_, entry) ->
            entry.isRestDay && isSameOrAfterTodayInCurrentWeek(entry.dayOfWeek, today.get(Calendar.DAY_OF_WEEK))
        }
        .map { it.index }
        .toMutableList()

    val extraWeekendSlots = mutableListOf(
        WorkoutPlanEntry("Sáb", Calendar.SATURDAY, "Descanso ativo", "Mobilidade", isRestDay = true),
        WorkoutPlanEntry("Dom", Calendar.SUNDAY, "Recuperação leve", "Mobilidade", isRestDay = true)
    ).filter { isSameOrAfterTodayInCurrentWeek(it.dayOfWeek, today.get(Calendar.DAY_OF_WEEK)) }.toMutableList()

    val appendedEntries = mutableListOf<WorkoutPlanEntry>()

    missedWorkouts.forEach { missed ->
        val rescheduledEntry = { target: WorkoutPlanEntry ->
            target.copy(
                workoutTitle = missed.workoutTitle,
                durationText = missed.durationText,
                isRestDay = false,
                isAutoRescheduled = true,
                rescheduledFromDayLabel = missed.dayLabel
            )
        }

        if (availableRestSlots.isNotEmpty()) {
            val index = availableRestSlots.removeAt(0)
            displayPlan[index] = rescheduledEntry(displayPlan[index])
        } else if (extraWeekendSlots.isNotEmpty()) {
            val weekendSlot = extraWeekendSlots.removeAt(0)
            appendedEntries += rescheduledEntry(weekendSlot)
        }
    }

    return (displayPlan + appendedEntries).sortedBy { mondayFirstDayOrder(it.dayOfWeek) }
}

private fun isSameOrAfterTodayInCurrentWeek(dayOfWeek: Int, todayDayOfWeek: Int): Boolean {
    val targetIndex = mondayFirstDayOrder(dayOfWeek)
    val todayIndex = mondayFirstDayOrder(todayDayOfWeek)
    if (targetIndex == -1 || todayIndex == -1) return false
    return targetIndex >= todayIndex
}

private fun mondayFirstDayOrder(dayOfWeek: Int): Int {
    return when (dayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> Int.MAX_VALUE
    }
}

private fun isPastPlannedWorkout(targetDate: Calendar): Boolean {
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val targetStart = (targetDate.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return targetStart.before(todayStart)
}

private fun completionEpochForPlannedDate(plannedDate: Calendar): Long {
    val now = Calendar.getInstance()
    return (plannedDate.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, now.get(Calendar.MINUTE))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun compactWorkoutDateLabel(dateTime: String): String {
    val normalized = when {
        dateTime.startsWith("Concluído ") -> dateTime.removePrefix("Concluído ")
        dateTime.startsWith("Registado depois de ") -> dateTime.removePrefix("Registado depois de ")
        else -> dateTime
    }
    return normalized
        .substringBefore(" • ")
        .substringBefore(" â€¢ ")
        .trim()
}

private fun notificationTimeLabel(createdAtEpochMs: Long): String {
    val diffMinutes = ((System.currentTimeMillis() - createdAtEpochMs) / 60000L).coerceAtLeast(0)
    return when {
        diffMinutes < 1 -> "agora"
        diffMinutes < 60 -> "há ${diffMinutes} min"
        diffMinutes < 1440 -> "há ${diffMinutes / 60} h"
        else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(createdAtEpochMs))
    }
}

private fun progressPeriodDays(filter: GoalsFilter): Int {
    return when (filter) {
        GoalsFilter.WEEK -> 7
        GoalsFilter.MONTH -> 30
        GoalsFilter.YEAR -> 365
    }
}

private fun buildGoalSummaries(
    filter: GoalsFilter,
    workouts: List<WorkoutEntity>,
    mealHistory: Map<String, List<MealEntity>>,
    caloriesTarget: Int,
    workoutGoalPerWeek: Int
): List<GoalProgressSummary> {
    val days = progressPeriodDays(filter)
    val workoutEntries = workouts.filter { it.createdAtEpochMs >= periodStartMillis(filter) }
    val workoutDays = workoutEntries
        .map { dayKey(it.createdAtEpochMs) }
        .distinct()
        .size
    val targetWorkoutDays = when (filter) {
        GoalsFilter.WEEK -> workoutGoalPerWeek
        GoalsFilter.MONTH -> workoutGoalPerWeek * 4
        GoalsFilter.YEAR -> workoutGoalPerWeek * 52
    }.coerceAtLeast(1)

    val mealDays = mealHistory
        .filterKeys { key -> isDateKeyInsidePeriod(key, filter) }
        .mapValues { (_, meals) -> meals.sumOf { it.calories } }

    val nutritionDaysOnTarget = mealDays.count { (_, totalCalories) ->
        totalCalories in (caloriesTarget * 0.8f).toInt()..(caloriesTarget * 1.1f).toInt()
    }
    val activeDays = (workoutEntries.map { dayKey(it.createdAtEpochMs) } + mealDays.keys).distinct().size

    return listOf(
        GoalProgressSummary(
            title = "Meta de treino",
            value = "$workoutDays/$targetWorkoutDays",
            subtitle = "Dias com treino registado no período selecionado.",
            progress = workoutDays / targetWorkoutDays.toFloat(),
            accent = Color(0xFF2563EB)
        ),
        GoalProgressSummary(
            title = "Meta nutricional",
            value = "$nutritionDaysOnTarget/${mealDays.size.coerceAtLeast(1)}",
            subtitle = "Dias em que as calorias ficaram numa faixa adequada da meta.",
            progress = if (mealDays.isEmpty()) 0f else nutritionDaysOnTarget / mealDays.size.toFloat(),
            accent = Color(0xFF16A34A)
        ),
        GoalProgressSummary(
            title = "Dias ativos",
            value = "$activeDays/$days",
            subtitle = "Dias com atividade registada entre refeições ou treinos.",
            progress = activeDays / days.toFloat(),
            accent = Color(0xFFF59E0B)
        )
    )
}

private fun buildProgressChartBars(
    filter: GoalsFilter,
    workouts: List<WorkoutEntity>,
    mealHistory: Map<String, List<MealEntity>>
): List<ProgressChartBar> {
    val now = Calendar.getInstance()
    return when (filter) {
        GoalsFilter.WEEK -> {
            (6 downTo 0).map { offset ->
                val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
                val key = dayKey(calendar.timeInMillis)
                val workoutCount = workouts.count { dayKey(it.createdAtEpochMs) == key }
                val mealCount = mealHistory[key]?.size ?: 0
                ProgressChartBar(label = weekDayShortLabel(calendar), value = (workoutCount + mealCount).toFloat())
            }
        }
        GoalsFilter.MONTH -> {
            (3 downTo 0).map { offset ->
                val start = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, -offset)
                }
                val month = start.get(Calendar.MONTH)
                val year = start.get(Calendar.YEAR)
                val total = workouts.count {
                    val c = Calendar.getInstance().apply { timeInMillis = it.createdAtEpochMs }
                    c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
                } + mealHistory.filterKeys {
                    val parsed = parseDateKey(it) ?: return@filterKeys false
                    parsed.get(Calendar.MONTH) == month && parsed.get(Calendar.YEAR) == year
                }.values.sumOf { it.size }
                ProgressChartBar(label = monthLabel(start), value = total.toFloat())
            }
        }
        GoalsFilter.YEAR -> {
            (11 downTo 0).map { offset ->
                val monthCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, -offset)
                }
                val month = monthCal.get(Calendar.MONTH)
                val year = monthCal.get(Calendar.YEAR)
                val total = workouts.count {
                    val c = Calendar.getInstance().apply { timeInMillis = it.createdAtEpochMs }
                    c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
                } + mealHistory.filterKeys {
                    val parsed = parseDateKey(it) ?: return@filterKeys false
                    parsed.get(Calendar.MONTH) == month && parsed.get(Calendar.YEAR) == year
                }.values.sumOf { it.size }
                ProgressChartBar(label = monthLabel(monthCal).take(3), value = total.toFloat())
            }
        }
    }
}

private fun periodStartMillis(filter: GoalsFilter): Long {
    return Calendar.getInstance().apply {
        when (filter) {
            GoalsFilter.WEEK -> add(Calendar.DAY_OF_YEAR, -6)
            GoalsFilter.MONTH -> add(Calendar.DAY_OF_YEAR, -29)
            GoalsFilter.YEAR -> add(Calendar.DAY_OF_YEAR, -364)
        }
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun isDateKeyInsidePeriod(dateKey: String, filter: GoalsFilter): Boolean {
    val parsed = parseDateKey(dateKey) ?: return false
    return parsed.timeInMillis >= periodStartMillis(filter)
}

private fun isDateKeyInCurrentWeek(dateKey: String): Boolean {
    val parsed = parseDateKey(dateKey) ?: return false
    val startOfWeek = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val endOfWeek = Calendar.getInstance().apply {
        timeInMillis = startOfWeek.timeInMillis
        add(Calendar.DAY_OF_YEAR, 7)
    }
    return parsed.timeInMillis in startOfWeek.timeInMillis until endOfWeek.timeInMillis
}

private fun parseDateKey(dateKey: String): Calendar? {
    val parsedDate = runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)
    }.getOrNull() ?: return null
    return Calendar.getInstance().apply { time = parsedDate }
}

private fun dayKey(epochMs: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(epochMs))
}

private fun weekDayShortLabel(calendar: Calendar): String {
    return when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Seg"
        Calendar.TUESDAY -> "Ter"
        Calendar.WEDNESDAY -> "Qua"
        Calendar.THURSDAY -> "Qui"
        Calendar.FRIDAY -> "Sex"
        Calendar.SATURDAY -> "Sab"
        else -> "Dom"
    }
}

private fun monthLabel(calendar: Calendar): String {
    return SimpleDateFormat("MMM", Locale("pt", "PT")).format(calendar.time).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString()
    }
}

private fun buildFitnessCalendarMonthState(
    monthOffset: Int,
    mealHistory: Map<String, List<MealEntity>>,
    hydrationHistory: Map<String, com.example.fitnessapp.data.HydrationEntity>,
    workouts: List<WorkoutEntity>
): FitnessCalendarMonthState {
    val targetMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, monthOffset)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startGrid = (targetMonth.clone() as Calendar).apply {
        val dayOrder = mondayFirstDayOrder(get(Calendar.DAY_OF_WEEK))
        add(Calendar.DAY_OF_YEAR, -dayOrder)
    }
    val workoutKeys = workouts.groupBy { dayKey(it.createdAtEpochMs) }
    val todayKey = dayKey(System.currentTimeMillis())
    val days = buildList {
        repeat(42) { index ->
            val day = (startGrid.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, index) }
            val key = dayKey(day.timeInMillis)
            val meals = mealHistory[key].orEmpty()
            val hydration = hydrationHistory[key]
            val hydrationPercent = if (hydration != null && hydration.goalMl > 0) {
                ((hydration.consumedMl / hydration.goalMl.toFloat()) * 100f).toInt().coerceIn(0, 100)
            } else 0
            val dayWorkouts = workoutKeys[key].orEmpty()
            val hasWorkout = dayWorkouts.isNotEmpty()
            val hitHydrationGoal = hydration != null && hydration.consumedMl >= hydration.goalMl && hydration.goalMl > 0
            val hydrationText = if (hydration != null) {
                "${hydration.consumedMl} / ${hydration.goalMl} ml"
            } else {
                "Sem registo de água"
            }
            add(
                FitnessCalendarDayUi(
                    dateKey = key,
                    dayNumberLabel = day.get(Calendar.DAY_OF_MONTH).toString(),
                    prettyLabel = SimpleDateFormat("dd 'de' MMMM", Locale("pt", "PT")).format(day.time),
                    isCurrentMonth = day.get(Calendar.MONTH) == targetMonth.get(Calendar.MONTH),
                    isToday = key == todayKey,
                    hasWorkout = hasWorkout,
                    mealCount = meals.size,
                    hitHydrationGoal = hitHydrationGoal,
                    hydrationPercent = hydrationPercent,
                    summaryText = buildCalendarDaySummary(hasWorkout, meals.size, hydrationPercent, hitHydrationGoal),
                    workoutTitles = dayWorkouts.map { it.title },
                    mealTitles = meals.map { "${it.title} • ${it.calories} kcal" },
                    hydrationText = hydrationText
                )
            )
        }
    }
    return FitnessCalendarMonthState(
        title = monthTitle(targetMonth),
        monthKey = "${targetMonth.get(Calendar.YEAR)}-${targetMonth.get(Calendar.MONTH)}",
        weekLabels = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sab", "Dom"),
        days = days
    )
}

private fun buildCalendarDaySummary(
    hasWorkout: Boolean,
    mealCount: Int,
    hydrationPercent: Int,
    hitHydrationGoal: Boolean
): String {
    return when {
        hasWorkout && mealCount > 0 && hitHydrationGoal ->
            "Dia completo: houve treino, refeições registadas e meta de água batida."
        hasWorkout && mealCount > 0 ->
            "Dia ativo com treino e alimentação acompanhada."
        hasWorkout ->
            "Treino registado neste dia. Boa consistência."
        mealCount > 0 && hydrationPercent > 0 ->
            "Dia com alimentação acompanhada e hidratação em progresso."
        mealCount > 0 ->
            "Existem refeições registadas neste dia."
        hydrationPercent > 0 ->
            "A hidratação foi acompanhada neste dia."
        else ->
            "Ainda não existem registos neste dia."
    }
}

private fun monthTitle(calendar: Calendar): String {
    val month = SimpleDateFormat("MMMM", Locale("pt", "PT")).format(calendar.time)
    return month.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString() } +
        " ${calendar.get(Calendar.YEAR)}"
}

private fun formatMetric(value: Float): String {
    return if (value % 1f == 0f) value.toInt().toString() else String.format(Locale.getDefault(), "%.1f", value)
}

private fun formatMetricsDate(epochMs: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(epochMs))
}

private fun metricsTrendLabel(latest: BodyMetricsEntity?, previous: BodyMetricsEntity?): String {
    if (latest == null || previous == null) return "A iniciar"
    val delta = latest.weightKg - previous.weightKg
    return when {
        delta > 0.2f -> "Subida"
        delta < -0.2f -> "Descida"
        else -> "Estavel"
    }
}

private fun weeklyAutoSummary(
    workouts: List<WorkoutEntity>,
    meals: List<MealEntity>,
    weeklyWorkoutGoal: Int,
    weeklyMinutes: Int,
    weeklyBurnedKcal: Int
): WeeklyAutoSummary {
    val workoutDays = workouts.map { dayKey(it.createdAtEpochMs) }.distinct().size
    val mealDays = meals.map { dayKey(it.createdAtEpochMs) }.distinct().size
    val totalMeals = meals.size
    val averageCalories = if (mealDays > 0) meals.sumOf { it.calories } / mealDays else 0

    val headline = when {
        workoutDays >= weeklyWorkoutGoal && mealDays >= 4 -> "Semana muito consistente"
        workoutDays >= weeklyWorkoutGoal -> "Treino em destaque"
        mealDays >= 4 -> "Boa consistência alimentar"
        workouts.isNotEmpty() || meals.isNotEmpty() -> "A semana já começou a andar"
        else -> "Ainda sem registos suficientes"
    }

    val supporting = when {
        workouts.isEmpty() && meals.isEmpty() ->
            "Assim que registares treinos e refeições, o app passa a resumir automaticamente a tua evolução semanal."
        workoutDays >= weeklyWorkoutGoal && mealDays >= 4 ->
            "Bateste a meta de treinos da semana e mantiveste presença na alimentação. Até agora somas $weeklyMinutes min de treino e $weeklyBurnedKcal kcal queimadas."
        workoutDays >= weeklyWorkoutGoal ->
            "A meta de treinos desta semana já foi atingida. Agora vale manter a regularidade na alimentação para fechar a semana ainda melhor."
        mealDays >= 4 ->
            "Tens boa regularidade nas refeições registadas. A média está em $averageCalories kcal por dia com registo, o que ajuda a acompanhar melhor a tua meta."
        else ->
            "Já tens $workoutDays dia(s) com treino e $mealDays dia(s) com refeições registadas. Mantém o ritmo para construir uma semana mais sólida."
    }

    return WeeklyAutoSummary(
        headline = headline,
        supporting = supporting,
        trainingValue = "$workoutDays/$weeklyWorkoutGoal",
        nutritionValue = "$totalMeals reg.",
        activeDaysValue = "${(workouts.map { dayKey(it.createdAtEpochMs) } + meals.map { dayKey(it.createdAtEpochMs) }).distinct().size}/7"
    )
}

private fun shouldShowNutritionAssistantHint(context: Context): Boolean {
    val prefs = context.getSharedPreferences("nutrition_ui_prefs", Context.MODE_PRIVATE)
    return !prefs.getBoolean("nutrition_assistant_hint_seen", false)
}

private fun markNutritionAssistantHintSeen(context: Context) {
    context.getSharedPreferences("nutrition_ui_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("nutrition_assistant_hint_seen", true)
        .apply()
}

private fun shouldShowAppTour(context: Context, userId: String): Boolean {
    val prefs = context.getSharedPreferences("app_tour_prefs", Context.MODE_PRIVATE)
    return !prefs.getBoolean("tour_seen_$userId", false)
}

private fun markAppTourSeen(context: Context, userId: String) {
    context.getSharedPreferences("app_tour_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("tour_seen_$userId", true)
        .apply()
}

private data class ReminderIntervalOption(val minutes: Int, val label: String)

private fun hydrationReminderOptions(): List<ReminderIntervalOption> = listOf(
    ReminderIntervalOption(0, "Padrão"),
    ReminderIntervalOption(1, "1 min"),
    ReminderIntervalOption(10, "10 min"),
    ReminderIntervalOption(20, "20 min"),
    ReminderIntervalOption(60, "1 hora")
)

private fun hydrationReminderLabel(minutes: Int): String {
    return when (minutes) {
        1 -> "alertas a cada 1 minuto"
        10 -> "alertas a cada 10 minutos"
        20 -> "alertas a cada 20 minutos"
        60 -> "alertas a cada 1 hora"
        else -> "o intervalo padrão"
    }
}

private fun durationMinutesFromPlan(durationText: String): Int {
    return durationText.filter { it.isDigit() }.toIntOrNull() ?: 30
}

private fun estimatedWorkoutCalories(plan: WorkoutPlanEntry, goal: String): Int {
    val duration = durationMinutesFromPlan(plan.durationText)
    val perMinute = when (normalizeTrainingGoal(goal)) {
        "lose" -> 9
        "build" -> 8
        else -> 7
    }
    return (duration * perMinute).coerceAtLeast(120)
}

private data class WorkoutMediaInfo(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val videoUrl: String
)

private fun videoSearchUrl(query: String): String {
    return "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8")
}

private fun workoutMediaInfoFor(workoutTitle: String): WorkoutMediaInfo {
    val icon = when {
        workoutTitle.contains("Corrida", ignoreCase = true) || workoutTitle.contains("Cardio", ignoreCase = true) ->
            Icons.Default.DirectionsRun
        workoutTitle.contains("Bike", ignoreCase = true) ->
            Icons.Default.PedalBike
        workoutTitle.contains("Core", ignoreCase = true) || workoutTitle.contains("Mobilidade", ignoreCase = true) || workoutTitle.contains("Descanso", ignoreCase = true) ->
            Icons.Default.SelfImprovement
        workoutTitle.contains("Perna", ignoreCase = true) || workoutTitle.contains("Pernas", ignoreCase = true) ->
            Icons.Default.AccessibilityNew
        else -> Icons.Default.FitnessCenter
    }
    val searchQuery = when {
        workoutTitle.contains("Descanso", ignoreCase = true) || workoutTitle.contains("Mobilidade", ignoreCase = true) ->
            "mobilidade recuperação treino"
        else -> "$workoutTitle treino exercícios"
    }
    val videoUrl = videoSearchUrl(searchQuery)
    return WorkoutMediaInfo(icon = icon, videoUrl = videoUrl)
}

private fun hasWorkoutOnDate(workouts: List<WorkoutEntity>, targetDate: Calendar): Boolean {
    return workouts.any { workout ->
        val calendar = Calendar.getInstance().apply { timeInMillis = workout.createdAtEpochMs }
        calendar.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == targetDate.get(Calendar.DAY_OF_YEAR)
    }
}

private fun workoutsDoneThisWeek(workouts: List<WorkoutEntity>): Int {
    val startOfWeek = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val endOfWeek = Calendar.getInstance().apply {
        timeInMillis = startOfWeek.timeInMillis
        add(Calendar.DAY_OF_YEAR, 7)
    }
    return workouts
        .filter { it.createdAtEpochMs in startOfWeek.timeInMillis until endOfWeek.timeInMillis }
        .map { workout ->
            val calendar = Calendar.getInstance().apply { timeInMillis = workout.createdAtEpochMs }
            "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
        }
        .distinct()
        .size
}

private fun currentPlanRotationInfo(): PlanRotationInfo {
    val now = Calendar.getInstance()
    val nextMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val millisPerDay = 24L * 60L * 60L * 1000L
    val daysRemaining = ((nextMonth.timeInMillis - todayStart.timeInMillis) / millisPerDay).toInt().coerceAtLeast(1)
    return PlanRotationInfo(
        daysRemaining = daysRemaining,
        nextChangeLabel = SimpleDateFormat("dd/MM", Locale.getDefault()).format(nextMonth.time),
        planIndex = now.get(Calendar.MONTH) % 4
    )
}

private fun trainingGoalSummary(goal: String): String {
    return when (normalizeTrainingGoal(goal)) {
        "lose" -> "Foco em gasto calórico: mais repetições, menos pausa e cardio estratégico."
        "build" -> "Foco em hipertrofia: menos repetições, mais carga e descansos um pouco maiores."
        else -> "Foco em manutenção e definição: 10 repetições com carga moderada e execução controlada."
    }
}

private fun workoutDetailsFor(workoutTitle: String, goal: String): List<String> {
    val normalizedGoal = normalizeTrainingGoal(goal)
    val preset = when (normalizedGoal) {
        "lose" -> "3-4 séries de 12-15 repetições"
        "build" -> "4 séries de 6-8 repetições com carga alta"
        else -> "3-4 séries de 10 repetições com carga moderada"
    }
    return when (workoutTitle) {
        "HIIT + Corrida" -> listOf("Corrida intervalada: 10 tiros de 30s forte / 60s leve", "Agachamento livre: $preset", "Avanço alternado: $preset", "Prancha: 3 x 40s")
        "HIIT + Bike" -> listOf("Bike intervalada: 12 tiros de 40s forte / 20s leve", "Kettlebell swing: $preset", "Burpee controlado: 3 séries de 12", "Abdominal infra: 3 séries de 15")
        "Circuito metabólico" -> listOf("Agachamento com halter: $preset", "Remada curvada: $preset", "Flexão inclinada: $preset", "Corrida leve: 12 min")
        "Corrida progressiva" -> listOf("Corrida progressiva: 25 a 35 min", "Passada no step: $preset", "Prancha lateral: 3 x 30s por lado")
        "Escada + Core" -> listOf("Escada ou subida: 20 min em blocos", "Mountain climber: 4 x 20", "Prancha com toque no ombro: 3 x 16")
        "Full body dinâmico", "HIIT + Core" -> listOf("Agachamento com press: $preset", "Remada com halter: $preset", "Swing com halter: $preset", "Prancha: 3 x 45s")
        "Peito + Tríceps" -> listOf("Supino reto: $preset", "Supino inclinado: $preset", "Crucifixo: $preset", "Tríceps na corda: $preset")
        "Peito + Ombro" -> listOf("Supino reto: $preset", "Desenvolvimento militar: $preset", "Elevação lateral: $preset", "Crucifixo na máquina: $preset")
        "Costas + Bíceps" -> listOf("Puxada frente: $preset", "Remada curvada: $preset", "Remada baixa: $preset", "Rosca direta: $preset")
        "Costas pesadas" -> listOf("Barra guiada: $preset", "Remada cavalinho: $preset", "Pulldown neutro: $preset", "Rosca martelo: $preset")
        "Perna pesada" -> listOf("Agachamento livre: $preset", "Leg press: $preset", "Stiff: $preset", "Panturrilha em pé: 4 x 12")
        "Perna + Core", "Pernas + Ombro", "Pernas moderadas" -> listOf("Agachamento: $preset", "Leg press: $preset", "Cadeira extensora: $preset", "Prancha: 3 x 40s")
        "Ombro + Braços", "Upper body com carga" -> listOf("Desenvolvimento com halteres: $preset", "Elevação lateral: $preset", "Rosca direta: $preset", "Tríceps francês: $preset")
        "Peito + Core", "Full body moderado", "Full body técnico" -> listOf("Supino com halter: $preset", "Agachamento goblet: $preset", "Remada unilateral: $preset", "Prancha: 3 x 30s")
        "Cardio moderado" -> listOf("Elíptica ou corrida: 25-30 min em ritmo moderado", "Mobilidade de quadril: 3 x 12", "Abdominal dead bug: 3 x 12")
        else -> listOf("Caminhada leve: 25 min", "Mobilidade geral: 15 min", "Alongamentos ativos: 10 min")
    }
}

private enum class ExerciseLibraryCategory(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color
) {
    All("Todas", Icons.Default.Apps, Color(0xFF475569)),
    Chest("Peito", Icons.Default.FitnessCenter, Color(0xFFDC2626)),
    Back("Costas", Icons.Default.AccessibilityNew, Color(0xFF2563EB)),
    Legs("Pernas", Icons.Default.DirectionsRun, Color(0xFF16A34A)),
    ShouldersArms("Ombros e Braços", Icons.Default.FitnessCenter, Color(0xFF9333EA)),
    Core("Core", Icons.Default.Bolt, Color(0xFFF59E0B)),
    Cardio("Cardio", Icons.Default.Favorite, Color(0xFFEF4444)),
    Mobility("Mobilidade", Icons.Default.SelfImprovement, Color(0xFF0891B2))
}

private data class ExerciseLibraryItem(
    val id: String,
    val name: String,
    val category: ExerciseLibraryCategory,
    val focus: String,
    val level: String,
    val equipment: String,
    val summary: String,
    val steps: List<String>,
    val tips: List<String>,
    val commonMistakes: List<String>,
    val videoQuery: String
) {
    val videoUrl: String
        get() = videoSearchUrl(videoQuery)
}

private fun exerciseLibraryItems(): List<ExerciseLibraryItem> = listOf(
    ExerciseLibraryItem(
        id = "supino_reto",
        name = "Supino reto",
        category = ExerciseLibraryCategory.Chest,
        focus = "Peito, ombros anteriores e tríceps",
        level = "Intermédio",
        equipment = "Barra e banco",
        summary = "Exercício base para desenvolver força e volume no peitoral com estabilidade e controle de carga.",
        steps = listOf(
            "Deita no banco com os olhos alinhados à barra e os pés firmes no chão.",
            "Retrai as escápulas, tira a barra do suporte e desce até a linha média do peito com controlo.",
            "Empurra a barra para cima sem perder a tensão no tronco e sem bater os cotovelos no final."
        ),
        tips = listOf(
            "Mantém o peito aberto e os ombros encaixados durante toda a série.",
            "Usa ritmo controlado na descida para proteger a articulação do ombro.",
            "Aperta os pés no solo para ganhar estabilidade."
        ),
        commonMistakes = listOf(
            "Descer a barra para muito acima do peito, forçando os ombros.",
            "Tirar os glúteos do banco para levantar a carga.",
            "Perder o controlo da barra na fase excêntrica."
        ),
        videoQuery = "supino reto execucao correta academia"
    ),
    ExerciseLibraryItem(
        id = "flexao",
        name = "Flexão de braços",
        category = ExerciseLibraryCategory.Chest,
        focus = "Peito, core e tríceps",
        level = "Iniciante",
        equipment = "Peso corporal",
        summary = "Movimento acessível para desenvolver empurrão, estabilidade do tronco e resistência muscular.",
        steps = listOf(
            "Coloca as mãos ligeiramente mais abertas do que os ombros e alinha corpo, quadril e pernas.",
            "Desce o peito em direção ao chão com o abdómen firme e os cotovelos a cerca de 45 graus.",
            "Empurra o solo até voltar à posição inicial sem deixar a lombar cair."
        ),
        tips = listOf(
            "Se for necessário, começa com as mãos num banco para reduzir a dificuldade.",
            "Mantém o pescoço neutro olhando para o chão.",
            "Contrai o abdómen para o corpo subir em bloco."
        ),
        commonMistakes = listOf(
            "Deixar a anca descer no meio da repetição.",
            "Abrir demasiado os cotovelos.",
            "Fazer amplitude muito curta e perder estímulo."
        ),
        videoQuery = "flexao de bracos tecnica correta"
    ),
    ExerciseLibraryItem(
        id = "remada_curvada",
        name = "Remada curvada",
        category = ExerciseLibraryCategory.Back,
        focus = "Costas, romboides e bíceps",
        level = "Intermédio",
        equipment = "Barra",
        summary = "Excelente para espessura das costas e fortalecimento da postura quando feito com tronco firme.",
        steps = listOf(
            "Segura a barra, dobra ligeiramente os joelhos e inclina o tronco mantendo a coluna neutra.",
            "Puxa a barra em direção ao abdómen levando os cotovelos para trás.",
            "Desce com controlo sem arredondar as costas e repete com a mesma base."
        ),
        tips = listOf(
            "Pensa em aproximar as escápulas no topo da remada.",
            "Usa uma carga que permita manter o tronco estável.",
            "Respira antes de puxar e trava o core."
        ),
        commonMistakes = listOf(
            "Transformar o exercício num balanço de corpo.",
            "Arredondar a lombar na descida.",
            "Puxar com os braços e esquecer a contração das costas."
        ),
        videoQuery = "remada curvada barra execucao correta"
    ),
    ExerciseLibraryItem(
        id = "puxada_frente",
        name = "Puxada frente",
        category = ExerciseLibraryCategory.Back,
        focus = "Grande dorsal e estabilização escapular",
        level = "Iniciante",
        equipment = "Polia",
        summary = "Um dos melhores movimentos para aprender a ativar dorsais e construir largura nas costas.",
        steps = listOf(
            "Senta com o peito alto e segura a barra com pegada aberta e firme.",
            "Puxa em direção à parte superior do peito trazendo os cotovelos para baixo.",
            "Sobe devagar até quase estender os braços, mantendo tensão na musculatura."
        ),
        tips = listOf(
            "Mantém o tronco quase fixo e evita inclinar em excesso.",
            "Começa a puxada pelas escápulas antes dos braços.",
            "Fecha a repetição com controlo total da subida."
        ),
        commonMistakes = listOf(
            "Puxar a barra atrás da nuca.",
            "Usar impulso com o corpo para completar a carga.",
            "Subir sem controlo e perder a tensão."
        ),
        videoQuery = "puxada frente na polia tecnica correta"
    ),
    ExerciseLibraryItem(
        id = "agachamento_livre",
        name = "Agachamento livre",
        category = ExerciseLibraryCategory.Legs,
        focus = "Quadríceps, glúteos e core",
        level = "Intermédio",
        equipment = "Barra",
        summary = "Movimento global para força, coordenação e desenvolvimento de membros inferiores.",
        steps = listOf(
            "Posiciona a barra no alto das costas, abre os pés na largura dos ombros e ativa o abdómen.",
            "Desce empurrando a anca para trás e os joelhos na linha dos pés até uma profundidade segura.",
            "Sobe pressionando o solo e mantendo o peito firme até voltar à extensão."
        ),
        tips = listOf(
            "Mantém o peso distribuído entre calcanhar e meio do pé.",
            "Olha em frente para ajudar na estabilidade.",
            "Trava o tronco antes de cada repetição."
        ),
        commonMistakes = listOf(
            "Deixar os joelhos colapsarem para dentro.",
            "Arredondar a lombar no fundo do movimento.",
            "Subir primeiro com a anca e depois com o tronco."
        ),
        videoQuery = "agachamento livre execucao correta academia"
    ),
    ExerciseLibraryItem(
        id = "leg_press",
        name = "Leg press",
        category = ExerciseLibraryCategory.Legs,
        focus = "Quadríceps e glúteos com alta estabilidade",
        level = "Iniciante",
        equipment = "Máquina",
        summary = "Boa opção para trabalhar pernas com controlo da trajetória e menor exigência técnica que o agachamento livre.",
        steps = listOf(
            "Senta bem apoiado, ajusta os pés na plataforma e segura as pegas laterais.",
            "Desce a carga até onde consigas manter a lombar colada ao banco.",
            "Empurra a plataforma sem esticar completamente os joelhos no topo."
        ),
        tips = listOf(
            "Usa amplitude segura sem tirar a anca do banco.",
            "Mantém os joelhos alinhados com a direção dos pés.",
            "Controla a velocidade principalmente na descida."
        ),
        commonMistakes = listOf(
            "Descer demasiado e arredondar a lombar.",
            "Trancar os joelhos no final.",
            "Colocar os pés muito baixos e sobrecarregar o joelho."
        ),
        videoQuery = "leg press execucao correta academia"
    ),
    ExerciseLibraryItem(
        id = "desenvolvimento_halteres",
        name = "Desenvolvimento com halteres",
        category = ExerciseLibraryCategory.ShouldersArms,
        focus = "Ombros, tríceps e estabilidade do core",
        level = "Intermédio",
        equipment = "Halteres",
        summary = "Empurrão vertical importante para fortalecer ombros com liberdade de movimento e controlo individual.",
        steps = listOf(
            "Senta ou fica de pé com os halteres na linha dos ombros e o abdómen ativo.",
            "Empurra os halteres para cima até quase juntar sobre a cabeça.",
            "Desce devagar até a posição inicial mantendo punhos neutros."
        ),
        tips = listOf(
            "Evita arquear demasiado a lombar.",
            "Mantém os cotovelos ligeiramente à frente do corpo.",
            "Usa amplitude completa sem perder estabilidade."
        ),
        commonMistakes = listOf(
            "Bater os halteres no topo.",
            "Transformar o movimento numa inclinação para trás.",
            "Descer muito rápido e perder controlo."
        ),
        videoQuery = "desenvolvimento com halteres execucao correta"
    ),
    ExerciseLibraryItem(
        id = "rosca_direta",
        name = "Rosca direta",
        category = ExerciseLibraryCategory.ShouldersArms,
        focus = "Bíceps e antebraço",
        level = "Iniciante",
        equipment = "Barra",
        summary = "Clássico para fortalecer o bíceps, com foco em controlo e eliminação de balanço corporal.",
        steps = listOf(
            "Fica de pé com a barra junto às coxas e os cotovelos próximos ao tronco.",
            "Flexiona os cotovelos até elevar a barra sem mover os ombros para a frente.",
            "Desce com controlo até quase estender totalmente os braços."
        ),
        tips = listOf(
            "Mantém os cotovelos fixos para isolar melhor o bíceps.",
            "Usa ritmo constante e sem trancos.",
            "Expira ao subir e inspira ao descer."
        ),
        commonMistakes = listOf(
            "Roubar com balanço do tronco.",
            "Encolher os ombros durante a subida.",
            "Encerrar a amplitude muito cedo."
        ),
        videoQuery = "rosca direta tecnica correta academia"
    ),
    ExerciseLibraryItem(
        id = "prancha",
        name = "Prancha",
        category = ExerciseLibraryCategory.Core,
        focus = "Core, estabilidade lombar e controlo corporal",
        level = "Iniciante",
        equipment = "Peso corporal",
        summary = "Base de estabilidade para proteger a coluna e melhorar desempenho em quase todos os treinos.",
        steps = listOf(
            "Apoia antebraços e pontas dos pés no chão formando uma linha reta do ombro ao tornozelo.",
            "Contrai abdómen e glúteos para evitar afundar a lombar.",
            "Mantém a posição respirando de forma controlada pelo tempo definido."
        ),
        tips = listOf(
            "Pensa em aproximar costelas e anca para ativar o centro do corpo.",
            "Mantém o olhar para o chão e pescoço neutro.",
            "Começa com tempos mais curtos e aumenta progressivamente."
        ),
        commonMistakes = listOf(
            "Subir demasiado a anca e perder a linha do corpo.",
            "Deixar a lombar afundar.",
            "Prender a respiração durante toda a série."
        ),
        videoQuery = "prancha abdominal execucao correta"
    ),
    ExerciseLibraryItem(
        id = "mountain_climber",
        name = "Mountain climber",
        category = ExerciseLibraryCategory.Core,
        focus = "Core, cardio e coordenação",
        level = "Intermédio",
        equipment = "Peso corporal",
        summary = "Combina ativação abdominal com componente cardiovascular, ótimo para circuitos e HIIT.",
        steps = listOf(
            "Entra em posição de prancha alta com mãos abaixo dos ombros.",
            "Traz um joelho de cada vez em direção ao peito de forma alternada.",
            "Mantém o tronco firme e acelera apenas se conseguires preservar a técnica."
        ),
        tips = listOf(
            "Mantém o peso repartido entre mãos e ponta dos pés.",
            "Executa primeiro devagar para fixar a mecânica.",
            "Usa séries por tempo em vez de repetir sem critério."
        ),
        commonMistakes = listOf(
            "Saltar excessivamente e perder a linha do corpo.",
            "Deixar os ombros muito atrás das mãos.",
            "Movimento curto e sem elevar o joelho."
        ),
        videoQuery = "mountain climber execucao correta"
    ),
    ExerciseLibraryItem(
        id = "corrida_esteira",
        name = "Corrida na esteira",
        category = ExerciseLibraryCategory.Cardio,
        focus = "Capacidade cardiovascular e gasto calórico",
        level = "Iniciante",
        equipment = "Esteira",
        summary = "Excelente para condicionamento e controlo de ritmo, tanto em treino moderado quanto intervalado.",
        steps = listOf(
            "Começa com caminhada ativa para ajustar postura e respiração.",
            "Aumenta gradualmente a velocidade mantendo passada curta e apoio suave.",
            "Fecha o treino com desaceleração progressiva para recuperar frequência cardíaca."
        ),
        tips = listOf(
            "Mantém o olhar em frente e os braços a acompanhar o ritmo.",
            "Evita apoiar-te constantemente nas barras laterais.",
            "Controla o impacto com passadas leves."
        ),
        commonMistakes = listOf(
            "Começar demasiado forte sem aquecimento.",
            "Correr a prender a respiração.",
            "Inclinar o tronco em excesso."
        ),
        videoQuery = "corrida na esteira postura correta"
    ),
    ExerciseLibraryItem(
        id = "bike_intervalada",
        name = "Bike intervalada",
        category = ExerciseLibraryCategory.Cardio,
        focus = "Cardio, pernas e alta intensidade",
        level = "Intermédio",
        equipment = "Bicicleta",
        summary = "Formato muito eficiente para elevar o gasto energético e melhorar condicionamento em menos tempo.",
        steps = listOf(
            "Ajusta selim e guiador para pedalar com boa extensão de joelho e postura neutra.",
            "Alterna blocos curtos de intensidade alta com recuperação leve a moderada.",
            "Mantém cadência estável e fecha com retorno gradual à calma."
        ),
        tips = listOf(
            "Controla resistência e tempo de cada bloco.",
            "Mantém os ombros relaxados durante o esforço.",
            "Usa o core para estabilizar a pelve."
        ),
        commonMistakes = listOf(
            "Pédalas demasiado pesadas logo no início.",
            "Subir os ombros e tensionar o pescoço.",
            "Perder consistência entre os intervalos."
        ),
        videoQuery = "bike intervalada spinning execucao correta"
    ),
    ExerciseLibraryItem(
        id = "mobilidade_quadril",
        name = "Mobilidade de quadril",
        category = ExerciseLibraryCategory.Mobility,
        focus = "Amplitude, controlo articular e prevenção",
        level = "Iniciante",
        equipment = "Peso corporal",
        summary = "Sequência simples para melhorar conforto em agachamentos, passadas e corrida.",
        steps = listOf(
            "Posiciona-te em base estável, com apoio no chão ou num banco se precisares.",
            "Executa rotações e aberturas de quadril com movimento controlado e sem dor.",
            "Repete para os dois lados com respiração calma e foco na amplitude."
        ),
        tips = listOf(
            "Faz estas séries no aquecimento ou em dias de recuperação.",
            "Procura movimento limpo, não velocidade.",
            "Mantém o tronco alto e o abdómen ativo."
        ),
        commonMistakes = listOf(
            "Forçar amplitude com desconforto agudo.",
            "Compensar com lombar em vez de mover o quadril.",
            "Executar de forma apressada."
        ),
        videoQuery = "mobilidade de quadril exercicios guiados"
    ),
    ExerciseLibraryItem(
        id = "alongamento_peitoral",
        name = "Alongamento peitoral",
        category = ExerciseLibraryCategory.Mobility,
        focus = "Postura, abertura torácica e recuperação",
        level = "Iniciante",
        equipment = "Parede ou porta",
        summary = "Alongamento útil para quem passa muitas horas sentado ou faz muito treino de empurrar.",
        steps = listOf(
            "Coloca o antebraço numa parede ou portal com o cotovelo à altura do ombro.",
            "Roda levemente o tronco para o lado oposto até sentir alongamento no peito.",
            "Mantém a respiração tranquila e repete do outro lado."
        ),
        tips = listOf(
            "Usa intensidade leve a moderada, sem dor.",
            "Evita elevar o ombro enquanto alongas.",
            "Combina com mobilidade torácica para melhor resultado."
        ),
        commonMistakes = listOf(
            "Forçar demasiado a articulação do ombro.",
            "Prender a respiração.",
            "Fazer o alongamento com o pescoço tenso."
        ),
        videoQuery = "alongamento peitoral postura correta"
    )
)

private fun exerciseLibraryLevels(): List<String> = listOf("Todos", "Iniciante", "Intermédio")

private fun loadExerciseFavorites(context: Context): Set<String> {
    return context.getSharedPreferences("exercise_library_prefs", Context.MODE_PRIVATE)
        .getStringSet("favorite_exercises", emptySet())
        ?.toSet()
        ?: emptySet()
}

private fun repairPtText(text: String): String = text

private fun toggleExerciseFavorite(context: Context, currentFavorites: Set<String>, exerciseId: String): Set<String> {
    val updated = currentFavorites.toMutableSet().apply {
        if (!add(exerciseId)) remove(exerciseId)
    }.toSet()
    context.getSharedPreferences("exercise_library_prefs", Context.MODE_PRIVATE)
        .edit()
        .putStringSet("favorite_exercises", updated)
        .apply()
    return updated
}

/* -------------------- MODELOS SIMPLES -------------------- */

private data class SimpleRow(val title: String, val trailing: String, val subtitle: String)

private data class DailyItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val trailing: String,
    val subtitle: String
)

private data class NutritionSuggestion(
    val name: String,
    val calories: Int,
    val description: String
)

/* -------------------- DIALOGS -------------------- */

@Composable
private fun AddMealDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, calories: Int, time: String, note: String?) -> Unit
) {
    val context = LocalContext.current
    val mealTypes = remember {
        listOf(
            "Cafe da manha",
            "Lanche da manha",
            "Almoco",
            "Lanche da tarde",
            "Janta",
            "Lanche da noite"
        )
    }
    var title by remember { mutableStateOf(mealTypes.first()) }
    var titleExpanded by remember { mutableStateOf(false) }
    var kcalText by remember { mutableStateOf("500") }
    var mealTime by remember { mutableStateOf(getCurrentTimeText()) }
    var note by remember { mutableStateOf("") }

    fun openTimePicker() {
        val currentSelection = Calendar.getInstance().apply {
            val parts = mealTime.split(":")
            set(Calendar.HOUR_OF_DAY, parts.getOrNull(0)?.toIntOrNull() ?: get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, parts.getOrNull(1)?.toIntOrNull() ?: get(Calendar.MINUTE))
        }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                mealTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            },
            currentSelection.get(Calendar.HOUR_OF_DAY),
            currentSelection.get(Calendar.MINUTE),
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val kcal = kcalText.toIntOrNull() ?: 0
                onSave(title, kcal, mealTime, note.ifBlank { null })
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Adicionar refeição") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = titleExpanded,
                    onExpandedChange = { titleExpanded = !titleExpanded }
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Titulo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = titleExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = titleExpanded,
                        onDismissRequest = { titleExpanded = false }
                    ) {
                        mealTypes.forEach { mealType ->
                            DropdownMenuItem(
                                text = { Text(mealType) },
                                onClick = {
                                    title = mealType
                                    titleExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = kcalText,
                    onValueChange = { kcalText = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Calorias (kcal)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = mealTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Horario") },
                    trailingIcon = {
                        IconButton(onClick = ::openTimePicker) {
                            Icon(Icons.Default.Schedule, contentDescription = "Selecionar horario")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = ::openTimePicker),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

private fun getCurrentTimeText(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}

@Composable
private fun AddWorkoutDialog(
    workout: WorkoutEntity? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, durationMin: Int, calories: Int, dateTime: String) -> Unit
) {
    var title by remember { mutableStateOf(workout?.title ?: "Treino") }
    var durationText by remember { mutableStateOf(workout?.durationMin?.toString() ?: "45") }
    var kcalText by remember { mutableStateOf(workout?.caloriesBurned?.toString() ?: "300") }
    var dateTime by remember { mutableStateOf(workout?.dateTime ?: "Hoje 18:30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val duration = durationText.toIntOrNull() ?: 0
                val kcal = kcalText.toIntOrNull() ?: 0
                onSave(title, duration, kcal, dateTime)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text(if (workout == null) "Registar treino" else "Editar treino") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") })
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Duração (min)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = kcalText,
                    onValueChange = { kcalText = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text("Kcal queimadas") },
                    singleLine = true
                )
                OutlinedTextField(value = dateTime, onValueChange = { dateTime = it }, label = { Text("Data/Hora") }, singleLine = true)
            }
        }
    )
}






