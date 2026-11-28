package com.example.util

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricManager
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface PinVerifyResult {
    data object Success : PinVerifyResult
    data class Invalid(val attemptsRemaining: Int) : PinVerifyResult
    data class LockedOut(val remainingSeconds: Long) : PinVerifyResult
}

enum class BiometricAvailability {
    AVAILABLE,
    NONE_ENROLLED,
    UNAVAILABLE
}

object AppLockManager {
    private const val PREFS_NAME = "money_ledger_app_lock_prefs"
    private const val KEY_ENABLED = "pin_lock_enabled"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_unlock_enabled"
    private const val KEY_PIN_HASH = "pin_hash_b64"
    private const val KEY_PIN_SALT = "pin_salt_b64"
    private const val KEY_AUTO_LOCK_DELAY_SEC = "auto_lock_delay_sec"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCKOUT_UNTIL_MS = "lockout_until_ms"

    private const val PIN_LENGTH = 4
    private const val SALT_BYTES = 16
    private const val KEY_LENGTH_BITS = 256
    private const val PBKDF2_ITERATIONS = 100_000
    private const val MAX_FAILED_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isValidPinFormat(pin: String): Boolean =
        pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

    fun isAppLockEnabled(context: Context): Boolean {
        val p = prefs(context)
        return p.getBoolean(KEY_ENABLED, false) &&
            !p.getString(KEY_PIN_HASH, null).isNullOrBlank() &&
            !p.getString(KEY_PIN_SALT, null).isNullOrBlank()
    }

    fun biometricAvailability(context: Context): BiometricAvailability {
        val manager = BiometricManager.from(context.applicationContext)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    fun isBiometricEnabled(context: Context): Boolean =
        isAppLockEnabled(context) &&
            prefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false) &&
            biometricAvailability(context) == BiometricAvailability.AVAILABLE

    fun isBiometricPreferenceOn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
    }

    fun autoLockDelaySeconds(context: Context): Int =
        prefs(context).getInt(KEY_AUTO_LOCK_DELAY_SEC, 0).coerceIn(0, 300)

    fun setAutoLockDelaySeconds(context: Context, seconds: Int) {
        prefs(context).edit()
            .putInt(KEY_AUTO_LOCK_DELAY_SEC, seconds.coerceIn(0, 300))
            .apply()
    }

    fun remainingLockoutSeconds(context: Context): Long {
        val until = prefs(context).getLong(KEY_LOCKOUT_UNTIL_MS, 0L)
        val now = System.currentTimeMillis()
        if (until <= now) return 0L
        val clampedRemainingMs = (until - now).coerceAtMost(LOCKOUT_DURATION_MS)
        return (clampedRemainingMs + 999L) / 1000L
    }

    fun setPin(context: Context, pin: String): Boolean {
        if (!isValidPinFormat(pin)) return false
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derivePinHash(pin, salt)
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, true)
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
            .apply()
        return true
    }

    fun verifyPin(context: Context, pin: String): PinVerifyResult {
        val remainingLock = remainingLockoutSeconds(context)
        if (remainingLock > 0L) {
            return PinVerifyResult.LockedOut(remainingLock)
        }
        if (!isValidPinFormat(pin)) {
            return recordFailedAttempt(context)
        }

        val p = prefs(context)
        val saltB64 = p.getString(KEY_PIN_SALT, null)
        val hashB64 = p.getString(KEY_PIN_HASH, null)
        if (saltB64.isNullOrBlank() || hashB64.isNullOrBlank()) {
            return PinVerifyResult.Invalid(MAX_FAILED_ATTEMPTS)
        }

        val salt = runCatching { Base64.decode(saltB64, Base64.NO_WRAP) }.getOrNull()
            ?: return PinVerifyResult.Invalid(MAX_FAILED_ATTEMPTS)
        val expectedHash = runCatching { Base64.decode(hashB64, Base64.NO_WRAP) }.getOrNull()
            ?: return PinVerifyResult.Invalid(MAX_FAILED_ATTEMPTS)

        val actualHash = derivePinHash(pin, salt)
        val matches = MessageDigest.isEqual(expectedHash, actualHash)
        actualHash.fill(0)

        return if (matches) {
            p.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
                .apply()
            PinVerifyResult.Success
        } else {
            recordFailedAttempt(context)
        }
    }

    fun disableAppLock(context: Context, currentPin: String): PinVerifyResult {
        val result = verifyPin(context, currentPin)
        if (result is PinVerifyResult.Success) {
            prefs(context).edit()
                .putBoolean(KEY_ENABLED, false)
                .remove(KEY_PIN_HASH)
                .remove(KEY_PIN_SALT)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
                .apply()
        }
        return result
    }

    fun changePin(context: Context, currentPin: String, newPin: String): PinVerifyResult {
        val result = verifyPin(context, currentPin)
        if (result is PinVerifyResult.Success) {
            if (!setPin(context, newPin)) {
                return PinVerifyResult.Invalid(MAX_FAILED_ATTEMPTS)
            }
        }
        return result
    }

    private fun recordFailedAttempt(context: Context): PinVerifyResult {
        val p = prefs(context)
        val attempts = p.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        return if (attempts >= MAX_FAILED_ATTEMPTS) {
            val until = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            p.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL_MS, until)
                .apply()
            PinVerifyResult.LockedOut(LOCKOUT_DURATION_MS / 1000L)
        } else {
            p.edit()
                .putInt(KEY_FAILED_ATTEMPTS, attempts)
                .apply()
            PinVerifyResult.Invalid((MAX_FAILED_ATTEMPTS - attempts).coerceAtLeast(1))
        }
    }

    private fun derivePinHash(pin: String, salt: ByteArray): ByteArray {
        val chars = pin.toCharArray()
        val spec = PBEKeySpec(chars, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = runCatching {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            }.getOrElse {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            }
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            chars.fill('\u0000')
        }
    }
}
