package com.example.util

import com.example.data.LedgerTransaction
import com.example.data.TransactionType
import com.example.viewmodel.LedgerDataBundle
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.abs

data class DoubleEntryExportRow(
    val date: String,
    val account: String,
    val note: String,
    val tag: String,
    val description: String,
    val amount: Double
)

object LedgerExcelExporter {

    const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    fun defaultFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "MoneyLedger_$stamp.xlsx"
    }

    /**
     * Converts every ledger transaction into balanced double-entry lines:
     * - Debit line = +amount
     * - Credit line = -amount
     *
     * Rules:
     * - Expense:  Debit = Expense Category (+), Credit = Account (-)
     * - Income:   Debit = Account (+),          Credit = Income Category (-)
     * - Transfer: Debit = To Account (+),       Credit = From Account (-)
     *   (If a transfer has feeAmount > 0, emits an additional Debit "Transfer Fee" (+) / Credit From Account (-) pair.)
     */
    fun buildDoubleEntryRows(bundle: LedgerDataBundle): List<DoubleEntryExportRow> {
        val sortedTx = bundle.transactions.sortedWith(
            compareBy<LedgerTransaction> { it.epochDay }.thenBy { it.timestampMillis }
        )

        val rows = ArrayList<DoubleEntryExportRow>(sortedTx.size * 2)
        sortedTx.forEach { tx ->
            val dateStr = formatExportDate(tx.epochDay)
            val noteStr = tx.note.trim()
            val tagStr = tx.effectiveTagging().orEmpty()
            val descStr = tx.description.orEmpty().trim()
            val amt = abs(tx.amount)

            fun categoryLabel(fallback: String): String {
                val catName = bundle.categoryById(tx.categoryId)?.name?.takeIf { it.isNotBlank() } ?: fallback
                val sub = tx.subcategory?.trim().orEmpty()
                return if (sub.isNotBlank() && bundle.settings.subcategoryEnabled) {
                    "$catName / $sub"
                } else {
                    catName
                }
            }

            when (tx.txType()) {
                TransactionType.EXPENSE -> {
                    val expenseCategory = categoryLabel("Expense")
                    val accountName = bundle.accountName(tx.accountId)
                    // Debit: Expense category (+amount)
                    rows.add(
                        DoubleEntryExportRow(
                            date = dateStr,
                            account = expenseCategory,
                            note = noteStr,
                            tag = tagStr,
                            description = descStr,
                            amount = amt
                        )
                    )
                    // Credit: Account (-amount)
                    rows.add(
                        DoubleEntryExportRow(
                            date = dateStr,
                            account = accountName,
                            note = noteStr,
                            tag = tagStr,
                            description = descStr,
                            amount = -amt
                        )
                    )
                }

                TransactionType.INCOME -> {
                    val accountName = bundle.accountName(tx.accountId)
                    val incomeCategory = categoryLabel("Income")
                    // Debit: Account (+amount)
                    rows.add(
                        DoubleEntryExportRow(
                            date = dateStr,
                            account = accountName,
                            note = noteStr,
                            tag = tagStr,
                            description = descStr,
                            amount = amt
                        )
                    )
                    // Credit: Income category (-amount)
                    rows.add(
                        DoubleEntryExportRow(
                            date = dateStr,
                            account = incomeCategory,
                            note = noteStr,
                            tag = tagStr,
                            description = descStr,
                            amount = -amt
                        )
                    )
                }

                TransactionType.TRANSFER -> {
                    val fromAccount = bundle.accountName(tx.accountId)
                    val toAccount = tx.toAccountId?.let { bundle.accountName(it) } ?: "To Account"
                    // Debit: To Account (+amount)
                    rows.add(
                        DoubleEntryExportRow(
                            date = dateStr,
                            account = toAccount,
                            note = noteStr,
                            tag = tagStr,
                            description = descStr,
                            amount = amt
                        )
                    )
                    // Credit: From Account (-amount)
                    rows.add(
                        DoubleEntryExportRow(
                            date = dateStr,
                            account = fromAccount,
                            note = noteStr,
                            tag = tagStr,
                            description = descStr,
                            amount = -amt
                        )
                    )
                    if (tx.feeAmount > 0.0) {
                        val fee = abs(tx.feeAmount)
                        val feeNote = if (noteStr.isNotEmpty()) "$noteStr (Fee)" else "Transfer Fee"
                        rows.add(
                            DoubleEntryExportRow(
                                date = dateStr,
                                account = "Transfer Fee",
                                note = feeNote,
                                tag = tagStr,
                                description = descStr,
                                amount = fee
                            )
                        )
                        rows.add(
                            DoubleEntryExportRow(
                                date = dateStr,
                                account = fromAccount,
                                note = feeNote,
                                tag = tagStr,
                                description = descStr,
                                amount = -fee
                            )
                        )
                    }
                }
            }
        }
        return rows
    }

    fun buildDoubleEntryCsv(bundle: LedgerDataBundle): String {
        val rows = buildDoubleEntryRows(bundle)
        val sb = StringBuilder()
        sb.appendLine("Date,Account,Note,Tag,Description / Remarks,Amount")
        fun esc(s: String) = "\"${s.replace("\"", "\"\"")}\""
        rows.forEach { row ->
            sb.appendLine(
                listOf(
                    esc(row.date),
                    esc(row.account),
                    esc(row.note),
                    esc(row.tag),
                    esc(row.description),
                    formatPlainNumeric(row.amount)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun generateXlsxBytes(bundle: LedgerDataBundle): ByteArray {
        val baos = ByteArrayOutputStream()
        writeXlsx(bundle, baos)
        return baos.toByteArray()
    }

    fun writeXlsx(bundle: LedgerDataBundle, outputStream: OutputStream) {
        val rows = buildDoubleEntryRows(bundle)
        ZipOutputStream(outputStream).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", CONTENT_TYPES_XML)
            writeZipEntry(zip, "_rels/.rels", ROOT_RELS_XML)
            writeZipEntry(zip, "xl/workbook.xml", WORKBOOK_XML)
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
            writeZipEntry(zip, "xl/styles.xml", STYLES_XML)
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildWorksheetXml(rows))
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun formatExportDate(epochDay: Long): String {
        val cal = LedgerFormatters.epochDayToCalendar(epochDay)
        return SimpleDateFormat("dd/MM/yyyy", Locale.US).format(cal.time)
    }

    private fun formatPlainNumeric(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value)
        }
    }

    private fun buildWorksheetXml(rows: List<DoubleEntryExportRow>): String {
        val totalRows = rows.size + 1
        val sb = StringBuilder(rows.size * 240 + 1024)
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<dimension ref="A1:F$totalRows"/>""")
        sb.append(
            """<sheetViews><sheetView tabSelected="1" workbookViewId="0">""" +
                """<pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>""" +
                """</sheetView></sheetViews>"""
        )
        sb.append("""<sheetFormatPr defaultRowHeight="20"/>""")
        sb.append(
            """<cols>""" +
                """<col min="1" max="1" width="14" customWidth="1"/>""" +
                """<col min="2" max="2" width="26" customWidth="1"/>""" +
                """<col min="3" max="3" width="26" customWidth="1"/>""" +
                """<col min="4" max="4" width="16" customWidth="1"/>""" +
                """<col min="5" max="5" width="32" customWidth="1"/>""" +
                """<col min="6" max="6" width="16" customWidth="1"/>""" +
                """</cols>"""
        )
        sb.append("""<sheetData>""")

        // Row 1: Header
        sb.append("""<row r="1" ht="24" customHeight="1">""")
        appendInlineStrCell(sb, "A1", 1, "Date")
        appendInlineStrCell(sb, "B1", 1, "Account")
        appendInlineStrCell(sb, "C1", 1, "Note")
        appendInlineStrCell(sb, "D1", 1, "Tag")
        appendInlineStrCell(sb, "E1", 1, "Description / Remarks")
        appendInlineStrCell(sb, "F1", 1, "Amount")
        sb.append("""</row>""")

        // Data rows (2 rows per transaction: Debit + and Credit -)
        rows.forEachIndexed { index, row ->
            val r = index + 2
            val amountStyle = if (row.amount >= 0.0) 2 else 3
            sb.append("""<row r="$r">""")
            appendInlineStrCell(sb, "A$r", 0, row.date)
            appendInlineStrCell(sb, "B$r", 0, row.account)
            appendInlineStrCell(sb, "C$r", 0, row.note)
            appendInlineStrCell(sb, "D$r", 0, row.tag)
            appendInlineStrCell(sb, "E$r", 0, row.description)
            sb.append("""<c r="F$r" s="$amountStyle"><v>${formatPlainNumeric(row.amount)}</v></c>""")
            sb.append("""</row>""")
        }

        sb.append("""</sheetData>""")
        sb.append("""<autoFilter ref="A1:F$totalRows"/>""")
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
            """<sheets><sheet name="Ledger Journal" sheetId="1" r:id="rId1"/></sheets>""" +
            """</workbook>"""

    private const val WORKBOOK_RELS_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>""" +
            """<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
            """</Relationships>"""

    private const val STYLES_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""" +
            """<numFmts count="1">""" +
            """<numFmt numFmtId="164" formatCode="#,##0.00;-#,##0.00;0.00"/>""" +
            """</numFmts>""" +
            """<fonts count="4">""" +
            """<font><sz val="11"/><color rgb="FF111827"/><name val="Calibri"/></font>""" +
            """<font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>""" +
            """<font><sz val="11"/><color rgb="FF1D4ED8"/><name val="Calibri"/></font>""" +
            """<font><sz val="11"/><color rgb="FFB91C1C"/><name val="Calibri"/></font>""" +
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
            """<cellXfs count="4">""" +
            """<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"><alignment vertical="center"/></xf>""" +
            """<xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>""" +
            """<xf numFmtId="164" fontId="2" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>""" +
            """<xf numFmtId="164" fontId="3" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>""" +
            """</cellXfs>""" +
            """</styleSheet>"""
}
