@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.rentmanager.app.ui.landlord.tenants

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.rentmanager.app.R
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.local.TenantEvents
import com.rentmanager.app.data.model.TenantDto
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.SectionTitleStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val EditCardGrey = Color(0xFFEFEFEF)
private val TabBarGrey = Color(0xFFEDEDED).copy(alpha = 0.9f)

data class TenantEditUiState(
    val isLoading: Boolean = true,
    val tenant: TenantDto? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class TenantEditViewModel @Inject constructor(
    private val tenantApi: TenantApi,
    private val tenantEvents: TenantEvents,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val tenantId: String = savedStateHandle.get<String>("tenantId").orEmpty()

    private val _uiState = MutableStateFlow(TenantEditUiState())
    val uiState: StateFlow<TenantEditUiState> = _uiState.asStateFlow()

    init {
        // Первый кадр из кеша карточки/списка, сеть молча подтверждает
        (TenantCardCache.takeCardIfMatches(tenantId) ?: TenantCardCache.takeIfMatches(tenantId))
            ?.let { cached -> _uiState.update { it.copy(isLoading = false, tenant = cached) } }
        viewModelScope.launch {
            runCatching { tenantApi.getTenantCard(tenantId).body() }.getOrNull()?.let { dto ->
                TenantCardCache.putCard(dto)
                _uiState.update { it.copy(isLoading = false, tenant = dto) }
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun save(fields: TenantDto, onDone: (Boolean) -> Unit) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val ok = runCatching {
                tenantApi.updateTenant(tenantId, fields).isSuccessful
            }.getOrDefault(false)
            if (ok) {
                // Карточка позади перечитается по тикеру события
                tenantEvents.notifyChanged()
            }
            _uiState.update { it.copy(isSaving = false) }
            onDone(ok)
        }
    }
}

/**
 * «Редактирование арендатора» (канвас «14»): все поля правятся; курсор —
 * по канону инлайн-редактирования «об объекте»: тап по всему полю ставит
 * курсор в конец, Done/галочка на клавиатуре прячет её и снимает фокус,
 * курсор не прыгает на другое поле и не мигает.
 * Тапбар (2983:42353/42264): без изменений «Сохранить изменения» серая
 * 40%, любое изменение включает; «Отменить изменения» — контурная.
 */
@Composable
fun TenantEditScreen(
    onBack: () -> Unit,
    viewModel: TenantEditViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val tenant = state.tenant

    // Поля живут в plain remember: экран создаётся заново на каждую навигацию,
    // а ключ tenant?.id пересоздавал состояние, когда сеть доезжала позже ввода
    // (null → id) — введённое стиралось. Первый кадр наполняется из кеша карточки.
    var fullName by remember { mutableStateOf(TextFieldValue(tenant?.fullName.orEmpty())) }
    var company by remember { mutableStateOf(TextFieldValue(tenant?.companyName.orEmpty())) }
    var email by remember { mutableStateOf(TextFieldValue(tenant?.email.orEmpty())) }
    var phone by remember { mutableStateOf(TextFieldValue(tenant?.phone.orEmpty())) }
    // Серия паспорта — первые 4 цифры passportData, номер — остальные
    val passportDigits = tenant?.passportData.orEmpty().filter { it.isDigit() }
    var passportSeries by remember { mutableStateOf(TextFieldValue(passportDigits.take(4))) }
    var passportNumber by remember { mutableStateOf(TextFieldValue(passportDigits.drop(4))) }
    var serviceInfo by remember { mutableStateOf(TextFieldValue(tenant?.serviceInfo.orEmpty())) }
    // Паспорт: глаза полей переключают видимость (42233 видимо / 50897 скрыто)
    var passportVisible by remember { mutableStateOf(false) }
    var showAttachDocSheet by remember { mutableStateOf(false) }

    val hasChanges = tenant != null && (
        fullName.text != tenant.fullName ||
            company.text != tenant.companyName.orEmpty() ||
            email.text != tenant.email.orEmpty() ||
            phone.text != tenant.phone ||
            passportSeries.text != passportDigits.take(4) ||
            passportNumber.text != passportDigits.drop(4) ||
            serviceInfo.text != tenant.serviceInfo.orEmpty()
        )

    val contentScroll = rememberScrollState()
    val revealScope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val imeInsets = WindowInsets.ime
    var rootHeightPx by remember { mutableStateOf(0f) }
    // Канон: клавиатура не прячет поле ввода. Фокус приходит РАНЬШЕ, чем
    // insets доедут (клавиатура ещё 0) — запоминаем провайдер нижней границы
    // поля и доскролливаем на каждом кадре анимации insets: пересчёт по СВЕЖЕЙ
    // позиции поля не даёт ни недоскролла на промежуточном кадре, ни перебега.
    // ВАЖНО: insets НЕ читаются в композиции — иначе каждый кадр анимации
    // клавиатуры перекомпозировал бы весь экран (в debug заметные лаги).
    var pendingReveal by remember { mutableStateOf<(() -> Float)?>(null) }
    suspend fun scrollFieldAboveKeyboard(imePx: Int, bottom: () -> Float) {
        if (rootHeightPx <= 0f || imePx <= 0) return
        // 24: всё поле (паспортные 64 / раскрытый баян) — целиком над клавиатурой
        val marginPx = with(density) { 24.dp.toPx() }
        val keyboardTopPx = rootHeightPx - imePx
        val need = bottom() - (keyboardTopPx - marginPx)
        if (need > 0) contentScroll.scrollBy(need)
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        androidx.compose.runtime.snapshotFlow { imeInsets.getBottom(density) }.collect { imePx ->
            if (imePx == 0) {
                pendingReveal = null
            } else {
                pendingReveal?.let { scrollFieldAboveKeyboard(imePx, it) }
            }
        }
    }
    fun revealField(bottom: () -> Float) {
        // чтение insets в обработчике события — нетрекнутое, композицию не дёргает
        val imeNow = imeInsets.getBottom(density)
        // клавиатура уже открыта (переключение полей) — скролл сразу
        if (imeNow > 0 && rootHeightPx > 0) {
            revealScope.launch { scrollFieldAboveKeyboard(imeNow, bottom) }
        } else {
            pendingReveal = bottom
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .onGloballyPositioned { rootHeightPx = it.size.height.toFloat() }
    ) {
        TenantEditToolbar(onBack = onBack)

        // Канон: клавиатура не прячет поле ввода — контент подскролливается
        // так, чтобы низ поля оказался над клавиатурой (revealField выше;
        // таббар не двигается)
        // БЕЗ imePadding на контейнере: на MIUI (adjustResize+edge-to-edge)
        // он давал белую полосу над клавиатурой и поднимал таббар. Вместо этого
        // — нижний паддинг ВНУТРИ скролла на высоту клавиатуры (модификатор
        // читает insets отложенно, только layout): запас прокрутки, без
        // которого последнее поле упирается в max и его нельзя докрутить
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(contentScroll)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.ime),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            // ---- Аватар + подпись ----
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(EditCardGrey),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = tenant?.avatarUrl
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(100.dp))
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
                Spacer(Modifier.height(8.dp))
                Text(
                    if (avatarUrl(tenant) != null) "Изменить фото" else "Добавить фото",
                    fontSize = 13.sp,
                    lineHeight = 15.7.sp,
                    letterSpacing = (-0.4).sp,
                    color = GreyText
                )
            }

            // ---- Основная информация ----
            Text("Основная информация", style = SectionTitleStyle)
            EditField(caption = "ФИО*", value = fullName, onValue = { fullName = it }, onFocused = { revealField(it) })
            EditField(caption = "Название компании", value = company, onValue = { company = it }, onFocused = { revealField(it) })
            EditField(
                caption = "Электронная почта",
                value = email,
                onValue = { email = it },
                keyboardType = KeyboardType.Email,
                onFocused = { revealField(it) }
            )
            EditField(
                caption = "Номер телефона*",
                value = phone,
                onValue = { phone = it },
                keyboardType = KeyboardType.Phone,
                onFocused = { revealField(it) }
            )

            // ---- Паспортные данные (только в редактировании, 2983:42340) ----
            Text("Паспортные данные", style = SectionTitleStyle)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PassportEditField(
                    caption = "Серия паспорта",
                    empty = passportSeries.text.isBlank() && passportNumber.text.isBlank(),
                    visible = passportVisible,
                    maskedValue = maskSeries(passportSeries.text),
                    value = passportSeries,
                    onValue = { passportSeries = it },
                    onToggleEye = { passportVisible = !passportVisible },
                    modifier = Modifier.weight(1f),
                    onFocused = { revealField(it) }
                )
                PassportEditField(
                    caption = "Номер паспорта",
                    empty = passportSeries.text.isBlank() && passportNumber.text.isBlank(),
                    visible = passportVisible,
                    maskedValue = maskNumber(passportNumber.text),
                    value = passportNumber,
                    onValue = { passportNumber = it },
                    onToggleEye = { passportVisible = !passportVisible },
                    modifier = Modifier.weight(1f),
                    onFocused = { revealField(it) }
                )
            }
            // «Прикрепить документ» (2983:42346/42347): скрепка 20 + 6 + текст
            // 13/500 #212121@0.85 С ПОДЧЁРКИВАНИЕМ (по тексту, не на всю ширину),
            // строка 20; до «Служебной информации» — 20 (spacedBy 12 + 8 здесь)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAttachDocSheet = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_attach_doc),
                        contentDescription = "Прикрепить документ",
                        tint = Graphite.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Прикрепить документ",
                        fontSize = 13.sp,
                        lineHeight = 15.7.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        letterSpacing = (-0.4).sp,
                        color = Graphite.copy(alpha = 0.85f),
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }
            }

            // ---- Служебная информация: баян, ЗАКРЫТ изначально (наш шеврон) ----
            var serviceExpanded by remember { mutableStateOf(false) }
            // Якорь на НИЗ формы: при раскрытии низ остаётся на месте,
            // экран докручивается вверх на высоту прироста
            val serviceScope = rememberCoroutineScope()
            var serviceAnchorBottom by remember { mutableStateOf<Float?>(null) }
            // Живой низ баяна: фокус в заметке докручивает ВЕСЬ раскрытый
            // баян над клавиатурой, а не одну строку текста
            var serviceBottomPx by remember { mutableStateOf(0f) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(EditCardGrey)
                    .padding(horizontal = 20.dp)
                    .then(if (serviceExpanded) Modifier.padding(vertical = 10.dp) else Modifier)
                    .onGloballyPositioned { c ->
                        serviceBottomPx = c.positionInRoot().y + c.size.height
                        val bottom = c.positionInRoot().y + c.size.height - contentScroll.value
                        if (!serviceExpanded) {
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (serviceExpanded) Modifier.height(40.dp) else Modifier.height(64.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { serviceExpanded = !serviceExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Служебная информация", style = Headline2MobStyle.copy(lineHeight = 18.2.sp))
                        if (!serviceExpanded) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                // Подпись закрытого баяна (2983:42335): 400/13 #727272
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
                            .graphicsLayer { rotationZ = if (serviceExpanded) 180f else 0f }
                    )
                }
                if (serviceExpanded) {
                    Spacer(Modifier.height(6.dp))
                    // Область заметки — вся свободная высота баяна: тап в любое
                    // её место ставит курсор в заметку, а не только по строкам
                    // текста; докрутка — по низу всего баяна
                    EditNoteField(
                        value = serviceInfo,
                        onValue = { serviceInfo = it },
                        onFocused = { revealField { serviceBottomPx } },
                        areaModifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 158.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ---- Шит «Прикрепить документ / Выберите источник» (2983:46741):
    // три РАБОЧИЕ ссылки — камера / галерея / файл ----
    if (showAttachDocSheet) {
        // Сфотографировать: файл во кэш через FileProvider
        val camContext = androidx.compose.ui.platform.LocalContext.current
        var camUri by remember { mutableStateOf<android.net.Uri?>(null) }
        val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.TakePicture()
        ) { _ -> }
        val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { _ -> }
        val fileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
        ) { _ -> }
        TenantActionModalSheet(onDismiss = { showAttachDocSheet = false }) {
            AttachDocumentSourceSheet(
                onCamera = {
                    showAttachDocSheet = false
                    runCatching {
                        val dir = java.io.File(camContext.cacheDir, "docs").apply { mkdirs() }
                        val file = java.io.File(dir, "doc_${System.currentTimeMillis()}.jpg")
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            camContext, camContext.packageName + ".fileprovider", file
                        )
                        camUri = uri
                        cameraLauncher.launch(uri)
                    }
                },
                onGallery = {
                    showAttachDocSheet = false
                    galleryLauncher.launch("image/*")
                },
                onFile = {
                    showAttachDocSheet = false
                    fileLauncher.launch(arrayOf("*/*"))
                }
            )
        }
    }

    // ---- Тапбар (2983:42353/42264): серый без изменений → чёрный после ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(TabBarGrey)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding()
        ) {
            com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton(
                text = "Сохранить изменения",
                enabled = hasChanges && !state.isSaving,
                onClick = {
                    val t = tenant ?: return@BlackCtaButton
                    viewModel.save(
                        t.copy(
                            fullName = fullName.text.trim().ifBlank { t.fullName },
                            companyName = company.text.trim(),
                            email = email.text.trim(),
                            phone = phone.text.trim().ifBlank { t.phone },
                            passportData = (passportSeries.text.trim() + passportNumber.text.trim()).ifBlank { null },
                            serviceInfo = serviceInfo.text
                        )
                    ) { ok -> if (ok) onBack() }
                }
            )
            Spacer(Modifier.height(6.dp))
            OutlineCtaButton(
                text = "Отменить изменения",
                borderColor = Graphite,
                onClick = onBack
            )
        }
    }
}

