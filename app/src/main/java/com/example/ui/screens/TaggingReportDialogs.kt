package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerDivider
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerSurfaceElevated
import com.example.ui.theme.LedgerTextMuted
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary
import com.example.util.LedgerFormatters
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToLong

internal const val UNTAGGED_KEY = "__UNTAGGED__"

internal enum class TagReportDatePreset(val label: String) {
    ALL("All Dates"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom")
}

internal fun resolveDatePresetBounds(
    preset: TagReportDatePreset,
    customStart: Long,
    customEnd: Long
): Pair<Long?, Long?> {
    val today = LedgerFormatters.todayEpochDay()
    val cal = LedgerFormatters.epochDayToCalendar(today)
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    return when (preset) {
        TagReportDatePreset.ALL -> null to null
        TagReportDatePreset.THIS_MONTH -> {
            val s = LedgerFormatters.calendarToEpochDay(year, month, 1)
            val e = LedgerFormatters.calendarToEpochDay(year, month, LedgerFormatters.daysInMonth(year, month))
            s to e
        }
        TagReportDatePreset.LAST_MONTH -> {
            val prevCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, -1)
            }
            val py = prevCal.get(Calendar.YEAR)
            val pm = prevCal.get(Calendar.MONTH)
            val s = LedgerFormatters.calendarToEpochDay(py, pm, 1)
            val e = LedgerFormatters.calendarToEpochDay(py, pm, LedgerFormatters.daysInMonth(py, pm))
            s to e
        }
        TagReportDatePreset.LAST_30_DAYS -> (today - 29L) to today
        TagReportDatePreset.THIS_YEAR -> {
            val s = LedgerFormatters.calendarToEpochDay(year, Calendar.JANUARY, 1)
            val e = LedgerFormatters.calendarToEpochDay(year, Calendar.DECEMBER, 31)
            s to e
        }
        TagReportDatePreset.CUSTOM -> {
            val s = minOf(customStart, customEnd)
            val e = maxOf(customStart, customEnd)
            s to e
        }
    }
}

internal fun formatDateChipLabel(
    preset: TagReportDatePreset,
    customStart: Long,
    customEnd: Long
): String {
    return if (preset == TagReportDatePreset.CUSTOM) {
        val s = minOf(customStart, customEnd)
        val e = maxOf(customStart, customEnd)
        LedgerFormatters.weekRangeLabel(s, e)
    } else {
        preset.label
    }
}

internal fun formatSignedLedgerAmount(value: Double, symbol: String, showPlus: Boolean): String {
    val rounded = value.roundToLong()
    val base = LedgerFormatters.formatWholeAmount(abs(value), symbol)
    return when {
        rounded < 0L -> "-$base"
        rounded > 0L && showPlus -> "+$base"
        else -> base
    }
}

