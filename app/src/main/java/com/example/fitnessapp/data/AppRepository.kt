package com.example.fitnessapp.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class AppRepository {

    private val db = FirebaseDatabase.getInstance().reference

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun parseMealSnapshot(mealSnap: DataSnapshot): MealEntity? {
        if (!mealSnap.hasChild("title") || !mealSnap.hasChild("calories") || !mealSnap.hasChild("time")) {
            Log.w("AppRepository", "Skipping malformed meal node at path: ${mealSnap.ref}")
            return null
        }

        return runCatching {
            mealSnap.getValue(MealEntity::class.java)?.also {
                it.id = mealSnap.key ?: ""
            }
        }.getOrElse { error ->
            Log.w("AppRepository", "Skipping unreadable meal node at path: ${mealSnap.ref}", error)
            null
        }
    }

    // -------- Auth (Manual Database only) --------
    suspend fun register(name: String, email: String, pass: String): String {
        Log.d("AppRepository", "Starting manual registration for $email")
        val usersRef = db.child("users")
        
        try {
            // Manual check for email
            val snapshot = usersRef.get().await()
            Log.d("AppRepository", "Fetched users snapshot")
            
            for (userSnap in snapshot.children) {
                val dbEmail = userSnap.child("email").value as? String
                if (dbEmail == email) {
                    throw Exception("Este email já está registado")
                }
            }

            val uid = usersRef.push().key ?: throw Exception("Failed to generate UID")
            Log.d("AppRepository", "Generated UID: $uid")
            
            val userMap = mapOf(
                "name" to name,
                "email" to email,
                "password" to pass
            )
            
            usersRef.child(uid).setValue(userMap).await()
            Log.d("AppRepository", "User data saved to database")
            
            return uid
        } catch (e: Exception) {
            Log.e("AppRepository", "Error during registration", e)
            throw e
        }
    }

    suspend fun login(email: String, pass: String): String? {
        Log.d("AppRepository", "Starting manual login for $email")
        try {
            val snapshot = db.child("users").get().await()
            
            for (userSnap in snapshot.children) {
                val dbEmail = userSnap.child("email").value as? String
                val dbPass = userSnap.child("password").value as? String
                if (dbEmail == email && dbPass == pass) {
                    Log.d("AppRepository", "Login successful for UID: ${userSnap.key}")
                    return userSnap.key
                }
            }
            Log.d("AppRepository", "Login failed: User not found or password mismatch")
            return null
        } catch (e: Exception) {
            Log.e("AppRepository", "Error during login", e)
            return null
        }
    }

    fun observeUser(uid: String): Flow<UserEntity?> = callbackFlow {
        val ref = db.child("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(UserEntity::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // -------- Profile --------
    fun observeProfile(uid: String): Flow<ProfileEntity?> = callbackFlow {
        val ref = db.child("profiles").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(ProfileEntity::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun upsertProfile(uid: String, profile: ProfileEntity) {
        db.child("profiles").child(uid).setValue(profile).await()
    }

    // -------- Hydration --------
    fun observeHydrationToday(uid: String): Flow<HydrationEntity?> = callbackFlow {
        val today = getTodayDate()
        val ref = db.child("hydration").child(uid).child(today)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(HydrationEntity::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun upsertHydrationToday(uid: String, hydration: HydrationEntity) {
        val today = getTodayDate()
        db.child("hydration").child(uid).child(today).setValue(hydration).await()
    }

    fun observeAllHydrationHistory(uid: String): Flow<Map<String, HydrationEntity>> = callbackFlow {
        val ref = db.child("hydration").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val history = snapshot.children.associate { daySnap ->
                    val key = daySnap.key ?: ""
                    key to (daySnap.getValue(HydrationEntity::class.java) ?: HydrationEntity())
                }
                trySend(history)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // -------- Meals (with History Support) --------
    fun observeMeals(uid: String): Flow<List<MealEntity>> = callbackFlow {
        val today = getTodayDate()
        val ref = db.child("meals").child(uid).child(today)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val meals = snapshot.children.mapNotNull(::parseMealSnapshot)
                trySend(meals.sortedByDescending { it.createdAtEpochMs })
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun addMeal(uid: String, title: String, calories: Int, time: String, note: String?) {
        val today = getTodayDate()
        val ref = db.child("meals").child(uid).child(today).push()
        val meal = MealEntity(id = ref.key!!, title = title, calories = calories, time = time, note = note)
        ref.setValue(meal).await()
    }

    suspend fun deleteMeal(uid: String, mealId: String) {
        val today = getTodayDate()
        db.child("meals").child(uid).child(today).child(mealId).removeValue().await()
    }

    /** Observes all meals for all days to generate history or reports */
    fun observeAllMealsHistory(uid: String): Flow<Map<String, List<MealEntity>>> = callbackFlow {
        val ref = db.child("meals").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val history = mutableMapOf<String, List<MealEntity>>()
                snapshot.children.forEach { dateSnap ->
                    val date = dateSnap.key ?: ""
                    val meals = dateSnap.children.mapNotNull(::parseMealSnapshot)
                    history[date] = meals
                }
                trySend(history)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // -------- Workouts --------
    fun observeWorkouts(uid: String): Flow<List<WorkoutEntity>> = callbackFlow {
        val ref = db.child("workouts").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val workouts = snapshot.children.mapNotNull {
                    val workout = it.getValue(WorkoutEntity::class.java)
                    workout?.id = it.key ?: ""
                    workout
                }
                trySend(workouts.sortedByDescending { it.createdAtEpochMs })
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun addWorkout(
        uid: String,
        title: String,
        duration: Int,
        kcal: Int,
        dateTime: String,
        createdAtEpochMs: Long = System.currentTimeMillis()
    ) {
        val ref = db.child("workouts").child(uid).push()
        val workout = WorkoutEntity(
            id = ref.key!!,
            title = title,
            durationMin = duration,
            caloriesBurned = kcal,
            dateTime = dateTime,
            createdAtEpochMs = createdAtEpochMs
        )
        ref.setValue(workout).await()
    }

    suspend fun deleteWorkout(uid: String, workoutId: String) {
        db.child("workouts").child(uid).child(workoutId).removeValue().await()
    }

    suspend fun updateWorkout(uid: String, workoutId: String, title: String, duration: Int, kcal: Int, dateTime: String) {
        val ref = db.child("workouts").child(uid).child(workoutId)
        val workout = WorkoutEntity(id = workoutId, title = title, durationMin = duration, caloriesBurned = kcal, dateTime = dateTime)
        ref.setValue(workout).await()
    }

    // -------- Body Metrics --------
    fun observeBodyMetrics(uid: String): Flow<List<BodyMetricsEntity>> = callbackFlow {
        val ref = db.child("body_metrics").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val metrics = snapshot.children.mapNotNull {
                    val item = it.getValue(BodyMetricsEntity::class.java)
                    item?.id = it.key ?: ""
                    item
                }
                trySend(metrics.sortedByDescending { it.createdAtEpochMs })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun addBodyMetrics(
        uid: String,
        weightKg: Float,
        waistCm: Float,
        chestCm: Float,
        armCm: Float,
        note: String,
        photoPath: String
    ) {
        val ref = db.child("body_metrics").child(uid).push()
        val metrics = BodyMetricsEntity(
            id = ref.key!!,
            weightKg = weightKg,
            waistCm = waistCm,
            chestCm = chestCm,
            armCm = armCm,
            note = note,
            photoPath = photoPath
        )
        ref.setValue(metrics).await()
    }

    suspend fun updateBodyMetrics(uid: String, entry: BodyMetricsEntity) {
        db.child("body_metrics").child(uid).child(entry.id).setValue(entry).await()
    }

    suspend fun deleteBodyMetrics(uid: String, entryId: String) {
        db.child("body_metrics").child(uid).child(entryId).removeValue().await()
    }
}
