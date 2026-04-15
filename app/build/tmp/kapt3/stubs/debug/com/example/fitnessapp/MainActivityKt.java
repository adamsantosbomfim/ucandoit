package com.example.fitnessapp;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000`\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\t\n\u0002\u0010\u0007\n\u0002\b\t\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0000\u001az\u0010\u0000\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032b\u0010\u0004\u001a^\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\t\u0012\u0013\u0012\u00110\n\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\u000b\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\f\u0012\u0015\u0012\u0013\u0018\u00010\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\r\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001ax\u0010\u000e\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032`\u0010\u0004\u001a\\\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\t\u0012\u0013\u0012\u00110\n\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\u000f\u0012\u0013\u0012\u00110\n\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\u000b\u0012\u0013\u0012\u00110\u0006\u00a2\u0006\f\b\u0007\u0012\b\b\b\u0012\u0004\b\b(\u0010\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001a\b\u0010\u0011\u001a\u00020\u0001H\u0003\u001a.\u0010\u0012\u001a\u00020\u00012$\u0010\u0013\u001a \u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u0014\u0012\u0004\u0012\u00020\u00010\u0005H\u0003\u001a\u0016\u0010\u0015\u001a\u00020\u00012\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a.\u0010\u0017\u001a\u00020\u00012\u0006\u0010\u0018\u001a\u00020\u00062\u0006\u0010\u0019\u001a\u00020\n2\u0006\u0010\u001a\u001a\u00020\n2\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a.\u0010\u001b\u001a\u00020\u00012\u0006\u0010\u001c\u001a\u00020\u00062\u0012\u0010\u001d\u001a\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u001e2\b\b\u0002\u0010\u001f\u001a\u00020 H\u0003\u001a\u0018\u0010!\u001a\u00020\u00012\u0006\u0010\"\u001a\u00020\u00062\u0006\u0010#\u001a\u00020\u0006H\u0003\u001a<\u0010$\u001a\u00020\u00012\u0006\u0010\u0018\u001a\u00020\u00062\u0006\u0010%\u001a\u00020\n2\u0006\u0010&\u001a\u00020\u00062\f\u0010\'\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a8\u0010(\u001a\u00020\u00012\u0006\u0010)\u001a\u00020*2\u0018\u0010+\u001a\u0014\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010,2\f\u0010-\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a@\u0010.\u001a\u00020\u00012\u0006\u0010/\u001a\u0002002\u0006\u0010\t\u001a\u00020\u00062\u0006\u00101\u001a\u00020\n2\u0006\u00102\u001a\u00020\n2\u0006\u00103\u001a\u00020\u00062\u0006\u00104\u001a\u00020\u00062\u0006\u00105\u001a\u00020\u0014H\u0003\u001a\u0016\u00106\u001a\u00020\u00012\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a4\u00107\u001a\u00020\u00012\u0006\u0010\u0018\u001a\u00020\u00062\u0006\u00108\u001a\u00020\u00062\f\u00109\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a>\u0010:\u001a\u00020\u00012\u0006\u0010)\u001a\u00020*2\u001e\u0010;\u001a\u001a\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010<2\f\u0010=\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a\u0016\u0010>\u001a\u00020\u00012\f\u0010?\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a\u0016\u0010@\u001a\u00020\u00012\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u001a\b\u0010A\u001a\u00020\u0001H\u0003\u001a\u001e\u0010B\u001a\u00020\u00012\u0006\u0010C\u001a\u00020D2\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u0003H\u0003\u00a8\u0006E"}, d2 = {"AddMealDialog", "", "onDismiss", "Lkotlin/Function0;", "onSave", "Lkotlin/Function4;", "", "Lkotlin/ParameterName;", "name", "title", "", "calories", "time", "note", "AddWorkoutDialog", "durationMin", "dateTime", "App", "CompleteProfileScreen", "onComplete", "", "DailyActivitiesScreen", "onOpenDrawer", "DashboardHeader", "userName", "weightKg", "kcal", "GoalDropdown", "selectedGoal", "onGoalSelected", "Lkotlin/Function1;", "modifier", "Landroidx/compose/ui/Modifier;", "HeaderPill", "label", "value", "HomeScreen", "userWeight", "userGoal", "onOpenDaily", "LoginScreen", "authState", "Lcom/example/fitnessapp/vm/AuthState;", "onLogin", "Lkotlin/Function2;", "onGoRegister", "MetricRowCard", "icon", "Landroidx/compose/ui/graphics/vector/ImageVector;", "currentValue", "goalValue", "unitPrefix", "unitSuffix", "progress", "NutritionScreen", "ProfileScreen", "userEmail", "onLogout", "RegisterScreen", "onRegister", "Lkotlin/Function3;", "onBack", "SplashScreen", "onTimeout", "TrainingScreen", "WeeklyProgressCard", "WorkoutDetailsDialog", "day", "Lcom/example/fitnessapp/SimpleRow;", "app_debug"})
@kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
public final class MainActivityKt {
    
