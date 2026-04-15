package com.example.fitnessapp

import android.app.Application
import com.example.fitnessapp.data.AppRepository
import com.example.fitnessapp.data.RepoProvider
import com.example.fitnessapp.vm.Session
import com.google.firebase.database.FirebaseDatabase

class FitnessApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Session with context
        Session.init(this)
        HydrationReminderManager.ensureChannel(this)
        HydrationReminderManager.scheduleNextReminder(this)

        // Enable Firebase persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        RepoProvider.repo = AppRepository()
    }
}
