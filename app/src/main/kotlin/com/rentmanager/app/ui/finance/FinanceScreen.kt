package com.rentmanager.app.ui.finance

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R

private val GradientBackground = Brush.verticalGradient(
    colors = listOf(Color.White, Color(0xFFF5F7FA))
)

private val CreamColor = Color(0xFFFAF8F5)

@Composable
fun FinanceScreen(
    onBack: () -> Unit,
    onSubscription: () -> Unit = {},
    onAddProperty: () -> Unit = {},
    onFinancialReport: () -> Unit = {},
    onRequisites: () -> Unit = {}
) {
    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GradientBackground)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onBack() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_arrow_left),
                            contentDescription = "Назад",
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            "Финансы",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1D1F),
                            letterSpacing = (-0.3).sp
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinanceMenuItem(
                        icon = Icons.Default.WorkspacePremium,
                        title = "Управление подпиской",
                        subtitle = "Тарифы, оплата, история",
                        onClick = onSubscription
                    )
                    FinanceMenuItem(
                        icon = Icons.Default.AddBusiness,
                        title = "Добавить объект",
                        subtitle = "Новый объект недвижимости",
                        onClick = onAddProperty
                    )
                    FinanceMenuItem(
                        icon = Icons.Default.Description,
                        title = "Финансовый отчёт по объектам",
                        subtitle = "Доходы и расходы за период",
                        onClick = onFinancialReport
                    )
                    FinanceMenuItem(
                        icon = Icons.Default.AttachMoney,
                        title = "Реквизиты",
                        subtitle = "Мои платёжные реквизиты",
                        onClick = onRequisites
                    )

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FinanceMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CreamColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = Color(0xFF1D1D1F)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1D1D1F)
                )
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
            }
        }
    }
}
