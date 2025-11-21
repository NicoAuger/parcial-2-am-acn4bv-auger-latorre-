package com.nauger.misgastosdiarios

data class DaySummary(
    val date: String,
    val budget: Double,
    val spent: Double,
    val balance: Double,
    val isActive: Boolean // Para saber si es el día actual
)
