package com.example.util

import com.example.data.AccountNature
import com.example.data.CategoryType
import com.example.viewmodel.LedgerDataBundle
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class LedgerUploadKind {
    ACCOUNT,
    EXPENSE_CATEGORY,
    INCOME_CATEGORY,
    EXPENSE_LEDGER,
    INCOME_LEDGER
}

data class ParsedLedgerUploadRow(
    val excelRowNumber: Int,
    val ledgerName: String,
    val groupName: String,
    val kind: LedgerUploadKind,
    val existingGroupId: String?,
    val existingCategoryId: String? = null,
    val inferredNature: AccountNature
)

data class NewGroupProposal(
    val name: String,
    val inferredNature: AccountNature
)

data class LedgerMasterUploadPreview(
    val fileName: String,
    val items: List<ParsedLedgerUploadRow>,
    val newGroups: List<NewGroupProposal>,
    val duplicateNames: List<String>,
    val warnings: List<String>
) {
    val accountCount: Int get() = items.count { it.kind == LedgerUploadKind.ACCOUNT }
    val expenseCategoryCount: Int get() = items.count {
        it.kind == LedgerUploadKind.EXPENSE_CATEGORY || it.kind == LedgerUploadKind.EXPENSE_LEDGER
    }
    val incomeCategoryCount: Int get() = items.count {
        it.kind == LedgerUploadKind.INCOME_CATEGORY || it.kind == LedgerUploadKind.INCOME_LEDGER
    }
}

object LedgerMasterExcelUtil {

    fun templateFileName(): String = "MoneyLedger_Ledgers_Template.xlsx"

