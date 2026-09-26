package com.antonbasson.productcatalog.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object PriceUtils {
    private val southAfricanLocale = Locale("en", "ZA")

    fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(southAfricanLocale)
        return formatter.format(amount)
    }

    fun parseCurrency(raw: String): Double {
        return runCatching {
            val clean = raw.replace("R", "")
                .replace(" ", "")
                .replace(",", "")
                .trim()
            clean.toDoubleOrNull() ?: 0.0
        }.getOrDefault(0.0)
    }
}
