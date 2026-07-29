package com.rentmanager.app.ui.auth.code

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rentmanager.app.ui.components.BackButton
import com.rentmanager.app.ui.components.StepProgressBar

@Composable
fun SmsCodeScreen(
    phoneNumber: String,
    onConfirmed: () -> Unit,
    onBack: () -> Unit,
    viewModel: SmsCodeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(phoneNumber) {
        viewModel.setPhoneNumber(phoneNumber)
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

            // Progress bar — step 2
            StepProgressBar(currentStep = 2)

            Spacer(modifier = Modifier.height(40.dp))

            // Title block
            Column(
                modifier = Modifier.width(320.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Введите код из СМС",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    letterSpacing = (-0.4).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Отправили на $phoneNumber",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0x993C3C43),
                    letterSpacing = (-0.4).sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Code input — 4 squares
            Row(
                modifier = Modifier.width(320.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(6) { index ->
                    CodeDigitBox(
                        value = if (index < uiState.code.length) uiState.code[index].toString() else "",
                        isFocused = index == uiState.code.length
                    )
                }
            }

            // Hidden TextField to capture input
            BasicTextField(
                value = uiState.code,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }
                    viewModel.onCodeChange(filtered)
                    if (filtered.length == 6) {
                        onConfirmed()
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(color = Color.Transparent),
                modifier = Modifier
                    .size(1.dp)
                    .padding(0.dp),
                singleLine = true,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun CodeDigitBox(value: String, isFocused: Boolean) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = if (isFocused) Color(0xFF212121) else Color(0xFFD3D3D3),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}