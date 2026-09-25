@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.rentmanager.app.ui.landlord.tenants

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rentmanager.app.R
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.SectionTitleStyle
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import java.util.Locale

private val CardGrey = Color(0xFFEFEFEF)
private val DividerGrey = Color(0xFFDBDBDB)
// Тапбар (3681:32808): #EDEDED@90%, верхние углы r30 — канон редактора
private val TabBarGrey = Color(0xFFEDEDED).copy(alpha = 0.9f)
private val RuDate = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))

/**
 * «Карточка арендатора» — режим просмотра (канвас «13», 2983:42399/42356/50881/51020):
 * шапка с вертикальным меню, аватар+имя+карандаш, инфо-карточки, Позвонить/Написать,
 * паспорт с маскировкой (глаз), «Документы · N файлов», служебная информация и
 * история аренды (аккордеоны), текущая аренда.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantCardScreen(
    tenantId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onAttach: () -> Unit = {},
    onDelete: () -> Unit = {},
    onPropertyClick: (String) -> Unit = {},
    onOpenMyProperties: () -> Unit = {},
    viewModel: TenantCardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showActionsSheet by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showBlockedDialog by remember { mutableStateOf(false) }
    var showDocumentsSheet by remember { mutableStateOf(false) }

    // Загрузка стартует в init ViewModel (вместе с посевом кеша в первый кадр);
    // ON_RESUME-триггер не нужен — экран самодостаточен и не мигает

    val tenant = state.tenant

    val contentScroll = rememberScrollState()
    val cardScrollScope = rememberCoroutineScope()
    // Верх блока «Арендует» в координатах контента: из диалога «Есть активные
    // аренды» кнопка «Посмотреть аренды» прокручивает карточку сюда
    // (аннотация к 3696:34110)
    var rentBlockTop by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TenantCardToolbar(onBack = onBack, onMenu = { showActionsSheet = true })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(contentScroll)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ---- Аватар + имя + карандаш (Figma: 80, pad v10, аватар 60, зазор 8) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(CardGrey),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ава с аккаунта арендатора (по телефону), иначе плейсхолдер
                        val avatarUrl = tenant?.avatarUrl
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(100.dp))
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_avatar_placeholder),
                                contentDescription = null,
                                modifier = Modifier.size(width = 42.dp, height = 38.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    tenant?.fullName.orEmpty(),
                    style = SectionTitleStyle,
                    modifier = Modifier.weight(1f)
                )
            }

            // ---- Инфо-карточки + Позвонить/Написать (sp6 между карточками и кнопками) ----
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // ПРАВИЛО: * — обязательные (телефон), остальные опциональны:
                // пустые необязательные поля в карточке НЕ отображаются
                // (3681:32765 — карточка арендатора только с обязательными)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!tenant?.companyName.isNullOrBlank()) {
                        InfoCard("Название компании", tenant!!.companyName)
                    }
                    if (!tenant?.email.isNullOrBlank()) {
                        InfoCard("Электронная почта", tenant!!.email)
                    }
                    InfoCard("Номер телефона", formatTenantPhone(tenant?.phone))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlineCtaButton(
                        text = "Позвонить",
                        iconRes = R.drawable.ic_tab_call,
                        iconSpacing = 4.dp,
                        borderColor = Graphite,
                        onClick = {
                            tenant?.phone?.filter(Char::isDigit)?.let { digits ->
                                val normalized = if (digits.startsWith("8")) "7" + digits.drop(1) else digits
                                if (normalized.length == 11) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:+$normalized"))
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OutlineCtaButton(
                        text = "Написать",
                        iconRes = R.drawable.ic_chat_message,
                        iconSpacing = 4.dp,
                        borderColor = Graphite,
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ---- Группа «паспорт → документы → служебная информация»: в макете
            // (3005:51020) это один блок с зазором 12: 64 → 12 → 55 → 12 → 210;
            // CTA документов живёт ВНУТРИ группы ----
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ---- Паспорт: ОДНО поле «Номер документа» (правка Вики 2026-09-24,
                // 3677:31337): карточка 372×64 r20 «Номер документа» + «45 08 7485912545».
                // Глаза и маски в новом макете нет — номер показывается целиком ----
                // Необязательное поле: пустое — не отображаем (правило *)
                val documentNumber = passportDisplay(tenant?.passportData)
                if (documentNumber != null) {
                    InfoCard(caption = "Номер документа", value = documentNumber)
                }

                // ---- CTA «Документы · N файлов» (3005:51340): контурная 55 r100,
                // иконка 24 (две страницы) + зазор 6, текст 15/600; показывается только
                // когда документы есть; тап — шит «Документы» (2983:44896) ----
                val documents = tenant?.documents.orEmpty()
                if (documents.isNotEmpty()) {
                    OutlineCtaButton(
                        text = "Документы · " + pluralFiles(documents.size),
                        iconRes = R.drawable.ic_documents_cta,
                        borderColor = Graphite,
                        onClick = { showDocumentsSheet = true }
                    )
                }

                // ---- Служебная информация (канвас «14», 3005:51020/51159): одна серая
                // карта r20: заголовок 15/600 в строке 40 + шеврон; раскрытая пустая —
                // просто пустое место, БЕЗ текста-заглушки ----
                // Якорь на НИЗ карты: при раскрытии низ остаётся на месте,
                // экран докручивается вверх на высоту прироста
                val serviceScope = rememberCoroutineScope()
                var serviceAnchorBottom by remember { mutableStateOf<Float?>(null) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardGrey)
                        // Закрытая карта — ровно 64 БЕЗ внешних отступов (2983:42372);
                        // раскрытая — 10 сверху/снизу вокруг строки 40 (3005:51159)
                        .padding(horizontal = 20.dp)
                        .then(if (state.serviceInfoExpanded) Modifier.padding(vertical = 10.dp) else Modifier)
                        .onGloballyPositioned { c ->
                            val bottom = c.positionInRoot().y + c.size.height - contentScroll.value
                            if (!state.serviceInfoExpanded) {
                                serviceAnchorBottom = bottom
                            } else if (serviceAnchorBottom != null) {
                                val target = c.positionInRoot().y + c.size.height - serviceAnchorBottom!!
                                serviceAnchorBottom = null
                                if (target > contentScroll.value) {
                                    val by = target - contentScroll.value
                                    serviceScope.launch { contentScroll.scrollBy(by) }
                                }
                            }
                        }
                ) {
                    val serviceInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    // Закрытый баян — 64: заголовок 15/600 + подпись 13/400 серым
                    // (3005-50901); раскрытый — заголовок в строке 40 (3005:51159)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (state.serviceInfoExpanded) Modifier.height(40.dp) else Modifier.height(64.dp))
                            .clickable(
                                interactionSource = serviceInteraction,
                                indication = null
                            ) { viewModel.toggleServiceInfo() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Служебная информация",
                                style = Headline2MobStyle.copy(lineHeight = 18.2.sp)
                            )
                            if (!state.serviceInfoExpanded) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Эта информация видна только вам",
                                    fontSize = 13.sp,
                                    lineHeight = 15.7.sp,
                                    letterSpacing = (-0.4).sp,
                                    color = GreyText
                                )
                            }
                        }
                        Image(
                            painter = painterResource(R.drawable.ic_card_chevron),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer { rotationZ = if (state.serviceInfoExpanded) 180f else 0f }
                        )
                    }
                    if (state.serviceInfoExpanded) {
                        val note = tenant?.serviceInfo?.takeIf { it.isNotBlank() }
                        if (note != null) {
                            Text(
                                note,
                                fontSize = 13.sp,
                                lineHeight = 15.7.sp,
                                letterSpacing = (-0.4).sp,
                                color = Graphite
                            )
                        } else {
                            // Раскрытая без заметок — пустое место (3005:51159)
                            Spacer(Modifier.height(144.dp))
                        }
                    }
                }
            }

            // ---- Арендует + История аренды (канвас «14», 3005:51020):
            // без фонов, тонкая черта под каждой записью, объекты — ссылки ----
            val currentBooking = state.currentBooking
            if (currentBooking != null || state.pastBookings.isNotEmpty() || true) {
                Column(
                    modifier = Modifier.onGloballyPositioned { c ->
                        rentBlockTop = (c.positionInRoot().y + contentScroll.value).toInt()
                    }
                ) {
                    if (currentBooking == null) {
                        // ---- «Нет активной аренды» (правка Вики 2026-09-24, 3677:31762):
                        // «Арендует» 22 → 12 → блок 55 (15/600 + 4 + 13/400 #727272)
                        // → 12 → контурная CTA 55 «Прикрепить к объекту» ----
                        Text("Арендует", style = SectionTitleStyle)
                        Spacer(Modifier.height(12.dp))
                        Column(
                            modifier = Modifier.height(55.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Нет активной аренды",
                                style = Headline2MobStyle.copy(lineHeight = 18.2.sp)
                            )
                            Text(
                                "Арендатора можно прикрепить к свободному объекту",
                                fontSize = 13.sp,
                                lineHeight = 15.7.sp,
                                letterSpacing = (-0.4).sp,
                                color = GreyText
                            )
                        }
                    }
                    if (currentBooking != null) {
                        Text("Арендует", style = SectionTitleStyle)
                        Spacer(Modifier.height(6.dp))
                        BookingEntry(
                            period = "с ${currentBooking.startDate.format(RuDate)} " +
                                "до ${currentBooking.endDate.format(RuDate)}",
                            propertyName = currentBooking.propertyName,
                            propertyAddress = currentBooking.propertyAddress,
                            photoUrl = currentBooking.propertyPhoto,
                            onClick = { onPropertyClick(currentBooking.propertyId) }
                        )
                    }
                    val historyScope = rememberCoroutineScope()
                    var historyAnchorBottom by remember { mutableStateOf<Float?>(null) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { c ->
                                val bottom = c.positionInRoot().y + c.size.height - contentScroll.value
                                if (!state.historyExpanded) {
                                    historyAnchorBottom = bottom
                                } else if (historyAnchorBottom != null) {
                                    val target = c.positionInRoot().y + c.size.height - historyAnchorBottom!!
                                    historyAnchorBottom = null
                                    if (target > contentScroll.value) {
                                        val by = target - contentScroll.value
                                        historyScope.launch { contentScroll.scrollBy(by) }
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "История аренды",
                                style = SectionTitleStyle,
                                modifier = Modifier.weight(1f)
                            )
                            Image(
                                painter = painterResource(R.drawable.ic_card_chevron),
                                contentDescription = "История аренды",
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer { rotationZ = if (state.historyExpanded) 180f else 0f }
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { viewModel.toggleHistory() }
                            )
                        }
                        if (state.historyExpanded) {
                            state.pastBookings.forEach { b ->
                                BookingEntry(
                                    period = "с ${b.startDate.format(RuDate)} до ${b.endDate.format(RuDate)}",
                                    propertyName = b.propertyName,
                                    propertyAddress = b.propertyAddress,
                                    photoUrl = b.propertyPhoto,
                                    onClick = { onPropertyClick(b.propertyId) }
                                )
                            }
                        }
                        }
                    }
                }

            Spacer(Modifier.height(20.dp))
        }

        // ---- Тапбар карточки (3681:32808/32809): панель #EDEDED@90%, верхние углы
        // r30, поля 10/20 (высота 75) — канон тапбара редактора; кнопка ЧЁРНАЯ
        // «Прикрепить к объекту» БЕЗ иконки, текст по центру. Видна ВСЕГДА,
        // независимо от наличия аренды и объектов. От контента до тапбара — 20
        // (3681:32765: «История аренды» → 20 → тапбар), поэтому «История аренды»
        // не уезжает под системную навигацию ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(TabBarGrey)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton(
                text = "Прикрепить к объекту",
                onClick = {
                    viewModel.loadFreeProperties()
                    showAttachSheet = true
                }
            )
        }
    }

    // ---- Шит «Действия с арендатором» (канвас «14», 3005:51850) ----
    if (showActionsSheet) {
        TenantActionModalSheet(
            onDismiss = { showActionsSheet = false },
            // В шите «Управление карточкой» ручка серая #79747E (3676:31314)
            handleColor = Color(0xFF79747E)
        ) {
            TenantActionsSheet(
                onEdit = { showActionsSheet = false; onEdit() },
                onDelete = {
                    showActionsSheet = false
                    showDeleteDialog = true
                }
            )
        }
    }

    // ---- Шит «Прикрепить к объекту» (3005:52495) ----
    if (showAttachSheet) {
        val freeProps by viewModel.freeProperties.collectAsState()
        TenantActionModalSheet(onDismiss = { showAttachSheet = false }) {
            AttachPropertySheet(
                properties = freeProps,
                onPick = { propertyId ->
                    // Даты аренды ещё не выбраны — прикреплять рано: открываем
                    // карточку выбранного объекта (правка Вики от 21.09),
                    // срок задаётся в её потоке брони
                    showAttachSheet = false
                    onPropertyClick(propertyId)
                }
            )
        }
    }

    // ---- Шит «Документы» (2983:44896): список файлов; тап — открыть файл ----
    if (showDocumentsSheet) {
        val docScope = rememberCoroutineScope()
        TenantDocumentsSheet(
            documents = tenant?.documents.orEmpty(),
            onOpen = { doc ->
                docScope.launch { openDocumentFromUrl(context, doc.url, doc.name, doc.fileType) }
            },
            onDismiss = { showDocumentsSheet = false }
        )
    }

    // ---- Диалог «Удалить карточку арендатора?» (3005:52537) ----
    if (showDeleteDialog) {
        TenantDialog(
            onDismiss = { showDeleteDialog = false },
            title = "Удалить карточку арендатора?",
            text = "Личные данные и прикрепленные документы\nбудут удалены"
        ) {
            TenantDialogButton(
                text = "Удалить карточку",
                container = Color(0xFFFF4249),
                textColor = Color.White,
                onClick = {
                    showDeleteDialog = false
                    viewModel.deleteTenant { ok ->
                        if (ok) showSuccessDialog = true else showBlockedDialog = true
                    }
                }
            )
            TenantDialogButton(
                text = "Отменить",
                stroke = Graphite,
                textColor = Graphite,
                onClick = { showDeleteDialog = false }
            )
        }
    }

    // ---- Диалог «Арендатор был удален» (3014:22433) ----
    if (showSuccessDialog) {
        TenantDialog(
            onDismiss = { showSuccessDialog = false },
            icon = R.drawable.ic_success_check,
            title = "Арендатор был удален"
        ) {
            TenantDialogButton(
                text = "Отменить удаление",
                container = Graphite,
                textColor = Color.White,
                onClick = {
                    showSuccessDialog = false
                    viewModel.restoreTenant { }
                }
            )
        }
    }

    // ---- Диалог «Есть активные аренды» (правка Вики 2026-09-24, 3696:34110) ----
    if (showBlockedDialog) {
        TenantDialog(
            onDismiss = { showBlockedDialog = false },
            title = "Есть активные аренды",
            text = "Чтобы удалить карточку, завершите аренды или открепите арендатора от объектов"
        ) {
            TenantDialogButton(
                text = "Посмотреть аренды",
                container = Graphite,
                textColor = Color.White,
                onClick = {
                    // Аннотация к 3696:34110: «Посмотреть аренды» закрывает окно
                    // и прокручивает карточку к блоку «Арендует»
                    showBlockedDialog = false
                    cardScrollScope.launch { contentScroll.animateScrollTo(rentBlockTop) }
                }
            )
            TenantDialogButton(
                text = "Отменить",
                stroke = Graphite,
                textColor = Graphite,
                onClick = { showBlockedDialog = false }
            )
        }
    }
}

/**
 * Общий каркас модального шита (3005:51850/52495): белый r20, полоса 32x4
 * #212121@40% по центру (16 сверху), контент с полями 20, снизу 36.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantActionModalSheet(
    onDismiss: () -> Unit,
    /** Ручка: канон #212121@40%, в шите «Управление карточкой» — #79747E (3676:31314) */
    handleColor: Color = Graphite.copy(alpha = 0.4f),
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .padding(top = 16.dp, bottom = 16.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(handleColor)
                )
            }
            content()
        }
    }
}

