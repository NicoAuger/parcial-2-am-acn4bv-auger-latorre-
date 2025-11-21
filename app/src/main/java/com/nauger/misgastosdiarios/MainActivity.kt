package com.nauger.misgastosdiarios

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.LinearLayout.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

/* Actividad principal. Administra el presupuesto, la carga de gastos, el listado y el gráfico por categoría. */
class MainActivity : AppCompatActivity() {

    /* Referencias a vistas principales del resumen y carga de datos. */
    private lateinit var auth: FirebaseAuth
    private lateinit var etBudget: EditText
    private lateinit var btnSetBudget: Button
    private lateinit var tvBudget: TextView
    private lateinit var tvSpent: TextView
    private lateinit var tvRemaining: TextView

    private lateinit var etAmount: EditText
    private lateinit var spCategory: Spinner
    private lateinit var etNote: EditText
    private lateinit var btnAdd: Button

    /* Listado de gastos usando RecyclerView para mejor rendimiento. */
    private lateinit var recyclerExpenses: RecyclerView
    private lateinit var expensesAdapter: ExpenseAdapter

    /* Contenedor de un gráfico simple por categoría (barras horizontales). */
    private lateinit var chartContainer: LinearLayout

    /* Botón flotante para acciones rápidas. */
    private lateinit var fabMenu: com.google.android.material.floatingactionbutton.FloatingActionButton

    /* Estado de resumen: presupuesto, gastado y color por defecto para restante. */
    private var budget = 0.0
    private var spent = 0.0
    private var remainingDefaultColor: Int = 0

