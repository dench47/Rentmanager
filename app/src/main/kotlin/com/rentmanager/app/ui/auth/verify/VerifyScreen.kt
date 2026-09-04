package com.rentmanager.app.ui.auth.verify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.rentmanager.app.R
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.ui.components.PrimaryButton
import com.rentmanager.app.ui.components.StepProgressBar

/**
 * Универсальная визуальная маска номера телефона.
 *
 * @param maskPattern список длин групп цифр
 * @param maxDigits максимум цифр (без префикса)
 * @param prefixes разделители, вставляемые ПЕРЕД каждой группой (размер должен совпадать с maskPattern)
 *
 * Россия:  maskPattern=[3,3,2,2], prefixes=["(", ") ", "-", "-"] → (XXX) XXX-XX-XX
 * Китай:   maskPattern=[3,4,4],   prefixes=["",  "-", "-"]      → XXX-XXXX-XXXX
 */
class PhoneMaskTransformation(
    private val maskPattern: List<Int>,
    private val maxDigits: Int,
    private val prefixes: List<String>
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(maxDigits)
        val masked = applyMask(digits)
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val limited = minOf(offset, digits.length)
                val prefix = digits.take(limited)
                return applyMask(prefix).length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                var digitCount = 0
                var maskPos = 0
                while (maskPos < offset && maskPos < masked.length) {
                    if (masked[maskPos].isDigit()) digitCount++
                    maskPos++
                }
                return digitCount
            }
        }
        return TransformedText(AnnotatedString(masked), offsetMapping)
    }

    private fun applyMask(digits: String): String {
        if (digits.isEmpty()) return ""
        val sb = StringBuilder()
        var pos = 0

        for (i in maskPattern.indices) {
            val groupLen = maskPattern[i]
            if (pos >= digits.length) break

            // Вставляем разделитель перед группой
            if (i < prefixes.size) {
                sb.append(prefixes[i])
            }

            val chunk = digits.substring(pos, minOf(pos + groupLen, digits.length))
            sb.append(chunk)
            pos += groupLen
        }
        return sb.toString()
    }

    companion object {
        /**
         * Маска для России: (XXX) XXX-XX-XX (10 цифр)
         */
        fun russian(): PhoneMaskTransformation = PhoneMaskTransformation(
            maskPattern = listOf(3, 3, 2, 2),
            maxDigits = 10,
            prefixes = listOf("(", ") ", "-", "-")
        )

        /**
         * Маска для Китая: (XXX) XXXX-XXXX (11 цифр)
         */
        fun chinese(): PhoneMaskTransformation = PhoneMaskTransformation(
            maskPattern = listOf(3, 4, 4),
            maxDigits = 11,
            prefixes = listOf("(", ") ", "-")
        )

        /**
         * Маска для Казахстана: (XXX) XXX-XX-XX (10 цифр)
         */
        fun kazakh(): PhoneMaskTransformation = PhoneMaskTransformation(
            maskPattern = listOf(3, 3, 2, 2),
            maxDigits = 10,
            prefixes = listOf("(", ") ", "-", "-")
        )

        /**
         * Маска для Беларуси: XX XXX-XX-XX (9 цифр)
         */
        fun belarus(): PhoneMaskTransformation = PhoneMaskTransformation(
            maskPattern = listOf(2, 3, 2, 2),
            maxDigits = 9,
            prefixes = listOf("", " ", "-", "-")
        )

        /**
         * Маска для Грузии: XXX-XX-XX-XX (9 цифр)
         */
        fun georgian(): PhoneMaskTransformation = PhoneMaskTransformation(
            maskPattern = listOf(3, 2, 2, 2),
            maxDigits = 9,
            prefixes = listOf("", "-", "-", "-")
        )

        /**
         * Фабрика по стране.
         */
        fun forCountry(country: CountryPhone): PhoneMaskTransformation = when (country.countryCode) {
            "CN" -> chinese()
            "KZ" -> kazakh()
            "BY" -> belarus()
            "GE" -> georgian()
            else -> russian()
        }

        /**
         * Placeholder-строка для заданной страны.
         */
        fun placeholderForCountry(country: CountryPhone): String = when (country.countryCode) {
            "CN" -> "(123) 4567-8901"
            "KZ" -> "(700) 000-00-00"
            "BY" -> "29 000-00-00"
            "GE" -> "500-00-00-00"
            else -> "(900) 000-00-00"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(
    onVerified: (Boolean) -> Unit,
    viewModel: VerifyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showCountryPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val mask = remember(uiState.selectedCountry) {
        PhoneMaskTransformation.forCountry(uiState.selectedCountry)
    }
    val placeholder = remember(uiState.selectedCountry) {
        PhoneMaskTransformation.placeholderForCountry(uiState.selectedCountry)
    }

    LaunchedEffect(uiState.isCalling) {
        if (uiState.isCalling) {
            viewModel.startCallChecking(onSuccess = { })
        }
    }

    // ===== Навигация после верификации =====
    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            onVerified(uiState.isNewUser)
        }
    }

    // ===== Telegram: ввод кода входа =====
    if (uiState.telegramCodeSent) {
        TelegramCodeScreen(
            phone = uiState.phone,
            errorMessage = uiState.telegramCodeError,
            attemptsLeft = uiState.telegramAttemptsLeft,
            onBack = { viewModel.cancelTelegramCode() },
            onConfirm = { code -> viewModel.onVerifyTelegramCode(code, onSuccess = { }) }
        )
        return
    }

    // ===== Email: ввод кода входа =====
    if (uiState.emailCodeSent) {
        EmailCodeScreen(
            phone = uiState.phone,
            errorMessage = uiState.emailCodeError,
            attemptsLeft = uiState.emailAttemptsLeft,
            onBack = { viewModel.cancelEmailCode() },
            onConfirm = { code -> viewModel.onVerifyEmailCode(code, onSuccess = { }) }
        )
        return
    }

    // ===== Device Trust: ожидание подтверждения входа на доверенном устройстве =====
    if (uiState.awaitingApproval) {
        ApprovalWaitingScreen(
            phone = uiState.phone,
            errorMessage = uiState.errorMessage,
            onUseCall = { viewModel.fallbackToCall() },
            onCancel = { viewModel.reset() },
            canTelegram = uiState.canTelegram,
            onTelegram = { viewModel.onTelegramLogin() }
        )
        return
    }

    // Country picker bottom sheet
    if (showCountryPicker) {
        CountryPickerDialog(
            sheetState = sheetState,
            selectedCountry = uiState.selectedCountry,
            onCountrySelected = { country ->
                viewModel.onCountrySelected(country)
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top safe area
        Spacer(modifier = Modifier.height(53.dp))

        // Step progress — 2 steps for verification
        StepProgressBar(currentStep = if (uiState.isCalling) 2 else 1, totalSteps = 2)

        // Back arrow (outside card, only on calling screen)
        if (uiState.isCalling) {
            Row(
                modifier = Modifier
                    .width(343.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clickable { viewModel.reset() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = "Назад",
                            tint = Color(0xFF000000),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Назад",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF000000),
                            letterSpacing = (-0.4).sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        // Content card
        Card(
            modifier = Modifier.width(343.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!uiState.isCalling) {
                    // ============================================================
                    // Phone input screen
                    // ============================================================
                    Text(
                        text = "Введите номер телефона",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF151515),
                        letterSpacing = (-0.4).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Мы проверим, есть ли вы в системе",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0x993C3C43),
                        letterSpacing = (-0.4).sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Phone input — cream card with shadow
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF5E6)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Clickable country flag + prefix
                            Row(
                                modifier = Modifier.clickable { showCountryPicker = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(uiState.selectedCountry.flagEmoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    uiState.selectedCountry.phonePrefix,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF151515),
                                    letterSpacing = (-0.4).sp
                                )
                                // Small chevron to indicate dropdown
                                Text(
                                    " ▾",
                                    fontSize = 12.sp,
                                    color = Color(0xFF8E8E93)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))
                            BasicTextField(
                                value = uiState.phone.removePrefix(uiState.selectedCountry.phonePrefix),
                                onValueChange = { raw ->
                                    viewModel.onDigitsChange(raw)
                                },
                                visualTransformation = mask,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF151515),
                                    letterSpacing = (-0.4).sp
                                ),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (uiState.phone.length <= uiState.selectedCountry.phonePrefix.length) {
                                        Text(
                                            placeholder,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xCCA6A6A6),
                                            letterSpacing = (-0.4).sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            uiState.errorMessage!!,
                            color = Color(0xFFE53935),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    PrimaryButton(
                        text = "Продолжить",
                        onClick = { viewModel.onContinue(onSuccess = { }) },
                        enabled = uiState.phone.removePrefix(uiState.selectedCountry.phonePrefix).length == uiState.selectedCountry.maxDigits,
                        isLoading = uiState.isLoading
                    )

                    // ===== Telegram: альтернативный вход для недоверенного устройства =====
                    if (uiState.canTelegram && !uiState.isVerified) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.onTelegramLogin() }) {
                            Text(
                                text = "Войти через Telegram",
                                color = Color(0xFF007AFF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // ===== Email: альтернативный вход =====
                    if (uiState.canEmail && !uiState.isVerified) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.onEmailLogin() }) {
                            Text(
                                text = "Войти через Email",
                                color = Color(0xFF007AFF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // ============================================================
                    // Calling screen
                    // ============================================================
                    var showCallInfoDialog by remember { mutableStateOf(false) }

                    // Info dialog
                    if (showCallInfoDialog) {
                        val infoText = buildAnnotatedString {
                            append("Звонок для подтверждения номера абсолютно бесплатен — ")
                            append("даже если вы находитесь в роуминге или звоните с зарубежного номера.\n\n")
                            append("После соединения вы услышите голосовое сообщение об успешной авторизации, ")
                            append("и звонок автоматически завершится. Звонок не тарифицируется, ")
                            append("так как соединение не считается установленным.\n\n")
                            append("Пожалуйста, позвоните на указанный номер в течение 5 минут.\n\n")
                            append("Услуга предоставляется сервисом ")
                            withLink(
                                LinkAnnotation.Url(
                                    url = "https://sms.ru",
                                    styles = TextLinkStyles(
                                        style = SpanStyle(
                                            color = Color(0x993C3C43),
                                            textDecoration = TextDecoration.Underline
                                        )
                                    )
                                )
                            ) {
                                append("sms.ru")
                            }
                            append(".")
                        }

                        AlertDialog(
                            onDismissRequest = { showCallInfoDialog = false },
                            title = {
                                Text(
                                    "О звонке",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF151515)
                                )
                            },
                            text = {
                                Text(
                                    text = infoText,
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = Color(0x993C3C43),
                                        lineHeight = 20.sp
                                    )
                                )
                            },
                            confirmButton = { },
                            dismissButton = { },
                            containerColor = Color.White,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    Text(
                        text = "Звонок для проверки",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF151515),
                        letterSpacing = (-0.4).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Проверьте правильность номера",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0x993C3C43),
                        letterSpacing = (-0.4).sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = uiState.selectedCountry.phonePrefix + mask.filter(AnnotatedString(uiState.phone.removePrefix(uiState.selectedCountry.phonePrefix))).text.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0x993C3C43),
                        letterSpacing = (-0.4).sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Phone display — cream card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF5E6)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Номер для звонка",
                                fontSize = 13.sp,
                                color = Color(0x993C3C43),
                                letterSpacing = (-0.4).sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.callPhonePretty,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007AFF),
                                letterSpacing = (-0.4).sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    PrimaryButton(
                        text = "📞 Позвонить",
                        onClick = {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CALL_PHONE
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                val intent =
                                    Intent(Intent.ACTION_CALL, "tel:${uiState.callPhone}".toUri())
                                context.startActivity(intent)
                            } else {
                                Toast
                                    .makeText(context, "Нет разрешения на звонки", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        },
                        enabled = true,
                        isLoading = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "После звонка Вы вернетесь в приложение\nдля автоматической проверки",
                        fontSize = 12.sp,
                        color = Color(0x993C3C43),
                        letterSpacing = (-0.4).sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Free call + info icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Text(
                            text = "Звонок бесплатный",
                            fontSize = 11.sp,
                            color = Color(0x663C3C43),
                            letterSpacing = (-0.4).sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showCallInfoDialog = true },
                            modifier = Modifier.width(20.dp).height(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Информация о звонке",
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // ===== Telegram: альтернативный вход =====
                    if (uiState.canTelegram && !uiState.isVerified) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.onTelegramLogin() }) {
                            Text(
                                text = "Войти через Telegram",
                                color = Color(0xFF007AFF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // ===== Email: альтернативный вход =====
                    if (uiState.canEmail && !uiState.isVerified) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.onEmailLogin() }) {
                            Text(
                                text = "Войти через Email",
                                color = Color(0xFF007AFF),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Экран ожидания подтверждения входа с доверенного устройства (Device Trust).
 * Push уже отправлен; пользователь может подождать, отклонить или войти через звонок.
 */
@Composable
fun ApprovalWaitingScreen(
    phone: String,
    errorMessage: String?,
    onUseCall: () -> Unit,
    onCancel: () -> Unit,
    canTelegram: Boolean = false,
    onTelegram: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_left),
                contentDescription = "Назад",
                tint = Color(0xFF000000),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onCancel() }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.width(343.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📱", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Подтвердите вход",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF151515),
                    letterSpacing = (-0.4).sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Мы отправили запрос на ваш телефон.\nОткройте приложение и подтвердите вход.",
                    fontSize = 14.sp,
                    color = Color(0x993C3C43),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = phone,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF151515)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMessage!!, color = Color(0xFFE53935), fontSize = 13.sp, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Пульсирующий индикатор ожидания
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Text(
                    text = "⏳ Ожидаем подтверждения…",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF007AFF).copy(alpha = alpha)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onUseCall) {
            Text(
                text = "Нет доступа к телефону? Войти по звонку",
                color = Color(0xFF007AFF),
                fontSize = 14.sp
            )
        }

        if (canTelegram) {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onTelegram) {
                Text(
                    text = "Войти через Telegram",
                    color = Color(0xFF007AFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ===== Telegram: ввод кода входа =====

@Composable
private fun TelegramCodeScreen(
    phone: String,
    errorMessage: String?,
    attemptsLeft: Int,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_left),
                contentDescription = "Назад",
                tint = Color(0xFF000000),
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.width(343.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("✈️", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Введите код из Telegram",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF151515),
                    letterSpacing = (-0.4).sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Мы отправили 8-значный код в ваш Telegram.\nОсталось попыток сегодня: $attemptsLeft",
                    fontSize = 14.sp,
                    color = Color(0x993C3C43),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF5E6)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    BasicTextField(
                        value = code,
                        onValueChange = { raw ->
                            code = raw.filter { it.isDigit() }.take(8)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF151515),
                            letterSpacing = 8.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (code.isEmpty()) {
                                    Text(
                                        "• • • • • • • •",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xCCA6A6A6),
                                        letterSpacing = 4.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMessage, color = Color(0xFFE53935), fontSize = 13.sp, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Подтвердить",
                    onClick = { onConfirm(code) },
                    enabled = code.length == 8
                )
            }
        }
    }
}

// ===== Email: ввод кода входа =====

@Composable
private fun EmailCodeScreen(
    phone: String,
    errorMessage: String?,
    attemptsLeft: Int,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_left),
                contentDescription = "Назад",
                tint = Color(0xFF000000),
                modifier = Modifier.size(24.dp).clickable { onBack() }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            modifier = Modifier.width(343.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📧", fontSize = 44.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Введите код из письма",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF151515),
                    letterSpacing = (-0.4).sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Мы отправили 6-значный код на вашу почту.\nОсталось попыток сегодня: $attemptsLeft",
                    fontSize = 14.sp,
                    color = Color(0x993C3C43),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF5E6)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    BasicTextField(
                        value = code,
                        onValueChange = { raw ->
                            code = raw.filter { it.isDigit() }.take(6)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF151515),
                            letterSpacing = 8.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (code.isEmpty()) {
                                    Text(
                                        "• • • • • •",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xCCA6A6A6),
                                        letterSpacing = 4.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMessage, color = Color(0xFFE53935), fontSize = 13.sp, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Подтвердить",
                    onClick = { onConfirm(code) },
                    enabled = code.length == 6
                )
            }
        }
    }
}
