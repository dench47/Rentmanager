package com.rentmanager.app.ui.landlord.propertycard

import android.widget.Toast
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rentmanager.app.R
import com.rentmanager.app.data.model.MeterDto
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.landlord.createproperty.GradientCtaButton
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.ErrorRed
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.FieldTextStyle
import com.rentmanager.app.ui.theme.Headline2MobPlaceholderStyle
import com.rentmanager.app.ui.theme.PropertyNameStyle
import com.rentmanager.app.ui.theme.TextIconeStyle
import com.rentmanager.app.ui.theme.ToolbarTitleStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch
import com.rentmanager.app.ui.components.DesignWidthDialog
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager

private val GreenIcon = Color(0xFFE5F2E7)
private val GreenText = Color(0xFF2F7D4D)
private val BrandTint = Color(0xFFFFF1CF)
private val White50 = Color(0x80FFFFFF)
// rgba(33,33,33,0.85) — рамка кнопок в нижнем шите действий
private val Graphite85 = Color(0xD9212121)
// rgba(33,33,33,0.4) — разделитель в шите действий (Figma 2574:21650)
private val DividerGrey = Color(0x66212121)
// rgba(33,33,33,0.4) — drag handle шита действий (Figma 2574:21650)
private val SheetHandleGrey = Color(0x66212121)

