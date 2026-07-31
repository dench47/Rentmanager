package com.rentmanager.app.ui.tenant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rentmanager.app.ui.components.BlackPaymentButton
import com.rentmanager.app.ui.components.DashboardCard
import com.rentmanager.app.ui.components.PremiumBanner

@Composable
fun TenantScreen(
    onBack: () -> Unit,
    onServiceClick: (String) -> Unit,
    onPayClick: () -> Unit,
    viewModel: TenantViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Content with horizontal padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Top gap
                Spacer(modifier = Modifier.height(8.dp))

                // Header — Figma: Frame 126 / "Арендатор" Lato SemiBold 24px
                Text(
                    text = "Арендатор",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.height(28.dp))

                // Payment info — Figma: Frame 2087329454
                PaymentInfoSection(
                    paymentDate = uiState.nextPaymentDate,
                    paymentAmount = uiState.nextPaymentAmount,
                    isPaid = uiState.isPaid
                )

                // Payment button (if enabled)
                if (uiState.hasPaymentButton) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BlackPaymentButton(
                        text = "Оплатить",
                        onClick = onPayClick
                    )
                }

                // Services grid (2 columns × 3 rows) — 370dp fixed
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(370.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    userScrollEnabled = false
                ) {
                    items(uiState.services) { service ->
                        DashboardCard(
                            iconRes = service.iconRes,
                            title = service.title,
                            twoLines = service.twoLines,
                            onClick = { onServiceClick(service.id) }
                        )
                    }
                }

                // Premium banner
                Spacer(modifier = Modifier.height(8.dp))
                PremiumBanner()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Payment info block — Figma: Frame 2087329454 / Frame 111
 * Shows next payment date, amount, and status badge.
 * Pixel-perfect colors from Figma.
 */
@Composable
private fun PaymentInfoSection(
    paymentDate: String,
    paymentAmount: String,
    isPaid: Boolean
) {
    Column(
        modifier = Modifier.width(353.dp)
    ) {
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

        // Status badge — Figma: Frame 97, 353×48dp, cornerRadius 100
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