package com.rentmanager.app.ui.auth.name

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rentmanager.app.ui.components.BackButton
import com.rentmanager.app.ui.components.PrimaryButton
import com.rentmanager.app.ui.components.StepProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterNameScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    viewModel: EnterNameViewModel = viewModel()
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

            // Progress bar — step 3
            StepProgressBar(currentStep = 3)

            Spacer(modifier = Modifier.height(40.dp))

            // Title block
            Column(
                modifier = Modifier.width(311.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Введите ваше имя",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    letterSpacing = (-0.4).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Имя будет отображаться в профиле",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0x993C3C43),
                    letterSpacing = (-0.4).sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Name input
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
                modifier = Modifier
                    .width(311.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(100.dp)),
                placeholder = {
                    Text(
                        text = "Иван",
                        color = Color(0xCCA6A6A6),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.4).sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Continue button
            PrimaryButton(
                text = "Продолжить",
                onClick = {
                    viewModel.onContinue()
                    if (uiState.name.trim().isNotEmpty() && uiState.errorMessage == null) {
                        onContinue()
                    }
                },
                enabled = uiState.name.trim().isNotEmpty(),
                isLoading = uiState.isLoading
            )
        }
    }
}