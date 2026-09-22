package com.rentmanager.app.ui.landlord.tenants

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.data.model.TenantDto
import com.rentmanager.app.ui.components.EmptyContactsState
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.ui.theme.RentManagerTheme

@Composable
fun TenantsListScreen(
    onTenantClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TenantsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var tenantToDelete by remember { mutableStateOf<TenantDto?>(null) }

    // Обновление списка при возврате на экран
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            // Паттерн эталонных экранов: Scaffold paddingValues (стабильны с первого кадра;
            // явный statusBarsPadding на переходе «нырял» вниз) → 27 → строка(20/13)
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.White)
            ) {
                Spacer(Modifier.height(27.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.clickable { onBack() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(painter = painterResource(R.drawable.ic_landlord_back), contentDescription = "Назад", modifier = Modifier.size(24.dp))
                        Text("Арендаторы", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, color = Color(0xFF212121), letterSpacing = (-0.3).sp)
                    }
                    if (uiState.tenants.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Image(painter = painterResource(R.drawable.ic_search), contentDescription = "Поиск", modifier = Modifier.size(24.dp).clickable { })
                            Image(painter = painterResource(R.drawable.ic_sort), contentDescription = "Сортировка", modifier = Modifier.size(24.dp).clickable { })
                        }
                    }
                }

                uiState.errorMessage?.let {
                    Text(it, color = Color(0xFFE53935), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                }

                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Загрузка…", fontSize = 14.sp, color = Color(0xFF8E8E93))
                    }
                } else if (uiState.tenants.isNotEmpty()) {
                    LazyColumn(Modifier.fillMaxSize()) {
                    items(uiState.tenants, key = { it.id }) { tenant ->
                        TenantCard(
                            tenant = tenant,
                            onClick = {
                                // Мгновенный рендер карточки: кладём DTO в кеш до навигации
                                TenantCardCache.put(tenant)
                                onTenantClick(tenant.id)
                            },
                            onLongClick = { tenantToDelete = tenant }
                        )
                            HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
                        }
                    }
                }
            }

            // Пустое состояние — оверлей на весь экран: центр блока по полной высоте кадра (макет 3108:56847)
            if (!uiState.isLoading && uiState.tenants.isEmpty()) {
                EmptyContactsState(
                    title = "Арендаторов пока нет",
                    subtitle = "Добавленные арендаторы появятся здесь",
                    ctaText = "Добавить арендатора",
                    onCtaClick = { }
                )
            }
        }
    }

    tenantToDelete?.let { tenant ->
        AlertDialog(
            onDismissRequest = { tenantToDelete = null },
            title = { Text("Удалить арендатора?") },
            text = { Text(tenant.fullName) },
            confirmButton = {
                TextButton(onClick = {
                    tenantToDelete = null
                    viewModel.deleteTenant(tenant.id)
                }) { Text("Удалить", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { tenantToDelete = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun TenantCard(
    tenant: TenantDto,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val displayName = tenant.fullName.ifBlank { tenant.phone }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(tenant.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEFEFEF)),
                contentAlignment = Alignment.Center
            ) {
                // Ава с аккаунта арендатора (если телефон совпал с юзером), иначе инициал
                val avatarUrl = tenant.avatarUrl
                if (avatarUrl != null) {
                    coil.compose.AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        displayName.take(1).uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8E8E93),
                        fontFamily = InterFontFamily
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(displayName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, letterSpacing = (-0.3).sp)
                val subtitle = tenant.companyName.orEmpty()
                if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 13.sp, fontWeight = FontWeight.Normal, color = Color(0x993C3C43), letterSpacing = (-0.4).sp)
            }
        }
    }
}

@Preview(showBackground = true, name = "Список арендаторов")
@Composable
private fun PreviewTenantsList() {
    RentManagerTheme {
        TenantsListScreen(onTenantClick = {}, onBack = {})
    }
}