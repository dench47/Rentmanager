package com.rentmanager.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsConfirmScreen(
    phoneNumber: String,
    onConfirmed: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Подтверждение",
                style = MaterialTheme.typography.headlineMedium,
                color = Primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Мы отправили код подтверждения на номер\n$phoneNumber",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = uiState.smsCode,
                onValueChange = viewModel::onSmsCodeChanged,
                label = { Text("Код из SMS/PUSH") },
                placeholder = { Text("_ _ _ _") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.confirmSmsCode()
                    onConfirmed()
                },
                enabled = uiState.smsCode.length >= 4 && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Подтвердить")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { viewModel.sendSmsCode() }
            ) {
                Text("Отправить код повторно")
            }
        }
    }
}