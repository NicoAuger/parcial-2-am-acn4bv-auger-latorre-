package com.nauger.misgastosdiarios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

/*
   Adaptador que gestiona la lista de gastos en el RecyclerView.
   Usa ListAdapter y DiffUtil para optimizar actualizaciones de la lista.
   Recibe callbacks para eliminar y para click en el ítem (navegación a detalle).
*/
class ExpenseAdapter(
    private val onDelete: (Expense) -> Unit,
    private val onClick: (Expense) -> Unit = {}   // <-- nuevo callback con valor por defecto
) : ListAdapter<Expense, ExpenseAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Expense>() {
            override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
                // Si luego agregan un ID único, cámbienlo por oldItem.id == newItem.id
                return oldItem == newItem
            }
            override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
                return oldItem == newItem
            }
        }
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return VH(v, onDelete, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        itemView: View,
        private val onDelete: (Expense) -> Unit,
        private val onClick: (Expense) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val imgCategory: ImageView = itemView.findViewById(R.id.imgCategory)
        private val tvLeft: TextView = itemView.findViewById(R.id.tvLeft)
        private val tvRight: TextView = itemView.findViewById(R.id.tvRight)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        private val ars: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

        private fun iconForCategory(category: String): Int = when (category.trim().lowercase()) {
            "comida" -> R.drawable.ic_cat_comida
            "transporte" -> R.drawable.ic_cat_transporte
            "hogar" -> R.drawable.ic_cat_hogar
            "ocio" -> R.drawable.ic_cat_ocio
            "salud" -> R.drawable.ic_cat_salud
            "educación", "educacion" -> R.drawable.ic_cat_educacion
            else -> R.drawable.ic_cat_otros
        }

        fun bind(e: Expense) {
            tvLeft.text = if (e.note.isBlank()) e.category else "${e.category} • ${e.note}"
            tvRight.text = ars.format(e.amount)
            imgCategory.setImageResource(iconForCategory(e.category))

            if (btnDelete.contentDescription == null) {
                btnDelete.contentDescription = itemView.context.getString(R.string.cd_delete_expense)
            }

            btnDelete.setOnClickListener { onDelete(e) }
            itemView.setOnClickListener { onClick(e) }   // <-- click para navegar a detalle
        }
    }
}