// Карточка объекта (Figma 2Y1uc9owPaF7N9jzQhhuIr, node 2574:20588)
@Composable
fun PropertyCardScreen(
    propertyId: String,
    onBack: () -> Unit,
    onPaymentSchedule: (String) -> Unit,
    onEditProperty: (String) -> Unit,
    onEditAbout: (String) -> Unit = {},
    onAddCounter: (String) -> Unit = {},
    onOpenMeters: (String) -> Unit = {},
    onDeleted: () -> Unit = {},
    viewModel: PropertyCardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(propertyId) { viewModel.load(propertyId) }
    val property = uiState.property

    // Возврат с экрана «Редактировать счетчики»/редактирования счётчика/
    // добавлении — освежаем список счётчиков карточки
    val cardLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(cardLifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME &&
                uiState.property?.id != null
            ) {
                viewModel.reloadMeters(uiState.property!!.id)
            }
        }
        cardLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { cardLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showActionsSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Шиты быстрого редактирования секций (карандаши у заголовков) и сетки фото
    var showRentSheet by remember { mutableStateOf(false) }
    var showTenantSheet by remember { mutableStateOf(false) }
    var readingMeter by remember { mutableStateOf<com.rentmanager.app.data.model.MeterDto?>(null) }
    var showPhotosSheet by remember { mutableStateOf(false) }

    // Инлайн-редактирование «Информация об объекте» (Figma 2677-26576):
    // карандаш превращает контент раскрытого аккордеона в редактируемые поля
    var objectInfoEditing by remember { mutableStateOf(false) }

    // Ошибки действий (публикация/удаление/загрузка) — Toast'ами
    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    // Изменения секции сохранены — короткий Toast
    LaunchedEffect(Unit) {
        viewModel.savedEvents.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
    // Объект удалён — закрываем карточку (со освежением списка объектов)
    LaunchedEffect(Unit) {
        viewModel.deleted.collect { onDeleted() }
    }

    var tenantExpanded by remember { mutableStateOf(false) }
    var serviceExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            // edge-to-edge: adjustResize не ресайзит окно — клавиатуру
            // обрабатываем вручную, контент и таб-бар поднимаются над IME
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.clickable(onClickLabel = "Назад") { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_landlord_back),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Карточка объекта", style = ToolbarTitleStyle)
            }
            Image(
                painter = painterResource(R.drawable.ic_toolbar_more),
                contentDescription = "Ещё",
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { showActionsSheet = true }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Карандаш на фото открывает сетку фотографий, а не экран редактирования
            PhotoSlider(property, onEdit = { showPhotosSheet = true })

            // Белая панель (наложение на фото, offset -19dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-19).dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color.White)
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (property != null) {
                    Text(property.name.ifBlank { "Без названия" }, style = PropertyNameStyle)
                    Text(property.address, style = Headline2MobStyle.copy(color = GreyText))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenIcon)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Задолженностей нет", style = Headline2MobStyle.copy(color = GreenText))
                }

                // Аренда и платежи
                SectionHeader("Аренда и платежи", onPencilClick = { showRentSheet = true })
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                    ) {
                        InfoPair("Арендная плата", property?.let { rentText(it, uiState.schedule) } ?: "", Modifier.weight(1f))
                        // Нет даты → значение пустое, «до» в карточке не показываем
                        InfoPair(
                            "Срок аренды",
                            property?.rentEndDate?.takeIf { it.isNotBlank() }?.let { "до $it" }.orEmpty(),
                            Modifier.weight(1f)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlineCtaButton(
                            text = "График платежей и реквизиты",
                            iconRes = R.drawable.ic_payment_schedule,
                            onClick = { onPaymentSchedule(propertyId) }
                        )
                        OutlineCtaButton(
                            text = "Добавить расходы",
                            iconRes = R.drawable.ic_expense_receipt,
                            onClick = {}
                        )
                    }
                }

                // Арендатор и договор
                // Без арендатора карандаш ничего не открывает (Figma 2574-20392:
                // шит редактирования доступен только при прикреплённом арендаторе)
                SectionHeader("Арендатор и договор", onPencilClick = {
                    if (property?.tenantInfo?.isNotBlank() == true) showTenantSheet = true
                })
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoPair(
                            label = "Арендатор",
                            value = property?.tenantInfo.orEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                        InfoPair(
                            label = "Договор",
                            value = property
                                ?.let { contractDisplayText(it.contractNumber, it.contractDate) }
                                .orEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Нет арендатора → кнопки связи неактивны (Figma 2574-20392:
                    // контур/иконка/текст #212121 на 40%, нажатие отключено)
                    val tenantAttached = property?.tenantInfo?.isNotBlank() == true
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlineCtaButton(
                                text = "Позвонить",
                                iconRes = R.drawable.ic_call_phone,
                                modifier = Modifier.weight(1f),
                                iconSpacing = 4.dp,
                                enabled = tenantAttached,
                                borderColor = if (tenantAttached) GreyText else Graphite,
                                onClick = {}
                            )
                            OutlineCtaButton(
                                text = "Написать",
                                iconRes = R.drawable.ic_chat_message,
                                modifier = Modifier.weight(1f),
                                iconSpacing = 4.dp,
                                enabled = tenantAttached,
                                borderColor = if (tenantAttached) GreyText else Graphite,
                                onClick = {}
                            )
                        }
                        BlackCtaButton(
                            // Прикреплённый арендатор (tenantInfo заполнен) — «Открепить», иначе «Прикрепить»
                            text = if (property?.tenantInfo?.isNotBlank() == true) "Открепить арендатора" else "Прикрепить арендатора",
                            onClick = {}
                        )
                    }
                }
