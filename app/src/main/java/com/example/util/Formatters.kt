package com.example.util

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LedgerCoral
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object LedgerFormatters {

    private val numberFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
    private val wholeNumberFormat = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))

    fun formatAmount(amount: Double, symbol: String = "₹", includeSymbol: Boolean = true): String {
        val formatted = numberFormat.format(abs(amount))
        val sign = if (amount < -0.004) "-" else ""
        return if (includeSymbol) {
            if (sign.isNotEmpty()) "$sign$symbol $formatted" else "$symbol $formatted"
        } else {
            "$sign$formatted"
        }
    }

    fun formatSignedCompact(amount: Double, symbol: String = "₹"): String {
        val formatted = numberFormat.format(abs(amount))
        return if (amount < -0.004) "-$symbol $formatted" else "$symbol $formatted"
    }

    fun formatWholeAmount(amount: Double, symbol: String = "₹", includeSymbol: Boolean = true): String {
        val rounded = amount.roundToLong()
        val formatted = wholeNumberFormat.format(abs(rounded))
        val sign = if (rounded < 0L) "-" else ""
        return if (includeSymbol) {
            if (sign.isNotEmpty()) "$sign$symbol $formatted" else "$symbol $formatted"
        } else {
            "$sign$formatted"
        }
    }

    fun formatSignedWhole(amount: Double, symbol: String = "₹"): String {
        val rounded = amount.roundToLong()
        val formatted = wholeNumberFormat.format(abs(rounded))
        return if (rounded < 0L) "-$symbol $formatted" else "$symbol $formatted"
    }

    fun formatAxisWhole(amount: Double, symbol: String = "₹"): String {
        val rounded = amount.roundToLong()
        val absVal = abs(rounded)
        val sign = if (rounded < 0L) "-" else ""
        return when {
            absVal >= 10_000_000L -> "${sign}${symbol}${String.format(Locale.US, "%.1f", absVal / 1_000_000.0)}M"
            absVal >= 1_000_000L -> "${sign}${symbol}${String.format(Locale.US, "%.1f", absVal / 1_000_000.0)}M"
            absVal >= 10_000L -> "${sign}${symbol}${absVal / 1_000L}K"
            absVal >= 1_000L -> "${sign}${symbol}${String.format(Locale.US, "%.1f", absVal / 1_000.0)}K"
            else -> "${sign}${symbol}$absVal"
        }
    }

    fun parseColorHex(hex: String, fallback: Color = LedgerCoral): Color {
        return try {
            val cleaned = hex.removePrefix("#")
            if (cleaned.length == 6) {
                Color(("FF$cleaned").toLong(16))
            } else {
                fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    fun todayEpochDay(): Long {
        val cal = Calendar.getInstance()
        return calendarToEpochDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    }

    fun calendarToEpochDay(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthZeroBased)
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 86_400_000L
    }

    fun epochDayToCalendar(epochDay: Long): Calendar {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochDay * 86_400_000L + 43_200_000L
        return cal
    }

    fun epochDayToMillis(epochDay: Long): Long {
        return epochDay * 86_400_000L + 43_200_000L
    }

    fun formatMonthYear(year: Int, monthZeroBased: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthZeroBased)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return SimpleDateFormat("MMM yyyy", Locale.US).format(cal.time)
    }

    fun formatFullDateInput(epochDay: Long): String {
        val cal = epochDayToCalendar(epochDay)
        return SimpleDateFormat("dd/MM/yyyy (EEE)", Locale.US).format(cal.time)
    }

    fun formatShortDate(epochDay: Long): String {
        val cal = epochDayToCalendar(epochDay)
        return SimpleDateFormat("dd MMM yyyy", Locale.US).format(cal.time)
    }

    fun dayOfMonthPadded(epochDay: Long): String {
        val cal = epochDayToCalendar(epochDay)
        return String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))
    }

    fun weekdayShort(epochDay: Long): String {
        val cal = epochDayToCalendar(epochDay)
        return SimpleDateFormat("EEE", Locale.US).format(cal.time)
    }

    fun monthYearDot(epochDay: Long): String {
        val cal = epochDayToCalendar(epochDay)
        return SimpleDateFormat("MM.yyyy", Locale.US).format(cal.time)
    }

    fun dayOfWeekIndex(epochDay: Long): Int {
        val cal = epochDayToCalendar(epochDay)
        return cal.get(Calendar.DAY_OF_WEEK) // Calendar.SUNDAY = 1 ... Calendar.SATURDAY = 7
    }

    fun weekRangeLabel(startEpochDay: Long, endEpochDay: Long): String {
        val fmt = SimpleDateFormat("dd MMM", Locale.US)
        val start = fmt.format(Date(epochDayToMillis(startEpochDay)))
        val end = fmt.format(Date(epochDayToMillis(endEpochDay)))
        return "$start - $end"
    }

    fun daysInMonth(year: Int, monthZeroBased: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthZeroBased)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun firstDayOfWeekInMonth(year: Int, monthZeroBased: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthZeroBased)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.get(Calendar.DAY_OF_WEEK) // 1 = Sun .. 7 = Sat
    }
}
