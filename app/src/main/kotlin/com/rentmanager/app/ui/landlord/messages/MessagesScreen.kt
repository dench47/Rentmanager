package com.rentmanager.app.ui.landlord.messages

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentmanager.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onChatClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сообщения", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = OnPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Primary, titleContentColor = OnPrimary, navigationIconContentColor = OnPrimary)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Список чатов", style = MaterialTheme.typography.titleMedium, color = GrayMedium)
                Spacer(Modifier.height(8.dp))
                Text("Здесь будут все переписки с арендаторами", style = MaterialTheme.typography.bodyMedium, color = GrayMedium)
            }
        }
    }
}