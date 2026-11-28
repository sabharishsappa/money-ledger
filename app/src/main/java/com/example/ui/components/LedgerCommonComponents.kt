package com.example.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary

/**
 * Reusable confirmation dialog for all destructive delete, archive, or remove actions.
 */
@Composable
fun ConfirmDestructiveDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    confirmTestTag: String = "confirm_destructive_button",
    cancelTestTag: String = "cancel_destructive_button",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LedgerSurface,
        title = {
            Text(
                text = title,
                color = LedgerTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = LedgerTextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LedgerCoral,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag(confirmTestTag)
            ) {
                Text(confirmLabel, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(cancelTestTag)
            ) {
                Text("Cancel", color = LedgerTextSecondary)
            }
        }
    )
}

/**
 * Detects a deliberate horizontal swipe left or right to step between view periods
 * without interfering with vertical LazyColumn scrolling.
 * Swipe left (finger moving right-to-left, negative dx) -> next period.
 * Swipe right (finger moving left-to-right, positive dx) -> previous period.
 */
fun Modifier.horizontalPeriodSwipe(
    enabled: Boolean = true,
    threshold: Dp = 64.dp,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit
): Modifier = composed {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentNext by rememberUpdatedState(onSwipeNext)
    val currentPrev by rememberUpdatedState(onSwipePrevious)
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }

    pointerInput(thresholdPx) {
        var totalDrag = 0f
        var triggered = false
        detectHorizontalDragGestures(
            onDragStart = {
                totalDrag = 0f
                triggered = false
            },
            onDragCancel = {
                totalDrag = 0f
                triggered = false
            },
            onDragEnd = {
                totalDrag = 0f
                triggered = false
            },
            onHorizontalDrag = { _, dragAmount ->
                if (!currentEnabled || triggered) return@detectHorizontalDragGestures
                totalDrag += dragAmount
                if (totalDrag <= -thresholdPx) {
                    triggered = true
                    currentNext()
                } else if (totalDrag >= thresholdPx) {
                    triggered = true
                    currentPrev()
                }
            }
        )
    }
}
