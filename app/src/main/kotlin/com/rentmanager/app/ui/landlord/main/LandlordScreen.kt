package com.rentmanager.app.ui.landlord.main

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

/**
 * Landlord main dashboard — Figma: Модуль арендодателя, 393×880dp (node=162:4145).
 * No scroll — all content fits the screen.
 */
@Composable
fun LandlordScreen(
    onNavigateToMyProperties: () -> Unit,
    onNavigateToTenants: () -> Unit,
    onNavigateToOtherProperties: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onBackToMain: () -> Unit,
    viewModel: LandlordViewModel = viewModel()
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
            // Content area — Figma: Frame 126, padding horizontal 20dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Top gap
                Spacer(modifier = Modifier.height(8.dp))

                // Header — Figma: "Арендодатель" Lato SemiBold 24px, letterSpacing -0.3
                Text(
                    text = "Арендодатель",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    letterSpacing = (-0.3).sp
                )

                // Gap header → stats: Figma itemSpacing=28 (Frame 126)
                Spacer(modifier = Modifier.height(28.dp))

                // Stats block — Figma: Frame 127 (itemSpacing=12)
                LandlordStatsSection(
                    paymentDate = uiState.nextPaymentDate,
                    paymentAmount = uiState.nextPaymentAmount,
                    monthlyIncome = uiState.monthlyIncome,
                    isPaid = uiState.isPaid
                )

                // Gap stats → cards
                Spacer(modifier = Modifier.height(8.dp))

                // Cards grid (2 columns × 3 rows) — Figma: Frame 2087329453, 370dp height
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
                            onClick = {
                                when (card.id) {
                                    "1" -> onNavigateToMyProperties()
                                    "2" -> onNavigateToTenants()
                                    "3" -> onNavigateToOtherProperties()
                                    "4" -> onNavigateToFinance()
                                    "5" -> onNavigateToMessages()
                                    "6" -> onNavigateToOtherProperties()
                                    else -> viewModel.onCardClick(card.id)
                                }
                            }
                        )
                    }
                }

                // Gap cards → premium
                Spacer(modifier = Modifier.height(8.dp))

                // Premium banner — Figma: Frame 140 (y=624)
                PremiumBanner(
                    modifier = Modifier
                )

                // Bottom gap
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Landlord stats section — Figma: Frame 127 / Frame 111.
 * Shows next payment date, amount, income by all objects, and status badge.
 * Pixel-perfect colors from Figma.
 */
@Composable
private fun LandlordStatsSection(
    paymentDate: String,
    paymentAmount: String,
    monthlyIncome: String,
    isPaid: Boolean
) {
    Column(
        modifier = Modifier.width(353.dp)
    ) {
        // Row: next payment (left, 163dp) | income (right, x=207)
        Row(
            modifier = Modifier.width(353.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left: next payment — Figma: Frame 109 (w=163)
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

            // Right: income across all objects — Figma: Frame 113 (x=207, w=122)
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

        // Gap: Figma Frame 127 itemSpacing=12
        Spacer(modifier = Modifier.height(12.dp))

        // Status badge — Figma: Frame 97, 353×48dp, cornerRadius 100
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