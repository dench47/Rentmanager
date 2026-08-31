package com.rentmanager.app.ui.services

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentmanager.app.ui.theme.*

data class ServiceItem(
    val title: String,
    val icon: @Composable () -> Unit,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(onBack: () -> Unit) {
    val services = remember {
        listOf(
            ServiceItem("Сантехники", { Icon(Icons.Default.Plumbing, null, tint = Primary) }, "Услуги сантехников"),
            ServiceItem("Электрики", { Icon(Icons.Default.ElectricalServices, null, tint = Primary) }, "Услуги электриков"),
            ServiceItem("Вскрытие замков", { Icon(Icons.Default.VpnKey, null, tint = Primary) }, "Аварийное вскрытие"),
            ServiceItem("Финансовые услуги", { Icon(Icons.Default.AccountBalance, null, tint = Primary) }, "Кредиты, страхование"),
            ServiceItem("Юридические услуги", { Icon(Icons.Default.Gavel, null, tint = Primary) }, "Консультации юристов"),
            ServiceItem("Страхование", { Icon(Icons.Default.Shield, null, tint = Primary) }, "Страхование недвижимости"),
            ServiceItem("Агентства недвижимости", { Icon(Icons.Default.RealEstateAgent, null, tint = Primary) }, "Риелторские услуги"),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Услуги", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clickable(onClickLabel = "Назад") { onBack() }) },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Сервисы и услуги", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Рекомендованные партнеры", style = MaterialTheme.typography.bodyMedium, color = GrayDark)
            Spacer(Modifier.height(16.dp))
            services.forEach { svc ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text(svc.title) },
                        supportingContent = { Text(svc.description) },
                        leadingContent = svc.icon
                    )
                }
            }
        }
    }
}