@Composable
internal fun ReportFilterPill(
    icon: ImageVector,
    label: String,
    active: Boolean,
    accentColor: Color,
    showDropdownChevron: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (active) accentColor.copy(alpha = 0.16f) else LedgerSurfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (active) accentColor.copy(alpha = 0.65f) else LedgerBorder
        ),
        modifier = modifier.heightIn(min = 38.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) accentColor else LedgerTextSecondary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                color = if (active) accentColor else LedgerTextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            if (showDropdownChevron) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (active) accentColor else LedgerTextMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun DateRangeFilterDialog(
    currentPreset: TagReportDatePreset,
    currentCustomStart: Long,
    currentCustomEnd: Long,
    onDismiss: () -> Unit,
    onApply: (TagReportDatePreset, Long, Long) -> Unit
) {
    var draftPreset by remember(currentPreset) { mutableStateOf(currentPreset) }
    var draftStart by remember(currentCustomStart) { mutableStateOf(currentCustomStart) }
    var draftEnd by remember(currentCustomEnd) { mutableStateOf(currentCustomEnd) }
    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LedgerSurface,
        title = {
            Text(
                text = "Filter by Date",
                color = LedgerTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TagReportDatePreset.entries.forEach { preset ->
                        val active = draftPreset == preset
                        Surface(
                            onClick = { draftPreset = preset },
                            shape = RoundedCornerShape(8.dp),
                            color = if (active) LedgerCoral.copy(alpha = 0.16f) else LedgerSurfaceElevated,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (active) LedgerCoral else LedgerBorder
                            ),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("date_preset_${preset.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (active) LedgerCoral else LedgerTextPrimary
                                )
                            }
                        }
                    }
                }

                if (draftPreset == TagReportDatePreset.CUSTOM) {
                    HorizontalDivider(color = LedgerDivider)
                    DatePickerFieldRow(
                        label = "From Date",
                        value = LedgerFormatters.formatShortDate(draftStart),
                        onClick = { pickingStartDate = true },
                        modifier = Modifier.testTag("date_filter_from_button")
                    )
                    DatePickerFieldRow(
                        label = "To Date",
                        value = LedgerFormatters.formatShortDate(draftEnd),
                        onClick = { pickingEndDate = true },
                        modifier = Modifier.testTag("date_filter_to_button")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(draftPreset, draftStart, draftEnd) },
                colors = ButtonDefaults.buttonColors(containerColor = LedgerCoral),
                modifier = Modifier.testTag("tag_report_date_apply_button")
            ) {
                Text("Apply", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LedgerTextSecondary)
            }
        }
    )

    if (pickingStartDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LedgerFormatters.epochDayToMillis(draftStart)
        )
        DatePickerDialog(
            onDismissRequest = { pickingStartDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            draftStart = millis / 86_400_000L
                            if (draftStart > draftEnd) draftEnd = draftStart
                        }
                        pickingStartDate = false
                    }
                ) { Text("OK", color = LedgerCoral) }
            },
            dismissButton = {
                TextButton(onClick = { pickingStartDate = false }) {
                    Text("Cancel", color = LedgerTextSecondary)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (pickingEndDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = LedgerFormatters.epochDayToMillis(draftEnd)
        )
        DatePickerDialog(
            onDismissRequest = { pickingEndDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            draftEnd = millis / 86_400_000L
                            if (draftEnd < draftStart) draftStart = draftEnd
                        }
                        pickingEndDate = false
                    }
                ) { Text("OK", color = LedgerCoral) }
            },
            dismissButton = {
                TextButton(onClick = { pickingEndDate = false }) {
                    Text("Cancel", color = LedgerTextSecondary)
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DatePickerFieldRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = LedgerSurfaceElevated,
        border = BorderStroke(1.dp, LedgerBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = LedgerTextSecondary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LedgerTextPrimary
                )
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = "Pick $label",
                    tint = LedgerCoral,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun TagMultiSelectFilterDialog(
    availableTags: List<String>,
    hasUntaggedEntries: Boolean,
    entryCountsByKey: Map<String, Int>,
    initialSelectedKeys: Set<String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit
) {
    var draftKeys by remember(initialSelectedKeys) { mutableStateOf(initialSelectedKeys) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LedgerSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter by Tag",
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                if (draftKeys.isNotEmpty()) {
                    TextButton(
                        onClick = { draftKeys = emptySet() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Show All", color = LedgerBlue, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        text = {
            if (availableTags.isEmpty() && !hasUntaggedEntries) {
                Text(
                    text = "No tags found yet. Add a tag while recording transactions to filter by tag.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LedgerTextSecondary
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (draftKeys.isEmpty()) {
                            "Showing all tagged entries (tick tags below to narrow down)"
                        } else {
                            "${draftKeys.size} ${if (draftKeys.size == 1) "tag" else "tags"} selected"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = LedgerTextMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    availableTags.forEach { tag ->
                        val key = tag.lowercase()
                        val checked = key in draftKeys
                        val count = entryCountsByKey[key] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    draftKeys = if (checked) draftKeys - key else draftKeys + key
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .testTag("tag_filter_option_$key"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    draftKeys = if (isChecked) draftKeys + key else draftKeys - key
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = LedgerBlue,
                                    uncheckedColor = LedgerTextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                                color = LedgerTextPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                color = LedgerTextMuted,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                    if (hasUntaggedEntries) {
                        HorizontalDivider(
                            color = LedgerDivider,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        val checked = UNTAGGED_KEY in draftKeys
                        val count = entryCountsByKey[UNTAGGED_KEY] ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    draftKeys = if (checked) draftKeys - UNTAGGED_KEY else draftKeys + UNTAGGED_KEY
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .testTag("tag_filter_option_untagged"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    draftKeys = if (isChecked) draftKeys + UNTAGGED_KEY else draftKeys - UNTAGGED_KEY
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = LedgerBlue,
                                    uncheckedColor = LedgerTextMuted
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Untagged",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LedgerTextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                color = LedgerTextMuted,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(draftKeys) },
                colors = ButtonDefaults.buttonColors(containerColor = LedgerBlue),
                modifier = Modifier.testTag("tag_report_tag_apply_button")
            ) {
                Text("Apply", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LedgerTextSecondary)
            }
        }
    )
}
