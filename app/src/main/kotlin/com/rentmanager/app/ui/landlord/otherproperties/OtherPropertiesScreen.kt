package com.rentmanager.app.ui.landlord.otherproperties

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rentmanager.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherPropertiesScreen(
    onPropertyClick: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Другие объекты", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clickable(onClickLabel = "Назад") { onBack() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = OnPrimary)
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
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Список других объектов", style = MaterialTheme.typography.titleMedium, color = GrayMedium)
        }
    }
}
