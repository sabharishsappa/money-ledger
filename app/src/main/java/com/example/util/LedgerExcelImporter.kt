package com.example.util

import android.util.Xml
import com.example.data.Category
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
    val fromAccountRaw: String,
    val toAccountRaw: String,
    val note: String,
    val note2: String = "",
    val tag: String,
    val hasExplicitToTagCol: Boolean = false,
    val toTag: String = "",
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
    val note2: String = "",
    val tagging: String?,
    val toTagging: String? = null,
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
            error("No transaction rows found. Expected columns: Date of Transaction, From Account, To Account, Note, Tag, Description, Amount.")
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
        entries["xl/worksheets/sheet1.xml"]?.let { return it }

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
    // 2. CSV Fallback Reader (matches LedgerExcelExporter.buildCsv)
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
    //    Expected: Date of Transaction, From Account, To Account, Note, Tag, Description, Amount
    // -------------------------------------------------------------------------

    private fun mapGridToRawRows(grid: List<Pair<Int, Map<Int, String>>>): List<RawImportRow> {
        if (grid.isEmpty()) return emptyList()

        var dateCol = 0
        var fromCol = 1
        var toCol = 2
        var noteCol = 3
        var note2Col = -1
        var tagCol = 4
        var toTagCol = -1
        var descCol = 5
        var amountCol = 6
        var startIdx = 0

        val firstCells = grid.first().second
        val normalizedHeader = firstCells.mapValues { it.value.trim().lowercase(Locale.US) }
        val looksLikeHeader = normalizedHeader.values.any { v ->
            v.contains("date") ||
                v.contains("from") ||
                v.contains("to account") ||
                v == "to" ||
                v == "amount" ||
                v.contains("description") ||
                v.contains("remark")
        }

        if (looksLikeHeader) {
            normalizedHeader.forEach { (col, name) ->
                when {
                    name.contains("date") -> dateCol = col
                    name == "to tag" || name == "to tag 1" || name == "to tag1" || name == "to tagging" || name == "to tagging 1" || name == "destination tag" -> toTagCol = col
                    name == "from tag" || name == "from tag 1" || name == "from tag1" || name == "from tagging" || name == "tag" || name == "tag 1" || name == "tag1" || name == "tagging" || name == "tagging 1" || name == "tags" -> tagCol = col
                    name.contains("from") || name.contains("credit") || name == "cr" -> fromCol = col
                    name.startsWith("to ") || name == "to" || name.contains("to account") || name.contains("debit") || name == "dr" -> toCol = col
                    name == "note 2" || name == "note2" || name == "second note" || name == "memo 2" || name == "narration 2" -> note2Col = col
                    name == "note 1" || name == "note1" || name == "note" || name == "memo" || name == "narration" -> noteCol = col
                    name.contains("description") || name.contains("remark") -> descCol = col
                    name == "amount" || name.startsWith("amount") -> amountCol = col
                }
            }
            startIdx = 1
        }

        val hasExplicitToTag = toTagCol >= 0
        val out = ArrayList<RawImportRow>((grid.size - startIdx).coerceAtLeast(0))
        for (idx in startIdx until grid.size) {
            val (rowNum, cells) = grid[idx]
            val dateVal = cells[dateCol].orEmpty().trim()
            val fromVal = cells[fromCol].orEmpty().trim()
            val toVal = cells[toCol].orEmpty().trim()
            val noteVal = cells[noteCol].orEmpty().trim()
            val note2Val = if (note2Col >= 0) cells[note2Col].orEmpty().trim() else ""
            val tagVal = cells[tagCol].orEmpty().trim()
            val toTagVal = if (toTagCol >= 0) cells[toTagCol].orEmpty().trim() else ""
            val descVal = cells[descCol].orEmpty().trim()
            val amtVal = cells[amountCol].orEmpty().trim()

            if (dateVal.isEmpty() && fromVal.isEmpty() && toVal.isEmpty() && amtVal.isEmpty()) continue
            out.add(
                RawImportRow(
                    excelRowNumber = rowNum,
                    dateRaw = dateVal,
                    fromAccountRaw = fromVal,
                    toAccountRaw = toVal,
                    note = noteVal,
                    note2 = note2Val,
                    tag = tagVal,
                    hasExplicitToTagCol = hasExplicitToTag,
                    toTag = toTagVal,
                    description = descVal,
                    amountRaw = amtVal
                )
            )
        }
        return out
    }

    // -------------------------------------------------------------------------
    // 4. Single-Row Accounting Reconstruction
    //    From Account = Credit, To Account = Debit
    //    Expense:  From Account = Payment Account (Credit), To Account = Expense Ledger (Debit)
    //    Income:   From Account = Income Ledger (Credit),   To Account = Receiving Account (Debit)
    //    Transfer: From Account = Source Account (Credit),  To Account = Destination Account (Debit)
    // -------------------------------------------------------------------------

    private data class ResolvedCategoryRef(
        val existingCategoryId: String?,
        val categoryName: String,
        val subcategory: String?
    )

    private fun buildPreview(
        fileName: String,
        rawRows: List<RawImportRow>,
        bundle: LedgerDataBundle
    ): LedgerImportPreview {
        val warnings = mutableListOf<String>()

        // Lookup tables from existing ledger (preferring active accounts)
        val existingAccounts = mutableMapOf<String, String>()
        bundle.accounts.sortedBy { it.archived }.forEach { acc ->
            existingAccounts.putIfAbsent(acc.name.trim().lowercase(Locale.US), acc.id)
        }

        val existingExpenseCats = mutableMapOf<String, Category>()
        val existingIncomeCats = mutableMapOf<String, Category>()
        val existingExpenseLedgers = mutableMapOf<String, Pair<Category, String>>()
        val existingIncomeLedgers = mutableMapOf<String, Pair<Category, String>>()

        bundle.categories.forEach { cat ->
            val catKey = cat.name.trim().lowercase(Locale.US)
            when (cat.categoryType()) {
                CategoryType.EXPENSE -> {
                    existingExpenseCats.putIfAbsent(catKey, cat)
                    cat.subcategories.forEach { sub ->
                        val subClean = sub.trim()
                        if (subClean.isNotEmpty()) {
                            existingExpenseLedgers.putIfAbsent(subClean.lowercase(Locale.US), cat to subClean)
                        }
                    }
                }
                CategoryType.INCOME -> {
                    existingIncomeCats.putIfAbsent(catKey, cat)
                    cat.subcategories.forEach { sub ->
                        val subClean = sub.trim()
                        if (subClean.isNotEmpty()) {
                            existingIncomeLedgers.putIfAbsent(subClean.lowercase(Locale.US), cat to subClean)
                        }
                    }
                }
            }
        }

        val newAccounts = LinkedHashMap<String, String>() // lower -> display name
        val newExpenseCats = LinkedHashMap<String, String>()
        val newIncomeCats = LinkedHashMap<String, String>()
        val parsedTransactions = mutableListOf<ParsedImportTransaction>()

        fun resolveCategory(
            rawLabel: String,
            existingCats: Map<String, Category>,
            existingLedgers: Map<String, Pair<Category, String>>,
            queuedNewCats: LinkedHashMap<String, String>
        ): ResolvedCategoryRef {
            val (base, sub) = splitCategoryLabel(rawLabel)
            val baseKey = base.lowercase(Locale.US)

            if (sub != null) {
                val matchedCat = existingCats[baseKey]
                if (matchedCat != null) {
                    val canonicalSub = matchedCat.subcategories.firstOrNull {
                        it.equals(sub, ignoreCase = true)
                    } ?: sub
                    return ResolvedCategoryRef(matchedCat.id, matchedCat.name, canonicalSub)
                }
                queuedNewCats.putIfAbsent(baseKey, base)
                return ResolvedCategoryRef(null, queuedNewCats[baseKey] ?: base, sub)
            }

            // Check if rawLabel directly matches an existing Expense/Income Ledger (subcategory)
            existingLedgers[baseKey]?.let { (parentCat, canonicalSub) ->
                return ResolvedCategoryRef(parentCat.id, parentCat.name, canonicalSub)
            }

            // Check if rawLabel matches a top-level Category
            existingCats[baseKey]?.let { cat ->
                return ResolvedCategoryRef(cat.id, cat.name, null)
            }

            queuedNewCats.putIfAbsent(baseKey, base)
            return ResolvedCategoryRef(null, queuedNewCats[baseKey] ?: base, null)
        }

        rawRows.forEach { raw ->
            val epochDay = parseDateToEpochDay(raw.dateRaw)
            val parsedAmt = parseAmount(raw.amountRaw)
            when {
                epochDay == null -> {
                    warnings.add("Row ${raw.excelRowNumber}: invalid date \"${raw.dateRaw}\" — skipped.")
                    return@forEach
                }
                raw.fromAccountRaw.isBlank() -> {
                    warnings.add("Row ${raw.excelRowNumber}: missing From Account — skipped.")
                    return@forEach
                }
                raw.toAccountRaw.isBlank() -> {
                    warnings.add("Row ${raw.excelRowNumber}: missing To Account — skipped.")
                    return@forEach
                }
                parsedAmt == null || abs(parsedAmt) < 0.0001 -> {
                    warnings.add("Row ${raw.excelRowNumber}: invalid or zero amount \"${raw.amountRaw}\" — skipped.")
                    return@forEach
                }
                raw.fromAccountRaw.equals(raw.toAccountRaw, ignoreCase = true) &&
                    (!raw.hasExplicitToTagCol || raw.tag.equals(raw.toTag, ignoreCase = true)) -> {
                    warnings.add("Row ${raw.excelRowNumber}: From Account and To Account are identical — skipped.")
                    return@forEach
                }
            }

            // Standard rule: From Account = Credit, To Account = Debit (negative amount reverses direction)
            val reversed = parsedAmt < 0.0
            val creditName = if (!reversed) raw.fromAccountRaw.trim() else raw.toAccountRaw.trim()
            val debitName = if (!reversed) raw.toAccountRaw.trim() else raw.fromAccountRaw.trim()
            val txAmount = abs(parsedAmt)
            val txNote = raw.note.trim()
            val txNote2 = raw.note2.trim()
            val rawFromTag = raw.tag.trim().takeIf { it.isNotBlank() }
            val rawToTag = if (raw.hasExplicitToTagCol) raw.toTag.trim().takeIf { it.isNotBlank() } else rawFromTag
            val txTag = if (!reversed) rawFromTag else rawToTag
            val txToTag = if (!reversed) rawToTag else rawFromTag
            val txDesc = raw.description.trim().takeIf { it.isNotBlank() }

            // Fold Transfer Fee row exported immediately after its parent Transfer
            val lastTx = parsedTransactions.lastOrNull()
            if (lastTx != null &&
                lastTx.type == TransactionType.TRANSFER &&
                lastTx.feeAmount == 0.0 &&
                lastTx.epochDay == epochDay &&
                debitName.equals("Transfer Fee", ignoreCase = true) &&
                creditName.equals(lastTx.accountName, ignoreCase = true) &&
                (txNote == "${lastTx.note} (Fee)" || (lastTx.note.isBlank() && txNote.equals("Transfer Fee", ignoreCase = true)))
            ) {
                parsedTransactions[parsedTransactions.lastIndex] = lastTx.copy(feeAmount = txAmount)
                return@forEach
            }

            val (debitBase, debitSub) = splitCategoryLabel(debitName)
            val (creditBase, creditSub) = splitCategoryLabel(creditName)
            val debitKey = debitName.lowercase(Locale.US)
            val creditKey = creditName.lowercase(Locale.US)
            val debitBaseKey = debitBase.lowercase(Locale.US)
            val creditBaseKey = creditBase.lowercase(Locale.US)

            val debitAccId = existingAccounts[debitKey]
            val creditAccId = existingAccounts[creditKey]

            val debitIsKnownAccount = debitAccId != null || newAccounts.containsKey(debitKey)
            val creditIsKnownAccount = creditAccId != null || newAccounts.containsKey(creditKey)

            val debitIsKnownExp = existingExpenseCats.containsKey(debitBaseKey) ||
                existingExpenseLedgers.containsKey(debitKey) ||
                newExpenseCats.containsKey(debitBaseKey) ||
                debitSub != null
            val creditIsKnownInc = existingIncomeCats.containsKey(creditBaseKey) ||
                existingIncomeLedgers.containsKey(creditKey) ||
                newIncomeCats.containsKey(creditBaseKey) ||
                creditSub != null

            val explicitCategoryToTag = if (raw.hasExplicitToTagCol) raw.toTag.trim().takeIf { it.isNotBlank() } else null

            val parsed = when {
                // Transfer: From Account (Credit) -> To Account (Debit), both are Accounts
                debitIsKnownAccount && creditIsKnownAccount -> {
                    ParsedImportTransaction(
                        type = TransactionType.TRANSFER,
                        epochDay = epochDay,
                        amount = txAmount,
                        accountName = creditName,
                        existingAccountId = creditAccId,
                        toAccountName = debitName,
                        existingToAccountId = debitAccId,
                        note = txNote,
                        note2 = txNote2,
                        tagging = txTag,
                        toTagging = txToTag,
                        description = txDesc
                    )
                }

                // Expense: From Account = Payment Account (Credit), To Account = Expense Ledger (Debit)
                creditIsKnownAccount && !debitIsKnownAccount -> {
                    val expRef = resolveCategory(debitName, existingExpenseCats, existingExpenseLedgers, newExpenseCats)
                    ParsedImportTransaction(
                        type = TransactionType.EXPENSE,
                        epochDay = epochDay,
                        amount = txAmount,
                        accountName = creditName,
                        existingAccountId = creditAccId,
                        categoryName = expRef.categoryName,
                        existingCategoryId = expRef.existingCategoryId,
                        subcategory = expRef.subcategory,
                        note = txNote,
                        note2 = txNote2,
                        tagging = txTag,
                        toTagging = explicitCategoryToTag,
                        description = txDesc
                    )
                }

                // Income: From Account = Income Ledger (Credit), To Account = Receiving Account (Debit)
                debitIsKnownAccount && !creditIsKnownAccount -> {
                    val incRef = resolveCategory(creditName, existingIncomeCats, existingIncomeLedgers, newIncomeCats)
                    ParsedImportTransaction(
                        type = TransactionType.INCOME,
                        epochDay = epochDay,
                        amount = txAmount,
                        accountName = debitName,
                        existingAccountId = debitAccId,
                        categoryName = incRef.categoryName,
                        existingCategoryId = incRef.existingCategoryId,
                        subcategory = incRef.subcategory,
                        note = txNote,
                        note2 = txNote2,
                        tagging = rawFromTag,
                        toTagging = explicitCategoryToTag,
                        description = txDesc
                    )
                }

                // Neither is a known account yet, but From Account (Credit) matches an Income Category/Ledger -> Income
                creditIsKnownInc && !debitIsKnownExp -> {
                    newAccounts.putIfAbsent(debitKey, debitName)
                    val incRef = resolveCategory(creditName, existingIncomeCats, existingIncomeLedgers, newIncomeCats)
                    ParsedImportTransaction(
                        type = TransactionType.INCOME,
                        epochDay = epochDay,
                        amount = txAmount,
                        accountName = debitName,
                        existingAccountId = null,
                        categoryName = incRef.categoryName,
                        existingCategoryId = incRef.existingCategoryId,
                        subcategory = incRef.subcategory,
                        note = txNote,
                        note2 = txNote2,
                        tagging = rawFromTag,
                        toTagging = explicitCategoryToTag,
                        description = txDesc
                    )
                }

                // Default (including known Expense Category/Ledger with a new payment account) -> Expense
                else -> {
                    newAccounts.putIfAbsent(creditKey, creditName)
                    val expRef = resolveCategory(debitName, existingExpenseCats, existingExpenseLedgers, newExpenseCats)
                    ParsedImportTransaction(
                        type = TransactionType.EXPENSE,
                        epochDay = epochDay,
                        amount = txAmount,
                        accountName = creditName,
                        existingAccountId = null,
                        categoryName = expRef.categoryName,
                        existingCategoryId = expRef.existingCategoryId,
                        subcategory = expRef.subcategory,
                        note = txNote,
                        note2 = txNote2,
                        tagging = txTag,
                        toTagging = explicitCategoryToTag,
                        description = txDesc
                    )
                }
            }

            parsedTransactions.add(parsed)
        }

        if (parsedTransactions.isEmpty()) {
            val detail = warnings.firstOrNull()
                ?: "Expected columns: Date of Transaction, From Account, To Account, Note, Tag, Description, Amount."
            error("No valid transactions found. $detail")
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
