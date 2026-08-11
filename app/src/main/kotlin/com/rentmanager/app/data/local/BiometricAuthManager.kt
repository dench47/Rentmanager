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
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun hasBiometricHardware(): Boolean {
        return BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) !=
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    }

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
                    // Не показываем ошибку пользователю — системный диалог сам обрабатывает
                    // Только если это не "отмена пользователем" или "нет биометрии"
                }

                override fun onAuthenticationFailed() {
                    // Отпечаток не распознан — системный диалог сам предложит повторить
                }
            }
        )

        prompt.authenticate(promptInfo)
    }
}