/** Шит «Управление карточкой» (правка Вики 2026-09-24, 3676:31314) — 412×232:
 *  заголовок 20/600 → 20 → чёрная «Редактировать карточку» (карандаш 24, зазор 6)
 *  + 6 + контурная красная «Удалить карточку арендатора» (корзина 24).
 *  «Прикрепить к объекту» из шита убрано — кнопка живёт в блоке «Нет активной
 *  аренды» в самой карточке (3677:31762). */
@Composable
private fun TenantActionsSheet(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        Text(
            "Управление карточкой",
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = Graphite
        )
        Spacer(Modifier.height(20.dp))
        com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton(
            text = "Редактировать карточку",
            iconRes = R.drawable.ic_action_edit,
            onClick = onEdit
        )
        Spacer(Modifier.height(6.dp))
        OutlineCtaButton(
            text = "Удалить карточку арендатора",
            iconRes = R.drawable.ic_trash_red,
            borderColor = Color(0xFFFF4249),
            textColor = Color(0xFFFF4249),
            onClick = onDelete
        )
    }
}

/** Шит «Прикрепить к объекту» (3005:52495): заголовок + подпись + свободные объекты. */
@Composable
private fun AttachPropertySheet(
    properties: List<com.rentmanager.app.data.model.PropertyDto>,
    onPick: (String) -> Unit
) {
    Column {
        Text(
            "Прикрепить к объекту",
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = Graphite
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Выберите свободный объект",
            fontSize = 15.sp,
            lineHeight = 18.2.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            color = GreyText
        )
        properties.forEach { p ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPick(p.id) }
                    .padding(top = 10.dp, bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    coil.compose.AsyncImage(
                        model = p.photos?.firstOrNull()?.url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            p.name.ifBlank { "Без названия" },
                            style = com.rentmanager.app.ui.theme.Headline2MobStyle.copy(lineHeight = 18.2.sp)
                        )
                        Text(
                            p.address.substringBefore(',').trim(),
                            fontSize = 13.sp,
                            lineHeight = 15.7.sp,
                            letterSpacing = (-0.4).sp,
                            color = GreyText
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.ic_card_chevron),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer { rotationZ = -90f }
                    )
                }
                androidx.compose.material3.HorizontalDivider(color = Color(0xFFDBDBDB))
            }
        }
    }
}