// Об объекте
                SectionHeader("Об объекте", onPencilClick = { onEditAbout(propertyId) })
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ValueCard("Название", property?.name ?: "", Modifier.fillMaxWidth())
                    ValueCard("Адрес", property?.address ?: "", Modifier.fillMaxWidth())
                    ValueCard("Кол-во комнат", property?.rooms ?: "", Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ValueCard(
                            "Площадь, м2",
                            property?.area?.let {
                                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                            } ?: "",
                            Modifier.weight(1f)
                        )
                        ValueCard("Спальные места", property?.sleepingPlaces ?: "", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ValueCard("Этаж", property?.floor ?: "", Modifier.weight(1f))
                        ValueCard("Этажей в доме", property?.floorsInHouse ?: "", Modifier.weight(1f))
                    }
                    ValueCard2Lines(
                        "Описание объявления",
                        "Эта информация будет видна в объявлении",
                        property?.description.orEmpty()
                    )
                    ValueCard(
                        if ((uiState.schedule?.type
                                ?: if (property?.rentType == "длительно") "auto" else "manual") == "auto"
                        ) "Стоимость за месяц, ₽" else "Стоимость за сутки, ₽",
                        property?.rentAmount?.let {
                            val s = if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                            formatAmount(s)
                        } ?: "",
                        Modifier.fillMaxWidth()
                    )
                }

                // Публикация
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GradientFullButton("Опубликовать объявление") {}
                    Text(
                        "После публикации объявление станет доступно для просмотра арендаторам",
                        style = TextIconeStyle.copy(color = GreyText)
                    )
                }

                // Аккордеоны (Figma 20741: между ними gap 12, до «Счетчиков» — вплотную, без отступа)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccordionCard(
                        "Информация об объекте",
                        "Эта информация будет видна арендатору",
                        tenantExpanded,
                        onToggle = {
                            tenantExpanded = !tenantExpanded
                            // Схлопнули аккордеон — выходим из редактирования (несохранённое отбрасывается)
                            if (!tenantExpanded) objectInfoEditing = false
                        },
                        onPencilClick = { objectInfoEditing = true },
                        showPencil = !objectInfoEditing
                    ) {
                        if (objectInfoEditing) {
                            ObjectInfoEditContent(
                                property = property,
                                isSaving = uiState.isActionInProgress,
                                onSave = { phone, wifi, rules ->
                                    objectInfoEditing = false
                                    viewModel.saveObjectInfo(phone, wifi, rules)
                                }
                            )
                        } else {
                            ObjectInfoContent(property)
                        }
                    }
                    AccordionCard(
                        "Служебная информация",
                        "Эта информация видна только вам",
                        serviceExpanded,
                        onToggle = { serviceExpanded = !serviceExpanded }
                    ) {
                        ServiceNoteContent(
                            text = property?.serviceInfo.orEmpty(),
                            focused = serviceExpanded,
                            onTextChange = { viewModel.saveServiceInfo(it) }
                        )
                    }
                }
            }

            // Счетчики
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFFFFF1CF),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader("Счетчики", pencilRes = R.drawable.ic_edit_pencil_white, onPencilClick = { onOpenMeters(propertyId) })
                BlackCtaButton(
                    text = "Добавить счетчики",
                    iconRes = R.drawable.ic_plus_circle_white,
                    onClick = { onAddCounter(propertyId) }
                )
                // Список счётчиков объекта (Figma 2574:20899): белые карточки
                // стеком gap 8 под CTA — добавили счётчик, здесь стало на один больше
                if (uiState.meters.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.meters.forEach { meter -> MeterCard(meter) { readingMeter = it } }
                    }
                }
            }
        }

        // Таббар — статичный (закреплён внизу, не скроллится).
        // ОДИН слой rgba(237,237,237,0.6) (Figma 2574:20590) на весь блок — и на строку таббара,
        // и на зону системной навигации ниже, поэтому они всегда «в один цвет» (Figma 2574:20589);
        // скругление верхних углов 30dp — через clip на Box.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Color(0x99EDEDED))
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(196.dp)
                        .height(55.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Graphite),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Финансовый отчет объекта", style = CardSubtitleStyle.copy(color = Color.White))
                }
                TabButton("Позвонить", R.drawable.ic_tab_call) {}
                TabButton("Написать", R.drawable.ic_tab_email) {}
            }
        }
    }

    // Шит действий с объектом (⋮): редактировать / публикация / удалить
    if (showActionsSheet) {
        PropertyActionsSheet(
            isPublished = property?.isPublished == true,
            isActionInProgress = uiState.isActionInProgress,
            onEdit = {
                showActionsSheet = false
                onEditProperty(propertyId)
            },
            onPublish = {
                showActionsSheet = false
                viewModel.publish()
            },
            onUnpublish = {
                showActionsSheet = false
                viewModel.unpublish()
            },
            onRequestDelete = {
                showActionsSheet = false
                showDeleteDialog = true
            },
            onDismiss = { showActionsSheet = false }
        )
    }

    // Диалог подтверждения удаления объекта
    if (showDeleteDialog) {
        DeletePropertyDialog(
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteProperty()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Шиты редактирования секций карточки (карандаши у заголовков секций)
    property?.let { p ->
        if (showRentSheet) {
            RentEditSheet(
                property = p,
                schedule = uiState.schedule,
                isSaving = uiState.isActionInProgress,
                onSave = { rentAmount, rentEndDate ->
                    showRentSheet = false
                    viewModel.saveRentInfo(rentAmount, rentEndDate)
                },
                onDismiss = { showRentSheet = false }
            )
        }
        if (showTenantSheet) {
            TenantContractEditSheet(
                property = p,
                isSaving = uiState.isActionInProgress,
                onSave = { tenantInfo, contractText, phone ->
                    showTenantSheet = false
                    viewModel.saveTenantInfo(tenantInfo, contractText, phone)
                },
                onDismiss = { showTenantSheet = false }
            )
        }
    }
    readingMeter?.let { meter ->
        com.rentmanager.app.ui.counter.readings.EnterReadingSheet(
            meter = meter,
            isSaving = uiState.isActionInProgress,
            onSave = { value ->
                viewModel.submitReading(meter.id, value)
                readingMeter = null
            },
            onDismiss = { readingMeter = null }
        )
    }
    if (showPhotosSheet) {
        property?.let { p ->
            PhotoEditSheet(
                initialPhotoUris = p.photos.orEmpty().map { it.url },
                isSaving = uiState.isActionInProgress,
                onSave = { uris ->
                    showPhotosSheet = false
                    viewModel.savePhotos(uris)
                },
                onDeletePhoto = { uri -> viewModel.deletePhoto(uri) },
                onDismiss = { showPhotosSheet = false }
            )
        }
    }
}
// ---------- вспомогательные ----------

@Composable
private fun PhotoSlider(property: PropertyDto?, onEdit: () -> Unit = {}) {
    val photos = property?.photos?.mapNotNull { it.url } ?: emptyList()
    val pagerState = rememberPagerState { photos.size }
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(316.dp)
            // Скругление только верхних углов (Figma 2578:21817: radii [20,20,0,0]):
            // нижние должны быть прямыми — белая панель с r20 перекрывает их наложением,
            // а скруглённые низы фото оставляли белые «рога» по краям стыка
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(CardBackground)
    ) {
        if (photos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет фотографий", style = Headline2MobStyle.copy(color = GreyText))
            }
        } else {
            // Свайп фотографий (Figma 2574:20665): горизонтальный пейджер на всю площадь фото
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { idx ->
                AsyncImage(
                    model = photos[idx],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Pager (Figma 20667): активная точка 20x8 #FFFFFF, неактивные 8x8 белые 50%, снизу 39
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 39.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                photos.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (i == pagerState.currentPage) 20.dp else 8.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (i == pagerState.currentPage) Color.White else White50)
                    )
                }
            }
            // Стрелки (Figma 20673/20674): белый шеврон 40x40, отступ 20 от краёв, по центру вертикали;
            // подложка — лёгкое затемнение 8% (в макете подложки нет; компромисс для читаемости на светлых фото)
            Image(
                painter = painterResource(R.drawable.ic_slider_arrow_left),
                contentDescription = "Назад",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x14000000))
                    .clickable {
                        val current = pagerState.currentPage
                        if (current > 0) scope.launch { pagerState.animateScrollToPage(current - 1) }
                    }
            )
            Image(
                painter = painterResource(R.drawable.ic_slider_arrow_right),
                contentDescription = "Вперёд",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x14000000))
                    .clickable {
                        val current = pagerState.currentPage
                        if (current < photos.size - 1) scope.launch { pagerState.animateScrollToPage(current + 1) }
                    }
            )
        }

        // Плашка «Опубликовано/Не опубликовано» и карандаш — ПОВЕРХ фото
        // (Figma 20675/20676/20679 и 2574:20032): отступы top/start/end = 20,
        // плашка radius 10, карандаш на белом круге.
        val published = property?.isPublished == true
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (published) GreenIcon else Color.White)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (published) GreenText else Color(0xFF212121))
                )
                Text(
                    if (published) "Опубликовано" else "Не опубликовано",
                    style = Headline2MobStyle.copy(
                        color = if (published) GreenText else Color(0xFF212121)
                    )
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_edit_pencil_white),
                contentDescription = "Фотографии объекта",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onEdit)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    pencilRes: Int = R.drawable.ic_edit_pencil,
    onPencilClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text, style = ToolbarTitleStyle)
        Image(
            painter = painterResource(pencilRes),
            contentDescription = "Редактировать",
            modifier = Modifier
                .size(40.dp)
                .then(
                    if (onPencilClick != null) {
                        Modifier.clip(CircleShape).clickable { onPencilClick() }
                    } else {
                        Modifier
                    }
                )
        )
    }
}

