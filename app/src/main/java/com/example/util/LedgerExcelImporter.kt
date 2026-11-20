package com.example.util

import android.util.Xml
import com.example.data.CategoryType
import com.example.data.TransactionType
import com.example.viewmodel.LedgerDataBundle
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.abs
import kotlin.math.roundToInt

data class RawImportRow(
    val excelRowNumber: Int,
    val dateRaw: String,
    val accountRaw: String,
    val note: String,
    val tag: String,
    val description: String,
    val amountRaw: String
)

data class ParsedImportTransaction(
    val type: TransactionType,
    val epochDay: Long,
    val amount: Double,
    val feeAmount: Double = 0.0,
    val accountName: String,
    val existingAccountId: String?,
    val toAccountName: String? = null,
    val existingToAccountId: String? = null,
    val categoryName: String? = null,
    val existingCategoryId: String? = null,
    val subcategory: String? = null,
    val note: String,
    val tagging: String?,
    val description: String?
)

data class LedgerImportPreview(
    val fileName: String,
    val transactions: List<ParsedImportTransaction>,
    val newAccountNames: List<String>,
    val newExpenseCategoryNames: List<String>,
    val newIncomeCategoryNames: List<String>,
    val warnings: List<String>
) {
    val expenseCount: Int get() = transactions.count { it.type == TransactionType.EXPENSE }
    val incomeCount: Int get() = transactions.count { it.type == TransactionType.INCOME }
    val transferCount: Int get() = transactions.count { it.type == TransactionType.TRANSFER }
}

object LedgerExcelImporter {

    fun parseStream(
        inputStream: InputStream,
        fileName: String,
        bundle: LedgerDataBundle
    ): Result<LedgerImportPreview> = runCatching {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) {
            error("The selected file is empty.")
        }

        val grid = readSpreadsheetGrid(bytes)
        val rawRows = mapGridToRawRows(grid)

        if (rawRows.isEmpty()) {
            error("No transaction rows found. Expected columns: Date, Account, Note, Tag, Description / Remarks, Amount.")
        }