/**
 * Центрированный диалог — единый паттерн Вики (25.09, новая звезда):
 * 20dp от краёв экрана (тянется), внутри 10dp, текст→кнопка 20dp,
 * тень как у боттомшита (elevation 8), белый r20.
 * Макет авторится на референсном экране 412: карточка 380, поля 20,
 * текстовая колонка 340 — она и задаёт переносы строк («Личные данные
 * и прикрепленные документы / будут удалены»). На узком экране колонка 340
 * физически не помещается, поэтому весь диалог строится как пропорциональная
 * копия макета: все размеры × (ширина экрана / 412). На 412 — один в один,
 * на меньших — уменьшенная копия с ТОЧНО теми же переносами строк.
 */
@Composable
fun TenantDialog(
    onDismiss: () -> Unit,
    title: String,
    text: String? = null,
    icon: Int? = null,
    // buttons = null — подтверждение без кнопок (2983:42825: 20+50+10+24+20 = 124)
    buttons: (@Composable () -> Unit)? = null
) {
    // БЕЗ масштабирования: в окне диалога LocalConfiguration врёт (нативные 360dp
    // вместо проектных 412) и сжимал карточку до 87% — кнопки 55 превращались в 48.
    // dp внутри диалога уже проектные (density-override приложения), как во всех экранах.

    // Диалог рисуем В ОКНЕ ЭКРАНА (не платформенное окно диалога — MIUI
    // инсетит его ~46px от краёв, из-за чего карточка не дотягивала до
    // «20 от экрана»). Скрим и Back — свои, ширина гарантирована окном экрана.
    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                // Паттерн 25.09 (2698:22247): бока 10, верх/низ 20
                .padding(horizontal = 10.dp, vertical = 20.dp)
                .clickable(enabled = false) {} // не пропускать тап скрима внутрь
        ) {
            if (icon != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = Color.Unspecified
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    title,
                    fontSize = 20.sp,
                    lineHeight = 24.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = -0.3.sp,
                    color = Graphite,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (buttons != null) Spacer(Modifier.height(12.dp))
            } else {
                // Тексты — 20 от края карточки (3696:34110: x20, ширина 332);
                // кнопки при этом на 10 (352)
                Column(Modifier.padding(horizontal = 10.dp)) {
                    Text(
                        title,
                        fontSize = 20.sp,
                        lineHeight = 24.2.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = -0.3.sp,
                        color = Graphite
                    )
                    if (text != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text,
                            fontSize = 15.sp,
                            lineHeight = 18.2.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = -0.4.sp,
                            color = GreyText
                        )
                    }
                }
                if (buttons != null) Spacer(Modifier.height(20.dp))
            }
            // Кнопки во всю ширину контента (352), 55 через 6 (2698:22247)
            if (buttons != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    buttons()
                }
            }
        }
    }
}

