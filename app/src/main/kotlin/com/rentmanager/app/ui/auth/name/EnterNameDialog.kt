package com.rentmanager.app.ui.auth.name

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.ui.components.CanonicalDialogButton

@Composable
fun EnterNameDialog(
    viewModel: EnterNameViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    com.rentmanager.app.ui.components.CanonicalContentDialog(
        onDismiss = { /* имя обязательно — не закрываем по клику вне */ },
        title = "Как вас зовут?"
    ) {
        Text(
            text = "Имя будет отображаться в профиле",
            fontSize = 14.sp,
            color = Color(0x993C3C43),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.name,
            onValueChange = { viewModel.onNameChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp)),
            placeholder = {
                Text(
                    text = "Иван",
                    color = Color(0x998E8E93),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF2F2F7),
                unfocusedContainerColor = Color(0xFFF2F2F7),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.errorMessage!!,
                color = Color(0xFFE53935),
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        CanonicalDialogButton(
            text = if (uiState.isLoading) "Сохранение..." else "Сохранить",
            container = Color(0xFF212121),
            textColor = Color.White,
            onClick = {
                viewModel.onContinue(onSuccess = {
                    onSaved(uiState.name.trim())
                })
            }
        )
    }
}
