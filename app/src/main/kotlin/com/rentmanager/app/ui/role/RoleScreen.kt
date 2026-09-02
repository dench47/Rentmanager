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
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.ui.components.DashboardCard
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.ui.landlord.createproperty.rememberCtaGradient

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
                // Часы статус-бара → заголовок = 42dp (замер по макету 2596-22499)
                Spacer(Modifier.height(27.dp))

                // Header with back arrow (Figma: Toolbar 50dp = 13 сверху + 24 текст + 13 снизу)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 0.dp, top = 0.dp, bottom = 13.dp, end = 0.dp),
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

                // Секция статистики (Figma 2596-22499/2533-17398/2533-17422):
                // у арендодателя — статистика + плашка-статус по ситуации;
                // без единого объекта — вместо них инфо-карточка (Figma 2523-28358).
                // 105dp у арендодателя: без мёртвого запаса, зазор плашка→карточки ≈ 24.5dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            when {
                                uiState.role != UserRole.LANDLORD -> 119.dp
                                uiState.hasDeals -> 105.dp
                                else -> 116.dp // текст 2×18 + зазор 10 + пилюля 55 + низ 14
                            }
                        )
                ) {
                    when {
                        uiState.isLoading -> { }
                        uiState.role == UserRole.LANDLORD && !uiState.hasDeals -> LandlordNoObjectsCard(
                            onAddFirstObject = onNavigateToCreateProperty
                        )
                        uiState.role == UserRole.LANDLORD -> LandlordStatsSection(
                            uiState.nextPaymentDate,
                            uiState.nextPaymentAmount,
                            uiState.monthlyIncome,
                            uiState.hasDebt,
                            uiState.hasDeals,
                            uiState.debtAmount,
                            uiState.hasActiveRent
                        )
                        !uiState.hasDeals -> EmptyStateBlock(
                            role = uiState.role,
                            onAction = { onNavigateToOtherProperties() }
                        )
                        else -> TenantStatsSection(uiState.nextPaymentDate, uiState.nextPaymentAmount, uiState.hasDebt, onPay = { viewModel.pay() })
                    }
                }
                Spacer(Modifier.height(12.dp))

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

@Composable
fun GradientCtaButton(iconRes: Int, text: String, onClick: () -> Unit) {
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
private fun LandlordStatsSection(
    paymentDate: String,
    paymentAmount: String,
    monthlyIncome: String,
    hasDebt: Boolean,
    hasDeals: Boolean,
    debtAmount: String,
    hasActiveRent: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            Column(modifier = Modifier.width(163.dp)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFF727272))) { append("Ближайшее поступление ") }
                        withStyle(SpanStyle(color = Color(0xFF151515))) { append(paymentDate) }
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = InterFontFamily,
                    lineHeight = 18.sp,
                    letterSpacing = (-0.4).sp,
                    maxLines = 2
                )
                // С датой под лейблом ничего нет — ниже сразу плашка (макет 2596-22510);
                // без даты — прочерк
                if (paymentDate.isBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "—",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily,
                        color = Color(0xFF212121),
                        letterSpacing = (-0.4).sp
                    )
                }
            }
            Column(modifier = Modifier.width(183.dp)) {
                Text("Доход по всем объектам", fontSize = 13.sp, fontWeight = FontWeight.Normal, fontFamily = InterFontFamily, color = Color(0xFF727272), lineHeight = 18.sp, letterSpacing = (-0.4).sp, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(
                    // При жёлтой плашке «Нет активной аренды» в макете 2596-22510
                    // под обеими колонками прочерки — никаких «0 ₽/мес»
                    if (!hasActiveRent) "—" else monthlyIncome.ifBlank { "—" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFontFamily,
                    color = Color(0xFF212121),
                    letterSpacing = (-0.4).sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        // Плашка-статус (Figma 2596-22499 / 2533-17398 / 2533-17422):
        // нет аренды — жёлтая, задолженность — красная с суммой, всё хорошо — зелёная
        val (bg, fg, text) = when {
            !hasActiveRent -> Triple(Color(0xFFFFF1CF), Color(0xFF212121), "Нет активной аренды")
            hasDebt -> Triple(Color(0xFFFBEAEC), Color(0xFFFF4249), "Задолженность $debtAmount")
            else -> Triple(Color(0xFFE5F2E7), Color(0xFF2F7D4D), "Задолженностей нет")
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = InterFontFamily,
                color = fg,
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

// Инфо-карточка «нет ни одного объекта» (Figma 2523-28358): белый фон r20,
// слева текст в две строки, ниже чёрная пилюля «Добавить первый объект» (55dp,
// как все CTA проекта) — тап открывает стандартный поток создания объекта
@Composable
private fun LandlordNoObjectsCard(onAddFirstObject: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Ведите аренду, платежи, договоры и показания счётчиков в одном месте.",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFontFamily,
            color = Color(0xFF212121),
            letterSpacing = (-0.4).sp,
            lineHeight = 18.sp
        )
        CtaButton(
            iconRes = R.drawable.ic_cta_plus,
            text = "Добавить первый объект",
            onClick = onAddFirstObject
        )
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
