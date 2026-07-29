package com.rentmanager.app.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rentmanager.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLandlordSelected: () -> Unit,
    onTenantSelected: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Менеджер аренды",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary,
                    actionIconContentColor = OnPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Выберите роль",
                style = MaterialTheme.typography.headlineMedium,
                color = OnBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Вы можете переключаться между ролями в любое время",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Кнопка "Арендую"
            Button(
                onClick = onTenantSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Surface),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "Арендую",
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary
                    )
                    Text(
                        "Я арендатор",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Кнопка "Сдаю"
            Button(
                onClick = onLandlordSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    tint = OnPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "Сдаю",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnPrimary
                    )
                    Text(
                        "Я арендодатель",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Иконка с именем пользователя
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = GrayDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Пользователь",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrayDark
                )
            }
        }
    }
}