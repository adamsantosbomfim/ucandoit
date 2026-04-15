package com.example.fitnessapp.vm;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J(\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00052\u0006\u0010\u0015\u001a\u00020\u00132\b\u0010\u0016\u001a\u0004\u0018\u00010\u0013J\u000e\u0010\u0017\u001a\u00020\u00112\u0006\u0010\u0018\u001a\u00020\nR\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007R\u001d\u0010\b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\u0007R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0019"}, d2 = {"Lcom/example/fitnessapp/vm/NutritionViewModel;", "Landroidx/lifecycle/ViewModel;", "()V", "caloriesToday", "Lkotlinx/coroutines/flow/StateFlow;", "", "getCaloriesToday", "()Lkotlinx/coroutines/flow/StateFlow;", "meals", "", "Lcom/example/fitnessapp/data/MealEntity;", "getMeals", "repo", "Lcom/example/fitnessapp/data/AppRepository;", "userId", "", "addMeal", "", "title", "", "calories", "time", "note", "deleteMeal", "meal", "app_debug"})
public final class NutritionViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.example.fitnessapp.data.AppRepository repo = null;
    private final long userId = 0L;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.fitnessapp.data.MealEntity>> meals = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> caloriesToday = null;
    
    public NutritionViewModel() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.fitnessapp.data.MealEntity>> getMeals() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Integer> getCaloriesToday() {
        return null;
    }
    
    public final void addMeal(@org.jetbrains.annotations.NotNull()
    java.lang.String title, int calories, @org.jetbrains.annotations.NotNull()
    java.lang.String time, @org.jetbrains.annotations.Nullable()
    java.lang.String note) {
    }
    
    public final void deleteMeal(@org.jetbrains.annotations.NotNull()
    com.example.fitnessapp.data.MealEntity meal) {
    }
}