/** Кнопка 55 в диалогах: заливка или контур #212121/1 (3005:52537). */
@Composable
fun TenantDialogButton(
    text: String,
    container: Color? = null,
    stroke: Color? = null,
    textColor: Color,
    onClick: () -> Unit
) {
    // Без масштабирования: raw dp как во всех экранах (масштаб сжимал кнопку
    // 55 -> 48 и текст в ней)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(container ?: Color.White)
            .then(
                if (stroke != null) Modifier.border(1.dp, stroke, RoundedCornerShape(100.dp))
                else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = com.rentmanager.app.ui.theme.Headline2MobStyle.copy(
                color = textColor,
                lineHeight = 18.2.sp
            )
        )
    }
}

/** Телефон из DTO (+7900…/8900…) → отображение +7-900-000-00-03. */
private fun formatTenantPhone(raw: String?): String {
    var digits = raw?.filter { it.isDigit() } ?: return "—"
    if (digits.startsWith("8")) digits = "7" + digits.drop(1)
    if (digits.length < 11) return raw ?: "—"
    val d = digits
    return buildString {
        append("+").append(d.take(1))
        append("-").append(d.substring(1, 4))
        append("-").append(d.substring(4, 7))
        append("-").append(d.substring(7, 9))
        append("-").append(d.substring(9, 11))
    }
}
