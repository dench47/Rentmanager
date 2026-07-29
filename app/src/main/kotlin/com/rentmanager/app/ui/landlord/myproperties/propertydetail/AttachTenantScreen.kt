package com.rentmanager.app.ui.landlord.myproperties.propertydetail

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
fun AttachTenantScreen(
    propertyId: String,
    onDismiss: () -> Unit,
    onAttached: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить арендатора", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Закрыть", tint = OnPrimary)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Выберите период аренды",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(24.dp))

            // Заглушка календаря
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(containerColor = GrayLight.copy(alpha = 0.3f))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Календарь выбора периода", color = GrayMedium)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Контакт арендатора",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Contacts, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Выбрать из контактов")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ввести вручную")
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onAttached,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Прикрепить арендатора")
            }
        }
    }
}