private fun avatarUrl(t: TenantDto?): String? = t?.avatarUrl

/** Шапка «Редактирование арендатора» — канон: статус → 27 → стрелка+8+текст → 13. */
@Composable
private fun TenantEditToolbar(onBack: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.statusBarsPadding()
    ) {
        Spacer(Modifier.height(27.dp))
        // Канон: заголовок экрана всегда нажимаем — как стрелка «назад»
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 13.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onBack() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_landlord_back),
                contentDescription = "Назад",
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Редактирование арендатора",
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontFamily = com.rentmanager.app.ui.theme.InterFontFamily,
                color = Graphite,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

/**
 * Поле редактирования 64 r20 #EFEFEF (подпись 13/400 + ввод 15/600).
 * Курсор по канону: тап в любом месте поля — курсор в конец; Done — клавиатура
 * прячется, фокус снимается (курсор исчезает и не мигает), на другое поле не прыгает.
 */
@Composable
private fun EditField(
    caption: String,
    value: TextFieldValue,
    onValue: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onFocused: (() -> Float) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var fieldBottomPx by remember { mutableStateOf(0f) }
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(EditCardGrey)
            .onGloballyPositioned { fieldBottomPx = it.positionInRoot().y + it.size.height }
            .padding(horizontal = 20.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    // Фокус только по факту ТАПА: жест прокрутки уводит палец
                    // за touch slop — ожидание отменяется, поле не активируется
                    // и клавиатура не выезжает
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        focusRequester.requestFocus()
                        onValue(value.copy(selection = TextRange(value.text.length)))
                        up.consume()
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(caption, fontSize = 13.sp, lineHeight = 15.7.sp, letterSpacing = (-0.4).sp, color = GreyText)
            BasicTextField(
                value = value,
                onValueChange = {
                    onValue(it)
                    onFocused { fieldBottomPx }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) onFocused { fieldBottomPx } }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val up = waitForUpOrCancellation()
                            if (up != null) {
                                focusRequester.requestFocus()
                                onValue(value.copy(selection = TextRange(value.text.length)))
                                up.consume()
                            }
                        }
                    },
                textStyle = Headline2MobStyle.copy(lineHeight = 18.2.sp),
                cursorBrush = SolidColor(Graphite),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )
        }
    }
}

