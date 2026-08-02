package com.rentmanager.app.ui.role

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rentmanager.app.ui.components.DashboardCard
import com.rentmanager.app.ui.components.PremiumBanner

@Composable
fun RoleScreen(
    role: UserRole,
    onNavigateToMyProperties: () -> Unit = {},
    onNavigateToTenants: () -> Unit = {},
    onNavigateToOtherProperties: () -> Unit = {},
    onNavigateToFinance: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToLandlordsList: () -> Unit = {},
    onBackToMain: () -> Unit = {},
    viewModel: RoleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Set role on first composition
    androidx.compose.runtime.LaunchedEffect(role) {
        viewModel.setRole(role)
    }

    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Header
                Text(
                    text = uiState.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Stats section — only if hasProperties = true
                if (uiState.hasProperties) {
                    when (uiState.role) {
                        UserRole.LANDLORD -> LandlordStatsSection(
                            paymentDate = uiState.nextPaymentDate,
                            paymentAmount = uiState.nextPaymentAmount,
                            monthlyIncome = uiState.monthlyIncome,
                            isPaid = uiState.isPaid
                        )
                        UserRole.TENANT -> TenantStatsSection(
                            paymentDate = uiState.nextPaymentDate,
                            paymentAmount = uiState.nextPaymentAmount,
                            isPaid = uiState.isPaid
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Cards grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(370.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    userScrollEnabled = false
                ) {
                    items(uiState.cards) { card ->
                        DashboardCard(
                            iconRes = card.iconRes,
                            title = card.title,
                            twoLines = card.twoLines,
                            onClick = {
                                when (uiState.role) {
                                    UserRole.LANDLORD -> {
                                        when (card.id) {
                                            "1" -> onNavigateToMyProperties()
                                            "2" -> onNavigateToTenants()
                                            "3" -> onNavigateToOtherProperties()
                                            "4" -> onNavigateToFinance()
                                            "5" -> onNavigateToMessages()
                                            "6" -> onNavigateToOtherProperties()
                                        }
                                    }
                                    UserRole.TENANT -> {
                                        when (card.id) {
                                            "2" -> onNavigateToLandlordsList()
                                            // other tenant services
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                PremiumBanner()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LandlordStatsSection(
    paymentDate: String,
    paymentAmount: String,
    monthlyIncome: String,
    isPaid: Boolean
) {
    Column(modifier = Modifier.width(353.dp)) {
        Row(
            modifier = Modifier.width(353.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(modifier = Modifier.width(163.dp)) {
                Text(
                    text = "Ближайшее поступление $paymentDate",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0x99151515),
                    letterSpacing = (-0.4).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = paymentAmount,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xE5151515),
                    letterSpacing = (-0.4).sp
                )
            }
            Column(modifier = Modifier.width(122.dp)) {
                Text(
                    text = "Доход \nпо всем объектам",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0x99151515),
                    letterSpacing = (-0.4).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = monthlyIncome,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xE5151515),
                    letterSpacing = (-0.4).sp
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .width(353.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPaid) "Просрочек нет" else "Просрочено",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF7AB66A),
                letterSpacing = (-0.4).sp
            )
        }
    }
}

@Composable
private fun TenantStatsSection(
    paymentDate: String,
    paymentAmount: String,
    isPaid: Boolean
) {
    Column(modifier = Modifier.width(353.dp)) {
        Text(
            text = "Ближайший платеж до $paymentDate",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0x99151515),
            letterSpacing = (-0.4).sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = paymentAmount,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xE5151515),
            letterSpacing = (-0.4).sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .width(353.dp)
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isPaid) "Просрочек нет" else "Просрочено",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF7AB66A),
                letterSpacing = (-0.4).sp
            )
        }
    }
}