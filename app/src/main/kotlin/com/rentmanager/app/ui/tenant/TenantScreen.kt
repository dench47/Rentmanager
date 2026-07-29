package com.rentmanager.app.ui.tenant

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
import com.rentmanager.app.ui.components.BlackPaymentButton

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
            // System Bar placeholder — Figma: 2. System Bar, 53dp
            Spacer(modifier = Modifier.height(53.dp))

            // Content with horizontal padding (20dp = horizontal margin from Figma 393-353=40 → 20 each side)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
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

                // Payment button (if enabled) — Figma: Frame 127 кнопка «Оплатить»
                if (uiState.hasPaymentButton) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BlackPaymentButton(
                        text = "Оплатить",
                        onClick = onPayClick
                    )
                }

                // Premium banner — Figma: Frame 140
                Spacer(modifier = Modifier.height(12.dp))
                PremiumBanner()

                // Services grid (2 columns × 3 rows) — Figma: Frame 2087329453
                Spacer(modifier = Modifier.height(5.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(370.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(uiState.services) { service ->
                        ServiceCardItem(
                            title = service.title,
                            iconLabel = service.iconPlaceholder,
                            onClick = { onServiceClick(service.id) }
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
                        .clickable { onBack() },
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
 * Payment info block — Figma: Frame 2087329454 / Frame 111
 * Shows next payment date, amount, and status badge.
 */
@Composable
private fun PaymentInfoSection(
    paymentDate: String,
    paymentAmount: String,
    isPaid: Boolean
) {
    Column(
        modifier = Modifier
            .width(353.dp)
            .padding(vertical = 8.dp)
    ) {
        // Next payment date — Figma: Inter Regular 14px
        Text(
            text = "Ближайший платеж до $paymentDate",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            letterSpacing = (-0.4).sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Payment amount — Figma: Inter Medium 16px, #151515E5
        Text(
            text = paymentAmount,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xE5151515),
            letterSpacing = (-0.4).sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Status badge — Figma: Frame 97, 353×48dp, cornerRadius 100, gradient
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
            // Left side — title + conditions
            Column(modifier = Modifier.weight(1f)) {
                // "Премиум" — Figma: Lato Bold 20px, #151515E5
                Text(
                    text = "Премиум",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xE5151515),
                    letterSpacing = (-0.4).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                // "Условие / Условие" — Figma: Inter Regular 14px, #151515CC
                Text(
                    text = "Условие\nУсловие",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xCC151515),
                    letterSpacing = (-0.4).sp,
                    lineHeight = 17.sp
                )
            }

            // "Подключить" button — Figma: Frame 7, cornerRadius 100, #212121
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
 * Service card — Figma: 174×120dp cards, cornerRadius 30, #EFEFEF background.
 * Icon: 50dp (Iconsans Bold icons), Label: Lato Bold 15px.
 */
@Composable
private fun ServiceCardItem(
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
            // Icon placeholder (50dp, would be replaced with real icons)
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
            // Label — Figma: Lato Bold 15px, #151515E5
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