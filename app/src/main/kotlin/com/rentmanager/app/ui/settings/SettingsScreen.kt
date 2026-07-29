package com.rentmanager.app.ui.settings

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
fun SettingsScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", style = MaterialTheme.typography.titleLarge) },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Настройки профиля", style = MaterialTheme.typography.titleMedium)
            Divider()

            // Установить пароль
            ListItem(
                headlineContent = { Text("Установить пароль") },
                leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) }
            )

            // Изменить ФИО
            ListItem(
                headlineContent = { Text("Изменить ФИО") },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) }
            )

            // Название юр. лица
            ListItem(
                headlineContent = { Text("Название юридического лица") },
                leadingContent = { Icon(Icons.Default.Business, contentDescription = null) }
            )

            // Начальный экран
            ListItem(
                headlineContent = { Text("Начальный экран") },
                supportingContent = { Text("Выберите модуль: Арендую / Сдаю") },
                leadingContent = { Icon(Icons.Default.Home, contentDescription = null) }
            )

            Divider()

            // Выйти
            ListItem(
                headlineContent = {
                    Text("Выйти из учетной записи", color = Error)
                },
                leadingContent = { Icon(Icons.Default.Logout, contentDescription = null, tint = Error) }
            )
        }
    }
}