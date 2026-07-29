package com.rentmanager.app.ui.landlord.myproperties

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
fun MyPropertiesScreen(
    onPropertyClick: (String) -> Unit,
    onCreateProperty: () -> Unit,
    onBack: () -> Unit,
    onFinanceClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Моя недвижимость", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад", tint = OnPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onFinanceClick) {
                        Icon(Icons.Default.AccountBalance, "Финансы", tint = OnPrimary)
                    }
                    IconButton(onClick = onNotificationsClick) {
                        Icon(Icons.Default.Notifications, "Уведомления", tint = OnPrimary)
                    }
                    IconButton(onClick = onCreateProperty) {
                        Icon(Icons.Default.Add, "Добавить", tint = OnPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary,
                    navigationIconContentColor = OnPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateProperty) {
                Icon(Icons.Default.Add, "Создать объект")
            }
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
                    "Список объектов недвижимости",
                    style = MaterialTheme.typography.titleMedium,
                    color = GrayMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Здесь будет отображаться шахматка загруженности\nи список объектов",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayMedium
                )
            }
        }
    }
}