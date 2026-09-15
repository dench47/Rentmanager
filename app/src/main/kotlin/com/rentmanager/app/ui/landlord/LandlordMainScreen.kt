package com.rentmanager.app.ui.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rentmanager.app.ui.theme.*

data class LandlordTab(
    val title: String,
    val icon: ImageVector,
    val hasIndicator: Boolean = false
)

private val tabs = listOf(
    LandlordTab("Недвижимость", Icons.Default.Apartment),
    LandlordTab("Арендаторы", Icons.Default.People),
    LandlordTab("Объекты", Icons.Default.HolidayVillage),
    LandlordTab("Финансы", Icons.Default.AccountBalance),
    LandlordTab("Сообщения", Icons.AutoMirrored.Filled.Chat, hasIndicator = true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordMainScreen(
    onNavigateToMyProperties: () -> Unit,
    onNavigateToTenants: () -> Unit,
    onNavigateToOtherProperties: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onBackToMain: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сдаю", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackToMain) {
                        Icon(Icons.Default.Home, "Главный экран", tint = OnPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Premium */ }) {
                        Icon(Icons.Default.Star, "Премиум", tint = OnPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary,
                    navigationIconContentColor = OnPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            when (index) {
                                0 -> onNavigateToMyProperties()
                                1 -> onNavigateToTenants()
                                2 -> onNavigateToOtherProperties()
                                3 -> onNavigateToFinance()
                                4 -> onNavigateToMessages()
                            }
                        },
                        icon = {
                            if (tab.hasIndicator && selectedTab != index) {
                                BadgedBox(badge = { Badge() }) {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            // Информация о предстоящих платежах
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Payment,
                                contentDescription = null,
                                tint = Success
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Предстоящее поступление",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GrayDark
                                )
                                Text(
                                    "25 000 ₽ — 10.02.2025",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = OnSurface
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Overdue.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Overdue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Просрочена оплата по объекту «Офис на Ленина» — 30 000 ₽",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Overdue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Выберите раздел в нижнем меню",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GrayMedium
                )
            }
        }
    }
}