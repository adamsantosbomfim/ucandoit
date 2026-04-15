@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.fitnessapp

import android.Manifest
import android.app.TimePickerDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.data.MealEntity
import com.example.fitnessapp.data.ProfileEntity
import com.example.fitnessapp.data.WorkoutEntity
import com.example.fitnessapp.ui.theme.FitnessTheme
import com.example.fitnessapp.vm.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
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
        Route.History.value
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

    LaunchedEffect(showNav) {
        if (!showNav) {
            drawerCanOpen = false
            drawerState.close()
        }
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
                    TrainingScreen(onOpenDrawer = {
                        drawerCanOpen = true
                        scope.launch { drawerState.open() }
                    })
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
                }
            }
        ) {
            appScaffold()
        }
    } else {
        appScaffold()
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
private fun DashboardHeader(userName: String, weightKg: Int, kcal: Int, onOpenDrawer: () -> Unit) {
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
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderPill(label = "Peso", value = "${weightKg} kg")
                HeaderPill(label = "Calorias consumidas", value = "${kcal} kcal")
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
        }
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
    val caloriesToday = nutritionViewModel.caloriesToday.collectAsState().value
    val meals = nutritionViewModel.meals.collectAsState().value
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

    val contentAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { contentAlpha.animateTo(1f, tween(1000)) }

    Column(Modifier.fillMaxSize().alpha(contentAlpha.value)) {
        DashboardHeader(
            userName = userName,
            weightKg = userWeight,
            kcal = caloriesToday,
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
                HomeMetricCard(
                    icon = Icons.Default.LocalFireDepartment,
                    title = "Calorias consumidas",
                    valueText = "$caloriesToday kcal",
                    goalText = "$caloriesTarget kcal",
                    supportingText = if (meals.isEmpty()) {
                        "Ainda nao existem refeicoes registadas hoje."
                    } else {
                        "${meals.size} refeicoes hoje • ultima: ${latestMeal?.title ?: "-"}"
                    },
                    progress = if (caloriesTarget > 0) caloriesToday / caloriesTarget.toFloat() else 0f
                )
            }

            item {
                HomeMetricCard(
                    icon = Icons.Default.WaterDrop,
                    title = "Hidratacao",
                    valueText = formatWaterLiters(hydrationNowMl),
                    goalText = formatWaterLiters(hydrationGoalMl),
                    supportingText = if (hydrationNowMl >= hydrationGoalMl) {
                        "Meta de agua concluida hoje."
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
        }
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
                            if (dailyItems.isEmpty()) "Ainda nao existe atividade registada hoje. Comece pela agua, uma refeicao ou um treino."
                            else "Resumo real do seu dia com alimentacao, hidratacao, treino e perfil atual.",
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
                "Dia forte. Ja estas a acertar em varias frentes e isso vai aparecer no resultado."
            hitWaterGoal && hasWorkoutToday ->
                "Boa! Agua em dia e treino concluido. Hoje foi um dia muito bem jogado."
            !hasWorkoutToday && !hasMeals ->
                "Vamos organizar o dia: faz uma boa refeicao e encaixa o treino para ganhar ritmo."
            waterProgress < 0.35f ->
                "Bebe mais agua agora. Isso ja melhora energia, foco e disposicao."
            !hasWorkoutToday ->
                "O treino de hoje ainda esta a tua espera. Faz mesmo que seja uma sessao curta."
            !hasMeals ->
                "Vamos manter a dieta. Uma refeicao bem feita agora muda o resto do dia."
            hasMeals && !caloriesOnTrack ->
                "Ajusta as proximas escolhas para manter o dia mais alinhado com a tua meta."
            hasMeals ->
                "Boa. Ja tens $mealCount refeicao${if (mealCount > 1) "es" else ""} registada${if (mealCount > 1) "s" else ""} hoje."
            goal == "Ganhar massa muscular" ->
                "Forca no plano. Comer bem e treinar forte vao construir resultado."
            goal == "Perder Gordura" || goal == "Perder Peso" ->
                "Consistencia ganha do impulso. Hoje vale muito."
            else ->
                "Vamos la. Hoje e mais um passo para a tua melhor versao."
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
                        label = if (hitWaterGoal) "Agua ok" else "Agua",
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
    val waterNowL = waterNowMl / 1000f

    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alphaAnim.animateTo(1f, tween(1000)) }

    LaunchedEffect(profile?.notificationsEnabled, waterNowMl, waterGoalMl) {
        val notificationsEnabled = profile?.notificationsEnabled == true
        HydrationReminderManager.syncState(
            context = context,
            consumedMl = waterNowMl,
            goalMl = waterGoalMl,
            notificationsEnabled = notificationsEnabled
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
            actions = { IconButton(onClick = {}) { Icon(Icons.Default.Notifications, contentDescription = null) } }
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
                                "Meta de agua batida hoje. Os lembretes voltam amanha."
                            } else {
                                "Lembretes ativos enquanto a meta nao for atingida e as notificacoes estiverem ligadas."
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

        NutritionAssistantBubble(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 22.dp),
            onClick = { showAssistant = true }
        )

        if (showAssistant) {
            NutritionAssistantDialog(
                goal = profile?.goal ?: "Manter Fisico",
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
                                "Toque para adicionar esta refeicao ao seu dia.",
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
            NutritionSuggestion("Iogurte proteico com fruta", 170, "Pratico e com boa saciedade sem pesar."),
            NutritionSuggestion("Maca com pasta de amendoim", 190, "Combina fibra com gordura boa."),
            NutritionSuggestion("Castanhas com queijo fresco", 220, "Lanche funcional e facil de transportar.")
        )
        "Almoco" -> listOf(
            NutritionSuggestion("Frango, arroz e salada", 480, "Prato base fit com proteina magra e carboidrato controlado."),
            NutritionSuggestion("Peixe com batata doce", 420, "Opcao leve e consistente para o meio do dia."),
            NutritionSuggestion("Carne magra com legumes", 530, "Melhor escolha quando ainda ha bastante margem calorica.")
        )
        "Lanche da tarde" -> listOf(
            NutritionSuggestion("Sandes integral de peru", 230, "Ajuda a segurar a fome ate a janta."),
            NutritionSuggestion("Banana com iogurte", 180, "Lanche simples e funcional para energia estavel."),
            NutritionSuggestion("Tapioca com cottage", 260, "Boa opcao quando o treino ou a janta ainda vao demorar.")
        )
        "Janta" -> listOf(
            NutritionSuggestion("Peixe com legumes", 320, "Janta fit com proteina e volume sem exagero."),
            NutritionSuggestion("Omelete com salada", 280, "Leve e boa para fechar o dia."),
            NutritionSuggestion("Frango desfiado com sopa de legumes", 350, "Mais conforto sem sair do plano.")
        )
        else -> listOf(
            NutritionSuggestion("Iogurte natural", 110, "Boa opcao leve para nao dormir pesado."),
            NutritionSuggestion("Kiwi com chia", 95, "Baixa caloria com fibra."),
            NutritionSuggestion("Leite magro com canela", 90, "Ajuda a controlar a fome noturna.")
        )
    }

    val adjusted = baseSuggestions.map { suggestion ->
        val adjustedCalories = adjustCaloriesToGoal(suggestion.calories, profile).coerceAtMost(mealBudget)
        val finalCalories = adjustedCalories.coerceAtLeast(minSuggestionCaloriesFor(mealType))
        val fitDescription = when (profile) {
            "light" -> "${suggestion.description} Mantem controle calorico para encaixar melhor na dieta."
            "build" -> "${suggestion.description} Traz mais energia e proteina para sustentar ganho muscular."
            else -> "${suggestion.description} Mantem equilibrio entre saciedade e controle calorico."
        }
        suggestion.copy(calories = finalCalories, description = fitDescription)
    }

    return if (remainingCalories < 120) {
        listOf(
            NutritionSuggestion(
                "Ceia muito leve",
                remainingCalories.coerceAtLeast(70),
                "Seu saldo calorico esta curto. Priorize algo pequeno, proteico e facil de digerir."
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
        if (mealType == currentMealType) "Pelo horario atual, esta e a melhor janela para $mealType."
        else "Voce pode planejar $mealType desde ja para nao sair da dieta."
    return "$timingText Objetivo: $goal. Restam $remainingCalories kcal no dia, entao a sugestao mira cerca de $mealBudget kcal."
}

private fun recommendedWorkoutSessionsFor(goal: String): Int {
    return when (goal) {
        "Perder Gordura", "Perder Peso" -> 5
        "Ganhar massa muscular" -> 4
        else -> 4
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
        title = "Hidratacao",
        trailing = formatWaterLiters(hydration.consumedMl.coerceAtLeast(0)),
        subtitle = "Meta atual: ${formatWaterLiters(waterGoalMl)}"
    )
    meals.firstOrNull()?.let { meal ->
        items += DailyItem(
            icon = Icons.Default.Restaurant,
            title = "Ultima refeicao",
            trailing = "${meal.calories} kcal",
            subtitle = "${meal.title} • ${meal.time}"
        )
    }
    workouts.firstOrNull()?.let { workout ->
        items += DailyItem(
            icon = Icons.Default.FitnessCenter,
            title = "Ultimo treino",
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
            tags = listOf("Proteina", "Pratico", "Leve"),
            colors = listOf(Color(0xFF0F766E), Color(0xFF2DD4BF))
        )
        suggestionName.contains("Panqueca", ignoreCase = true) || suggestionName.contains("Tapioca", ignoreCase = true) -> NutritionVisualInfo(
            emoji = "🥞",
            caption = "Opcao funcional para energia estavel",
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
            caption = "Refeicao leve e eficiente",
            tags = listOf("Leve", "Proteina", "Janta"),
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
            tags = listOf("Proteina", "Rapido", "Sustenta"),
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
                    caption = "Lanche inteligente para nao sair da rota",
                    tags = listOf("Snack", "Pratico", "Fit"),
                    colors = listOf(Color(0xFF1E3A8A), Color(0xFF38BDF8))
                )
            }
            byMeal
        }
    }
}

/* -------------------- TREINOS -------------------- */

@Composable
private fun TrainingScreen(onOpenDrawer: () -> Unit) {
    val vm: TrainingViewModel = viewModel()
    val profileVM: ProfileViewModel = viewModel()
    val workouts = vm.workouts.collectAsState().value
    val aiSuggestion by vm.aiSuggestion.collectAsState()
    val profile = profileVM.profile.collectAsState().value

    val today = remember { Calendar.getInstance() }
    val trainingGoal = profile?.goal ?: "Manter Fisico"
    val planRotation = remember { currentPlanRotationInfo() }
    val weeklyPlan = remember(trainingGoal, planRotation.planIndex) {
        buildGoalBasedWorkoutPlan(trainingGoal, planRotation.planIndex)
    }
    val weekGoal = weeklyPlan.count { !it.isRestDay }
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
        todayPlan != null && todayPlan.isRestDay -> "Hoje e recuperacao"
        todayPlan != null && !didWorkoutToday -> "Treino de hoje"
        else -> "Proximo treino"
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
            "Hoje o plano pede recuperacao. Proximo: ${upcomingWorkout?.dayLabel ?: "-"} - ${upcomingWorkout?.workoutTitle ?: "A definir"}"
        todayPlan != null && !didWorkoutToday ->
            "Depois: ${upcomingWorkout?.dayLabel ?: "-"} - ${upcomingWorkout?.workoutTitle ?: "A definir"}"
        primaryWorkout != null ->
            "Planeado para ${primaryWorkout.dayLabel.lowercase(Locale.getDefault())} - ${primaryWorkout.durationText}"
        else -> "Atualize o plano para ver os proximos treinos."
    }

    var selectedWorkoutDay by remember { mutableStateOf<WorkoutPlanEntry?>(null) }
    var editingWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }

    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alphaAnim.animateTo(1f, tween(1000)) }


    Column(Modifier.fillMaxSize().alpha(alphaAnim.value)) {
        CenterAlignedTopAppBar(
            title = { Text("Treinos", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = null) } },
            actions = { IconButton(onClick = {}) { Icon(Icons.Default.Notifications, contentDescription = null) } }
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
                                aiSuggestion ?: "A analisar o seu perfil...",
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                            Text(highlightLabel, fontWeight = FontWeight.SemiBold)
                            Text(highlightWorkoutName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(highlightSupportingText, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                        }
                        Text(highlightWhen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item { Text("Plano da Semana", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

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
                            "Faltam ${planRotation.daysRemaining} dias para o proximo bloco. Troca em ${planRotation.nextChangeLabel}.",
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
                val isPastPending = !isCompleted && isPastPlannedWorkout(plannedDate)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCompleted -> Color(0xFFF0FDF4)
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
                                        isPastPending -> Color(0xFFFEF3C7)
                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when {
                                    isCompleted -> Icons.Default.EmojiEvents
                                    isPastPending -> Icons.Default.Warning
                                    else -> mediaInfo.icon
                                },
                                contentDescription = null,
                                tint = when {
                                    isCompleted -> Color(0xFF16A34A)
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
                                        isPastPending -> Color(0xFFFEF3C7)
                                        p.isRestDay -> Color(0xFFE2E8F0)
                                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                    }
                                ) {
                                    Text(
                                        when {
                                            isCompleted -> "Treino feito"
                                            isPastPending -> "Pendente"
                                            p.isRestDay -> "Recuperacao"
                                            else -> "Video"
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        color = when {
                                            isCompleted -> Color(0xFF15803D)
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
                                if (p.isRestDay) "${p.durationText} • toque para ver mobilidade" else "${p.durationText} • toque para ver exercicios e video",
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
                                            dateTime = "Concluido ${p.dayLabel} • ${getCurrentTimeText()}"
                                        )
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (p.isRestDay) Color(0xFF0EA5E9) else MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        if (p.isRestDay) Icons.Default.SelfImprovement else Icons.Default.Check,
                                        contentDescription = if (p.isRestDay) "Marcar recuperacao de hoje como feita" else "Marcar treino como feito",
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
                                            dateTime = "Registado depois ${p.dayLabel} • ${getCurrentTimeText()}"
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
                            modifier = Modifier.widthIn(max = 132.dp),
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
                            IconButton(onClick = { vm.deleteWorkout(w) }) {
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
                        Text("Regista o teu treino para manter o progresso semanal.", color = Color(0xFF64748B))
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
                            Text("Exemplo em video", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            if (day.isRestDay) {
                                "Abrimos uma demonstracao de mobilidade e recuperacao para orientar este dia."
                            } else {
                                "Veja um exemplo visual do treino antes de executar para melhorar tecnica e confianca."
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
                            Text("Abrir video de exemplo")
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
                                    "Toque para ver a execucao",
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

    var ageText by remember(profile) { mutableStateOf((profile?.age ?: 0).toString()) }
    var goal by remember(profile) { mutableStateOf(profile?.goal ?: "Manter Físico") }
    var heightText by remember(profile) { mutableStateOf((profile?.heightCm ?: 170).toString()) }
    var weightText by remember(profile) { mutableStateOf((profile?.weightKg ?: 75f).toString()) }
    var notifications by remember(profile) { mutableStateOf(profile?.notificationsEnabled ?: true) }

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
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
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

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val a = ageText.toIntOrNull() ?: 0
                                val h = heightText.toIntOrNull() ?: 170
                                val w = weightText.replace(",", ".").toFloatOrNull() ?: 75f
                                vm.save(a, goal, h, w, notifications)
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
    val isRestDay: Boolean = false
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
            WorkoutPlanEntry("Ter", Calendar.TUESDAY, "Circuito metabolico", "45 min"),
            WorkoutPlanEntry("Qua", Calendar.WEDNESDAY, if (safePlanIndex < 2) "Corrida progressiva" else "Escada + Core", "35 min"),
            WorkoutPlanEntry("Qui", Calendar.THURSDAY, "Mobilidade e caminhada", "30 min", isRestDay = true),
            WorkoutPlanEntry("Sex", Calendar.FRIDAY, if (safePlanIndex % 2 == 0) "Full body dinamico" else "HIIT + Core", "45 min")
        )
        "build" -> listOf(
            WorkoutPlanEntry("Seg", Calendar.MONDAY, if (safePlanIndex % 2 == 0) "Peito + Triceps" else "Peito + Ombro", "60 min"),
            WorkoutPlanEntry("Ter", Calendar.TUESDAY, if (safePlanIndex < 2) "Costas + Biceps" else "Costas pesada", "60 min"),
            WorkoutPlanEntry("Qua", Calendar.WEDNESDAY, "Descanso ativo", "Mobilidade", isRestDay = true),
            WorkoutPlanEntry("Qui", Calendar.THURSDAY, if (safePlanIndex % 2 == 0) "Perna pesada" else "Perna + Core", "65 min"),
            WorkoutPlanEntry("Sex", Calendar.FRIDAY, if (safePlanIndex < 2) "Ombro + Bracos" else "Upper body carga", "55 min")
        )
        else -> listOf(
            WorkoutPlanEntry("Seg", Calendar.MONDAY, if (safePlanIndex % 2 == 0) "Peito + Core" else "Full body moderado", "50 min"),
            WorkoutPlanEntry("Ter", Calendar.TUESDAY, "Cardio moderado", "30 min"),
            WorkoutPlanEntry("Qua", Calendar.WEDNESDAY, if (safePlanIndex < 2) "Costas + Biceps" else "Pernas moderado", "50 min"),
            WorkoutPlanEntry("Qui", Calendar.THURSDAY, "Descanso ativo", "Mobilidade", isRestDay = true),
            WorkoutPlanEntry("Sex", Calendar.FRIDAY, if (safePlanIndex % 2 == 0) "Pernas + Ombro" else "Full body tecnico", "50 min")
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
    return when {
        dateTime.startsWith("Concluido ") -> dateTime.removePrefix("Concluido ")
        dateTime.startsWith("Registado depois ") -> dateTime.removePrefix("Registado depois ")
        else -> dateTime
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
            "mobilidade recuperacao treino"
        else -> "$workoutTitle treino exercicios"
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
        "lose" -> "Foco em gasto calorico: mais repeticoes, menos pausa e cardio estrategico."
        "build" -> "Foco em hipertrofia: menos repeticoes, mais carga e descansos um pouco maiores."
        else -> "Foco em manutencao e definicao: 10 repeticoes com carga moderada e execucao controlada."
    }
}

private fun workoutDetailsFor(workoutTitle: String, goal: String): List<String> {
    val normalizedGoal = normalizeTrainingGoal(goal)
    val preset = when (normalizedGoal) {
        "lose" -> "3-4 series de 12-15 repeticoes"
        "build" -> "4 series de 6-8 repeticoes com carga alta"
        else -> "3-4 series de 10 repeticoes com carga moderada"
    }
    return when (workoutTitle) {
        "HIIT + Corrida" -> listOf("Corrida intervalada: 10 tiros de 30s forte / 60s leve", "Agachamento livre: $preset", "Avanco alternado: $preset", "Prancha: 3 x 40s")
        "HIIT + Bike" -> listOf("Bike intervalada: 12 tiros de 40s forte / 20s leve", "Kettlebell swing: $preset", "Burpee controlado: 3 series de 12", "Abdominal infra: 3 series de 15")
        "Circuito metabolico" -> listOf("Agachamento com halter: $preset", "Remada curvada: $preset", "Flexao inclinada: $preset", "Corrida leve: 12 min")
        "Corrida progressiva" -> listOf("Corrida progressiva: 25 a 35 min", "Passada no step: $preset", "Prancha lateral: 3 x 30s por lado")
        "Escada + Core" -> listOf("Escada ou subida: 20 min em blocos", "Mountain climber: 4 x 20", "Prancha com toque no ombro: 3 x 16")
        "Full body dinamico", "HIIT + Core" -> listOf("Agachamento com press: $preset", "Remada com halter: $preset", "Swing com halter: $preset", "Prancha: 3 x 45s")
        "Peito + Triceps" -> listOf("Supino reto: $preset", "Supino inclinado: $preset", "Crucifixo: $preset", "Triceps corda: $preset")
        "Peito + Ombro" -> listOf("Supino reto: $preset", "Desenvolvimento militar: $preset", "Elevacao lateral: $preset", "Crucifixo maquina: $preset")
        "Costas + Biceps" -> listOf("Puxada frente: $preset", "Remada curvada: $preset", "Remada baixa: $preset", "Rosca direta: $preset")
        "Costas pesada" -> listOf("Barra guiada: $preset", "Remada cavalinho: $preset", "Pulldown neutro: $preset", "Rosca martelo: $preset")
        "Perna pesada" -> listOf("Agachamento livre: $preset", "Leg press: $preset", "Stiff: $preset", "Panturrilha em pe: 4 x 12")
        "Perna + Core", "Pernas + Ombro", "Pernas moderado" -> listOf("Agachamento: $preset", "Leg press: $preset", "Cadeira extensora: $preset", "Prancha: 3 x 40s")
        "Ombro + Bracos", "Upper body carga" -> listOf("Desenvolvimento com halteres: $preset", "Elevacao lateral: $preset", "Rosca direta: $preset", "Triceps frances: $preset")
        "Peito + Core", "Full body moderado", "Full body tecnico" -> listOf("Supino com halter: $preset", "Agachamento goblet: $preset", "Remada unilateral: $preset", "Prancha: 3 x 30s")
        "Cardio moderado" -> listOf("Eliptica ou corrida: 25-30 min em ritmo moderado", "Mobilidade de quadril: 3 x 12", "Abdominal dead bug: 3 x 12")
        else -> listOf("Caminhada leve: 25 min", "Mobilidade geral: 15 min", "Alongamentos ativos: 10 min")
    }
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
        title = { Text("Adicionar refeicao") },
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