/**
 * Многострочная заметка «Служебная информация» (та же механика курсора).
 * areaModifier задаётся на обёртке, растянутой на всю область баяна:
 * тап в ЛЮБОЕ место области (не только по строкам текста) ставит фокус
 * и курсор в конец.
 */
@Composable
private fun EditNoteField(
    value: TextFieldValue,
    onValue: (TextFieldValue) -> Unit,
    onFocused: (() -> Float) -> Unit = {},
    areaModifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var noteBottomPx by remember { mutableStateOf(0f) }
    Box(
        modifier = areaModifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                // Курсор — только по тапу; прокрутка отменит ожидание по slop
                val up = waitForUpOrCancellation()
                if (up != null) {
                    focusRequester.requestFocus()
                    onValue(value.copy(selection = TextRange(value.text.length)))
                    up.consume()
                }
            }
        }
    ) {
        BasicTextField(
            value = value,
            onValueChange = {
                onValue(it)
                onFocused { noteBottomPx }
            },
            modifier = Modifier
                .onGloballyPositioned { noteBottomPx = it.positionInRoot().y + it.size.height }
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onFocused { noteBottomPx } }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            focusRequester.requestFocus()
                            onValue(value.copy(selection = TextRange(value.text.length)))
                            up.consume()
                        }
                    }
                },
        textStyle = Headline2MobStyle.copy(
            fontSize = 13.sp,
            lineHeight = 15.7.sp,
            letterSpacing = (-0.4).sp
        ),
        cursorBrush = SolidColor(Graphite),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )
    }
}


