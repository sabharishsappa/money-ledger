package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.LedgerCoral
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToLong

object IndianNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val hasMinus = original.startsWith("-")
        val intStart = if (hasMinus) 1 else 0
        val dotIdx = original.indexOf('.', startIndex = intStart)
        val intEnd = if (dotIdx >= 0) dotIdx else original.length

        val sb = StringBuilder(original.length + 6)
        val origToTrans = IntArray(original.length + 1)
        val transToOrigList = ArrayList<Int>(original.length + 8)

        for (origIdx in original.indices) {
            if (origIdx in (intStart + 1) until intEnd) {
                val digitsRemaining = intEnd - origIdx
                if (digitsRemaining >= 3 && (digitsRemaining - 3) % 2 == 0) {
                    sb.append(',')
                    transToOrigList.add(origIdx)
                }
            }
            origToTrans[origIdx] = sb.length
            sb.append(original[origIdx])
            transToOrigList.add(origIdx)
        }
        origToTrans[original.length] = sb.length
        transToOrigList.add(original.length)

        val transformed = sb.toString()
        val transToOrig = IntArray(transToOrigList.size) { transToOrigList[it] }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return origToTrans[offset.coerceIn(0, original.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transToOrig[offset.coerceIn(0, transformed.length)]
            }
        }
        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

object LedgerFormatters {

    private val monthYearFmt = ThreadLocal.withInitial {
        SimpleDateFormat("MMM yyyy", Locale.US)
    }
    private val fullDateInputFmt = ThreadLocal.withInitial {
        SimpleDateFormat("dd/MM/yyyy (EEE)", Locale.US)
    }
    private val shortDateFmt = ThreadLocal.withInitial {
        SimpleDateFormat("dd MMM yyyy", Locale.US)
    }
    private val weekdayShortFmt = ThreadLocal.withInitial {
        SimpleDateFormat("EEE", Locale.US)
    }
    private val monthYearDotFmt = ThreadLocal.withInitial {
        SimpleDateFormat("MM.yyyy", Locale.US)
    }
    private val dayMonthFmt = ThreadLocal.withInitial {
        SimpleDateFormat("dd MMM", Locale.US)
    }
    private val scratchCalendar = ThreadLocal.withInitial { Calendar.getInstance() }
    private val colorCache = ConcurrentHashMap<String, Color>()

    private fun reuseCalendarAtEpochDay(epochDay: Long): Calendar {
        val cal = scratchCalendar.get()!!
        cal.timeInMillis = epochDay * 86_400_000L + 43_200_000L
        return cal
    }

    fun formatIndianIntegerDigits(digits: String): String {
        val n = digits.length
        if (n <= 3) return digits
        val sb = StringBuilder(n + n / 2)
        for (i in 0 until n) {
            if (i > 0) {
                val rem = n - i
                if (rem >= 3 && (rem - 3) % 2 == 0) {
                    sb.append(',')
                }
            }
            sb.append(digits[i])
        }
        return sb.toString()
    }

    private fun formatIndianDecimal(absAmount: Double): String {
        val scaled = (absAmount * 100.0).roundToLong()
        val intPart = scaled / 100L
        val decPart = scaled % 100L
        val decStr = if (decPart < 10L) "0$decPart" else decPart.toString()
        return "${formatIndianIntegerDigits(intPart.toString())}.$decStr"
    }

    fun formatAmount(amount: Double, symbol: String = "₹", includeSymbol: Boolean = true): String {
        val formatted = formatIndianDecimal(abs(amount))
        return if (includeSymbol) "$symbol $formatted" else formatted
    }

    fun formatSignedCompact(amount: Double, symbol: String = "₹"): String {
        val formatted = formatIndianDecimal(abs(amount))
        return "$symbol $formatted"
    }

    fun formatWholeAmount(amount: Double, symbol: String = "₹", includeSymbol: Boolean = true): String {
        val rounded = abs(amount.roundToLong())
        val formatted = formatIndianIntegerDigits(rounded.toString())
        return if (includeSymbol) "$symbol $formatted" else formatted
    }

    fun formatSignedWhole(amount: Double, symbol: String = "₹"): String {
        val rounded = abs(amount.roundToLong())
        val formatted = formatIndianIntegerDigits(rounded.toString())
        return "$symbol $formatted"
    }

    fun formatAxisWhole(amount: Double, symbol: String = "₹"): String {
        val rounded = amount.roundToLong()
        val absVal = abs(rounded)
        return when {
            absVal >= 1_000_000L -> "${symbol}${String.format(Locale.US, "%.1f", absVal / 1_000_000.0)}M"
            absVal >= 10_000L -> "${symbol}${absVal / 1_000L}K"
            absVal >= 1_000L -> "${symbol}${String.format(Locale.US, "%.1f", absVal / 1_000.0)}K"
            else -> "${symbol}$absVal"
        }
    }

    fun parseColorHex(hex: String, fallback: Color = LedgerCoral): Color {
        if (hex.isBlank()) return fallback
        colorCache[hex]?.let { return it }
        val parsed = try {
            val cleaned = hex.removePrefix("#")
            if (cleaned.length == 6) {
                Color(("FF$cleaned").toLong(16))
            } else {
                fallback
            }
        } catch (_: Exception) {
            fallback
        }
        if (colorCache.size < 256) {
            colorCache[hex] = parsed
        }
        return parsed
    }

    fun todayEpochDay(): Long {
        val cal = scratchCalendar.get()!!
        cal.timeInMillis = System.currentTimeMillis()
        return calendarToEpochDay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun calendarToEpochDay(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
        val cal = scratchCalendar.get()!!
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
        val cal = scratchCalendar.get()!!
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthZeroBased)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return monthYearFmt.get()!!.format(cal.time)
    }

    fun formatFullDateInput(epochDay: Long): String {
        return fullDateInputFmt.get()!!.format(Date(epochDayToMillis(epochDay)))
    }

    fun formatShortDate(epochDay: Long): String {
        return shortDateFmt.get()!!.format(Date(epochDayToMillis(epochDay)))
    }

    fun dayOfMonthPadded(epochDay: Long): String {
        val day = reuseCalendarAtEpochDay(epochDay).get(Calendar.DAY_OF_MONTH)
        return if (day < 10) "0$day" else day.toString()
    }

    fun weekdayShort(epochDay: Long): String {
        return weekdayShortFmt.get()!!.format(Date(epochDayToMillis(epochDay)))
    }

    fun monthYearDot(epochDay: Long): String {
        return monthYearDotFmt.get()!!.format(Date(epochDayToMillis(epochDay)))
    }

    fun dayOfWeekIndex(epochDay: Long): Int {
        return reuseCalendarAtEpochDay(epochDay).get(Calendar.DAY_OF_WEEK)
    }

    fun weekRangeLabel(startEpochDay: Long, endEpochDay: Long): String {
        val fmt = dayMonthFmt.get()!!
        val start = fmt.format(Date(epochDayToMillis(startEpochDay)))
        val end = fmt.format(Date(epochDayToMillis(endEpochDay)))
        return "$start - $end"
    }

    fun daysInMonth(year: Int, monthZeroBased: Int): Int {
        val cal = scratchCalendar.get()!!
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthZeroBased)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun firstDayOfWeekInMonth(year: Int, monthZeroBased: Int): Int {
        val cal = scratchCalendar.get()!!
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthZeroBased)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal.get(Calendar.DAY_OF_WEEK) // 1 = Sun .. 7 = Sat
    }
}