        buildPreview(fileName = fileName, rawRows = rawRows, bundle = bundle)
    }

    internal fun readSpreadsheetGrid(bytes: ByteArray): List<Pair<Int, Map<Int, String>>> {
        return if (isZipHeader(bytes)) {
            readXlsxGrid(bytes)
        } else {
            readCsvGrid(bytes)
        }
    }

    private fun isZipHeader(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() && // 'P'
            bytes[1] == 0x4B.toByte()    // 'K'
    }

    // -------------------------------------------------------------------------
    // 1. OOXML (.xlsx) Reader (handles inlineStr, sharedStrings, formulas, dates)
    // -------------------------------------------------------------------------

    private fun readXlsxGrid(bytes: ByteArray): List<Pair<Int, Map<Int, String>>> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (!entry.isDirectory) {
                    val normalized = entry.name.replace('\\', '/').trimStart('/')
                    entries[normalized.lowercase(Locale.US)] = zis.readBytes()
                }
                zis.closeEntry()
            }
        }

        val sharedStrings = entries["xl/sharedstrings.xml"]?.let { parseSharedStrings(it) }.orEmpty()
        val sheetBytes = resolveFirstWorksheetBytes(entries)
            ?: error("Could not locate a worksheet inside the Excel (.xlsx) file.")

        return parseWorksheetRows(sheetBytes, sharedStrings)
    }

    private fun resolveFirstWorksheetBytes(entries: Map<String, ByteArray>): ByteArray? {
        // Direct fast path matching our exporter
        entries["xl/worksheets/sheet1.xml"]?.let { return it }

        // Resolve via workbook.xml + workbook.xml.rels if sheet has a custom name/path
        val workbookBytes = entries["xl/workbook.xml"]
        val relsBytes = entries["xl/_rels/workbook.xml.rels"]
        if (workbookBytes != null && relsBytes != null) {
            val firstRelId = parseFirstSheetRelId(workbookBytes)
            if (firstRelId != null) {
                val target = parseRelTarget(relsBytes, firstRelId)
                if (target != null) {
                    val clean = target.trimStart('/')
                    val resolved = if (clean.startsWith("xl/", ignoreCase = true)) {
                        clean.lowercase(Locale.US)
                    } else {
                        "xl/${clean.lowercase(Locale.US)}"
                    }
                    entries[resolved]?.let { return it }
                }
            }
        }

        // Fallback: first entry under xl/worksheets/
        return entries.entries
            .sortedBy { it.key }
            .firstOrNull { it.key.startsWith("xl/worksheets/") && it.key.endsWith(".xml") }
            ?.value
    }

    private fun parseFirstSheetRelId(xmlBytes: ByteArray): String? {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && localName(parser.name) == "sheet") {
                for (i in 0 until parser.attributeCount) {
                    val attrName = parser.getAttributeName(i)
                    if (attrName == "r:id" || localName(attrName) == "id") {
                        return parser.getAttributeValue(i)
                    }
                }
            }
            event = parser.next()
        }
        return null
    }

    private fun parseRelTarget(xmlBytes: ByteArray, relId: String): String? {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && localName(parser.name) == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                if (id == relId) {
                    return parser.getAttributeValue(null, "Target")
                }
            }
            event = parser.next()
        }
        return null
    }

    private fun parseSharedStrings(xmlBytes: ByteArray): List<String> {
        val result = ArrayList<String>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var inSi = false
        var inT = false
        val current = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (localName(parser.name)) {
                        "si" -> {
                            inSi = true
                            current.setLength(0)
                        }
                        "t" -> if (inSi) inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inSi && inT) {
                        current.append(parser.text.orEmpty())
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (localName(parser.name)) {
                        "t" -> inT = false
                        "si" -> {
                            if (inSi) {
                                result.add(current.toString())
                                inSi = false
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseWorksheetRows(
        xmlBytes: ByteArray,
        sharedStrings: List<String>
    ): List<Pair<Int, Map<Int, String>>> {
        val rows = ArrayList<Pair<Int, Map<Int, String>>>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var inRow = false
        var currentRowNumber = 0
        var currentRowCells = mutableMapOf<Int, String>()
        var nextSequentialCol = 0

        var inCell = false
        var cellCol = 0
        var cellType = ""
        var inV = false
        var inIs = false
        var inT = false
        val valueBuf = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (localName(parser.name)) {
                        "row" -> {
                            inRow = true
                            val rAttr = parser.getAttributeValue(null, "r")?.toIntOrNull()
                            currentRowNumber = rAttr ?: (rows.size + 1)
                            currentRowCells = mutableMapOf()
                            nextSequentialCol = 0
                        }
                        "c" -> if (inRow) {
                            inCell = true
                            val ref = parser.getAttributeValue(null, "r")
                            cellCol = parseColumnIndex(ref) ?: nextSequentialCol
                            nextSequentialCol = cellCol + 1
                            cellType = parser.getAttributeValue(null, "t").orEmpty()
                            valueBuf.setLength(0)
                            inV = false
                            inIs = false
                            inT = false
                        }
                        "v" -> if (inCell) inV = true
                        "is" -> if (inCell) inIs = true
                        "t" -> if (inCell && inIs) inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inCell && (inV || (inIs && inT))) {
                        valueBuf.append(parser.text.orEmpty())
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (localName(parser.name)) {
                        "v" -> inV = false
                        "t" -> inT = false
                        "is" -> inIs = false
                        "c" -> if (inCell) {
                            val rawText = valueBuf.toString()
                            val resolved = when (cellType) {
                                "s" -> {
                                    val idx = rawText.trim().toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                                }
                                "b" -> if (rawText.trim() == "1") "TRUE" else "FALSE"
                                else -> rawText
                            }
                            if (resolved.isNotEmpty()) {
                                currentRowCells[cellCol] = resolved
                            }
                            inCell = false
                        }
                        "row" -> if (inRow) {
                            if (currentRowCells.values.any { it.isNotBlank() }) {
                                rows.add(currentRowNumber to currentRowCells)
                            }
                            inRow = false
                        }
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun parseColumnIndex(cellRef: String?): Int? {
        if (cellRef.isNullOrBlank()) return null
        var col = 0
        var hasLetters = false
        for (ch in cellRef) {
            if (ch in 'A'..'Z') {
                hasLetters = true
                col = col * 26 + (ch - 'A' + 1)
            } else if (ch in 'a'..'z') {
                hasLetters = true
                col = col * 26 + (ch - 'a' + 1)
            } else {
                break
            }
        }
        return if (hasLetters) col - 1 else null
    }

    private fun localName(tag: String?): String {
        if (tag == null) return ""
        val idx = tag.indexOf(':')
        return if (idx >= 0) tag.substring(idx + 1) else tag
    }

    // -------------------------------------------------------------------------
    // 2. CSV Fallback Reader (matches LedgerExcelExporter.buildDoubleEntryCsv)
    // -------------------------------------------------------------------------

    private fun readCsvGrid(bytes: ByteArray): List<Pair<Int, Map<Int, String>>> {
        var text = bytes.toString(Charsets.UTF_8)
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1)
        }
        val records = parseCsvRecords(text)
        val grid = ArrayList<Pair<Int, Map<Int, String>>>(records.size)
        records.forEachIndexed { idx, fields ->
            if (fields.any { it.isNotBlank() }) {
                val map = fields.mapIndexed { colIdx, value -> colIdx to value }.toMap()
                grid.add((idx + 1) to map)
            }
        }
        return grid
    }

    private fun parseCsvRecords(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        val n = content.length

        while (i < n) {
            val ch = content[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < n && content[i + 1] == '"') {
                        field.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(field.toString())
                        field.setLength(0)
                    }
                    '\r' -> {
                        currentRow.add(field.toString())
                        field.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                        if (i + 1 < n && content[i + 1] == '\n') i++
                    }
                    '\n' -> {
                        currentRow.add(field.toString())
                        field.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> field.append(ch)
                }
            }
            i++
        }
        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(field.toString())
            rows.add(currentRow.toList())
        }
        return rows
    }

    // -------------------------------------------------------------------------
    // 3. Column Mapping & Header Detection
    // -------------------------------------------------------------------------

    private fun mapGridToRawRows(grid: List<Pair<Int, Map<Int, String>>>): List<RawImportRow> {
        if (grid.isEmpty()) return emptyList()

        var dateCol = 0
        var accountCol = 1
        var noteCol = 2
        var tagCol = 3
        var descCol = 4
        var amountCol = 5
        var startIdx = 0

        val firstCells = grid.first().second
        val normalizedHeader = firstCells.mapValues { it.value.trim().lowercase(Locale.US) }
        val looksLikeHeader = normalizedHeader.values.any { v ->
            v == "date" || v == "account" || v == "amount" || v.contains("description") || v.contains("remarks")
        }

        if (looksLikeHeader) {
            normalizedHeader.forEach { (col, name) ->
                when {
                    name == "date" || name.startsWith("date") -> dateCol = col
                    name == "account" || name == "ledger" || name.contains("account") -> accountCol = col
                    name == "note" || name == "memo" || name == "narration" -> noteCol = col
                    name == "tag" || name == "tagging" || name == "tags" -> tagCol = col
                    name.contains("description") || name.contains("remark") -> descCol = col
                    name == "amount" || name.startsWith("amount") -> amountCol = col
                }
            }
            startIdx = 1
        }

        val out = ArrayList<RawImportRow>((grid.size - startIdx).coerceAtLeast(0))
        for (idx in startIdx until grid.size) {
            val (rowNum, cells) = grid[idx]
            val dateVal = cells[dateCol].orEmpty().trim()
            val accVal = cells[accountCol].orEmpty().trim()
            val noteVal = cells[noteCol].orEmpty().trim()
            val tagVal = cells[tagCol].orEmpty().trim()
            val descVal = cells[descCol].orEmpty().trim()
            val amtVal = cells[amountCol].orEmpty().trim()

            if (dateVal.isEmpty() && accVal.isEmpty() && amtVal.isEmpty()) continue
            out.add(
                RawImportRow(
                    excelRowNumber = rowNum,
                    dateRaw = dateVal,
                    accountRaw = accVal,
                    note = noteVal,
                    tag = tagVal,
                    description = descVal,
                    amountRaw = amtVal
                )
            )
        }
        return out
    }

    // -------------------------------------------------------------------------
    // 4. Double-Entry Pair Reconstruction
    // -------------------------------------------------------------------------

    private data class ValidatedLine(
        val rowNum: Int,
        val epochDay: Long,
        val accountName: String,
        val note: String,
        val tag: String,
        val description: String,
        val amount: Double
    )

    private fun buildPreview(
        fileName: String,
        rawRows: List<RawImportRow>,
        bundle: LedgerDataBundle
    ): LedgerImportPreview {
        val warnings = mutableListOf<String>()
        val validLines = ArrayList<ValidatedLine>(rawRows.size)

        rawRows.forEach { raw ->
            val epochDay = parseDateToEpochDay(raw.dateRaw)
            val amount = parseAmount(raw.amountRaw)
            when {
                epochDay == null -> warnings.add("Row ${raw.excelRowNumber}: invalid date \"${raw.dateRaw}\" — skipped.")
                raw.accountRaw.isBlank() -> warnings.add("Row ${raw.excelRowNumber}: missing Account name — skipped.")
                amount == null || abs(amount) < 0.0001 -> warnings.add("Row ${raw.excelRowNumber}: invalid or zero amount \"${raw.amountRaw}\" — skipped.")
                else -> validLines.add(
                    ValidatedLine(
                        rowNum = raw.excelRowNumber,
                        epochDay = epochDay,
                        accountName = raw.accountRaw.trim(),
                        note = raw.note.trim(),
                        tag = raw.tag.trim(),
                        description = raw.description.trim(),
                        amount = amount
                    )
                )
            }
        }

        // Lookup tables from existing ledger (preferring active accounts)
        val existingAccounts = mutableMapOf<String, String>()
        bundle.accounts.sortedBy { it.archived }.forEach { acc ->
            existingAccounts.putIfAbsent(acc.name.trim().lowercase(Locale.US), acc.id)
        }
        val existingExpenseCats = mutableMapOf<String, String>()
        val existingIncomeCats = mutableMapOf<String, String>()
        bundle.categories.forEach { cat ->
            val key = cat.name.trim().lowercase(Locale.US)
            when (cat.categoryType()) {
                CategoryType.EXPENSE -> existingExpenseCats.putIfAbsent(key, cat.id)
                CategoryType.INCOME -> existingIncomeCats.putIfAbsent(key, cat.id)
            }
        }

        val newAccounts = LinkedHashMap<String, String>() // lower -> display name
        val newExpenseCats = LinkedHashMap<String, String>()
        val newIncomeCats = LinkedHashMap<String, String>()
        val parsedTransactions = mutableListOf<ParsedImportTransaction>()

        var idx = 0
        while (idx < validLines.size) {
            if (idx + 1 >= validLines.size) {
                warnings.add("Row ${validLines[idx].rowNum}: unpaired line without matching Debit/Credit entry — skipped.")
                break
            }

            val first = validLines[idx]
            val second = validLines[idx + 1]

            val oppositeSigns = (first.amount > 0.0 && second.amount < 0.0) ||
                (first.amount < 0.0 && second.amount > 0.0)
            val balanced = abs(first.amount + second.amount) < 0.015

            if (!oppositeSigns || !balanced) {
                warnings.add(
                    "Rows ${first.rowNum}–${second.rowNum}: unbalanced pair (${first.amount} and ${second.amount}) — row ${first.rowNum} skipped."
                )
                idx += 1
                continue
            }

            val debitLine = if (first.amount > 0.0) first else second
            val creditLine = if (first.amount < 0.0) first else second
            val txAmount = abs(debitLine.amount)
            val txEpochDay = debitLine.epochDay
            val txNote = debitLine.note.ifBlank { creditLine.note }
            val txTag = debitLine.tag.ifBlank { creditLine.tag }.takeIf { it.isNotBlank() }
            val txDesc = debitLine.description.ifBlank { creditLine.description }.takeIf { it.isNotBlank() }

            // Check if this pair is a Transfer Fee exported immediately after its parent Transfer
            val lastTx = parsedTransactions.lastOrNull()
            if (lastTx != null &&
                lastTx.type == TransactionType.TRANSFER &&
                lastTx.feeAmount == 0.0 &&
                lastTx.epochDay == txEpochDay &&
                debitLine.accountName.equals("Transfer Fee", ignoreCase = true) &&
                creditLine.accountName.equals(lastTx.accountName, ignoreCase = true) &&
                (txNote == "${lastTx.note} (Fee)" || (lastTx.note.isBlank() && txNote.equals("Transfer Fee", ignoreCase = true)))
            ) {
                parsedTransactions[parsedTransactions.lastIndex] = lastTx.copy(feeAmount = txAmount)
                idx += 2
                continue
            }

            val (debitBase, debitSub) = splitCategoryLabel(debitLine.accountName)
            val (creditBase, creditSub) = splitCategoryLabel(creditLine.accountName)
            val debitKey = debitLine.accountName.lowercase(Locale.US)
            val creditKey = creditLine.accountName.lowercase(Locale.US)
            val debitBaseKey = debitBase.lowercase(Locale.US)
            val creditBaseKey = creditBase.lowercase(Locale.US)

            val debitAccId = existingAccounts[debitKey]
            val creditAccId = existingAccounts[creditKey]
            val debitExpCatId = existingExpenseCats[debitBaseKey]
            val creditIncCatId = existingIncomeCats[creditBaseKey]

            val debitIsKnownAccount = debitAccId != null || newAccounts.containsKey(debitKey)
            val creditIsKnownAccount = creditAccId != null || newAccounts.containsKey(creditKey)
            val debitIsKnownExpCat = debitExpCatId != null || newExpenseCats.containsKey(debitBaseKey) || debitSub != null
            val creditIsKnownIncCat = creditIncCatId != null || newIncomeCats.containsKey(creditBaseKey) || creditSub != null

            val parsed = when {
                // Transfer: Debit = To Account (+), Credit = From Account (-)
                debitIsKnownAccount && creditIsKnownAccount -> {
                    ParsedImportTransaction(
                        type = TransactionType.TRANSFER,
                        epochDay = txEpochDay,
                        amount = txAmount,
                        accountName = creditLine.accountName,
                        existingAccountId = creditAccId,
                        toAccountName = debitLine.accountName,
                        existingToAccountId = debitAccId,
                        note = txNote,
                        tagging = txTag,
                        description = txDesc
                    )
                }

                // Expense: Debit = Expense Category (+), Credit = Account (-)
                creditIsKnownAccount && !debitIsKnownAccount -> {
                    if (debitExpCatId == null) {
                        newExpenseCats.putIfAbsent(debitBaseKey, debitBase)
                    }
                    ParsedImportTransaction(
                        type = TransactionType.EXPENSE,
                        epochDay = txEpochDay,
                        amount = txAmount,
                        accountName = creditLine.accountName,
                        existingAccountId = creditAccId,
                        categoryName = debitBase,
                        existingCategoryId = debitExpCatId,
                        subcategory = debitSub,
                        note = txNote,
                        tagging = txTag,
                        description = txDesc
                    )
                }

                // Income: Debit = Account (+), Credit = Income Category (-)
                debitIsKnownAccount && !creditIsKnownAccount -> {
                    if (creditIncCatId == null) {
                        newIncomeCats.putIfAbsent(creditBaseKey, creditBase)
                    }
                    ParsedImportTransaction(
                        type = TransactionType.INCOME,
                        epochDay = txEpochDay,
                        amount = txAmount,
                        accountName = debitLine.accountName,
                        existingAccountId = debitAccId,
                        categoryName = creditBase,
                        existingCategoryId = creditIncCatId,
                        subcategory = creditSub,
                        note = txNote,
                        tagging = txTag,
                        description = txDesc
                    )
                }

                // Neither is a known account yet, but credit matches an Income Category -> Income
                creditIsKnownIncCat && !debitIsKnownExpCat -> {
                    newAccounts.putIfAbsent(debitKey, debitLine.accountName)
                    if (creditIncCatId == null) {
                        newIncomeCats.putIfAbsent(creditBaseKey, creditBase)
                    }
                    ParsedImportTransaction(
                        type = TransactionType.INCOME,
                        epochDay = txEpochDay,
                        amount = txAmount,
                        accountName = debitLine.accountName,
                        existingAccountId = null,
                        categoryName = creditBase,
                        existingCategoryId = creditIncCatId,
                        subcategory = creditSub,
                        note = txNote,
                        tagging = txTag,
                        description = txDesc
                    )
                }

                // Default (including known Expense Category with a brand-new Account name) -> Expense
                else -> {
                    newAccounts.putIfAbsent(creditKey, creditLine.accountName)
                    if (debitExpCatId == null) {
                        newExpenseCats.putIfAbsent(debitBaseKey, debitBase)
                    }
                    ParsedImportTransaction(
                        type = TransactionType.EXPENSE,
                        epochDay = txEpochDay,
                        amount = txAmount,
                        accountName = creditLine.accountName,
                        existingAccountId = null,
                        categoryName = debitBase,
                        existingCategoryId = debitExpCatId,
                        subcategory = debitSub,
                        note = txNote,
                        tagging = txTag,
                        description = txDesc
                    )
                }
            }

            parsedTransactions.add(parsed)
            idx += 2
        }

        if (parsedTransactions.isEmpty()) {
            val detail = warnings.firstOrNull() ?: "Ensure each entry has 2 rows: one Debit (+amount) and one Credit (-amount)."
            error("No valid double-entry transactions found. $detail")
        }

        return LedgerImportPreview(
            fileName = fileName,
            transactions = parsedTransactions,
            newAccountNames = newAccounts.values.toList(),
            newExpenseCategoryNames = newExpenseCats.values.toList(),
            newIncomeCategoryNames = newIncomeCats.values.toList(),
            warnings = warnings
        )
    }

    private fun splitCategoryLabel(raw: String): Pair<String, String?> {
        val parts = raw.split(" / ", limit = 2)
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            parts[0].trim() to parts[1].trim()
        } else {
            raw.trim() to null
        }
    }

    private fun parseAmount(raw: String): Double? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val isAccountingNeg = trimmed.startsWith("(") && trimmed.endsWith(")")
        val cleaned = trimmed
            .removePrefix("(")
            .removeSuffix(")")
            .replace(",", "")
            .replace("₹", "")
            .replace("$", "")
            .replace("€", "")
            .replace("£", "")
            .replace(" ", "")
        val num = cleaned.toDoubleOrNull() ?: return null
        return if (isAccountingNeg && num > 0.0) -num else num
    }

    private val datePatterns = listOf(
        "dd/MM/yyyy",
        "d/M/yyyy",
        "dd-MM-yyyy",
        "d-M-yyyy",
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "dd MMM yyyy",
        "dd-MMM-yyyy",
        "d MMM yyyy",
        "dd/MM/yy",
        "dd-MM-yy",
        "MM/dd/yyyy"
    )

    private fun parseDateToEpochDay(raw: String): Long? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // Handle Excel numeric serial date (e.g., 46345 -> days since 1899-12-30)
        val numeric = trimmed.toDoubleOrNull()
        if (numeric != null && numeric in 15000.0..90000.0) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, 1899)
                set(Calendar.MONTH, Calendar.DECEMBER)
                set(Calendar.DAY_OF_MONTH, 30)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, numeric.roundToInt())
            }
            return LedgerFormatters.calendarToEpochDay(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        // Strip trailing weekday or time portion if present (e.g. "19/11/2026 (Thu)" or "2026-11-19 10:30:00")
        val cleaned = trimmed.substringBefore(" (").substringBefore("T").trim()

        for (pattern in datePatterns) {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val pos = ParsePosition(0)
            val parsed = sdf.parse(cleaned, pos)
            if (parsed != null && pos.index == cleaned.length) {
                val cal = Calendar.getInstance().apply { time = parsed }
                val year = cal.get(Calendar.YEAR)
                if (year in 1970..2150) {
                    return LedgerFormatters.calendarToEpochDay(
                        year,
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    )
                }
            }
        }
        return null
    }
}
