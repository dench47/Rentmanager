package com.rentmanager.app.ui.role

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.ui.components.DashboardCard
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.ui.landlord.createproperty.rememberCtaGradient
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
    onNavigateToCreateProperty: () -> Unit = {},
    onNavigateToSubscription: () -> Unit = {},
    onNavigateToServices: () -> Unit = {},
    onBackToMain: () -> Unit = {},
    onPay: () -> Unit = {},
    onNavigateToEmptyState: () -> Unit = {},
    viewModel: RoleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(role) {
        viewModel.setRole(role)
        viewModel.refresh(role)
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(13.dp))

                // Header with back arrow
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 0.dp, top = 0.dp, bottom = 24.dp, end = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onBackToMain() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_landlord_back),
                            contentDescription = "Назад",
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(uiState.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, color = Color(0xFF212121), letterSpacing = (-0.3).sp)
                    }
                }

                // Пустое состояние (Figma «Арендодатель_1»): инфо-текст + чёрная CTA «Добавить первый объект»
                if (uiState.role == UserRole.LANDLORD && !uiState.hasDeals && !uiState.isLoading) {
                    LandlordInfBlock(onAddFirstObject = onNavigateToCreateProperty)
                    Spacer(Modifier.height(14.dp))
                } else {
                    // Секция статистики: 3 состояния — нет сделок / всё хорошо / задолженность (Figma INF: 104dp)
                    Box(modifier = Modifier.fillMaxWidth().height(104.dp)) {
                        when {
                            uiState.isLoading -> { }
                            !uiState.hasDeals -> EmptyStateBlock(
                                role = uiState.role,
                                onAction = {
                                    if (uiState.role == UserRole.LANDLORD) onNavigateToMyProperties() else onNavigateToOtherProperties()
                                }
                            )
                            uiState.role == UserRole.LANDLORD -> LandlordStatsSection(uiState.nextPaymentDate, uiState.nextPaymentAmount, uiState.monthlyIncome, uiState.hasDebt)
                            else -> TenantStatsSection(uiState.nextPaymentDate, uiState.nextPaymentAmount, uiState.hasDebt, onPay = { viewModel.pay() })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Cards grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height(384.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    userScrollEnabled = false
                ) {
                    items(uiState.cards) { card ->
                        DashboardCard(
                            iconRes = card.iconRes,
                            title = card.title,
                         onClick = {
                             // Пометка дизайнера (Figma 2533-17834): карточки не «мёртвые» —
                             // без объектов каждая открывает своё пустое состояние
                             if (uiState.role == UserRole.LANDLORD && !uiState.hasDeals && !uiState.isLoading) {
                                 onNavigateToEmptyState()
                             } else {
                                 handleCardClick(card.id, uiState.role, onNavigateToMyProperties, onNavigateToTenants, onNavigateToOtherProperties, onNavigateToFinance, onNavigateToMessages, onNavigateToLandlordsList, onNavigateToTenantProperties, onNavigateToServices)
                             }
                         }
                        )
                    }
                }

                if (uiState.role == UserRole.LANDLORD) {
                    // Figma «Арендодатель_1» (4005-20627): зазоры до/между кнопками 12dp
                    Spacer(Modifier.height(12.dp))
                    GradientCtaButton(
                        iconRes = R.drawable.ic_cta_diamond,
                        text = "Управление подпиской",
                        onClick = onNavigateToSubscription
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlineCtaButton(
                        iconRes = R.drawable.ic_cta_pin,
                        text = "Разместить объявление о сдаче",
                        onClick = onNavigateToMyProperties
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                    CtaButton(
                        iconRes = R.drawable.ic_subscription_diamond,
                        text = "Управление подпиской",
                        onClick = { /* TODO: подписка */ }
                    )
                    Spacer(Modifier.height(12.dp))
                    CtaButton(
                        iconRes = R.drawable.ic_find_rent,
                        text = "Найти и арендовать",
                        onClick = onNavigateToOtherProperties
                    )
                }

            }
        }
    }
}

@Composable
private fun CtaButton(
    iconRes: Int,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xFF212121))
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Color.White)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            color = Color.White,
            letterSpacing = (-0.4).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Инфо-блок пустого состояния экрана «Арендодатель» (Figma node 2533:17798, фрейм Inf):
 * центрированный текст + чёрная pill-кнопка «Добавить первый объект».
 */
