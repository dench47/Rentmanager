package com.rentmanager.app.ui.auth.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rentmanager.app.ui.components.BackButton
import com.rentmanager.app.ui.components.PrimaryButton
import com.rentmanager.app.ui.components.StepProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberScreen(
    onCodeSent: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PhoneNumberViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            // Back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BackButton(onClick = onBack)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Progress bar — step 1
            StepProgressBar(currentStep = 1)

            Spacer(modifier = Modifier.height(40.dp))

            // Title block
            Column(
                modifier = Modifier.width(311.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Введите номер телефона",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    letterSpacing = (-0.4).sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Мы пришлем временный код",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0x993C3C43),
                    letterSpacing = (-0.4).sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Phone input with error support
            Column(
                modifier = Modifier.width(311.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = uiState.phoneNumber,
                    onValueChange = { viewModel.onPhoneNumberChange(it) },
                    modifier = Modifier
                        .width(311.dp)
                        .height(58.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .then(
                            if (uiState.errorMessage != null) {
                                Modifier.border(
                                    width = 1.5.dp,
                                    color = Color(0xFFFF0000),
                                    shape = RoundedCornerShape(100.dp)
                                )
                            } else {
                                Modifier
                            }
                        ),
                    isError = uiState.errorMessage != null,
                    placeholder = {
                        Text(
                            text = "+7 (900) 000-00-00",
                            color = Color(0xCCA6A6A6),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.4).sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = if (uiState.errorMessage != null) Color(0xFFFF0000) else Color.Transparent,
                        unfocusedBorderColor = if (uiState.errorMessage != null) Color(0xFFFF0000) else Color.Transparent,
                        errorContainerColor = Color.White,
                        errorBorderColor = Color(0xFFFF0000)
                    )
                )

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
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Continue button
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

            Spacer(modifier = Modifier.height(20.dp))

            // Agreement text
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