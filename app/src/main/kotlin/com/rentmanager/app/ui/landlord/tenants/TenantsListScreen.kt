package com.rentmanager.app.ui.landlord.tenants

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.data.model.TenantDto
import com.rentmanager.app.ui.components.ContactRow
import com.rentmanager.app.ui.components.ContactRowDivider
import com.rentmanager.app.ui.components.ContactsToolbar
import com.rentmanager.app.ui.components.EmptyContactsState
import com.rentmanager.app.ui.components.RentFilter
import com.rentmanager.app.ui.components.RentFilterPopup
import com.rentmanager.app.ui.components.tenantRentSubtitle
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.ui.theme.RentManagerTheme

@Composable
fun TenantsListScreen(
    onTenantClick: (String) -> Unit,
    onBack: () -> Unit,
    onAddTenant: () -> Unit = {},
    viewModel: TenantsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var tenantToDelete by remember { mutableStateOf<TenantDto?>(null) }

    // Удаление из карточки: «Арендатор был удален» на фоне списка (3014:22433)
    var deletedTenantId by remember { mutableStateOf<String?>(null) }

    // Лупа и фильтр (канвас 13): поиск в тулбаре + поповер состояний аренды
    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var rentFilter by remember { mutableStateOf(RentFilter.ALL) }
    var filterOpen by remember { mutableStateOf(false) }

    // Обновление списка при возврате на экран
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load()
                deletedTenantId = DeletedTenantNotice.take()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Сначала фильтр по состоянию аренды, затем поиск по имени/компании
    val byFilter = uiState.tenants.filter { rentFilter.matches(it.rentStatus) }
    val visible = byFilter.filter { tenant ->
        val q = query.trim()
        q.isEmpty() ||
            tenant.fullName.contains(q, ignoreCase = true) ||
            tenant.companyName.orEmpty().contains(q, ignoreCase = true)
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            // Паттерн эталонных экранов: Scaffold paddingValues (стабильны с первого кадра;
            // явный statusBarsPadding на переходе «нырял» вниз) → 27 → строка(20/13)
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).background(Color.White)
            ) {
                Spacer(Modifier.height(27.dp))
                ContactsToolbar(
                    title = "Арендаторы",
                    searchActive = searchActive,
                    query = query,
                    onQueryChange = { query = it },
                    onSearchClick = { searchActive = true },
                    onFilterClick = { filterOpen = true },
                    onBack = {
                        if (searchActive) {
                            searchActive = false
                            query = ""
                        } else {
                            onBack()
                        }
                    },
                    showActions = visible.isNotEmpty()
                )

                if (uiState.isLoading) {
                    // Первый запуск без кэша: глобус + объяснение по центру.
                    // fillMaxWidth обязателен: вес даёт только высоту, без него
                    // Box схлопывается по ширине и текст уезжает влево
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(R.drawable.ic_globe_warning_vec),
                                contentDescription = null,
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Загружаем арендаторов…",
                                fontSize = 15.sp,
                                lineHeight = 18.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily,
                                letterSpacing = (-0.4).sp,
                                color = Color(0xFF212121)
                            )
                        }
                    }
                } else if (visible.isNotEmpty()) {
                    LazyColumn(Modifier.weight(1f)) {
                        items(visible, key = { it.id }) { tenant ->
                            TenantCard(
                                tenant = tenant,
                                onClick = {
                                    // Мгновенный рендер карточки: кладём DTO в кеш до навигации
                                    TenantCardCache.put(tenant)
                                    onTenantClick(tenant.id)
                                },
                                onLongClick = { tenantToDelete = tenant }
                            )
                            ContactRowDivider()
                        }
                    }
                } else if (uiState.errorMessage != null) {
                    // Сети нет и данных нет: не показываем «Арендаторов пока нет»
                    // (это враньё) — глобус с объяснением, тап повторяет загрузку
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.load() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(R.drawable.ic_globe_warning_vec),
                                contentDescription = null,
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Нет связи с сервером",
                                fontSize = 15.sp,
                                lineHeight = 18.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily,
                                letterSpacing = (-0.4).sp,
                                color = Color(0xFF212121)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Проверьте подключение и попробуйте ещё раз",
                                fontSize = 13.sp,
                                lineHeight = 15.7.sp,
                                letterSpacing = (-0.4).sp,
                                color = Color(0xFF727272)
                            )
                        }
                    }
                }

                // ---- Тапбар с CTA «Добавить арендатора» (3122:60008) — ТОЛЬКО
                // при непустом списке: в пустом состоянии (3108:56847) CTA живёт
                // в самой карточке-заглушке, второй кнопки быть не может ----
                if (visible.isNotEmpty()) Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                        .background(Color(0xFFEDEDED).copy(alpha = 0.9f))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFF212121))
                            .clickable { onAddTenant() },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_cta_person_plus_white),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Добавить арендатора",
                            fontSize = 15.sp,
                            lineHeight = 18.2.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = InterFontFamily,
                            letterSpacing = (-0.4).sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Пустое состояние — оверлей на весь экран: центр блока по полной
            // высоте кадра (макет 3108:56847). Только БЕЗ ошибки: при обрыве
            // сети «Арендаторов пока нет» — ложь, там свой глобус выше
            // Три взаимоисключающих пустых состояния (канвас 13, состояние
            // «сеть упала» — отдельно выше): списка нет / фильтр пуст / поиск пуст
            if (!uiState.isLoading && uiState.errorMessage == null && uiState.tenants.isEmpty()) {
                EmptyContactsState(
                    title = "Арендаторов пока нет",
                    subtitle = "Добавленные арендаторы появятся здесь",
                    ctaText = "Добавить арендатора",
                    onCtaClick = onAddTenant
                )
            } else if (!uiState.isLoading && uiState.errorMessage == null &&
                uiState.tenants.isNotEmpty() && byFilter.isEmpty()
            ) {
                // 3124:64037 — фильтр ничего не нашёл: CTA без иконки, сброс фильтра
                EmptyContactsState(
                    title = "Нет арендаторов с таким статусом",
                    subtitle = "Выберите другой статус или покажите весь список",
                    ctaText = "Показать всех",
                    ctaIconRes = null,
                    illustration = painterResource(R.drawable.ic_contacts_not_found),
                    onCtaClick = { rentFilter = RentFilter.ALL }
                )
            } else if (!uiState.isLoading && uiState.errorMessage == null &&
                uiState.tenants.isNotEmpty() && visible.isEmpty()
            ) {
                // 3157:64495 — поиск ничего не нашёл
                EmptyContactsState(
                    title = "Ничего не найдено",
                    subtitle = "Проверьте написание или добавьте нового арендатора",
                    ctaText = "Добавить арендатора",
                    illustration = painterResource(R.drawable.ic_contacts_not_found),
                    onCtaClick = onAddTenant
                )
            }

            // Окошко фильтра: справа 20, сверху — низ тулбара + 2 (макет 3110:57109)
            if (filterOpen) {
                RentFilterPopup(
                    selected = rentFilter,
                    onSelect = {
                        rentFilter = it
                        filterOpen = false
                    },
                    onDismiss = { filterOpen = false },
                    topPadding = 66.dp
                )
            }
        }
    }

    // Список есть (из кэша), но тихий рефреш упал — канонный диалог с глобусом
    // (как в «Моя недвижимость»); при пустом списке диалог не дублируем —
    // там инлайн-состояние с объяснением
    uiState.errorMessage?.takeIf { uiState.tenants.isNotEmpty() }?.let { msg ->
        com.rentmanager.app.ui.components.IconNotificationDialog(
            iconRes = R.drawable.ic_globe_warning_vec,
            text = msg,
            onDismiss = { viewModel.clearError() }
        )
    }

    // «Арендатор был удален» — после возврата с карточки (3014:22433)
    deletedTenantId?.let { id ->
        com.rentmanager.app.ui.components.CanonicalDialog(
            onDismiss = { deletedTenantId = null },
            icon = R.drawable.ic_success_check,
            title = "Арендатор был удален"
        ) {
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Отменить удаление",
                container = Color(0xFF212121),
                textColor = Color.White,
                onClick = {
                    deletedTenantId = null
                    viewModel.restoreTenant(id)
                }
            )
        }
    }

    tenantToDelete?.let { tenant ->
        com.rentmanager.app.ui.components.CanonicalDialog(
            onDismiss = { tenantToDelete = null },
            title = "Удалить карточку арендатора?",
            text = "Личные данные и прикрепленные документы будут удалены"
        ) {
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Удалить карточку",
                container = Color(0xFFFF4249),
                textColor = Color.White,
                onClick = {
                    tenantToDelete = null
                    viewModel.deleteTenant(tenant.id)
                }
            )
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Отменить",
                stroke = Color(0xFF212121),
                textColor = Color(0xFF212121),
                onClick = { tenantToDelete = null }
            )
        }
    }
}

@Composable
private fun TenantCard(
    tenant: TenantDto,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Вторая строка — объект и срок аренды (канвас 13, заметки Вики):
    // «БЦ Легенда · до 23.12.2027» / «… · завершено 01.02.2024» /
    // «Аренда ещё не оформлялась». Пока сервер не отдаёт rent_status,
    // показываем прежнюю вторую строку (название компании) — без вранья.
    // Зелёная точка — «арендует сейчас».
    val subtitle = if (tenant.rentStatus == null) {
        tenant.companyName.orEmpty()
    } else {
        tenantRentSubtitle(tenant.propertyTitle, tenant.rentEndDate, tenant.rentStatus)
    }
    ContactRow(
        title = tenant.fullName.ifBlank { tenant.phone },
        subtitle = subtitle,
        avatarUrl = tenant.avatarUrl,
        showActiveDot = tenant.rentStatus == "active",
        phone = tenant.phone,
        onClick = onClick,
        onLongClick = onLongClick
    )
}

@Preview(showBackground = true, name = "Список арендаторов")
@Composable
private fun PreviewTenantsList() {
    RentManagerTheme {
        TenantsListScreen(onTenantClick = {}, onBack = {})
    }
}