@Composable
private fun LandlordInfBlock(onAddFirstObject: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Ведите аренду, платежи, договоры и показания счётчиков в одном месте.",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            color = Color(0xFF212121),
            letterSpacing = (-0.4).sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFF212121))
                .clickable { onAddFirstObject() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_cta_plus),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Добавить первый объект",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                color = Color.White,
                letterSpacing = (-0.4).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Градиентная CTA-кнопка (Figma: linear-gradient 136°, #F6D85E 19% → #E89B5A 60% → #D97D5D 100%).
 */
@Composable
private fun GradientCtaButton(iconRes: Int, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(brush = rememberCtaGradient())
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            color = Color(0xFF212121),
            letterSpacing = (-0.4).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Контурная CTA-кнопка (Figma: белый фон, рамка 1px Grey/Text #727272, радиус 100).
 */
@Composable
private fun OutlineCtaButton(iconRes: Int, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFF727272), RoundedCornerShape(100.dp))
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            color = Color(0xFF212121),
            letterSpacing = (-0.4).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun handleCardClick(
    cardId: String, role: UserRole,
    onMyProperties: () -> Unit, onTenants: () -> Unit, onOther: () -> Unit,
    onFinance: () -> Unit, onMessages: () -> Unit, onLandlords: () -> Unit,
    onTenantProperties: () -> Unit, onServices: () -> Unit
) {
    when (role) {
        UserRole.LANDLORD -> when (cardId) {
            "1" -> onMyProperties()
            "2" -> onTenants()
            "3" -> onOther()
            "4" -> onFinance()
            "5" -> onMessages()
            "6" -> onServices()
        }
        UserRole.TENANT -> when (cardId) {
            "1" -> onTenantProperties()
            "2" -> onLandlords()
        }
    }
}

@Composable
private fun LandlordStatsSection(paymentDate: String, paymentAmount: String, monthlyIncome: String, hasDebt: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 14.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                Column(modifier = Modifier.width(163.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFF727272))) { append("Ближайшее поступление ") }
                            withStyle(SpanStyle(color = Color(0xCC212121))) { append(paymentDate) }
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = InterFontFamily,
                        lineHeight = 18.sp,
                        letterSpacing = (-0.4).sp,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(paymentAmount, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, color = Color(0xFF212121), letterSpacing = (-0.4).sp)
                }
                Column(modifier = Modifier.width(122.dp)) {
                    Text("Доход \nпо всем объектам", fontSize = 13.sp, fontWeight = FontWeight.Normal, fontFamily = InterFontFamily, color = Color(0xFF727272), lineHeight = 18.sp, letterSpacing = (-0.4).sp)
                    Spacer(Modifier.height(4.dp))
                    Text(monthlyIncome, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, color = Color(0xFF212121), letterSpacing = (-0.4).sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (hasDebt) "Есть задолженность" else "Просрочек нет",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFontFamily,
                color = if (hasDebt) Color(0xFFFF4249) else Color(0xFF66A256),
                letterSpacing = (-0.4).sp
            )
        }
    }
}

@Composable
private fun TenantStatsSection(paymentDate: String, paymentAmount: String, hasDebt: Boolean, onPay: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(bottom = 10.dp)) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color(0xFF727272))) { append("Ближайший платеж до ") }
                withStyle(SpanStyle(color = Color(0xCC212121))) { append(paymentDate) }
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = InterFontFamily,
            lineHeight = 18.sp,
            letterSpacing = (-0.4).sp
        )
        Spacer(Modifier.height(4.dp))
        Text(paymentAmount, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, color = Color(0xFF212121), letterSpacing = (-0.4).sp)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (hasDebt) "Есть задолженность" else "Просрочек нет",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFontFamily,
                color = if (hasDebt) Color(0xFFFF4249) else Color(0xFF66A256),
                letterSpacing = (-0.4).sp
            )
            Box(
                modifier = Modifier
                    .width(183.dp)
                    .height(55.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF212121))
                    .clickable { onPay() },
                contentAlignment = Alignment.Center
            ) {
                Text("Оплатить", fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, color = Color.White, letterSpacing = (-0.4).sp)
            }
        }
    }
}

@Composable
private fun EmptyStateBlock(role: UserRole, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF5F5F5)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (role == UserRole.LANDLORD) "Добавьте первый объект" else "Найдите объект",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = InterFontFamily,
            color = Color(0xFF212121),
            letterSpacing = (-0.4).sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .width(183.dp)
                .height(55.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFF212121))
                .clickable { onAction() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (role == UserRole.LANDLORD) "Добавить" else "Найти",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFontFamily,
                color = Color.White,
                letterSpacing = (-0.4).sp
            )
        }
    }
}
