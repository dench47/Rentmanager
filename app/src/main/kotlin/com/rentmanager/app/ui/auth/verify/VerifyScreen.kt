package com.rentmanager.app.ui.auth.verify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
 * Визуальная маска (XXX) XXX-XX-XX.
 * Реальный текст — только цифры, маска накладывается при отображении.
 * Курсор не прыгает.
 */
class PhoneMaskTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(10)
        val masked = applyMask(digits)
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // Offset в исходных цифрах -> offset в маскированной строке
                if (offset <= 0) return 0
                val limited = minOf(offset, digits.length)
                val prefix = digits.take(limited)
                return applyMask(prefix).length
            }

            override fun transformedToOriginal(offset: Int): Int {
                // Offset в маскированной строке -> offset в исходных цифрах
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
        val len = digits.length
        val first = digits.take(3)
        if (len <= 3) {
            sb.append("($first")
            if (len == 3) sb.append(")")
            return sb.toString()
        }
        sb.append("($first) ")
        val second = digits.substring(3, minOf(6, len))
        sb.append(second)
        if (len <= 6) return sb.toString()
        sb.append("-")
        val third = digits.substring(6, minOf(8, len))
        sb.append(third)
        if (len <= 8) return sb.toString()
        sb.append("-")
        val fourth = digits.substring(8, minOf(10, len))
        sb.append(fourth)
        return sb.toString()
    }
}

@Composable
fun VerifyScreen(
    onVerified: (String) -> Unit,
    viewModel: VerifyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            onVerified(uiState.phone)
        }
    }

    LaunchedEffect(uiState.isCalling) {
        if (uiState.isCalling) {
            viewModel.startCallChecking(onSuccess = { phone -> onVerified(phone) })
        }
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

        Spacer(modifier = Modifier.height(60.dp))

        // Content card
        Card(
            modifier = Modifier
                .width(343.dp),
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
                            Text("🇷🇺", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "+7",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF151515),
                                letterSpacing = (-0.4).sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicTextField(
                                value = uiState.phone.removePrefix("+7"),
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }.take(10)
                                    viewModel.onDigitsChange(digits)
                                },
                                visualTransformation = PhoneMaskTransformation(),
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
                                    if (uiState.phone.length <= 2) {
                                        Text(
                                            "(900) 000–00–00",
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
                        onClick = { viewModel.onContinue(onSuccess = { onVerified(it) }) },
                        enabled = uiState.phone.removePrefix("+7").length == 10,
                        isLoading = uiState.isLoading
                    )
                } else {
                    // ============================================================
                    // Calling screen
                    // ============================================================
                    Text(
                        text = "Звонок для проверки",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF151515),
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Позвоните, чтобы подтвердить номер",
                        fontSize = 14.sp,
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
                        text = "После звонка вернитесь в приложение\nдля автоматической проверки",
                        fontSize = 12.sp,
                        color = Color(0x993C3C43),
                        letterSpacing = (-0.4).sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Звонок бесплатный",
                        fontSize = 11.sp,
                        color = Color(0x663C3C43),
                        letterSpacing = (-0.4).sp
                    )
                }
            }
        }
    }
}