package com.example.fitnessapp.vm

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.data.RepoProvider
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class ReportViewModel : ViewModel() {
    private val repo = RepoProvider.repo

    fun generateReport(context: Context) {
        val userId = Session.userId.value ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = repo.observeUser(userId).first()
                val profile = repo.observeProfile(userId).first()
                val meals = repo.observeMeals(userId).first()
                val workouts = repo.observeWorkouts(userId).first()

                val file = File(context.cacheDir, "Relatorio_Fitness.pdf")
                val writer = PdfWriter(file)
                val pdf = PdfDocument(writer)
                val document = Document(pdf)

                val boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD)

                document.add(Paragraph("Relatório Fitness - ${user?.name ?: "Utilizador"}").setFontSize(20f).setFont(boldFont))
                document.add(Paragraph("Email: ${user?.email ?: "N/A"}"))
                
                if (profile != null) {
                    document.add(Paragraph("Informação do Perfil:").setFontSize(14f).setFont(boldFont))
                    document.add(Paragraph("Idade: ${profile.age} anos"))
                    document.add(Paragraph("Objetivo: ${profile.goal}"))
                    document.add(Paragraph("Altura: ${profile.heightCm} cm"))
                    document.add(Paragraph("Peso: ${profile.weightKg} kg"))
                }

                document.add(Paragraph("\n"))

                // Meals Table
                document.add(Paragraph("Lista de Refeições").setFontSize(16f).setFont(boldFont))
                val mealTable = Table(UnitValue.createPercentArray(floatArrayOf(3f, 2f, 2f)))
                mealTable.useAllAvailableWidth()
                mealTable.addHeaderCell("Título")
                mealTable.addHeaderCell("Calorias")
                mealTable.addHeaderCell("Hora")
                
                meals.forEach { meal ->
                    mealTable.addCell(meal.title)
                    mealTable.addCell("${meal.calories} kcal")
                    mealTable.addCell(meal.time)
                }
                document.add(mealTable)
                document.add(Paragraph("\n"))

                // Workouts Table
                document.add(Paragraph("Lista de Treinos").setFontSize(16f).setFont(boldFont))
                val workoutTable = Table(UnitValue.createPercentArray(floatArrayOf(3f, 2f, 2f, 3f)))
                workoutTable.useAllAvailableWidth()
                workoutTable.addHeaderCell("Título")
                workoutTable.addHeaderCell("Duração")
                workoutTable.addHeaderCell("Kcal")
                workoutTable.addHeaderCell("Data/Hora")

                workouts.forEach { w ->
                    workoutTable.addCell(w.title)
                    workoutTable.addCell("${w.durationMin} min")
                    workoutTable.addCell("${w.caloriesBurned} kcal")
                    workoutTable.addCell(w.dateTime)
                }
                document.add(workoutTable)

                document.close()

                openPdf(context, file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openPdf(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(Intent.createChooser(intent, "Abrir Relatório"))
    }
}
