package com.nauger.misgastosdiarios

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.NumberFormat
import java.util.Locale

/* Muestra la información detallada de un gasto y permite editar/eliminar (T6). */
class ExpenseDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_EXPENSE = "extra_expense"

        // Extras de resultado
        const val RES_ACTION = "action"
        const val RES_ORIGINAL = "original"
        const val RES_UPDATED = "updated"

        const val ACTION_DELETE = "delete"
        const val ACTION_EDIT = "edit"

        fun newIntent(context: Context, expense: Expense): Intent {
            return Intent(context, ExpenseDetailActivity::class.java).apply {
                putExtra(EXTRA_EXPENSE, expense)
            }
        }
    }

    private val ars: NumberFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    private lateinit var expense: Expense

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_detail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val e = intent.getParcelableExtra<Expense>(EXTRA_EXPENSE)
        if (e == null) {
            finish(); return
        } else {
            expense = e
        }

        val img = findViewById<ImageView>(R.id.imgCategory)
        val tvCategory = findViewById<TextView>(R.id.tvCategory)
        val tvAmount = findViewById<TextView>(R.id.tvAmount)
        val tvNote = findViewById<TextView>(R.id.tvNote)
        val btnEdit = findViewById<MaterialButton>(R.id.btnEdit)
        val btnDelete = findViewById<MaterialButton>(R.id.btnDelete)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)

        img.setImageResource(iconForCategory(expense.category))
        tvCategory.text = expense.category
        tvAmount.text = ars.format(expense.amount)
        tvNote.text = expense.note.ifBlank { getString(R.string.sin_nota) }

        btnEdit.setOnClickListener { showEditDialog() }
        btnDelete.setOnClickListener {
            // Devolvemos acción de borrado + el gasto original
            setResult(RESULT_OK, Intent().apply {
                putExtra(RES_ACTION, ACTION_DELETE)
                putExtra(RES_ORIGINAL, expense)
            })
            finish()
        }
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showEditDialog() {
        // UI simple programática: 2 campos (monto y nota)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(4))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val etAmount = EditText(this).apply {
            hint = getString(R.string.amount_hint)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(expense.amount.toString())
        }
        val etNote = EditText(this).apply {
            hint = getString(R.string.note_hint)
            setText(expense.note)
        }

        container.addView(etAmount, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        container.addView(etNote, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.edit_expense))
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val newAmount = etAmount.text?.toString()?.replace(",", ".")?.toDoubleOrNull()
                if (newAmount == null || newAmount <= 0.0) {
                    // Si es inválido, no devolvemos nada; podrías mostrar un Toast si querés.
                    return@setPositiveButton
                }
                val newNote = etNote.text?.toString()?.trim().orEmpty()

                val updated = expense.copy(amount = newAmount, note = newNote)
                // Devolvemos acción de edición + original + actualizado
                setResult(RESULT_OK, Intent().apply {
                    putExtra(RES_ACTION, ACTION_EDIT)
                    putExtra(RES_ORIGINAL, expense)
                    putExtra(RES_UPDATED, updated)
                })
                finish()
            }
            .show()
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

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