    // Persistencia de resumen diario / mensual
    private val prefs by lazy { getSharedPreferences("gastos_prefs", Context.MODE_PRIVATE) }
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())


    /* Fuente de datos en memoria para los gastos (alcance académico). */
    private val expenses = mutableListOf<Expense>()

    /* Acumulados por categoría para el gráfico. */
    private val categoryTotals = linkedMapOf<String, Double>()

    /* Colores asociados a cada categoría (referencias del sistema). */
    private val categoryColors by lazy {
        mapOf(
            "Comida" to android.R.color.holo_orange_dark,
            "Transporte" to android.R.color.holo_blue_dark,
            "Ocio" to android.R.color.holo_purple,
            "Salud" to android.R.color.holo_red_dark,
            "Educación" to android.R.color.holo_blue_bright,
            "Otros" to android.R.color.darker_gray
        )
    }

    /* Formateadores localizados para ARS y porcentajes. */
    private val ars: NumberFormat by lazy { NumberFormat.getCurrencyInstance(Locale("es", "AR")) }
    private val pct: NumberFormat by lazy {
        NumberFormat.getNumberInstance(Locale("es", "AR")).apply { maximumFractionDigits = 1 }
    }

    /* Ciclo de vida: configura vistas, adaptadores, listeners y estado inicial. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        bindViews()
        val tvCurrentDate: TextView = findViewById(R.id.tvCurrentDate)
        val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        tvCurrentDate.text = dateFormat.format(Date())
        setupSpinner()
        initRecycler()
        initCategoryTotals()
        // Revisa si cambió el día y guarda el total del día anterior
        initDayRollingSummary()
        setupListeners()

        /* Al inicio, se bloquea la carga de gastos hasta fijar presupuesto. */
        setInputsEnabled(false)

        updateSummary()
        updateCategoryChart()
    }

    /* Vincula componentes de la interfaz con sus IDs. */
    private fun bindViews() {
        etBudget = findViewById(R.id.etBudget)
        btnSetBudget = findViewById(R.id.btnSetBudget)
        tvBudget = findViewById(R.id.tvBudget)
        tvSpent = findViewById(R.id.tvSpent)
        tvRemaining = findViewById(R.id.tvRemaining)
        remainingDefaultColor = tvRemaining.currentTextColor

        etAmount = findViewById(R.id.etAmount)
        spCategory = findViewById(R.id.spCategory)
        etNote = findViewById(R.id.etNote)
        btnAdd = findViewById(R.id.btnAdd)

        recyclerExpenses = findViewById(R.id.recyclerExpenses)
        chartContainer = findViewById(R.id.chartContainer)
        fabMenu = findViewById(R.id.fabMenu)
    }

    /* Configura el Spinner de categorías desde recursos. */
    private fun setupSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.categories,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spCategory.adapter = adapter
        }
    }

    /* Inicializa el RecyclerView y define el comportamiento de eliminación. */
    private fun initRecycler() {
        recyclerExpenses.layoutManager = LinearLayoutManager(this)
        expensesAdapter = ExpenseAdapter(
            onDelete = { expense ->
                expenses.remove(expense)
                recalcTotalsFromScratch()
                expensesAdapter.submitList(expenses.toList())
                updateSummary()
                updateCategoryChart()
                toast(getString(R.string.msg_deleted))
            },
            onClick = { expense ->
                val intent = ExpenseDetailActivity.newIntent(this, expense)
                detailLauncher.launch(intent)
            }
        )
        recyclerExpenses.adapter = expensesAdapter
        expensesAdapter.submitList(expenses.toList())
    }

    /* Registra listeners para fijar presupuesto, agregar gastos y mostrar menú del FAB. */
    private fun setupListeners() {
        btnSetBudget.setOnClickListener {
            val value = parseAmount(etBudget.text?.toString().orEmpty())
            if (value == null || value <= 0.0) {
                etBudget.error = getString(R.string.err_budget_invalid)
                toast(getString(R.string.err_budget_invalid))
                return@setOnClickListener
            }
            budget = value
            setInputsEnabled(true)
            updateSummary()
            updateCategoryChart()
            hideKeyboard()
            toast(getString(R.string.msg_budget_set))
        }

        btnAdd.setOnClickListener {
            /* Impide cargas si no hay presupuesto válido. */
            if (budget <= 0.0) {
                etBudget.error = getString(R.string.err_budget_invalid)
                toast(getString(R.string.err_budget_invalid))
                etBudget.requestFocus()
                return@setOnClickListener
            }

            val amount = parseAmount(etAmount.text?.toString().orEmpty())
            if (amount == null || amount <= 0.0) {
                etAmount.error = getString(R.string.err_amount_invalid)
                toast(getString(R.string.err_amount_invalid))
                return@setOnClickListener
            }

            val category = spCategory.selectedItem?.toString().orEmpty().ifBlank { "Otros" }
            val note = etNote.text?.toString().orEmpty().trim()

            /* Inserta el gasto y actualiza acumulados y UI. */
            val expense = Expense(category = category, amount = amount, note = note)
            expenses.add(expense)
            spent += amount
            categoryTotals[category] = (categoryTotals[category] ?: 0.0) + amount
            expensesAdapter.submitList(expenses.toList())
            updateSummary()
            updateCategoryChart()

            /* Limpia campos y mejora la UX post-carga. */
            etAmount.text?.clear()
            etNote.text?.clear()
            etAmount.clearFocus()
        }

        fabMenu.setOnClickListener { showFabMenu(it) }
    }

    /* Habilita o deshabilita inputs de carga de gastos según presupuesto. */
    private fun setInputsEnabled(enabled: Boolean) {
        etAmount.isEnabled = enabled
        spCategory.isEnabled = enabled
        etNote.isEnabled = enabled
        btnAdd.isEnabled = enabled
        etBudget.isEnabled = true
    }

    /* Muestra menú flotante con acciones de limpieza, recálculo y resumen mensual. */
    private fun showFabMenu(anchor: android.view.View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, getString(R.string.menu_clear_expenses))
        popup.menu.add(0, 2, 1, getString(R.string.menu_recalc_summary))
        popup.menu.add(0, 3, 2, getString(R.string.menu_monthly_summary))
        popup.menu.add(1, 4, 3, getString(R.string.menu_logout)) // MODIFICADO PARA USAR STRINGS.XML
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                1 -> { clearAll(); true }
                2 -> { recalcTotalsFromScratch(); updateSummary(); updateCategoryChart(); true }
                3 -> {
                    startActivity(Intent(this, MonthlySummaryActivity::class.java))
                    true
                }
                4 -> {
                    signOut() // Llamamos a la función de cierre de sesión
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun signOut() {
        // Cierra la sesión del usuario en Firebase
        auth.signOut()

        // Crea un Intent para volver a LoginActivity
        val intent = Intent(this, LoginActivity::class.java)

        // Limpia la pila de actividades para que el usuario no pueda
        // volver a MainActivity con el botón de "atrás"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        // Inicia la actividad de Login y cierra la actual
        startActivity(intent)
        finish()
    }


    /* Elimina todos los gastos y reinicia acumulados y vistas. */
    private fun clearAll(withToast: Boolean = true) {
        expenses.clear()
        spent = 0.0
        categoryTotals.keys.toList().forEach { categoryTotals[it] = 0.0 }
        expensesAdapter.submitList(expenses.toList())
        updateSummary()
        updateCategoryChart()
        if (withToast) {
            toast(getString(R.string.msg_expenses_cleared))
        }
    }


    // Revisa día actual vs último día guardado.
    // Si cambió, guarda el total del día anterior y limpia datos para el nuevo día.
    private fun initDayRollingSummary() {
        val today = dayFormat.format(Date())
        val lastDay = prefs.getString("last_day", null)

        if (lastDay == null) {
            // Primera vez que se abre la app
            prefs.edit().putString("last_day", today).apply()
        } else if (lastDay != today) {
            // Cambió el día: guardamos total del día anterior
            saveDayTotal(lastDay)

            // Limpiamos los gastos en memoria pero SIN mostrar el toast
            clearAll(withToast = false)

            // Actualizamos el día actual
            prefs.edit().putString("last_day", today).apply()
        }
    }

    // Guarda el total gastado de un día en SharedPreferences
    private fun saveDayTotal(day: String) {
        val spentKey = "day_total_$day"
        val budgetKey = "day_budget_$day" // Nueva clave para el presupuesto
        prefs.edit()
            .putFloat(spentKey, spent.toFloat())
            .putFloat(budgetKey, budget.toFloat()) // Guardamos el presupuesto del día
            .apply()
    }




    /* Inicializa el mapa de categorías con valor 0. */
    private fun initCategoryTotals() {
        resources.getStringArray(R.array.categories).forEach { cat ->
            categoryTotals.putIfAbsent(cat, 0.0)
        }
    }

    /* Recalcula todos los totales a partir de la lista actual de gastos. */
    private fun recalcTotalsFromScratch() {
        spent = 0.0
        categoryTotals.keys.toList().forEach { categoryTotals[it] = 0.0 }
        expenses.forEach { e ->
            spent += e.amount
            categoryTotals[e.category] = (categoryTotals[e.category] ?: 0.0) + e.amount
        }
    }

    /* Actualiza las etiquetas de Presupuesto, Gastado y Restante con formato. */
    private fun updateSummary() {
        tvBudget.text = getString(R.string.lbl_budget, ars.format(budget))
        tvSpent.text = getString(R.string.lbl_spent, ars.format(spent))

        val remaining = budget - spent
        tvRemaining.text = getString(R.string.lbl_remaining, ars.format(remaining))

        if (remaining < 0) {
            tvRemaining.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else {
            tvRemaining.setTextColor(remainingDefaultColor)
        }

        // --- INICIO DE LA MODIFICACIÓN ---
        // Guarda los valores actuales para que MonthlySummaryActivity pueda leerlos.
        prefs.edit()
            .putFloat("current_budget", budget.toFloat())
            .putFloat("current_spent", spent.toFloat())
            .apply()
        // --- FIN DE LA MODIFICACIÓN ---
    }

    /* Redibuja el gráfico por categoría en base a los acumulados y presupuesto. */
    private fun updateCategoryChart() {
        chartContainer.removeAllViews()

        if (budget <= 0.0) {
            val hint = TextView(this).apply {
                text = getString(R.string.hint_chart_needs_budget)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setTextColor(tvBudget.currentTextColor)
            }
            chartContainer.addView(hint)
            return
        }

        var any = false
        categoryTotals.forEach { (category, total) ->
            if (total <= 0) return@forEach
            any = true

            val percent = (total / budget * 100.0)

            /* Etiqueta con categoría, monto y porcentaje del presupuesto. */
            val label = TextView(this).apply {
                text = getString(
                    R.string.fmt_chart_line,
                    category,
                    ars.format(total),
                    "${pct.format(percent)}%"
                )
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(8), dp(6), dp(8), dp(2))
            }
            chartContainer.addView(label)

            /* Barra horizontal que representa el porcentaje (roja si excede 100%). */
            val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = percent.toInt().coerceIn(0, 100)
                val colorRes = if (percent > 100.0)
                    android.R.color.holo_red_dark
                else
                    (categoryColors[category] ?: android.R.color.darker_gray)
                progressTintList = ContextCompat.getColorStateList(this@MainActivity, colorRes)
            }
            chartContainer.addView(
                bar,
                LayoutParams(LayoutParams.MATCH_PARENT, dp(6)).apply {
                    leftMargin = dp(8); rightMargin = dp(8); bottomMargin = dp(8)
                }
            )
        }

        if (!any) {
            val empty = TextView(this).apply {
                text = getString(R.string.hint_chart_no_expenses)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setTextColor(tvBudget.currentTextColor)
            }
            chartContainer.addView(empty)
        }
    }
    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode != RESULT_OK) return@registerForActivityResult
        val data = res.data ?: return@registerForActivityResult

        val action = data.getStringExtra(ExpenseDetailActivity.RES_ACTION)
        val original = data.getParcelableExtra<Expense>(ExpenseDetailActivity.RES_ORIGINAL)
        when (action) {
            ExpenseDetailActivity.ACTION_DELETE -> {
                if (original != null) {
                    expenses.remove(original)
                    recalcTotalsFromScratch()
                    expensesAdapter.submitList(expenses.toList())
                    updateSummary()
                    updateCategoryChart()
                    toast(getString(R.string.msg_deleted))
                }
            }
            ExpenseDetailActivity.ACTION_EDIT -> {
                val updated = data.getParcelableExtra<Expense>(ExpenseDetailActivity.RES_UPDATED)
                if (original != null && updated != null) {
                    val idx = expenses.indexOf(original)
                    if (idx >= 0) {
                        expenses[idx] = updated
                        recalcTotalsFromScratch()
                        expensesAdapter.submitList(expenses.toList())
                        updateSummary()
                        updateCategoryChart()
                        toast(getString(R.string.msg_saved))
                    }
                }
            }
        }
    }

    /* Helpers de parsing y UX. */
    private fun parseAmount(raw: String): Double? =
        raw.replace(",", ".").trim().toDoubleOrNull()

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        currentFocus?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