    @androidx.compose.runtime.Composable()
    private static final void App() {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void SplashScreen(kotlin.jvm.functions.Function0<kotlin.Unit> onTimeout) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void GoalDropdown(java.lang.String selectedGoal, kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onGoalSelected, androidx.compose.ui.Modifier modifier) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void CompleteProfileScreen(kotlin.jvm.functions.Function4<? super java.lang.Integer, ? super java.lang.String, ? super java.lang.Integer, ? super java.lang.Float, kotlin.Unit> onComplete) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void LoginScreen(com.example.fitnessapp.vm.AuthState authState, kotlin.jvm.functions.Function2<? super java.lang.String, ? super java.lang.String, kotlin.Unit> onLogin, kotlin.jvm.functions.Function0<kotlin.Unit> onGoRegister) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void RegisterScreen(com.example.fitnessapp.vm.AuthState authState, kotlin.jvm.functions.Function3<? super java.lang.String, ? super java.lang.String, ? super java.lang.String, kotlin.Unit> onRegister, kotlin.jvm.functions.Function0<kotlin.Unit> onBack) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void HomeScreen(java.lang.String userName, int userWeight, java.lang.String userGoal, kotlin.jvm.functions.Function0<kotlin.Unit> onOpenDaily, kotlin.jvm.functions.Function0<kotlin.Unit> onOpenDrawer) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void DashboardHeader(java.lang.String userName, int weightKg, int kcal, kotlin.jvm.functions.Function0<kotlin.Unit> onOpenDrawer) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void HeaderPill(java.lang.String label, java.lang.String value) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void MetricRowCard(androidx.compose.ui.graphics.vector.ImageVector icon, java.lang.String title, int currentValue, int goalValue, java.lang.String unitPrefix, java.lang.String unitSuffix, float progress) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void WeeklyProgressCard() {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void DailyActivitiesScreen(kotlin.jvm.functions.Function0<kotlin.Unit> onOpenDrawer) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void NutritionScreen(kotlin.jvm.functions.Function0<kotlin.Unit> onOpenDrawer) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void TrainingScreen(kotlin.jvm.functions.Function0<kotlin.Unit> onOpenDrawer) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void WorkoutDetailsDialog(com.example.fitnessapp.SimpleRow day, kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void ProfileScreen(java.lang.String userName, java.lang.String userEmail, kotlin.jvm.functions.Function0<kotlin.Unit> onLogout, kotlin.jvm.functions.Function0<kotlin.Unit> onOpenDrawer) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void AddMealDialog(kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function4<? super java.lang.String, ? super java.lang.Integer, ? super java.lang.String, ? super java.lang.String, kotlin.Unit> onSave) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void AddWorkoutDialog(kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, kotlin.jvm.functions.Function4<? super java.lang.String, ? super java.lang.Integer, ? super java.lang.Integer, ? super java.lang.String, kotlin.Unit> onSave) {
    }
}