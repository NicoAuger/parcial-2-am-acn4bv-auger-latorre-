package com.nauger.misgastosdiarios

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonthlySummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_summary)

        setupToolbar()

        val recyclerView: RecyclerView = findViewById(R.id.recycler_daily_summaries)
        val summaries = loadSummaries()
        recyclerView.adapter = DaySummaryAdapter(summaries)
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar_monthly_summary)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun loadSummaries(): List<DaySummary> {
        val prefs = getSharedPreferences("gasto_prefs", Context.MODE_PRIVATE)
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dayFormat.format(Date())

        val summaryList = mutableListOf<DaySummary>()

        // 1. Obtener datos del día ACTIVO (que están en memoria)
        val currentBudget = prefs.getFloat("current_budget", 0f).toDouble()
        val currentSpent = prefs.getFloat("current_spent", 0f).toDouble()
        summaryList.add(
            DaySummary(
                date = todayStr,
                budget = currentBudget,
                spent = currentSpent,
                balance = currentBudget - currentSpent,
                isActive = true
            )
        )

        // 2. Leer todos los días CERRADOS de SharedPreferences
        val allKeys = prefs.all.keys
        val dateKeys = allKeys
            .filter { it.startsWith("day_total_") }
            .map { it.removePrefix("day_total_") }
            .filter { it != todayStr } // Excluimos el día de hoy, que ya es "Activo"

        for (date in dateKeys) {
            val spent = prefs.getFloat("day_total_$date", 0f).toDouble()
            // Leemos el presupuesto guardado para ESE día específico
            val budget = prefs.getFloat("day_budget_$date", 0f).toDouble()

            if (budget > 0 || spent > 0) { // Solo añadir si hay datos relevantes
                summaryList.add(
                    DaySummary(
                        date = date,
                        budget = budget,
                        spent = spent,
                        balance = budget - spent,
                        isActive = false
                    )
                )
            }
        }

        // 3. Ordenar la lista por fecha, descendente
        return summaryList.sortedByDescending { it.date }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}