@Composable
private fun InfoPair(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = CardSubtitleStyle)
        // Пустое значение не показываем — состояние «арендатор не добавлен» (Figma 2574-20392)
        if (value.isNotBlank()) {
            Text(value, style = Headline2MobStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ValueCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(start = 20.dp, end = 10.dp),
        // Зазор подпись→значение 4dp — как в макете (16 + 4 + 18 = 38 внутри поля 64)
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Text(label, style = CardSubtitleStyle)
        Text(value.ifBlank { "—" }, style = Headline2MobStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ValueCard2Lines(label: String, sub: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(start = 20.dp, end = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        Text(if (value.isBlank()) label else value, style = Headline2MobStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = CardSubtitleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
@Composable
private fun TabButton(label: String, iconRes: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .height(55.dp)
            .clip(RoundedCornerShape(30.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Image(painter = painterResource(iconRes), contentDescription = label, modifier = Modifier.size(24.dp))
        Text(label, style = TextIconeStyle)
    }
}

private fun rentText(p: PropertyDto, schedule: com.rentmanager.app.data.model.PaymentScheduleDto?): String {
    // Тип графика задаёт и сумму (актуальная из графика), и «/ месяц(сутки)»;
    // без графика — по типу аренды объекта
    val type = schedule?.type ?: if (p.rentType == "длительно") "auto" else "manual"
    val amount = if (type == "auto") (schedule?.amount ?: p.rentAmount) else p.rentAmount
    val formatted = amount?.let {
        if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
    } ?: "0"
    return "${formatAmount(formatted)} ₽ / ${if (type == "auto") "месяц" else "сутки"}"
}

@Composable
private fun AccordionCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPencilClick: (() -> Unit)? = null,
    showPencil: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
    ) {
        // Без ripple-подсветки на заголовке: раскрытие/закрытие аккордеона
        // не должно давать анимированный эффект
        val headerInteraction = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Развёрнутый: 10dp сверху карточки + строка 40dp (Figma 2519-14173:
                // paddingTop=10, header 40, itemSpacing=12 до контента);
                // свёрнутый — 64dp с подзаголовком-плейсхолдером
                .padding(top = if (expanded) 10.dp else 0.dp)
                .height(if (expanded) 40.dp else 64.dp)
                .clickable(
                    interactionSource = headerInteraction,
                    indication = null,
                    onClick = onToggle
                )
                .padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = Headline2MobStyle)
                // Свёрнутый аккордеон показывает подзаголовок-плейсхолдер,
                // развёрнутый — только заголовок (Figma 2519-14173 / 2677-26172)
                if (!expanded) {
                    Text(subtitle, style = CardSubtitleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            // Карандаш редактирования — только в развёрнутом состоянии;
            // скрывается в режиме инлайн-редактирования (Figma 2677-26576:
            // в заголовке редактируемой карточки только название + шеврон)
            if (expanded && onPencilClick != null && showPencil) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPencilClick),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_action_edit),
                        contentDescription = "Редактировать",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Image(
                painter = painterResource(R.drawable.ic_card_chevron),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { rotationZ = if (expanded) 180f else 0f }
            )
        }
        if (expanded) content()
    }
}

// Контент аккордеона «Информация об объекте» (Figma 2519-14173) — только чтение:
// инлайн-строки с иконками «Номер телефона:»/«Пароль WiFi:» + значения из данных,
// блок «Правила объекта» — текст; редактирование — карандаш в заголовке
@Composable
private fun ObjectInfoContent(property: PropertyDto?) {
    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoInlineRow(iconRes = R.drawable.ic_call_phone, label = "Номер телефона:", value = property?.phone.orEmpty())
            InfoInlineRow(iconRes = R.drawable.ic_wifi, label = "Пароль WiFi:", value = property?.wifiPassword.orEmpty())
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Правила объекта", style = Headline2MobStyle)
            Text(
                property?.houseRules.orEmpty()
                    .ifBlank { "Использовать помещение исключительно в целях, указанных в договоре" },
                style = CardSubtitleStyle
            )
        }
    }
}

// Строка «иконка + статичная подпись + значение» (Figma 2519-14173):
// подпись и иконка #212121@85%, значение #212121; пустое значение — только подпись
@Composable
private fun InfoInlineRow(iconRes: Int, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(Graphite85)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = Headline2MobStyle.copy(color = Graphite85))
            if (value.isNotBlank()) {
                Text(value, style = Headline2MobStyle)
            }
        }
    }
}

