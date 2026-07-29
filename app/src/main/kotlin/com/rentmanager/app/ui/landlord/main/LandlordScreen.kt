package com.rentmanager.app.ui.landlord.main

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Landlord main dashboard — Figma: Модуль арендодателя, 393×880dp.
 * Shows income stats, premium banner, and 6 navigation cards.
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
            // System Bar placeholder — Figma: 2. System Bar, 53dp
            Spacer(modifier = Modifier.height(53.dp))

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Header — Figma: Frame 126 / "Арендодатель" Lato SemiBold 24px, letterSpacing -0.3
                Text(
                    text = "Арендодатель",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.height(28.dp))

                // Income stats block — Figma: Frame 127 / Frame 111
                LandlordStatsSection(
                    paymentDate = uiState.nextPaymentDate,
                    paymentAmount = uiState.nextPaymentAmount,
                    monthlyIncome = uiState.monthlyIncome,
                    isPaid = uiState.isPaid
                )

                // Premium banner — Figma: Frame 140, identical to TenantScreen
                Spacer(modifier = Modifier.height(12.dp))
                PremiumBanner()

                // Cards grid (2 columns × 3 rows) — Figma: Frame 2087329453
                Spacer(modifier = Modifier.height(5.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(370.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(uiState.cards) { card ->
                        LandlordCardItem(
                            title = card.title,
                            iconLabel = card.iconPlaceholder,
                            onClick = {
                                when (card.id) {
                                    "1" -> onNavigateToMyProperties()
                                    "2" -> onNavigateToTenants()
                                    "3" -> onNavigateToFinance()
                                    "4" -> onNavigateToMessages()
                                    "6" -> onNavigateToOtherProperties()
                                    else -> viewModel.onCardClick(card.id)
                                }
                            }
                        )
                    }
                }
            }

            // Bottom Tab Bar — Figma: 4. Tab Bar, 95dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .padding(top = 33.dp)
                    .background(Color(0xFFF3F3F3)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .clickable { onBackToMain() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "На Главную",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

/**
 * Landlord stats section — Figma: Frame 127 / Frame 111.
 * Shows next payment date, amount, income by all objects, and status badge.
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
        // Row: next payment (left) | income (right)
        Row(
            modifier = Modifier
                .width(353.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left: next payment — Figma: Frame 109
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // "Ближайшее поступление 10.02.2026" — Inter Regular 14px
                Text(
                    text = "Ближайшее поступление $paymentDate",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    letterSpacing = (-0.4).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Amount — Inter Medium 16px, #151515E5
                Text(
                    text = paymentAmount,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xE5151515),
                    letterSpacing = (-0.4).sp
                )
            }

            // Right: income across all objects — Figma: Frame 113
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // "Доход по всем объектам" — Inter Regular 14px, #15151599
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
                // Monthly income — Inter Medium 16px, #151515E5
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

        // Status badge — Figma: Frame 97, 353×48dp, cornerRadius 100
        Box(
            modifier = Modifier
                .width(353.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isPaid) {
                            listOf(Color(0xFF9DD68D), Color(0xFF418B2D))
                        } else {
                            listOf(Color(0xFFF4A259), Color(0xFFE53935))
                        }
                    )
                ),
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

/**
 * Premium banner — Figma: Frame 140, 353×153dp, cornerRadius 30, #FEFFBB background.
 * Identical to TenantScreen premium banner.
 */
@Composable
private fun PremiumBanner() {
    Box(
        modifier = Modifier
            .width(353.dp)
            .height(153.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFFFEFFBB))
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Премиум",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xE5151515),
                    letterSpacing = (-0.4).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Условие\nУсловие",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xCC151515),
                    letterSpacing = (-0.4).sp,
                    lineHeight = 17.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF212121))
                    .clickable { }
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Подключить",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    letterSpacing = (-0.4).sp
                )
            }
        }
    }
}

/**
 * Landlord card — Figma: 174×120dp cards, cornerRadius 30, #EFEFEF background.
 * Icon: 50dp from Iconsans Bold set, Label: Lato Bold 15px.
 */
@Composable
private fun LandlordCardItem(
    title: String,
    iconLabel: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(174.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFFEFEFEF))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon placeholder (50dp)
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconLabel,
                    fontSize = 50.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Label — Lato Bold 15px, #151515E5
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xE5151515),
                letterSpacing = (-0.4).sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}