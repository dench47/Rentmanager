package com.rentmanager.app.data.local

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: CryptoManager
) {

    fun canAuthenticate(): Boolean {
        val result = BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun hasBiometricHardware(): Boolean {
        return BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) !=
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    }

    /** Сохранён ли PIN для входа по отпечатку: без него биометрия бесполезна. */
    fun hasStoredPin(): Boolean = cryptoManager.getPin() != null

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onFallback: () -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Ввести код")
            .build()

        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val pin = cryptoManager.getPin()
                    if (pin != null) {
                        onSuccess(pin)
                    } else {
                        onError("PIN не сохранён")
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Log.w("BiometricAuth", "error=$errorCode msg=$errString")
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onFallback()
                        BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_CANCELED -> Unit
                        BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_NO_BIOMETRICS,
                        BiometricPrompt.ERROR_HW_NOT_PRESENT ->
                            onError("Биометрия временно недоступна. Введите код доступа")
                        else -> onError("Не удалось распознать. Введите код доступа")
                    }
                }

                override fun onAuthenticationFailed() {
                    onError("Не удалось распознать. Введите код доступа")
                }
            }
        )

        prompt.authenticate(promptInfo)
    }
}