/** Маска серии: «·· 08» — видны последние 2 цифры. */
private fun maskSeries(raw: String): String {
    val d = raw.filter { it.isDigit() }
    return if (d.length < 2) "" else "·· " + d.takeLast(2)
}

/** Маска номера: «········45» — по требованию видны последние 2 цифры
 *  (в макете 50897 показаны 4 — делаем как просили: 2). */
private fun maskNumber(raw: String): String {
    val d = raw.filter { it.isDigit() }
    return if (d.length < 2) "" else "········" + d.takeLast(2)
}

/**
 * Поле паспорта в редактировании — три состояния (канвас «14»):
 *  • пустое (42340): подпись 15/600 #727272 в две строки (ширина 110), глаз-слэш;
 *  • заполнено видимо (42233): подпись 13/400 + значение 15/600, глаз;
 *  • заполнено скрыто (50897): подпись 13/400 + маска, глаз-слэш.
 * Тап по полю: скрытое — раскрыть и править; видимое — курсор в конец.
 */
@Composable
private fun PassportEditField(
    caption: String,
    empty: Boolean,
    visible: Boolean,
    maskedValue: String,
    value: TextFieldValue,
    onValue: (TextFieldValue) -> Unit,
    onToggleEye: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (() -> Float) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Поле активируется первым тапом/вводом — до этого выглядит как заглушка 42340
    var fieldActive by remember { mutableStateOf(false) }
    var fieldBottomPx by remember { mutableStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(fieldActive) {
        if (fieldActive) runCatching { focusRequester.requestFocus() }
    }
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(EditCardGrey)
            .onGloballyPositioned { fieldBottomPx = it.positionInRoot().y + it.size.height }
            .padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (empty && !fieldActive) {
                // Пустое поле до первого тапа: подпись 15/600 серым в две
                // строки (42340); поле ввода спрятано ПОД ней — тап активирует
                Text(
                    caption,
                    fontSize = 15.sp,
                    lineHeight = 18.2.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = GreyText,
                    modifier = Modifier
                        .width(110.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            fieldActive = true
                            if (!visible) onToggleEye()
                            onFocused { fieldBottomPx }
                            // фокус — ПОСЛЕ рекомпозиции (поле ещё не в дереве,
                            // мгновенный requestFocus падал FocusRequester is not initialized)
                        }
                )
            } else {
                Text(caption, fontSize = 13.sp, lineHeight = 15.7.sp, letterSpacing = (-0.4).sp, color = GreyText)
                // Глаз закрыт — ТОЛЬКО маска (последние 2 цифры), даже если поле
                // уже правилось: тап по маске раскрывает и возвращает редактор
                if (visible) {
                    BasicTextField(
                        value = value,
                        onValueChange = {
                            fieldActive = true
                            onValue(it)
                            onFocused { fieldBottomPx }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged { if (it.isFocused) onFocused { fieldBottomPx } }
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    // фокус/клавиатура — только по тапу, не по скроллу
                                    val up = waitForUpOrCancellation()
                                    if (up != null) {
                                        focusRequester.requestFocus()
                                        onValue(value.copy(selection = TextRange(value.text.length)))
                                        up.consume()
                                    }
                                }
                            },
                        textStyle = Headline2MobStyle.copy(lineHeight = 18.2.sp),
                        cursorBrush = SolidColor(Graphite),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                } else {
                    // fillMaxWidth: и пустая маска занимает колонку — тап по любому
                    // месту поля раскрывает (иначе пустой Text нулевой ширины)
                    Text(
                        maskedValue,
                        style = Headline2MobStyle.copy(lineHeight = 18.2.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    // раскрытие — только по тапу, не по скроллу
                                    val up = waitForUpOrCancellation()
                                    if (up != null) {
                                        onToggleEye()
                                        up.consume()
                                    }
                                }
                            }
                    )
                }
            }
        }
        Icon(
            painter = painterResource(if (visible && !empty) R.drawable.ic_eye else R.drawable.ic_eye_slash),
            contentDescription = if (visible) "Скрыть паспорт" else "Показать паспорт",
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggleEye() }
        )
    }
}


