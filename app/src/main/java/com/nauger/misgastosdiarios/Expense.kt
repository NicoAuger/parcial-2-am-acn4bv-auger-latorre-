package com.nauger.misgastosdiarios

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/* Modelo de gasto con categoría, monto y nota opcional. Parcelable para navegar entre pantallas. */
@Parcelize
data class Expense(
    val id: String = "",
    val category: String,
    val amount: Double,
    val note: String = ""
) : Parcelable
