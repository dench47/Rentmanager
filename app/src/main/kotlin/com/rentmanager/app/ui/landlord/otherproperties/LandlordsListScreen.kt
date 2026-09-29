package com.rentmanager.app.ui.landlord.otherproperties

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.ContactRow
import com.rentmanager.app.ui.components.ContactRowDivider
import com.rentmanager.app.ui.components.ContactsToolbar
import com.rentmanager.app.ui.components.EmptyContactsState
import com.rentmanager.app.ui.components.RentFilter
import com.rentmanager.app.ui.components.RentFilterPopup
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.ui.theme.RentManagerTheme

/**
 * Список арендодателей (канвас 13, 3122:60026). Строка: аватар 40 (фото или
 * силуэт) + имя 15/600 + название компании 13/400 + кнопки «письмо»/«звонок».
 * Зелёной точки «арендует сейчас» здесь нет — она только у арендаторов.
 * Лупа и фильтр из тулбара рабочие: поиск строки и поповер состояний аренды.
 */
@Composable
fun LandlordsListScreen(
    onLandlordClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LandlordsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var rentFilter by remember { mutableStateOf(RentFilter.ALL) }
    var filterOpen by remember { mutableStateOf(false) }

    // Обновление списка при возврате на экран / из фона
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.load()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Сначала фильтр по состоянию аренды, затем поиск по строке
    val byFilter = uiState.landlords.filter { rentFilter.matches(it.rentStatus) }
    val visible = byFilter.filter { landlord ->
        val q = query.trim()
        q.isEmpty() ||
            landlord.name.contains(q, ignoreCase = true) ||
            landlord.companyName.orEmpty().contains(q, ignoreCase = true)
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(paddingValues).background(Color.White)) {
                Spacer(Modifier.height(27.dp))
                ContactsToolbar(
                    title = "Арендодатели",
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
                uiState.errorMessage?.let {
                    Text(
                        it,
                        color = Color(0xFFE53935),
                        fontSize = 13.sp,
                        fontFamily = InterFontFamily,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Загрузка…", fontSize = 14.sp, color = Color(0xFF8E8E93))
                    }
                } else if (visible.isNotEmpty()) {
                    LazyColumn(Modifier.weight(1f)) {
                        items(visible, key = { it.id }) { landlord ->
                            ContactRow(
                                title = landlord.name.ifBlank { landlord.phone },
                                // Вторая строка у арендодателей — название компании
                                // (в настройках не указано → строки нет)
                                subtitle = landlord.companyName?.takeIf { it.isNotBlank() },
                                avatarUrl = landlord.avatarUrl,
                                showActiveDot = false,
                                phone = landlord.phone,
                                onClick = { onLandlordClick(landlord.id) }
                            )
                            ContactRowDivider()
                        }
                    }
                }

                // Тапбар с CTA «Добавить арендодателя» (3122:60026) — только при
                // непустом списке: в пустых состояниях CTA живёт в карточке-заглушке.
                // Действие — заглушка, флоу добавления появится позже.
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
                            .clickable { },
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
                            "Добавить арендодателя",
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

            // Три взаимоисключающих пустых состояния (канвас 13):
            // 3108:56859 — списка нет, 3124:64037 — фильтр ничего не нашёл,
            // 3122:60046 — поиск ничего не нашёл.
            val nothingYet = !uiState.isLoading && uiState.landlords.isEmpty()
            val filterEmpty =
                !uiState.isLoading && uiState.landlords.isNotEmpty() && byFilter.isEmpty()
            val searchEmpty = !uiState.isLoading && uiState.landlords.isNotEmpty() &&
                !filterEmpty && visible.isEmpty()
            when {
                nothingYet -> EmptyContactsState(
                    title = "Арендодателей пока нет",
                    subtitle = "Добавленные арендодатели появятся здесь",
                    ctaText = "Добавить арендодателя",
                    onCtaClick = { }
                )

                filterEmpty -> EmptyContactsState(
                    title = "Нет арендодателей с таким статусом",
                    subtitle = "Выберите другой статус или покажите весь список",
                    ctaText = "Показать всех",
                    ctaIconRes = null,
                    illustration = painterResource(R.drawable.ic_contacts_not_found),
                    onCtaClick = { rentFilter = RentFilter.ALL }
                )

                searchEmpty -> EmptyContactsState(
                    title = "Ничего не найдено",
                    subtitle = "Проверьте написание или добавьте нового арендодателя",
                    ctaText = "Добавить арендодателя",
                    illustration = painterResource(R.drawable.ic_contacts_not_found),
                    onCtaClick = { }
                )
            }

            // Окошко фильтра (3110:57109): справа 20, сверху — низ тулбара + 2
            // (в макете панель 182×188 на (210,109), тулбар кончается на 103)
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
}

@Preview(showBackground = true, name = "Список арендодателей")
@Composable
private fun PreviewLandlordsList() {
    RentManagerTheme { LandlordsListScreen(onLandlordClick = {}, onBack = {}) }
}
