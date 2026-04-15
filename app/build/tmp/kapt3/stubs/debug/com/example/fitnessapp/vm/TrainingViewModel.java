package com.example.fitnessapp.vm;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J&\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00122\u0006\u0010\u0014\u001a\u00020\u0010R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\f\u00a8\u0006\u0015"}, d2 = {"Lcom/example/fitnessapp/vm/TrainingViewModel;", "Landroidx/lifecycle/ViewModel;", "()V", "repo", "Lcom/example/fitnessapp/data/AppRepository;", "userId", "", "workouts", "Lkotlinx/coroutines/flow/StateFlow;", "", "Lcom/example/fitnessapp/data/WorkoutEntity;", "getWorkouts", "()Lkotlinx/coroutines/flow/StateFlow;", "addWorkout", "", "title", "", "durationMin", "", "kcal", "dateTime", "app_debug"})
public final class TrainingViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.example.fitnessapp.data.AppRepository repo = null;
    private final long userId = 0L;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.fitnessapp.data.WorkoutEntity>> workouts = null;
    
    public TrainingViewModel() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.example.fitnessapp.data.WorkoutEntity>> getWorkouts() {
        return null;
    }
    
    public final void addWorkout(@org.jetbrains.annotations.NotNull()
    java.lang.String title, int durationMin, int kcal, @org.jetbrains.annotations.NotNull()
    java.lang.String dateTime) {
    }
}