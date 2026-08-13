package com.rentmanager.app.ui.landlord.myproperties.propertydetail

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
fun PropertyDetailScreen(
    propertyId: String,
    onBack: () -> Unit,
    onEditProperty: () -> Unit,
    onAttachTenant: () -> Unit,
    onTenantClick: (String) -> Unit,
    onMeterClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали объекта", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = OnPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onEditProperty) {
                        Icon(Icons.Default.Edit, "Редактировать", tint = OnPrimary)
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
                .padding(16.dp)
        ) {
            // Заглушка детальной карточки
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ID объекта: $propertyId", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Здесь будет детальная информация об объекте:", style = MaterialTheme.typography.bodyMedium)
                    Text("• Название объекта", style = MaterialTheme.typography.bodySmall, color = GrayDark)
                    Text("• Адрес", style = MaterialTheme.typography.bodySmall, color = GrayDark)
                    Text("• Фото", style = MaterialTheme.typography.bodySmall, color = GrayDark)
                    Text("• Площадь", style = MaterialTheme.typography.bodySmall, color = GrayDark)
                    Text("• Размер арендной платы", style = MaterialTheme.typography.bodySmall, color = GrayDark)
                    Text("• Срок аренды", style = MaterialTheme.typography.bodySmall, color = GrayDark)
                    Text("• Договор", style = MaterialTheme.typography.bodySmall, color = GrayDark)
                    Text("• Арендатор", style = MaterialTheme.typography.bodySmall, color = GrayDark)
                    Text("• Счетчики", style = MaterialTheme.typography.bodySmall, color = GrayDark)
                    Text("• Расходы", style = MaterialTheme.typography.bodySmall, color = GrayDark)
                    Text("• Финансовый отчет", style = MaterialTheme.typography.bodySmall, color = GrayDark)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Кнопка "Добавить арендатора"
            Button(
                onClick = onAttachTenant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Добавить арендатора")
            }
        }
    }
}