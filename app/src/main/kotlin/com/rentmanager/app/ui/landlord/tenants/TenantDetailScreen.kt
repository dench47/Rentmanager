package com.rentmanager.app.ui.landlord.tenants

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentmanager.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantDetailScreen(
    tenantId: String,
    onBack: () -> Unit,
    onPropertyClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Арендатор", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = OnPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.Call, "Позвонить", tint = OnPrimary) }
                    IconButton(onClick = { }) { Icon(Icons.Default.Email, "Написать", tint = OnPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = OnPrimary, navigationIconContentColor = OnPrimary)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("ФИО: —", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Компания: —", style = MaterialTheme.typography.bodyLarge, color = GrayDark)
            Text("Телефон: —", style = MaterialTheme.typography.bodyMedium, color = GrayDark)
            Text("Почта: —", style = MaterialTheme.typography.bodyMedium, color = GrayDark)
            Spacer(Modifier.height(16.dp))
            Text("Арендует сейчас:", style = MaterialTheme.typography.titleMedium)
            Text("—", style = MaterialTheme.typography.bodyMedium, color = GrayMedium)
            Spacer(Modifier.height(8.dp))
            Text("История аренды:", style = MaterialTheme.typography.titleMedium)
            Text("—", style = MaterialTheme.typography.bodyMedium, color = GrayMedium)
        }
    }
}