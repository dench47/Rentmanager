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

/** Выбранный в редактировании файл, ещё НЕ прикреплённый к карточке.
 *  Аннотация Вики 2983:45668: при «Отменить и выйти» добавленные файлы НЕ
 *  прикрепляются, значит запись на сервер — только по «Сохранить изменения». */
data class StagedDocument(
    val uri: android.net.Uri,
    val name: String,
    val storageName: String,
    val mimeType: String?
)

data class TenantEditUiState(
    val isLoading: Boolean = true,
    val tenant: TenantDto? = null,
    val isSaving: Boolean = false,
    /** Добавленные в этой сессии файлы (ещё не на сервере) */
    val staged: List<StagedDocument> = emptyList(),
    /** Прикреплённые документы, помеченные на удаление (удалим при сохранении) */
    val markedForDeletion: Set<String> = emptySet()
)

@HiltViewModel
class TenantEditViewModel @Inject constructor(
    private val tenantApi: TenantApi,
    private val tenantEvents: TenantEvents,
    private val photoUploader: com.rentmanager.app.data.repository.PhotoUploader,
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

    /** Добавить файл в стейдж (строка сразу видна, сервер — только при сохранении) */
    fun stageDocument(uri: android.net.Uri, name: String, storageName: String, mimeType: String?) {
        _uiState.update { it.copy(staged = it.staged + StagedDocument(uri, name, storageName, mimeType)) }
    }

    /** Корзина на ещё не прикреплённом файле — просто убрать из стейджа */
    fun unstageDocument(uri: android.net.Uri) {
        _uiState.update { it.copy(staged = it.staged.filterNot { d -> d.uri == uri }) }
    }

    /** Корзина на прикреплённом документе — пометить: удаляем при сохранении */
    fun markDocumentForDeletion(docId: String) {
        _uiState.update { it.copy(markedForDeletion = it.markedForDeletion + docId) }
    }

    /**
     * «Сохранить изменения»: сначала документы (загрузка в хранилище + прикрепление
     * стейджа, затем удаление помеченных), потом поля карточки. false — окно с глобусом.
     */
    fun save(fields: TenantDto, onDone: (Boolean) -> Unit) {
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val staged = _uiState.value.staged
            val marked = _uiState.value.markedForDeletion
            val docsOk = runCatching {
                staged.forEach { d ->
                    val uploaded = photoUploader.uploadDocument(d.uri, d.storageName, d.mimeType)
                    val type = d.storageName.substringAfterLast('.', "").uppercase()
                    val added = tenantApi.addTenantDocument(
                        tenantId,
                        com.rentmanager.app.data.api.AddTenantDocumentRequest(
                            name = d.name,
                            fileType = type.ifEmpty { null },
                            url = uploaded.url,
                            size = uploaded.size
                        )
                    ).isSuccessful
                    if (!added) throw IllegalStateException("attach document failed")
                }
                marked.forEach { docId ->
                    val removed = tenantApi.deleteTenantDocument(tenantId, docId).isSuccessful
                    if (!removed) throw IllegalStateException("delete document failed")
                }
            }.isSuccess
            if (!docsOk) {
                _uiState.update { it.copy(isSaving = false) }
                onDone(false)
                return@launch
            }
            val ok = runCatching {
                tenantApi.updateTenant(tenantId, fields).isSuccessful
            }.getOrDefault(false)
            if (ok) {
                // Карточка позади перечитается по тикеру события
                _uiState.update { it.copy(staged = emptyList(), markedForDeletion = emptySet()) }
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
    // Номер документа (правка Вики 2026-09-24, 3677:31337): серия и номер — ОДНО поле
    // Номер документа — СВОБОДНЫЙ текст: серия может быть буквенной (не-РФ)
    val passportStored = tenant?.passportData.orEmpty()
    var passportValue by remember { mutableStateOf(TextFieldValue(passportStored)) }
    var serviceInfo by remember { mutableStateOf(TextFieldValue(tenant?.serviceInfo.orEmpty())) }
    var showAttachDocSheet by remember { mutableStateOf(false) }

    // Несохранённые изменения ПОЛЕЙ (документы учитываются ниже: стейдж + пометки)
    val fieldChanges = tenant != null && (
        fullName.text != tenant.fullName ||
            company.text != tenant.companyName.orEmpty() ||
            email.text != tenant.email.orEmpty() ||
            phone.text != tenant.phone ||
            passportValue.text.trim() != passportStored ||
            serviceInfo.text != tenant.serviceInfo.orEmpty()
        )
    // Нижние CTA активны при ЛЮБОМ изменении, включая добавленные/помеченные документы
    val hasChanges = fieldChanges || state.staged.isNotEmpty() || state.markedForDeletion.isNotEmpty()

    var showSavedDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    // Выход с несохранёнными изменениями — сперва диалог (2983:45634)
    fun attemptExit() {
        if (hasChanges) showCancelDialog = true else onBack()
    }

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
    // ---- Документы: лаунчеры живут на уровне ЭКРАНА, а не внутри шита:
    // шит закрывается сразу после запуска интента, и объявленный в нём лаунчер
    // размонтировался бы вместе с диалогом — результат терялся бы.
    val docContext = androidx.compose.ui.platform.LocalContext.current
    val todayLabel = remember {
        java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }
    fun docStorageName(mime: String?, displayName: String): String {
        val ext = displayName.substringAfterLast('.', "").lowercase()
            .ifBlank { if (mime?.startsWith("image/") == true) "jpg" else "bin" }
        return "doc_" + System.currentTimeMillis() + "." + ext
    }
    val camUriHolder = remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = camUriHolder.value
        if (ok && uri != null) {
            viewModel.stageDocument(
                uri, "Фото от " + todayLabel, docStorageName("image/jpeg", "doc.jpg"), "image/jpeg"
            )
        }
    }
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val mime = runCatching { docContext.contentResolver.getType(uri) }.getOrNull()
            val display = docDisplayName(docContext, uri).ifBlank {
                if (mime?.startsWith("image/") == true) "Фото от " + todayLabel
                else "Документ от " + todayLabel
            }
            viewModel.stageDocument(uri, display, docStorageName(mime, display), mime)
        }
    }
    val fileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val mime = runCatching { docContext.contentResolver.getType(uri) }.getOrNull()
            val display = docDisplayName(docContext, uri).ifBlank {
                if (mime?.startsWith("image/") == true) "Фото от " + todayLabel
                else "Документ от " + todayLabel
            }
            viewModel.stageDocument(uri, display, docStorageName(mime, display), mime)
        }
    }

    // Корень — Box: экран-фон + канонические диалоги-оверлеи ПОВЕРХ него.
    // CanonicalDialog — оверлей в окне экрана (Box+скрим), ребёнком колонки
    // он забирал высоту раскладки и ломал экран
    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .onGloballyPositioned { rootHeightPx = it.size.height.toFloat() }
    ) {
        TenantEditToolbar(onBack = { attemptExit() })

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

            // ---- Паспорт / ID (правка Вики 2026-09-24, 3677:31337): серия и номер
            // объединены в ОДНО поле «Номер документа», глаза и маски убраны ----
            Text("Паспорт / ID", style = SectionTitleStyle)
            PassportNumberField(
                value = passportValue,
                onValue = { passportValue = it },
                onFocused = { revealField(it) }
            )
            // ---- Документы (2983:42232): строка 40 + 12 + дивайдер #DBDBDB,
            // между строками 12. Показываем прикреплённые (кроме помеченных на
            // удаление) и добавленные в этой сессии. Корзина: прикреплённый —
            // пометка на удаление, добавленный — убрать из стейджа. Тап — открыть ----
            val docScope = rememberCoroutineScope()
            tenant?.documents.orEmpty()
                .filterNot { it.id in state.markedForDeletion }
                .forEach { doc ->
                    DocumentRow(
                        doc = doc,
                        onOpen = {
                            docScope.launch { openDocumentFromUrl(docContext, doc.url, doc.name, doc.fileType) }
                        },
                        onDelete = { viewModel.markDocumentForDeletion(doc.id) }
                    )
                }
            state.staged.forEach { staged ->
                DocumentRow(
                    doc = com.rentmanager.app.data.model.TenantDocumentDto(
                        id = staged.uri.toString(),
                        name = staged.name,
                        fileType = staged.storageName.substringAfterLast('.', "").uppercase().ifEmpty { null },
                        url = staged.uri.toString()
                    ),
                    onOpen = {
                        docScope.launch { openDocumentFromUrl(docContext, staged.uri.toString(), staged.name, staged.mimeType) }
                    },
                    onDelete = { viewModel.unstageDocument(staged.uri) }
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
        TenantActionModalSheet(onDismiss = { showAttachDocSheet = false }) {
            AttachDocumentSourceSheet(
                onCamera = {
                    showAttachDocSheet = false
                    try {
                        val dir = java.io.File(docContext.cacheDir, "docs").apply { mkdirs() }
                        val file = java.io.File(dir, "doc_${System.currentTimeMillis()}.jpg")
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            docContext, docContext.packageName + ".fileprovider", file
                        )
                        camUriHolder.value = uri
                        cameraLauncher.launch(uri)
                    } catch (e: Exception) {
                        // Немой runCatching тут скрывал причину: камера могла не
                        // открыться из-за разрешения/отсутствия приложения
                        android.util.Log.e("DOCS", "camera launch failed: " + e.message, e)
                        errorText = "Не удалось прикрепить документ. \nПовторите еще раз"
                    }
                },
                onGallery = {
                    showAttachDocSheet = false
                    try {
                        galleryLauncher.launch("image/*")
                    } catch (e: Exception) {
                        android.util.Log.e("DOCS", "gallery launch failed: " + e.message, e)
                        errorText = "Не удалось прикрепить документ. \nПовторите еще раз"
                    }
                },
                onFile = {
                    showAttachDocSheet = false
                    try {
                        fileLauncher.launch(arrayOf("*/*"))
                    } catch (e: Exception) {
                        android.util.Log.e("DOCS", "file launch failed: " + e.message, e)
                        errorText = "Не удалось прикрепить документ. \nПовторите еще раз"
                    }
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
                            passportData = passportValue.text.trim().ifBlank { null },
                            serviceInfo = serviceInfo.text
                        )
                    ) { ok ->
                        if (ok) {
                            showSavedDialog = true
                        } else {
                            errorText = "Не удалось сохранить изменения. \nПовторите еще раз"
                        }
                    }
                }
            )
            Spacer(Modifier.height(6.dp))
            OutlineCtaButton(
                text = "Отменить изменения",
                borderColor = Graphite,
                onClick = { attemptExit() }
            )
        }
    } // конец экрана-фона; ниже — оверлеи поверх Box

        // ---- Окно с глобусом: сбой загрузки/прикрепления/сохранения ----
        errorText?.let { msg ->
            com.rentmanager.app.ui.components.IconNotificationDialog(
                iconRes = R.drawable.ic_globe_warning_vec,
                text = msg,
                iconGap = 12.dp,
                onDismiss = { errorText = null }
            )
        }

        // ---- «Изменения сохранены» (2983:42825): короткое подтверждение → карточка ----
        if (showSavedDialog) {
            com.rentmanager.app.ui.components.CanonicalDialog(
                onDismiss = {
                    if (showSavedDialog) {
                        showSavedDialog = false
                        onBack()
                    }
                },
                icon = R.drawable.ic_success_check,
                title = "Изменения сохранены"
            )
            androidx.compose.runtime.LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1400)
                if (showSavedDialog) {
                    showSavedDialog = false
                    onBack()
                }
            }
        }

        // ---- «Отменить изменения?» (2983:45634): выход с несохранёнными ----
        // После «Отменить и выйти»: ранее сохранённые данные восстанавливаются,
        // добавленные файлы НЕ прикрепляются, помеченные на удаление сохраняются
        if (showCancelDialog) {
            com.rentmanager.app.ui.components.CanonicalDialog(
                onDismiss = { showCancelDialog = false },
                title = "Отменить изменения?",
                text = "Внесенные изменения не сохранятся. Вы вернетесь к сохраненной карточке арендатора"
            ) {
                com.rentmanager.app.ui.components.CanonicalDialogButton(
                    text = "Продолжить редактирование",
                    container = Graphite,
                    textColor = Color.White,
                    onClick = { showCancelDialog = false }
                )
                com.rentmanager.app.ui.components.CanonicalDialogButton(
                    text = "Отменить и выйти",
                    stroke = Graphite,
                    textColor = Graphite,
                    onClick = {
                        showCancelDialog = false
                        onBack()
                    }
                )
            }
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
 * и курсор в конец. Переиспользуется экраном «Новый арендатор».
 */
@Composable
fun EditNoteField(
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


/**
 * Шит источников документа (2983:46741, отступы по замерам):
 * заголовок 20/600 (24) + 6 + подпись 15/600 (18) + 20 до строк;
 * строка = 9 + [иконка 18 + 8 + текст 15/600 (19)] + 9 + черта #DBDBDB + 12
 * до следующей. Три РАБОЧИЕ ссылки: камера / галерея / файл.
 * Переиспользуется экраном «Новый арендатор».
 */
@Composable
fun AttachDocumentSourceSheet(
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

/**
 * Поле «Номер документа» в редактировании (правка Вики 2026-09-24, 3677:31337):
 * пустое — подпись 15/600 #727272 одной строкой; заполненное — подпись 13/400 +
 * значение 15/600. ПОЛЕ БЕЗ МАСКИ И БЕЗ ЧИСЛОВОЙ КЛАВИАТУРЫ: серия бывает
 * буквенной (арендаторы не только из РФ) — это просто текст.
 * Тап по полю — курсор в конец (канон инлайн-редактирования).
 */
@Composable
private fun PassportNumberField(
    value: TextFieldValue,
    onValue: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (() -> Float) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // Фокус — ТОЛЬКО по тапу: при открытии экрана курсор нигде не мигает.
    // Наличие данных влияет лишь на отображение (подпись+значение либо заглушка)
    var tapped by remember { mutableStateOf(false) }
    var fieldBottomPx by remember { mutableStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(tapped) {
        if (tapped) runCatching { focusRequester.requestFocus() }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(EditCardGrey)
            .onGloballyPositioned { fieldBottomPx = it.positionInRoot().y + it.size.height }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (!tapped && value.text.isBlank()) {
                Text(
                    "Номер документа",
                    fontSize = 15.sp,
                    lineHeight = 18.2.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    letterSpacing = (-0.4).sp,
                    color = GreyText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            tapped = true
                            onFocused { fieldBottomPx }
                        }
                )
            } else {
                Text("Номер документа", fontSize = 13.sp, lineHeight = 15.7.sp, letterSpacing = (-0.4).sp, color = GreyText)
                BasicTextField(
                    value = value,
                    onValueChange = {
                        tapped = true
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }
        }
    }
}
