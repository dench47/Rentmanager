package com.rentmanager.app.ui.landlord.finance

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentmanager.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Финансы", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад", tint = OnPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Экспорт в Excel */ }) {
                        Icon(Icons.Default.FileDownload, "Экспорт", tint = OnPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = OnPrimary, navigationIconContentColor = OnPrimary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Финансовый отчет", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Доходы за период", style = MaterialTheme.typography.titleMedium)
                    Text("—", style = MaterialTheme.typography.headlineLarge, color = Success)
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Расходы за период", style = MaterialTheme.typography.titleMedium)
                    Text("—", style = MaterialTheme.typography.headlineLarge, color = Error)
                }
            }
        }
    }
}