// Редактируемый контент аккордеона «Информация об объекте» (Figma 2677-26576):
// белые пилюли-поля «Номер телефона»/«Пароль WiFi» на серой карточке, блок
// «Правила объекта» с автофокусом и CTA «Сохранить изменения» (332×55, r100).
// Зазоры: пилюли 6dp, правила в 6dp от пилюль, кнопка в 20dp (itemSpacing 20)
@Composable
private fun ObjectInfoEditContent(
    property: PropertyDto?,
    isSaving: Boolean,
    onSave: (phone: String, wifiPassword: String, houseRules: String) -> Unit
) {
    var phone by remember(property?.id) { mutableStateOf(property?.phone.orEmpty()) }
    var wifi by remember(property?.id) { mutableStateOf(property?.wifiPassword.orEmpty()) }
    var rules by remember(property?.id) { mutableStateOf(property?.houseRules.orEmpty()) }
    val rulesFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { rulesFocus.requestFocus() } catch (_: Exception) {}
    }
    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        InlineIconField(
            iconRes = R.drawable.ic_call_phone,
            caption = "Номер телефона",
            value = phone,
            onValueChange = { phone = it },
            placeholder = "+7",
            keyboardType = KeyboardType.Phone
        )
        InlineIconField(
            iconRes = R.drawable.ic_wifi,
            caption = "Пароль WiFi",
            value = wifi,
            onValueChange = { wifi = it },
            placeholder = ""
        )
        // Правила: текст выровнен с иконками пилюль (горизонтальный отступ 10dp)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Правила объекта", style = Headline2MobStyle)
            // Многострочный ввод без ограничений; плейсхолдер — только когда пусто
            BasicTextField(
                value = rules,
                onValueChange = { rules = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .focusRequester(rulesFocus),
                textStyle = CardSubtitleStyle.copy(color = Graphite),
                cursorBrush = SolidColor(Graphite),
                decorationBox = { innerTextField ->
                    Box {
                        if (rules.isEmpty()) {
                            Text(
                                "Использовать помещение исключительно в целях, указанных в договоре",
                                style = CardSubtitleStyle
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        // 6dp от spacedBy + 14dp паддинга = 20dp до кнопки (Figma: itemSpacing 20)
        BlackCtaButton(
            modifier = Modifier.padding(top = 14.dp),
            text = "Сохранить изменения",
            enabled = !isSaving,
            onClick = { onSave(phone, wifi, rules) }
        )
    }
}

// Поле-пилюля с иконкой для инлайн-редактирования (Figma 2677-26576):
// белая 64dp r20, иконка 20dp, отступ 10dp; подпись 13/400 #727272 над вводом 15/600
@Composable
private fun InlineIconField(
    iconRes: Int,
    caption: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = FieldTextStyle,
            cursorBrush = SolidColor(Graphite),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            decorationBox = { innerTextField ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(caption, style = CardSubtitleStyle)
                    Box {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, style = Headline2MobPlaceholderStyle)
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

// Карточка счётчика в жёлтой секции «Счетчики» (Figma 2574:20905):
// белая 372×146 r20 pad 12; строка «Тип + №зав.номер», три строки
// показаний (лейбл 13/400 + значение 13 Medium #212121), «Внести новые показания»
@Composable
private fun MeterCard(meter: MeterDto, onEnterReading: (MeterDto) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(meterTypeName(meter.type), style = Headline2MobStyle)
            Text(
                "№${meter.factoryNumber}",
                style = Headline2MobStyle.copy(color = GreyText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MeterInfoRow("Текущие показания:", formatMeterValue(meter.currentValue, meter.unit))
            MeterInfoRow("Последнее изменение:", formatMeterDate(meter.lastUpdated))
            MeterInfoRow("Дата следующей проверки:", formatMeterDate(meter.nextVerificationDate))
        }
        Text(
            "Внести новые показания",
            style = Headline2MobStyle.copy(color = Graphite85),
            modifier = Modifier.clickable { onEnterReading(meter) }
        )
    }
}

// Строка «лейбл: значение» внутри карточки счётчика (13sp; значение — Medium)
@Composable
private fun MeterInfoRow(label: String, value: String) {
    // Оба текста чёрные #212121 (макет 2755-37385): подпись 13/400, значение 13/500
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = CardSubtitleStyle.copy(color = Graphite))
        Text(value, style = CardSubtitleStyle.copy(color = Graphite, fontWeight = FontWeight.Medium))
    }
}

// electricity → «Электроэнергия» и т.д. (обратное преобразование типа из API)
private fun meterTypeName(apiType: String): String = when (apiType) {
    "electricity" -> "Электроэнергия"
    "cold_water" -> "Холодная вода"
    "hot_water" -> "Горячая вода"
    "heat" -> "Отопление"
    else -> apiType
}

// 456.0 → «456 кВт·ч»; дробные — как есть
private fun formatMeterValue(value: Double, unit: String): String {
    val v = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    return if (unit.isBlank()) v else "$v $unit"
}

// yyyy-MM-dd → dd.MM.yyyy; пусто/нераспарсенное → «—»
private fun formatMeterDate(raw: String?): String {
    if (raw.isNullOrBlank()) return "—"
    return try {
        val parts = raw.split("-")
        if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else raw
    } catch (_: Exception) {
        raw
    }
}

// Контент аккордеона «Служебная информация» (Figma 2677-26172):
// заметка прямо на карточке; при пустоте — только мигающий курсор
@Composable
private fun ServiceNoteContent(
    text: String,
    focused: Boolean,
    onTextChange: (String) -> Unit
) {
    // 6dp от заголовка и 10dp снизу — как в макете (itemSpacing 6, paddingBottom 10)
    Column(
        Modifier.fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp)
    ) {
        NoteField(text = text, focused = focused, onTextChange = onTextChange, placeholder = null)
    }
}

// Заметка: многострочный ввод без ограничений длины (Figma 2677-26172: 13 Regular).
// focused=true — автофокус с курсором в начале первой строки при раскрытии аккордеона;
// сохранение — по потере фокуса
@Composable
private fun NoteField(
    text: String,
    focused: Boolean,
    onTextChange: (String) -> Unit,
    placeholder: String?
) {
    var value by remember(text) { mutableStateOf(text) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var hasFocus by remember { mutableStateOf(false) }

    // При появлении поля (раскрытие аккордеона) — сразу фокус, мигающий курсор в начале
    LaunchedEffect(focused) {
        if (focused) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }
    BasicTextField(
        value = value,
        onValueChange = { value = it },
        modifier = Modifier
            .fillMaxWidth()
            // Фиксированная начальная высота зоны ввода 144dp (Figma 2677-26172):
            // карточка держит исходный размер и при пустом значении, курсор — сверху
            .heightIn(min = 144.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) {
                    hasFocus = true
                } else if (hasFocus) {
                    // Потеряли фокус — сохраняем заметку
                    hasFocus = false
                    if (value != text) onTextChange(value)
                }
            },
        textStyle = CardSubtitleStyle.copy(color = Graphite),
        cursorBrush = SolidColor(Graphite),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, style = CardSubtitleStyle)
                }
                innerTextField()
            }
        }
    )
    // Случай «свернули аккордеон с несохранённым текстом» — тоже сохраняем
    if (!focused && value != text) {
        LaunchedEffect(Unit) { onTextChange(value) }
    }
}