/**
 * Шит источников документа (2983:46741, отступы по замерам):
 * заголовок 20/600 (24) + 6 + подпись 15/600 (18) + 20 до строк;
 * строка = 9 + [иконка 18 + 8 + текст 15/600 (19)] + 9 + черта #DBDBDB + 12
 * до следующей. Три РАБОЧИЕ ссылки: камера / галерея / файл.
 */
@Composable
private fun AttachDocumentSourceSheet(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onFile: () -> Unit
) {
    Column {
        Text(
            "Прикрепить документ",
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = Graphite
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Выберите источник",
            fontSize = 15.sp,
            lineHeight = 18.2.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            color = GreyText
        )
        Spacer(Modifier.height(20.dp))
        val sources = listOf(
            Triple(R.drawable.ic_doc_camera, "Сфотографировать", onCamera),
            Triple(R.drawable.ic_doc_gallery, "Выбрать из галереи", onGallery),
            Triple(R.drawable.ic_doc_file, "Выбрать файл", onFile)
        )
        sources.forEachIndexed { index, (icon, label, action) ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { action() }
            ) {
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        label,
                        style = Headline2MobStyle.copy(lineHeight = 19.sp)
                    )
                }
                Spacer(Modifier.height(9.dp))
                androidx.compose.material3.HorizontalDivider(color = Color(0xFFDBDBDB))
            }
        }
    }
}
