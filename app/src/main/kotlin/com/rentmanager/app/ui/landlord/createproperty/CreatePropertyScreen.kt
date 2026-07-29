package com.rentmanager.app.ui.landlord.createproperty

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
fun CreatePropertyScreen(
    propertyId: String? = null,
    onBack: () -> Unit,
    onCreated: () -> Unit
) {
    val isEdit = propertyId != null
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var tenantInfo by remember { mutableStateOf("") }
    var serviceInfo by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Редактирование" else "Создание объекта", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад", tint = OnPrimary)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (isEdit) "Редактирование объекта" else "Новый объект",
                style = MaterialTheme.typography.headlineSmall
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Адрес") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = area,
                onValueChange = { area = it },
                label = { Text("Площадь (м²)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = tenantInfo,
                onValueChange = { tenantInfo = it },
                label = { Text("Информация для арендатора") },
                supportingText = { Text("Укажите информацию для арендатора: правила пользования, wi-fi и др.") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = serviceInfo,
                onValueChange = { serviceInfo = it },
                label = { Text("Служебная информация") },
                supportingText = { Text("Эта информация будет видна только вам") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onCreated,
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && address.isNotBlank()
            ) {
                Text(if (isEdit) "Сохранить" else "Создать")
            }
        }
    }
}