@Composable
private fun GradientFullButton(text: String, onClick: () -> Unit) {
    val density = LocalDensity.current
    val brush = remember(density) {
        with(density) {
            val w = 372.dp.toPx()
            val h = 55.dp.toPx()
            val angle = Math.toRadians(136.0)
            val dx = sin(angle).toFloat()
            val dy = (-cos(angle)).toFloat()
            val len = abs(w * dx) + abs(h * dy)
            val cc = Offset(w / 2f, h / 2f)
            val half = Offset(dx * len / 2f, dy * len / 2f)
            Brush.linearGradient(
                colorStops = arrayOf(
                    0.19f to Color(0xFFF6D85E),
                    0.60f to Color(0xFFE89B5A),
                    1.00f to Color(0xFFD97D5D)
                ),
                start = cc - half,
                end = cc + half
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(brush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = Headline2MobStyle)
    }
}

// Нижний шит «Действие с объектом» (Figma 2574:21650 — не опубликован /
// 2574:21684 — опубликован): редактирование, публикация/снятие с публикации,
// разделитель, удаление.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertyActionsSheet(
    isPublished: Boolean,
    isActionInProgress: Boolean,
    onEdit: () -> Unit,
    onPublish: () -> Unit,
    onUnpublish: () -> Unit,
    onRequestDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 16.dp, bottom = 16.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(SheetHandleGrey)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Действие с объектом", style = ToolbarTitleStyle)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlineCtaButton(
                    text = "Редактировать объект",
                    iconRes = R.drawable.ic_action_edit,
                    borderColor = Graphite85,
                    onClick = onEdit
                )
                if (isPublished) {
                    BlackCtaButton(
                        text = "Снять с публикации",
                        enabled = !isActionInProgress,
                        onClick = onUnpublish
                    )
                } else {
                    GradientCtaButton(
                        text = "Опубликовать объявление",
                        enabled = !isActionInProgress,
                        onClick = onPublish
                    )
                }
            }
            HorizontalDivider(color = DividerGrey, thickness = 1.dp)
            OutlineCtaButton(
                text = "Удалить объект",
                iconRes = R.drawable.ic_action_delete,
                borderColor = ErrorRed,
                textColor = ErrorRed,
                onClick = onRequestDelete
            )
        }
    }
}

// Диалог подтверждения удаления объекта (Figma 2698-22247):
// центр-выровненная карточка, радиус 20, паддинг 20, заголовок+текст с зазором 6,
// красная залитая кнопка + контурная «Отменить».
@Composable
private fun DeletePropertyDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DesignWidthDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Заголовок и текст вплотную, зазор 6 (Figma 2698-22247: txt itemSpacing 6);
            // \u00A0 — неразрывные пробелы из макета, задают переносы как в дизайне
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Удалить объект?", style = ToolbarTitleStyle)
                Text(
                    "Будут удалены данные объекта, договор, история платежей и\u00A0показания счетчиков. Это\u00A0действие нельзя отменить",
                    style = Headline2MobStyle.copy(color = GreyText)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BlackCtaButton(
                    text = "Удалить объект",
                    containerColor = ErrorRed,
                    onClick = onConfirm
                )
                OutlineCtaButton(
                    text = "Отменить",
                    borderColor = Graphite85,
                    onClick = onDismiss
                )
            }
        }
    }
}