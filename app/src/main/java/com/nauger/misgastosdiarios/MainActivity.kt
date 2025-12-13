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
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

// IMPORTS NUEVOS PARA HTTP
import okhttp3.OkHttpClient
import okhttp3.Request

/* Actividad principal. Administra el presupuesto, la carga de gastos, el listado y el gráfico por categoría. */
class MainActivity : AppCompatActivity() {

    /* Referencias a vistas principales del resumen y carga de datos. */
    private lateinit var db: FirebaseFirestore
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

    private lateinit var imgHeader: ImageView

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

    // NUEVO: cliente HTTP reutilizable y URL del tips.txt
    private val httpClient by lazy { OkHttpClient() }

    private companion object {
        private const val TIPS_URL =
            "https://raw.githubusercontent.com/NicoAuger/parcial-1-am-acn4bv-auger-latorre-/refs/heads/main/contenido/tips.txt"
    }

    /* Ciclo de vida: configura vistas, adaptadores, listeners y estado inicial. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance()

        // Obtener UID del usuario actual
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // Si tenemos un usuario, cargamos sus datos
        if (userId != null) {
            loadUserData(userId)
            loadExpensesData(userId)
        } else {
            // Opcional: Manejar el caso en que no haya usuario logueado
            // Podrías llevarlo al LoginActivity, por ejemplo.
            Log.w("MainActivity", "No se encontró UID de usuario.")
            Toast.makeText(this, "Error: Usuario no identificado.", Toast.LENGTH_LONG).show()
        }

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

        // NUEVO: carga de tip desde URL
        loadTipFromUrl()

        // Carga la imagen del encabezado
        val imageUrl = "https://raw.githubusercontent.com/NicoAuger/parcial-2-am-acn4b-auger-latorre/main/banner_mis_gastos.jpg"
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_cat_otros) // Opcional: se muestra mientras carga
            .error(R.drawable.ic_cat_otros)       // Opcional: se muestra si hay un error
            .into(imgHeader)

    }

    private fun loadUserData(uid: String) {
        val userDocRef = db.collection("usuarios").document(uid)

        userDocRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val presupuestoFirestore = documentSnapshot.getDouble("presupuestoMensual")

                    if (presupuestoFirestore != null && presupuestoFirestore > 0) {
                        // 1. ACTUALIZAR la variable de la clase
                        this.budget = presupuestoFirestore

                        // 2. MOSTRAR el valor en las vistas que ya inicializaste en bindViews()
                        etBudget.setText(this.budget.toString())

                        // 3. HABILITAR los inputs para que se puedan agregar gastos
                        setInputsEnabled(true)

                        // 4. ACTUALIZAR el resumen
                        updateSummary()

                        Log.d("FIRESTORE_SUCCESS", "Presupuesto cargado y aplicado: ${this.budget}")
                    } else {
                        Log.d("FIRESTORE_INFO", "No se encontró presupuesto o es cero.")
                    }

                } else {
                    Log.w("FIRESTORE_WARN", "El documento del usuario con UID $uid no existe.")
                    Toast.makeText(this, "No se encontró perfil de usuario.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FIRESTORE_ERROR", "Error al cargar datos del usuario", exception)
                Toast.makeText(this, "Error al cargar el presupuesto.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadExpensesData(uid: String) {
        // 1. Apuntamos a la colección "gastos" y filtramos por el UID del usuario.
        // Usamos orderBy para que los gastos se muestren en el orden en que se crearon.
        db.collection("gastos")
            .whereEqualTo("userId", uid)
            .orderBy("fecha", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // ¡Éxito al leer la colección!
                // Limpiamos la lista local para no duplicar datos si esta función se llama de nuevo.
                expenses.clear()

                // 2. Recorremos cada documento (gasto) que encontramos
                for (document in querySnapshot.documents) {
                    // 3. Convertimos el documento de Firestore a nuestro objeto "Expense"
                    val gasto = Expense(
                        id = document.id,
                        amount = document.getDouble("monto") ?: 0.0,
                        category = document.getString("categoria") ?: "Otros",
                        note = document.getString("nota") ?: ""
                    )
                    expenses.add(gasto)
                }

                // 4. Una vez cargados todos los gastos, recalculamos todo y actualizamos la UI
                recalcTotalsFromScratch()
                expensesAdapter.submitList(expenses.toList())
                updateSummary()
                updateCategoryChart()

                Log.d("FIRESTORE_READ_LIST", "${expenses.size} gastos cargados desde Firestore.")
            }
            .addOnFailureListener { exception ->
                // ¡Error al leer la colección!
                Log.e("FIRESTORE_READ_LIST", "Error al cargar los gastos", exception)
                toast("No se pudieron cargar los gastos anteriores.")
            }
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

        imgHeader = findViewById(R.id.imgHeader)
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
            onDelete = { expenseToDelete ->
                // Primero, verificamos si el gasto tiene un ID de Firestore.
                // Si el ID está en blanco, es un dato viejo o un error, así que no intentamos borrarlo de la nube.
                if (expenseToDelete.id.isBlank()) {
                    Log.w("FIRESTORE_DELETE", "Intento de borrar gasto sin ID de Firestore. Abortado.")
                    toast("No se puede borrar este gasto.")
                    return@ExpenseAdapter
                }

                // Si tiene ID, procedemos a borrarlo de Firestore.
                db.collection("gastos").document(expenseToDelete.id)
                    .delete()
                    .addOnSuccessListener {
                        // ¡Éxito al borrar en Firestore!
                        Log.d("FIRESTORE_DELETE", "Gasto ${expenseToDelete.id} borrado de Firestore.")
                        toast(getString(R.string.msg_deleted))

                        // SOLO SI SE BORRÓ DE LA NUBE, lo quitamos de la lista local y actualizamos la UI.
                        expenses.remove(expenseToDelete)
                        recalcTotalsFromScratch()
                        expensesAdapter.submitList(expenses.toList())
                        updateSummary()
                        updateCategoryChart()
                    }
                    .addOnFailureListener { e ->
                        // ¡Error al borrar!
                        Log.e("FIRESTORE_DELETE", "Error al borrar gasto ${expenseToDelete.id}", e)
                        toast("Error al borrar el gasto. Inténtalo de nuevo.")
                    }
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

            // 1. Asegurarnos de que tenemos el UID para asociar el gasto
            val userId = auth.currentUser?.uid
            if (userId == null) {
                toast("Error: No se pudo identificar al usuario para guardar el gasto.")
                return@setOnClickListener
            }

            // 2. Creamos un mapa de datos para Firestore
            val gastoParaGuardar = hashMapOf(
                "userId" to userId,
                "monto" to amount,
                "categoria" to category,
                "nota" to note,
                "fecha" to com.google.firebase.firestore.FieldValue.serverTimestamp() // Usamos la fecha del servidor
            )

            // 3. Guardamos en la colección "gastos"
            db.collection("gastos")
                .add(gastoParaGuardar)
                .addOnSuccessListener { documentReference ->
                    // ¡Éxito al guardar en Firestore!
                    Log.d("FIRESTORE_WRITE", "Gasto guardado con ID: ${documentReference.id}")
                    toast("Gasto agregado con éxito")

                    // Ahora sí, actualizamos la UI localmente como antes
                    // PERO ya no crearemos un "Expense" nuevo, sino que el que se muestre
                    // debería venir de Firestore. Por ahora, lo dejamos así para probar la escritura.
                    val expense = Expense(id = documentReference.id, category = category, amount = amount, note = note)
                    expenses.add(expense)
                    recalcTotalsFromScratch() // Usamos esto para recalcular todo
                    expensesAdapter.submitList(expenses.toList())
                    updateSummary()
                    updateCategoryChart()

                    etAmount.text?.clear()
                    etNote.text?.clear()
                    etAmount.clearFocus()
                }
                .addOnFailureListener { e ->
                    // ¡Error al guardar en Firestore!
                    Log.e("FIRESTORE_WRITE", "Error al guardar el gasto", e)
                    toast("Error al guardar el gasto. Intenta de nuevo.")
                }

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
        popup.menu.add(1, 4, 3, getString(R.string.menu_logout))
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                1 -> { clearAll(); true }
                2 -> { recalcTotalsFromScratch(); updateSummary(); updateCategoryChart(); true }
                3 -> {
                    startActivity(Intent(this, MonthlySummaryActivity::class.java))
                    true
                }
                4 -> {
                    signOut()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun signOut() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
    private fun initDayRollingSummary() {
        val today = dayFormat.format(Date())
        val lastDay = prefs.getString("last_day", null)

        if (lastDay == null) {
            prefs.edit().putString("last_day", today).apply()
        } else if (lastDay != today) {
            saveDayTotal(lastDay)
            clearAll(withToast = false)
            prefs.edit().putString("last_day", today).apply()
        }
    }

    // Guarda el total gastado de un día en SharedPreferences
    private fun saveDayTotal(day: String) {
        val spentKey = "day_total_$day"
        val budgetKey = "day_budget_$day"
        prefs.edit()
            .putFloat(spentKey, spent.toFloat())
            .putFloat(budgetKey, budget.toFloat())
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

        // Guarda los valores actuales para que MonthlySummaryActivity pueda leerlos.
        prefs.edit()
            .putFloat("current_budget", budget.toFloat())
            .putFloat("current_spent", spent.toFloat())
            .apply()
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

    // NUEVO: descarga del tip desde URL usando OkHttp
    private fun loadTipFromUrl() {
        val tvTip = findViewById<TextView>(R.id.tvTip)
        tvTip.text = "Cargando tip..."

        Thread {
            try {
                val request = Request.Builder()
                    .url(TIPS_URL)
                    .build()

                val response = httpClient.newCall(request).execute()
                val text = response.body?.string()?.trim()

                runOnUiThread {
                    if (!text.isNullOrEmpty()) {
                        val lines = text.split("\n")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        if (lines.isNotEmpty()) {
                            val randomTip = lines.random()
                            tvTip.text = randomTip
                        } else {
                            tvTip.text = "No se pudo cargar el tip."
                        }
                    } else {
                        tvTip.text = "No se pudo cargar el tip."
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    tvTip.text = "No se pudo cargar el tip."
                }
            }
        }.start()
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