package com.rentmanager.app.ui.role

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.rentmanager.app.R
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rentmanager.app.ui.components.DashboardCard
import com.rentmanager.app.ui.components.PremiumBanner
import com.rentmanager.app.ui.theme.RentManagerTheme

@Composable
fun RoleScreen(
    role: UserRole,
    onNavigateToMyProperties: () -> Unit = {},
    onNavigateToTenants: () -> Unit = {},
    onNavigateToOtherProperties: () -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToLandlordsList: () -> Unit = {},
    onNavigateToTenantProperties: () -> Unit = {},
    onBackToMain: () -> Unit = {},
    onPay: () -> Unit = {},
    viewModel: RoleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(role) {
        viewModel.setRole(role)
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(8.dp))

                // Header with back arrow
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 0.dp, top = 0.dp, bottom = 12.dp, end = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onBackToMain() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_arrow_left),
                            contentDescription = "Назад",
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(uiState.title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, letterSpacing = (-0.3).sp)
                    }
                }

                // Reduced gap between title and stats (Figma: itemSpacing=28 → 12dp saves space)
                Spacer(Modifier.height(12.dp))

                // Секция статистики: 3 состояния — нет сделок / всё хорошо / задолженность
                when {
                    !uiState.hasDeals -> EmptyStateBlock(
                        role = uiState.role,
                        onAction = {
                            if (uiState.role == UserRole.LANDLORD) onNavigateToMyProperties() else onNavigateToOtherProperties()
                        }
                    )
                    uiState.role == UserRole.LANDLORD -> LandlordStatsSection(uiState.nextPaymentDate, uiState.nextPaymentAmount, uiState.monthlyIncome, uiState.hasDebt)
                    else -> TenantStatsSection(uiState.nextPaymentDate, uiState.nextPaymentAmount, uiState.hasDebt, onPay)
                }
                Spacer(Modifier.height(8.dp))

                // Cards grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height(370.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    userScrollEnabled = false
                ) {
                    items(uiState.cards) { card ->
                        DashboardCard(
                            iconRes = card.iconRes,
                            title = card.title,
                            twoLines = card.twoLines,
                         onClick = { handleCardClick(card.id, uiState.role, onNavigateToMyProperties, onNavigateToTenants, onNavigateToOtherProperties, onNavigateToFinance, onNavigateToMessages, onNavigateToLandlordsList, onNavigateToTenantProperties) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                PremiumBanner()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun handleCardClick(
    cardId: String, role: UserRole,
    onMyProperties: () -> Unit, onTenants: () -> Unit, onOther: () -> Unit,
    onFinance: () -> Unit, onMessages: () -> Unit, onLandlords: () -> Unit,
    onTenantProperties: () -> Unit
) {
    when (role) {
        UserRole.LANDLORD -> when (cardId) {
            "1" -> onMyProperties()
            "2" -> onTenants()
            "3" -> onOther()
            "4" -> onFinance()
            "5" -> onMessages()
            "6" -> onOther()
        }
        UserRole.TENANT -> when (cardId) {
            "1" -> onTenantProperties()
            "2" -> onLandlords()
        }
    }
}

@Composable
private fun LandlordStatsSection(paymentDate: String, paymentAmount: String, monthlyIncome: String, hasDebt: Boolean) {
    Column(modifier = Modifier.width(353.dp)) {
        Row(modifier = Modifier.width(353.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(modifier = Modifier.width(163.dp)) {
                Text("Ближайшее поступление $paymentDate", fontSize = 13.sp, fontWeight = FontWeight.Normal, color = Color(0x99151515), letterSpacing = (-0.4).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(paymentAmount, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xE5151515), letterSpacing = (-0.4).sp)
            }
            Column(modifier = Modifier.width(122.dp)) {
                Text("Доход \nпо всем объектам", fontSize = 13.sp, fontWeight = FontWeight.Normal, color = Color(0x99151515), letterSpacing = (-0.4).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(monthlyIncome, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xE5151515), letterSpacing = (-0.4).sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.width(353.dp).height(48.dp).clip(RoundedCornerShape(100.dp)).background(Color.Transparent), contentAlignment = Alignment.Center) {
            Text(
                if (hasDebt) "Есть задолженность" else "Просрочек нет",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (hasDebt) Color(0xFFFF4249) else Color(0xFF66A256),
                letterSpacing = (-0.4).sp
            )
        }
    }
}

@Composable
private fun TenantStatsSection(paymentDate: String, paymentAmount: String, hasDebt: Boolean, onPay: () -> Unit) {
    Column(modifier = Modifier.width(353.dp)) {
        Text("Ближайший платеж до $paymentDate", fontSize = 13.sp, fontWeight = FontWeight.Normal, color = Color(0x99151515), letterSpacing = (-0.4).sp)
        Spacer(Modifier.height(4.dp))
        Text(paymentAmount, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xE5151515), letterSpacing = (-0.4).sp)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.width(353.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (hasDebt) "Есть задолженность" else "Просрочек нет",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (hasDebt) Color(0xFFFF4249) else Color(0xFF66A256),
                letterSpacing = (-0.4).sp
            )
            Box(
                modifier = Modifier
                    .width(183.dp)
                    .height(49.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF212121))
                    .clickable { onPay() },
                contentAlignment = Alignment.Center
            ) {
                Text("Оплатить", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White, letterSpacing = (-0.4).sp)
            }
        }
    }
}

@Composable
private fun EmptyStateBlock(role: UserRole, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .width(353.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (role == UserRole.LANDLORD) "Добавьте первый объект" else "Найдите объект",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF212121),
            letterSpacing = (-0.4).sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .width(183.dp)
                .height(49.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFF212121))
                .clickable { onAction() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (role == UserRole.LANDLORD) "Добавить" else "Найти",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                letterSpacing = (-0.4).sp
            )
        }
    }
}

@Preview(showBackground = true, name = "Арендодатель — всё хорошо")
@Composable
private fun PreviewRoleScreenLandlord() {
    RentManagerTheme {
        val vm = RoleViewModel()
        vm.setRole(UserRole.LANDLORD)
        RoleScreen(role = UserRole.LANDLORD, viewModel = vm)
    }
}

@Preview(showBackground = true, name = "Арендатор — задолженность")
@Composable
private fun PreviewRoleScreenTenant() {
    RentManagerTheme {
        val vm = RoleViewModel()
        vm.setRole(UserRole.TENANT, hasDebt = true)
        RoleScreen(role = UserRole.TENANT, viewModel = vm)
    }
}

@Preview(showBackground = true, name = "Нет сделок")
@Composable
private fun PreviewRoleScreenEmpty() {
    RentManagerTheme {
        val vm = RoleViewModel()
        vm.setRole(UserRole.LANDLORD, hasDeals = false)
        RoleScreen(role = UserRole.LANDLORD, viewModel = vm)
    }
}