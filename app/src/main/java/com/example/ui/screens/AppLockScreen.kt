package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LedgerAmber
import com.example.ui.theme.LedgerBackground
import com.example.ui.theme.LedgerBlue
import com.example.ui.theme.LedgerBorder
import com.example.ui.theme.LedgerCoral
import com.example.ui.theme.LedgerDivider
import com.example.ui.theme.LedgerGreen
import com.example.ui.theme.LedgerSurface
import com.example.ui.theme.LedgerSurfaceElevated
import com.example.ui.theme.LedgerSurfaceHighlight
import com.example.ui.theme.LedgerTextMuted
import com.example.ui.theme.LedgerTextPrimary
import com.example.ui.theme.LedgerTextSecondary
import com.example.util.AppLockManager
import com.example.util.PinVerifyResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class PinFlowMode {
    ENABLE_NEW,
    DISABLE_VERIFY,
    CHANGE_PIN
}

private enum class PinFlowStep {
    VERIFY_CURRENT,
    ENTER_NEW,
    CONFIRM_NEW
}

@Composable
fun PinLockGateScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Consume system back button so the PIN gate cannot be bypassed
    BackHandler(enabled = true) { /* intentionally blocked while locked */ }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var lockoutSeconds by remember { mutableLongStateOf(AppLockManager.remainingLockoutSeconds(context)) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0L) {
            delay(1000L)
            lockoutSeconds = AppLockManager.remainingLockoutSeconds(context)
            if (lockoutSeconds == 0L) {
                errorMessage = null
            }
        }
    }

    fun triggerErrorShake() {
        scope.launch {
            listOf(-18f, 18f, -12f, 12f, -6f, 6f, 0f).forEach { target ->
                shakeOffset.animateTo(target, animationSpec = tween(36))
            }
        }
    }

    fun handleDigit(digit: Char) {
        if (isVerifying || lockoutSeconds > 0L || enteredPin.length >= 4) return
        val next = enteredPin + digit
        enteredPin = next
        errorMessage = null
        if (next.length == 4) {
            isVerifying = true
            scope.launch {
                val result = withContext(Dispatchers.Default) {
                    AppLockManager.verifyPin(context, next)
                }
                isVerifying = false
                when (result) {
                    PinVerifyResult.Success -> {
                        enteredPin = ""
                        onUnlocked()
                    }
                    is PinVerifyResult.Invalid -> {
                        enteredPin = ""
                        errorMessage = "Incorrect PIN • ${result.attemptsRemaining} ${if (result.attemptsRemaining == 1) "attempt" else "attempts"} left"
                        triggerErrorShake()
                    }
                    is PinVerifyResult.LockedOut -> {
                        enteredPin = ""
                        lockoutSeconds = result.remainingSeconds
                        errorMessage = "Too many attempts. Try again in ${result.remainingSeconds}s"
                        triggerErrorShake()
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("pin_lock_gate_screen"),
        contentAlignment = Alignment.Center
    ) {
        PinPadScaffold(
            icon = Icons.Filled.Lock,
            iconTint = LedgerCoral,
            title = "Money Ledger Locked",
            subtitle = if (lockoutSeconds > 0L) {
                "Locked for ${lockoutSeconds}s due to failed attempts"
            } else {
                "Enter your 4-digit PIN to continue"
            },
            pinLength = enteredPin.length,
            errorMessage = if (lockoutSeconds > 0L) {
                "Too many attempts. Try again in ${lockoutSeconds}s"
            } else errorMessage,
            shakeOffsetPx = shakeOffset.value,
            enabled = !isVerifying && lockoutSeconds == 0L,
            onDigit = ::handleDigit,
            onBackspace = {
                if (enteredPin.isNotEmpty() && !isVerifying) {
                    enteredPin = enteredPin.dropLast(1)
                    errorMessage = null
                }
            },
            onClear = {
                if (!isVerifying) {
                    enteredPin = ""
                    errorMessage = null
                }
            }
        )
    }
}

@Composable
fun AppLockSettingsScreen(
    isEnabled: Boolean,
    onBack: () -> Unit,
    onLockStateChanged: (enabled: Boolean, message: String) -> Unit,
    onLockNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeFlow by remember { mutableStateOf<PinFlowMode?>(null) }
    var delaySeconds by remember { mutableIntStateOf(AppLockManager.autoLockDelaySeconds(context)) }
    var showDelayDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (activeFlow != null) {
            activeFlow = null
        } else {
            onBack()
        }
    }

    val delayLabel = when (delaySeconds) {
        0 -> "Immediately"
        30 -> "After 30 seconds"
        60 -> "After 1 minute"
        else -> "After ${delaySeconds}s"
    }

    AnimatedContent(
        targetState = activeFlow,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(130)) },
        modifier = modifier
            .fillMaxSize()
            .background(LedgerBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        label = "app_lock_settings_flow"
    ) { flow ->
        if (flow != null) {
            PinSetupFlowScreen(
                mode = flow,
                onCancel = { activeFlow = null },
                onCompleted = { nowEnabled, msg ->
                    activeFlow = null
                    onLockStateChanged(nowEnabled, msg)
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LedgerBackground)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LedgerSurface)
                        .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("app_lock_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LedgerTextPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 2.dp)) {
                        Text(
                            text = "App Lock",
                            style = MaterialTheme.typography.titleLarge,
                            color = LedgerTextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "4-digit PIN security",
                            style = MaterialTheme.typography.labelSmall,
                            color = LedgerTextSecondary,
                            maxLines = 1
                        )
                    }
                }
                HorizontalDivider(color = LedgerDivider)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        // Primary Enable / Disable Toggle Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = LedgerSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isEnabled) LedgerGreen.copy(alpha = 0.4f) else LedgerDivider
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeFlow = if (isEnabled) {
                                            PinFlowMode.DISABLE_VERIFY
                                        } else {
                                            PinFlowMode.ENABLE_NEW
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            (if (isEnabled) LedgerGreen else LedgerTextMuted).copy(alpha = 0.14f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isEnabled) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                        contentDescription = null,
                                        tint = if (isEnabled) LedgerGreen else LedgerTextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = "4-Digit PIN Lock",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LedgerTextPrimary
                                    )
                                    Text(
                                        text = if (isEnabled) {
                                            "Active • PIN required when opening the app"
                                        } else {
                                            "Off • Protect your ledger with a 4-digit PIN"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LedgerTextSecondary
                                    )
                                }
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { wantEnable ->
                                        activeFlow = if (wantEnable) {
                                            PinFlowMode.ENABLE_NEW
                                        } else {
                                            PinFlowMode.DISABLE_VERIFY
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = LedgerGreen,
                                        uncheckedThumbColor = LedgerTextSecondary,
                                        uncheckedTrackColor = LedgerSurfaceHighlight
                                    ),
                                    modifier = Modifier.testTag("app_lock_toggle_switch")
                                )
                            }
                        }

                        if (isEnabled) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LedgerSurface)
                            ) {
                                HorizontalDivider(color = LedgerDivider)
                                AppLockOptionRow(
                                    icon = Icons.Filled.Pin,
                                    iconTint = LedgerBlue,
                                    title = "Change 4-Digit PIN",
                                    subtitle = "Verify current PIN and set a new one",
                                    trailingValue = null,
                                    onClick = { activeFlow = PinFlowMode.CHANGE_PIN },
                                    modifier = Modifier.testTag("app_lock_change_pin_row")
                                )
                                HorizontalDivider(
                                    color = LedgerDivider,
                                    modifier = Modifier.padding(start = 66.dp)
                                )
                                AppLockOptionRow(
                                    icon = Icons.Filled.Schedule,
                                    iconTint = LedgerAmber,
                                    title = "Auto-Lock Timing",
                                    subtitle = "Lock automatically after leaving the app",
                                    trailingValue = delayLabel,
                                    onClick = { showDelayDialog = true },
                                    modifier = Modifier.testTag("app_lock_timeout_row")
                                )
                                HorizontalDivider(
                                    color = LedgerDivider,
                                    modifier = Modifier.padding(start = 66.dp)
                                )
                                AppLockOptionRow(
                                    icon = Icons.Filled.Lock,
                                    iconTint = LedgerCoral,
                                    title = "Lock App Now",
                                    subtitle = "Immediately lock and require PIN",
                                    trailingValue = null,
                                    onClick = onLockNow,
                                    modifier = Modifier.testTag("app_lock_lock_now_row")
                                )
                                HorizontalDivider(color = LedgerDivider)
                            }
                        }

                        Text(
                            text = "Your PIN is stored locally using a salted cryptographic hash and is never saved in plain text. After 5 failed attempts, entry pauses briefly for 30 seconds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LedgerTextMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDelayDialog) {
        val options = listOf(
            0 to "Immediately",
            30 to "After 30 seconds",
            60 to "After 1 minute"
        )
        AlertDialog(
            onDismissRequest = { showDelayDialog = false },
            containerColor = LedgerSurface,
            title = {
                Text(
                    text = "Auto-Lock Timing",
                    color = LedgerTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    options.forEach { (sec, label) ->
                        val selected = delaySeconds == sec
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    delaySeconds = sec
                                    AppLockManager.setAutoLockDelaySeconds(context, sec)
                                    showDelayDialog = false
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .testTag("app_lock_delay_$sec"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selected) LedgerCoral else LedgerTextPrimary,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = LedgerCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDelayDialog = false }) {
                    Text("Close", color = LedgerTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PinSetupFlowScreen(
    mode: PinFlowMode,
    onCancel: () -> Unit,
    onCompleted: (enabled: Boolean, message: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember(mode) {
        mutableStateOf(
            when (mode) {
                PinFlowMode.ENABLE_NEW -> PinFlowStep.ENTER_NEW
                PinFlowMode.DISABLE_VERIFY, PinFlowMode.CHANGE_PIN -> PinFlowStep.VERIFY_CURRENT
            }
        )
    }
    var verifiedCurrentPin by remember(mode) { mutableStateOf("") }
    var draftNewPin by remember(mode) { mutableStateOf("") }
    var enteredPin by remember(mode) { mutableStateOf("") }
    var errorMessage by remember(mode) { mutableStateOf<String?>(null) }
    var isProcessing by remember(mode) { mutableStateOf(false) }
    var lockoutSeconds by remember(mode) { mutableLongStateOf(AppLockManager.remainingLockoutSeconds(context)) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0L) {
            delay(1000L)
            lockoutSeconds = AppLockManager.remainingLockoutSeconds(context)
            if (lockoutSeconds == 0L) {
                errorMessage = null
            }
        }
    }

    fun triggerErrorShake() {
        scope.launch {
            listOf(-18f, 18f, -12f, 12f, -6f, 6f, 0f).forEach { target ->
                shakeOffset.animateTo(target, animationSpec = tween(36))
            }
        }
    }

    fun handleDigit(digit: Char) {
        if (isProcessing || (step == PinFlowStep.VERIFY_CURRENT && lockoutSeconds > 0L) || enteredPin.length >= 4) {
            return
        }
        val next = enteredPin + digit
        enteredPin = next
        errorMessage = null
        if (next.length < 4) return

        when (step) {
            PinFlowStep.VERIFY_CURRENT -> {
                isProcessing = true
                scope.launch {
                    val result = withContext(Dispatchers.Default) {
                        if (mode == PinFlowMode.DISABLE_VERIFY) {
                            AppLockManager.disableAppLock(context, next)
                        } else {
                            AppLockManager.verifyPin(context, next)
                        }
                    }
                    isProcessing = false
                    when (result) {
                        PinVerifyResult.Success -> {
                            enteredPin = ""
                            if (mode == PinFlowMode.DISABLE_VERIFY) {
                                onCompleted(false, "App Lock disabled")
                            } else {
                                verifiedCurrentPin = next
                                step = PinFlowStep.ENTER_NEW
                            }
                        }
                        is PinVerifyResult.Invalid -> {
                            enteredPin = ""
                            errorMessage = "Incorrect PIN • ${result.attemptsRemaining} ${if (result.attemptsRemaining == 1) "attempt" else "attempts"} left"
                            triggerErrorShake()
                        }
                        is PinVerifyResult.LockedOut -> {
                            enteredPin = ""
                            lockoutSeconds = result.remainingSeconds
                            errorMessage = "Too many attempts. Try again in ${result.remainingSeconds}s"
                            triggerErrorShake()
                        }
                    }
                }
            }

            PinFlowStep.ENTER_NEW -> {
                draftNewPin = next
                enteredPin = ""
                step = PinFlowStep.CONFIRM_NEW
            }

            PinFlowStep.CONFIRM_NEW -> {
                if (next != draftNewPin) {
                    enteredPin = ""
                    draftNewPin = ""
                    step = PinFlowStep.ENTER_NEW
                    errorMessage = "PINs didn't match. Please enter a new 4-digit PIN."
                    triggerErrorShake()
                    return
                }
                isProcessing = true
                scope.launch {
                    val ok = withContext(Dispatchers.Default) {
                        if (mode == PinFlowMode.CHANGE_PIN) {
                            AppLockManager.changePin(context, verifiedCurrentPin, next) is PinVerifyResult.Success
                        } else {
                            AppLockManager.setPin(context, next)
                        }
                    }
                    isProcessing = false
                    enteredPin = ""
                    draftNewPin = ""
                    verifiedCurrentPin = ""
                    if (ok) {
                        val msg = if (mode == PinFlowMode.CHANGE_PIN) {
                            "4-digit PIN updated"
                        } else {
                            "App Lock enabled"
                        }
                        onCompleted(true, msg)
                    } else {
                        errorMessage = "Could not save PIN. Please try again."
                        step = PinFlowStep.ENTER_NEW
                        triggerErrorShake()
                    }
                }
            }
        }
    }

    val (title, subtitle) = when (step) {
        PinFlowStep.VERIFY_CURRENT -> {
            val sub = if (mode == PinFlowMode.DISABLE_VERIFY) {
                "Enter your current 4-digit PIN to disable App Lock"
            } else {
                "Enter your current 4-digit PIN first"
            }
            "Verify Current PIN" to sub
        }
        PinFlowStep.ENTER_NEW -> {
            val titleText = if (mode == PinFlowMode.CHANGE_PIN) "Enter New PIN" else "Create 4-Digit PIN"
            titleText to "Choose a 4-digit PIN you'll remember"
        }
        PinFlowStep.CONFIRM_NEW -> "Confirm 4-Digit PIN" to "Re-enter your 4-digit PIN to confirm"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerSurface)
                .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.testTag("pin_setup_cancel_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Cancel",
                    tint = LedgerTextPrimary
                )
            }
            Text(
                text = when (mode) {
                    PinFlowMode.ENABLE_NEW -> "Set Up App Lock"
                    PinFlowMode.DISABLE_VERIFY -> "Disable App Lock"
                    PinFlowMode.CHANGE_PIN -> "Change PIN"
                },
                style = MaterialTheme.typography.titleLarge,
                color = LedgerTextPrimary
            )
        }
        HorizontalDivider(color = LedgerDivider)

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PinPadScaffold(
                icon = if (step == PinFlowStep.VERIFY_CURRENT) Icons.Filled.Lock else Icons.Filled.Pin,
                iconTint = if (step == PinFlowStep.CONFIRM_NEW) LedgerGreen else LedgerCoral,
                title = title,
                subtitle = subtitle,
                pinLength = enteredPin.length,
                errorMessage = if (step == PinFlowStep.VERIFY_CURRENT && lockoutSeconds > 0L) {
                    "Too many attempts. Try again in ${lockoutSeconds}s"
                } else errorMessage,
                shakeOffsetPx = shakeOffset.value,
                enabled = !isProcessing && !(step == PinFlowStep.VERIFY_CURRENT && lockoutSeconds > 0L),
                onDigit = ::handleDigit,
                onBackspace = {
                    if (enteredPin.isNotEmpty() && !isProcessing) {
                        enteredPin = enteredPin.dropLast(1)
                        errorMessage = null
                    }
                },
                onClear = {
                    if (!isProcessing) {
                        enteredPin = ""
                        errorMessage = null
                    }
                }
            )
        }
    }
}

@Composable
private fun PinPadScaffold(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    pinLength: Int,
    errorMessage: String?,
    shakeOffsetPx: Float,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        val compactHeight = maxHeight < 580.dp
        val keySize: Dp = if (compactHeight) 60.dp else 68.dp
        val keyGap: Dp = if (compactHeight) 10.dp else 14.dp

        Column(
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(if (compactHeight) 52.dp else 60.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.14f))
                    .border(1.dp, iconTint.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(if (compactHeight) 24.dp else 28.dp)
                )
            }

            Spacer(modifier = Modifier.height(if (compactHeight) 12.dp else 16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = LedgerTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LedgerTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(if (compactHeight) 18.dp else 24.dp))

            // 4-Digit PIN Indicator Dots
            Row(
                modifier = Modifier
                    .offset { IntOffset(shakeOffsetPx.roundToInt(), 0) }
                    .testTag("pin_dots_indicator"),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val filled = index < pinLength
                    val dotColor by animateColorAsState(
                        targetValue = when {
                            errorMessage != null && pinLength == 0 -> LedgerCoral.copy(alpha = 0.45f)
                            filled -> LedgerCoral
                            else -> Color.Transparent
                        },
                        animationSpec = tween(140),
                        label = "pin_dot_color_$index"
                    )
                    val dotScale by animateFloatAsState(
                        targetValue = if (filled) 1.15f else 1f,
                        animationSpec = tween(140),
                        label = "pin_dot_scale_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(
                                width = 1.5.dp,
                                color = when {
                                    errorMessage != null && pinLength == 0 -> LedgerCoral
                                    filled -> LedgerCoral
                                    else -> LedgerBorder
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            // Fixed-height error message line so keypad never jumps
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compactHeight) 30.dp else 36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.labelMedium,
                        color = LedgerCoral,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("pin_error_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (compactHeight) 8.dp else 14.dp))

            // 3x4 Numeric Keypad
            val rows = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9')
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(keyGap),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                rows.forEach { rowDigits ->
                    Row(horizontalArrangement = Arrangement.spacedBy(keyGap + 8.dp)) {
                        rowDigits.forEach { digit ->
                            PinDigitKey(
                                label = digit.toString(),
                                keySize = keySize,
                                enabled = enabled,
                                onClick = { onDigit(digit) },
                                modifier = Modifier.testTag("pin_key_$digit")
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(keyGap + 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Clear button
                    Box(
                        modifier = Modifier
                            .size(keySize)
                            .clip(CircleShape)
                            .clickable(enabled = enabled && pinLength > 0, onClick = onClear)
                            .testTag("pin_key_clear"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (enabled && pinLength > 0) LedgerTextSecondary else LedgerTextMuted
                        )
                    }
                    PinDigitKey(
                        label = "0",
                        keySize = keySize,
                        enabled = enabled,
                        onClick = { onDigit('0') },
                        modifier = Modifier.testTag("pin_key_0")
                    )
                    // Backspace button
                    Box(
                        modifier = Modifier
                            .size(keySize)
                            .clip(CircleShape)
                            .clickable(enabled = enabled && pinLength > 0, onClick = onBackspace)
                            .testTag("pin_key_backspace"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Delete digit",
                            tint = if (enabled && pinLength > 0) LedgerTextPrimary else LedgerTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDigitKey(
    label: String,
    keySize: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = LedgerSurfaceElevated,
        border = BorderStroke(1.dp, LedgerBorder),
        modifier = modifier
            .size(keySize)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (enabled) LedgerTextPrimary else LedgerTextMuted
            )
        }
    }
}

@Composable
private fun AppLockOptionRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingValue: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = LedgerTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LedgerTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingValue != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = trailingValue,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = LedgerCoral,
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = LedgerTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
