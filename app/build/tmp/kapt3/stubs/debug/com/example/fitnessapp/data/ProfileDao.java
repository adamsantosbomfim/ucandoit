package com.example.fitnessapp.data;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\bg\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u0004\u0018\u00010\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0018\u0010\u0007\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00030\b2\u0006\u0010\u0004\u001a\u00020\u0005H\'J\u0016\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\f\u00a8\u0006\r"}, d2 = {"Lcom/example/fitnessapp/data/ProfileDao;", "", "get", "Lcom/example/fitnessapp/data/ProfileEntity;", "userId", "", "(JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observe", "Lkotlinx/coroutines/flow/Flow;", "upsert", "", "profile", "(Lcom/example/fitnessapp/data/ProfileEntity;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
@androidx.room.Dao()
public abstract interface ProfileDao {
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object upsert(@org.jetbrains.annotations.NotNull()
    com.example.fitnessapp.data.ProfileEntity profile, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<com.example.fitnessapp.data.ProfileEntity> observe(long userId);
    
    @androidx.room.Query(value = "SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object get(long userId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.example.fitnessapp.data.ProfileEntity> $completion);
}