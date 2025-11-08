package com.nauger.misgastosdiarios

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

/* Muestra la información detallada de un gasto.
   Luego servirá también para editar o eliminar (T6). */
class ExpenseDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_EXPENSE = "extra_expense"

        fun newIntent(context: Context, expense: Expense): Intent {
            return Intent(context, ExpenseDetailActivity::class.java).apply {
                putExtra(EXTRA_EXPENSE, expense)
            }
        }
    }

    private val ars: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val expense = intent.getParcelableExtra<Expense>(EXTRA_EXPENSE)
            ?: run { finish(); return }

        val img = findViewById<ImageView>(R.id.imgCategory)
        val tvCategory = findViewById<TextView>(R.id.tvCategory)
        val tvAmount = findViewById<TextView>(R.id.tvAmount)
        val tvNote = findViewById<TextView>(R.id.tvNote)
        val btnEdit = findViewById<MaterialButton>(R.id.btnEdit)
        val btnDelete = findViewById<MaterialButton>(R.id.btnDelete)

        img.setImageResource(iconForCategory(expense.category))
        tvCategory.text = expense.category
        tvAmount.text = ars.format(expense.amount)
        tvNote.text = if (expense.note.isBlank()) getString(R.string.sin_nota) else expense.note

        btnEdit.setOnClickListener {
            // TODO: implementaremos edición en T6
        }

        btnDelete.setOnClickListener {
            // TODO: implementaremos eliminación con resultado en T6
            finish()
        }
    }

    private fun iconForCategory(category: String): Int {
        return when (category.trim().lowercase()) {
            "comida" -> R.drawable.ic_cat_comida
            "transporte" -> R.drawable.ic_cat_transporte
            "hogar" -> R.drawable.ic_cat_hogar
            "ocio" -> R.drawable.ic_cat_ocio
            "salud" -> R.drawable.ic_cat_salud
            "educación", "educacion" -> R.drawable.ic_cat_educacion
            else -> R.drawable.ic_cat_otros
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