    /**
     * Generates a 2-sheet Excel (.xlsx) template:
     * - Sheet 1 ("Ledgers"): Clean 2-column upload template ("Ledger Name", "Group")
     * - Sheet 2 ("Available Groups"): Read-only reference listing existing Account Groups + Expense/Income Categories
     */
    fun writeLedgerTemplateXlsx(bundle: LedgerDataBundle, outputStream: OutputStream) {
        val groupRows = buildList {
            bundle.activeGroups.forEach { grp ->
                val typeLabel = if (grp.nature() == AccountNature.LIABILITY) "Liability Group" else "Asset Group"
                add(grp.name to typeLabel)
            }
            bundle.categories.filter { it.categoryType() == CategoryType.EXPENSE }.forEach { cat ->
                add(cat.name to "Expense Category (adds Expense Ledger)")
            }
            bundle.categories.filter { it.categoryType() == CategoryType.INCOME }.forEach { cat ->
                add(cat.name to "Income Category (adds Income Ledger)")
            }
            add("Expenses" to "New Expense Category")
            add("Income" to "New Income Category")
        }

        ZipOutputStream(outputStream).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", CONTENT_TYPES_XML)
            writeZipEntry(zip, "_rels/.rels", ROOT_RELS_XML)
            writeZipEntry(zip, "xl/workbook.xml", WORKBOOK_XML)
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
            writeZipEntry(zip, "xl/styles.xml", STYLES_XML)
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildLedgersTemplateSheetXml())
            writeZipEntry(zip, "xl/worksheets/sheet2.xml", buildAvailableGroupsSheetXml(groupRows))
        }
    }

    fun parseStream(
        inputStream: InputStream,
        fileName: String,
        bundle: LedgerDataBundle
    ): Result<LedgerMasterUploadPreview> = runCatching {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) {
            error("The selected file is empty.")
        }

        val grid = LedgerExcelImporter.readSpreadsheetGrid(bytes)
        if (grid.isEmpty()) {
            error("No rows found. Expected columns: Ledger Name, Group.")
        }

        var nameCol = 0
        var groupCol = 1
        var natureCol: Int? = null
        var startIdx = 0

        val firstCells = grid.first().second
        val normalizedHeader = firstCells.mapValues { it.value.trim().lowercase(Locale.US) }
        val looksLikeHeader = normalizedHeader.values.any { v ->
            v.contains("ledger") || v == "name" || v == "account" || v == "account name" || v == "group" || v == "group name"
        }

        if (looksLikeHeader) {
            normalizedHeader.forEach { (col, text) ->
                when {
                    text == "ledger name" || text == "ledger" || text == "account name" || text == "account" || text == "name" -> {
                        nameCol = col
                    }
                    text == "group" || text == "group name" || text == "ledger group" || text == "account group" || text == "under" -> {
                        groupCol = col
                    }
                    text == "type" || text == "nature" || text == "group type" -> {
                        natureCol = col
                    }
                }
            }
            startIdx = 1
        }

        val existingGroupsByLower = bundle.accountGroups
            .sortedBy { it.archived }
            .associateBy { it.name.trim().lowercase(Locale.US) }

        val existingExpenseCategoriesByLower = bundle.categories
            .filter { it.categoryType() == CategoryType.EXPENSE }
            .associateBy { it.name.trim().lowercase(Locale.US) }
        val existingIncomeCategoriesByLower = bundle.categories
            .filter { it.categoryType() == CategoryType.INCOME }
            .associateBy { it.name.trim().lowercase(Locale.US) }

        val existingAccountNames = bundle.accounts
            .map { it.name.trim().lowercase(Locale.US) }
            .toMutableSet()
        val existingExpenseCatNames = existingExpenseCategoriesByLower.keys.toMutableSet()
        val existingIncomeCatNames = existingIncomeCategoriesByLower.keys.toMutableSet()
        val existingCategoryLedgerKeys = bundle.categories.flatMap { cat ->
            cat.subcategories.map { sub -> "${cat.id}:${sub.trim().lowercase(Locale.US)}" }
        }.toMutableSet()

        val items = mutableListOf<ParsedLedgerUploadRow>()
        val newGroupsMap = LinkedHashMap<String, NewGroupProposal>()
        val duplicates = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        for (idx in startIdx until grid.size) {
            val (rowNum, cells) = grid[idx]
            val rawName = cells[nameCol].orEmpty().trim()
            val rawGroup = cells[groupCol].orEmpty().trim()
            val rawNature = natureCol?.let { cells[it].orEmpty().trim() }.orEmpty()

            if (rawName.isEmpty() && rawGroup.isEmpty()) continue
            if (rawName.isEmpty()) {
                warnings.add("Row $rowNum: missing Ledger Name — skipped.")
                continue
            }
            if (rawGroup.isEmpty()) {
                warnings.add("Row $rowNum ($rawName): missing Group — skipped.")
                continue
            }

            val nameKey = rawName.lowercase(Locale.US)
            val groupKey = rawGroup.lowercase(Locale.US)

            val matchedAccGroup = existingGroupsByLower[groupKey]
            val matchedExpenseCat = if (matchedAccGroup == null) existingExpenseCategoriesByLower[groupKey] else null
            val matchedIncomeCat = if (matchedAccGroup == null && matchedExpenseCat == null) {
                existingIncomeCategoriesByLower[groupKey]
            } else null

            when {
                isExpenseGroupAlias(groupKey) -> {
                    val baseName = rawName.substringBefore(" / ").trim()
                    val baseKey = baseName.lowercase(Locale.US)
                    if (existingExpenseCatNames.contains(baseKey)) {
                        duplicates.add("$rawName ($rawGroup)")
                    } else {
                        existingExpenseCatNames.add(baseKey)
                        items.add(
                            ParsedLedgerUploadRow(
                                excelRowNumber = rowNum,
                                ledgerName = baseName.take(60),
                                groupName = "Expenses",
                                kind = LedgerUploadKind.EXPENSE_CATEGORY,
                                existingGroupId = null,
                                inferredNature = AccountNature.ASSET
                            )
                        )
                    }
                }

                isIncomeGroupAlias(groupKey) -> {
                    val baseName = rawName.substringBefore(" / ").trim()
                    val baseKey = baseName.lowercase(Locale.US)
                    if (existingIncomeCatNames.contains(baseKey)) {
                        duplicates.add("$rawName ($rawGroup)")
                    } else {
                        existingIncomeCatNames.add(baseKey)
                        items.add(
                            ParsedLedgerUploadRow(
                                excelRowNumber = rowNum,
                                ledgerName = baseName.take(60),
                                groupName = "Income",
                                kind = LedgerUploadKind.INCOME_CATEGORY,
                                existingGroupId = null,
                                inferredNature = AccountNature.ASSET
                            )
                        )
                    }
                }

                matchedExpenseCat != null -> {
                    val ledgerKey = "${matchedExpenseCat.id}:$nameKey"
                    if (existingCategoryLedgerKeys.contains(ledgerKey)) {
                        duplicates.add("$rawName (${matchedExpenseCat.name})")
                    } else {
                        existingCategoryLedgerKeys.add(ledgerKey)
                        items.add(
                            ParsedLedgerUploadRow(
                                excelRowNumber = rowNum,
                                ledgerName = rawName.take(60),
                                groupName = matchedExpenseCat.name,
                                kind = LedgerUploadKind.EXPENSE_LEDGER,
                                existingGroupId = null,
                                existingCategoryId = matchedExpenseCat.id,
                                inferredNature = AccountNature.ASSET
                            )
                        )
                    }
                }

                matchedIncomeCat != null -> {
                    val ledgerKey = "${matchedIncomeCat.id}:$nameKey"
                    if (existingCategoryLedgerKeys.contains(ledgerKey)) {
                        duplicates.add("$rawName (${matchedIncomeCat.name})")
                    } else {
                        existingCategoryLedgerKeys.add(ledgerKey)
                        items.add(
                            ParsedLedgerUploadRow(
                                excelRowNumber = rowNum,
                                ledgerName = rawName.take(60),
                                groupName = matchedIncomeCat.name,
                                kind = LedgerUploadKind.INCOME_LEDGER,
                                existingGroupId = null,
                                existingCategoryId = matchedIncomeCat.id,
                                inferredNature = AccountNature.ASSET
                            )
                        )
                    }
                }

                else -> {
                    if (existingAccountNames.contains(nameKey)) {
                        duplicates.add("$rawName ($rawGroup)")
                        continue
                    }
                    existingAccountNames.add(nameKey)

                    val nature = matchedAccGroup?.nature()
                        ?: parseExplicitNature(rawNature)
                        ?: inferGroupNature(rawGroup)

                    if (matchedAccGroup == null) {
                        newGroupsMap.putIfAbsent(
                            groupKey,
                            NewGroupProposal(name = rawGroup.take(60), inferredNature = nature)
                        )
                    }

                    items.add(
                        ParsedLedgerUploadRow(
                            excelRowNumber = rowNum,
                            ledgerName = rawName.take(80),
                            groupName = matchedAccGroup?.name ?: rawGroup.take(60),
                            kind = LedgerUploadKind.ACCOUNT,
                            existingGroupId = matchedAccGroup?.id,
                            inferredNature = nature
                        )
                    )
                }
            }
        }

        if (items.isEmpty()) {
            val reason = when {
                duplicates.isNotEmpty() -> "All ${duplicates.size} ledger(s) in this file already exist."
                warnings.isNotEmpty() -> warnings.first()
                else -> "Ensure the sheet has Ledger Name and Group columns with at least one ledger row."
            }
            error("No new ledgers to upload. $reason")
        }

        LedgerMasterUploadPreview(
            fileName = fileName,
            items = items,
            newGroups = newGroupsMap.values.toList(),
            duplicateNames = duplicates,
            warnings = warnings
        )
    }

    private fun isExpenseGroupAlias(groupLower: String): Boolean {
        return groupLower in setOf(
            "expense",
            "expenses",
            "expense category",
            "expense categories",
            "direct expense",
            "direct expenses",
            "indirect expense",
            "indirect expenses"
        )
    }

    private fun isIncomeGroupAlias(groupLower: String): Boolean {
        return groupLower in setOf(
            "income",
            "incomes",
            "income category",
            "income categories",
            "direct income",
            "direct incomes",
            "indirect income",
            "indirect incomes"
        )
    }

    private fun parseExplicitNature(raw: String): AccountNature? {
        val clean = raw.trim().lowercase(Locale.US)
        return when {
            clean.startsWith("liab") -> AccountNature.LIABILITY
            clean.startsWith("asset") -> AccountNature.ASSET
            else -> null
        }
    }

    private fun inferGroupNature(groupName: String): AccountNature {
        val lower = groupName.lowercase(Locale.US)
        val liabilityKeywords = listOf(
            "liab", "credit", "card", "loan", "debt", "payable",
            "mortgage", "emi", "overdraft", "borrowing", "due", "creditor"
        )
        return if (liabilityKeywords.any { lower.contains(it) }) {
            AccountNature.LIABILITY
        } else {
            AccountNature.ASSET
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildLedgersTemplateSheetXml(): String {
        val sb = StringBuilder(1024)
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<dimension ref="A1:B1"/>""")
        sb.append(
            """<sheetViews><sheetView tabSelected="1" workbookViewId="0">""" +
                """<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>""" +
                """</sheetView></sheetViews>"""
        )
        sb.append("""<sheetFormatPr defaultRowHeight="20"/>""")
        sb.append(
            """<cols>""" +
                """<col min="1" max="1" width="32" customWidth="1"/>""" +
                """<col min="2" max="2" width="28" customWidth="1"/>""" +
                """</cols>"""
        )
        sb.append("""<sheetData>""")
        sb.append("""<row r="1" ht="24" customHeight="1">""")
        appendInlineStrCell(sb, "A1", 1, "Ledger Name")
        appendInlineStrCell(sb, "B1", 1, "Group")
        sb.append("""</row>""")
        sb.append("""</sheetData>""")
        sb.append("""<autoFilter ref="A1:B1"/>""")
        sb.append("""</worksheet>""")
        return sb.toString()
    }

    private fun buildAvailableGroupsSheetXml(rows: List<Pair<String, String>>): String {
        val totalRows = rows.size + 1
        val sb = StringBuilder(rows.size * 180 + 1024)
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<dimension ref="A1:B$totalRows"/>""")
        sb.append(
            """<sheetViews><sheetView workbookViewId="0">""" +
                """<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>""" +
                """</sheetView></sheetViews>"""
        )
        sb.append("""<sheetFormatPr defaultRowHeight="20"/>""")
        sb.append(
            """<cols>""" +
                """<col min="1" max="1" width="28" customWidth="1"/>""" +
                """<col min="2" max="2" width="24" customWidth="1"/>""" +
                """</cols>"""
        )
        sb.append("""<sheetData>""")
        sb.append("""<row r="1" ht="24" customHeight="1">""")
        appendInlineStrCell(sb, "A1", 1, "Group Name")
        appendInlineStrCell(sb, "B1", 1, "Type")
        sb.append("""</row>""")
        rows.forEachIndexed { idx, (name, typeLabel) ->
            val r = idx + 2
            sb.append("""<row r="$r">""")
            appendInlineStrCell(sb, "A$r", 0, name)
            appendInlineStrCell(sb, "B$r", 0, typeLabel)
            sb.append("""</row>""")
        }
        sb.append("""</sheetData>""")
        sb.append("""</worksheet>""")
        return sb.toString()
    }

    private fun appendInlineStrCell(sb: StringBuilder, ref: String, styleIndex: Int, value: String) {
        sb.append("""<c r="$ref" s="$styleIndex" t="inlineStr"><is><t xml:space="preserve">""")
        sb.append(escapeXml(value))
        sb.append("""</t></is></c>""")
    }

    private fun escapeXml(input: String): String {
        val out = StringBuilder(input.length + 8)
        for (ch in input) {
            when (ch) {
                '&' -> out.append("&amp;")
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                '"' -> out.append("&quot;")
                '\'' -> out.append("&apos;")
                '\t', '\n', '\r' -> out.append(ch)
                else -> if (ch.code >= 0x20) out.append(ch)
            }
        }
        return out.toString()
    }

    private const val CONTENT_TYPES_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
            """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
            """<Default Extension="xml" ContentType="application/xml"/>""" +
            """<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
            """<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
            """<Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" +
            """<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
            """</Types>"""

    private const val ROOT_RELS_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""" +
            """</Relationships>"""

    private const val WORKBOOK_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""" +
            """<sheets>""" +
            """<sheet name="Ledgers" sheetId="1" r:id="rId1"/>""" +
            """<sheet name="Available Groups" sheetId="2" r:id="rId2"/>""" +
            """</sheets>""" +
            """</workbook>"""

    private const val WORKBOOK_RELS_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
            """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>""" +
            """<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
            """</Relationships>"""

    private const val STYLES_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
            """<fonts count="2">""" +
            """<font><sz val="11"/><color rgb="FF111827"/><name val="Calibri"/></font>""" +
            """<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>""" +
            """</fonts>""" +
            """<fills count="3">""" +
            """<fill><patternFill patternType="none"/></fill>""" +
            """<fill><patternFill patternType="gray125"/></fill>""" +
            """<fill><patternFill patternType="solid"><fgColor rgb="FF1F2937"/><bgColor indexed="64"/></patternFill></fill>""" +
            """</fills>""" +
            """<borders count="2">""" +
            """<border><left/><right/><top/><bottom/><diagonal/></border>""" +
            """<border><left/><right/><top/><bottom style="thin"><color rgb="FFE5E7EB"/></bottom><diagonal/></border>""" +
            """</borders>""" +
            """<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>""" +
            """<cellXfs count="2">""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"><alignment vertical="center"/></xf>""" +
            """<xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>""" +
            """</cellXfs>""" +
            """</styleSheet>"""
}
