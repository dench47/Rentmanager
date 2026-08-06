package com.rentmanager.app.ui.auth.phone

import androidx.compose.foundation.background
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.PrimaryButton
import com.rentmanager.app.ui.components.StepProgressBar

@Composable
fun PhoneNumberScreen(
    onCodeSent: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PhoneNumberViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetLoading()
    }

    Scaffold(
        containerColor = Color(0xFFFEFFBB)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 53.dp)
                .background(Color(0xFFFEFFBB)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress bar (Figma: y=65, h=2)
            Spacer(modifier = Modifier.height(12.dp))
            StepProgressBar(currentStep = 1)

            // Gap: progress bar (y=67) → title block (y=188) = 121dp
            Spacer(modifier = Modifier.height(121.dp))

            // Title block (Figma: Frame 132, w=275, itemSpacing=8)
            Column(
                modifier = Modifier.width(275.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Введите номер телефона",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    letterSpacing = (-0.4).sp,
                    lineHeight = 29.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Мы пришлем временный код",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0x993C3C43),
                    letterSpacing = (-0.4).sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Gap: title (y=245) → phone input (y=265) = 20dp (Figma: itemSpacing=20)
            Spacer(modifier = Modifier.height(20.dp))

            // Phone input (Figma: Input Number, 311×58dp, white bg, cornerRadius=100)
            Box(
                modifier = Modifier
                    .width(311.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.White)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Flag icon (Figma: twemoji:flag-russia, 18×18)
                    // TODO: заменить на реальный флаг из Figma
                    Text(
                        text = "🇷🇺",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // Prefix "+7"
                    Text(
                        text = "+7",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF151515),
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    // Chevron icon (Figma: Frame 130)
                    Text(
                        text = "▾",
                        fontSize = 12.sp,
                        color = Color(0xFFA6A6A6)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    // Phone number input
                    BasicTextField(
                        value = uiState.phoneNumber,
                        onValueChange = { viewModel.onPhoneNumberChange(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.phoneNumber.isEmpty()) Color(0xCCA6A6A6) else Color(0xFF151515),
                            letterSpacing = (-0.4).sp
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (uiState.phoneNumber.isEmpty()) {
                                Text(
                                    text = "(900) 000–00–00",
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

            // Error message
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage!!,
                    color = Color(0xFFFF0000),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.4).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Gap: input (y=323) → button (y=363) = 40dp (Figma: itemSpacing=40)
            Spacer(modifier = Modifier.height(40.dp))

            // Continue button (Figma: Frame 91, 311×48dp)
            PrimaryButton(
                text = "Продолжить",
                onClick = {
                    viewModel.onContinue()
                    if (uiState.phoneNumber.length >= 10 && uiState.errorMessage == null) {
                        onCodeSent(uiState.phoneNumber)
                    }
                },
                enabled = uiState.phoneNumber.isNotBlank(),
                isLoading = uiState.isLoading
            )

            // Gap: button (y=411) → agreement (y=431) = 20dp (Figma: itemSpacing=20)
            Spacer(modifier = Modifier.height(20.dp))

            // Agreement text (Figma: Inter Regular 12sp, lineHeight=18sp, w=311dp)
            Text(
                text = "Нажимая на кнопку, вы соглашаетесь \nна обработку персональных данных и политикой конфидинциальности",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0x993C3C43),
                letterSpacing = (-0.4).sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(311.dp)
            )
        }
    }
}