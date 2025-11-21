package com.nauger.misgastosdiarios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class DaySummaryAdapter(private val summaries: List<DaySummary>) :
    RecyclerView.Adapter<DaySummaryAdapter.ViewHolder>() {

    private val ars: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.tv_date)
        val activeStatus: TextView = view.findViewById(R.id.tv_active_status)
        val budget: TextView = view.findViewById(R.id.tv_budget_summary)
        val spent: TextView = view.findViewById(R.id.tv_spent_summary)
        val balance: TextView = view.findViewById(R.id.tv_balance_summary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_summary, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val summary = summaries[position]
        holder.date.text = summary.date
        holder.budget.text = "Presupuesto: ${ars.format(summary.budget)}"
        holder.spent.text = "Gastado: ${ars.format(summary.spent)}"
        holder.balance.text = "Saldo: ${ars.format(summary.balance)}"

        holder.activeStatus.visibility = if (summary.isActive) View.VISIBLE else View.GONE

        // Opcional: Cambiar color del saldo si es negativo
        val context = holder.itemView.context
        if (summary.balance < 0) {
            holder.balance.setTextColor(context.getColor(android.R.color.holo_red_dark))
        } else {
            // VERSIÓN CORREGIDA Y MÁS SEGURA
            holder.balance.setTextColor(context.getColor(android.R.color.tertiary_text_light))
        }

    }

    override fun getItemCount() = summaries.size
}
