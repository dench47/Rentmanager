package com.rentmanager.app.ui.landlord.tenants

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
fun TenantsListScreen(
    onTenantClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Арендаторы", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад", tint = OnPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Добавить */ }) {
                        Icon(Icons.Default.PersonAdd, "Добавить арендатора", tint = OnPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary,
                    navigationIconContentColor = OnPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Список арендаторов",
                    style = MaterialTheme.typography.titleMedium,
                    color = GrayMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Здесь будет отображаться список арендаторов\nс сортировкой и поиском",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayMedium
                )
            }
        }
    }
}