package com.rentmanager.app.ui.landlord.tenants

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.R
import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.local.TenantEvents
import com.rentmanager.app.data.model.TenantDto
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.util.PhoneUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val NewCardGrey = Color(0xFFEFEFEF)
private val NewTabBarGrey = Color(0xFFEDEDED).copy(alpha = 0.9f)

@HiltViewModel
class NewTenantViewModel @Inject constructor(
    private val tenantApi: TenantApi,
    private val tenantEvents: TenantEvents
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    fun save(tenant: TenantDto, onDone: (Boolean) -> Unit) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            val ok = runCatching { tenantApi.createTenant(tenant).isSuccessful }.getOrDefault(false)
            if (ok) tenantEvents.notifyChanged()
            _isSaving.value = false
            onDone(ok)
        }
    }
}

/**
 * «Новый арендатор» (канвас «15», 3695:33307): «Выбрать контакт» (системный
 * пикер заполняет ФИО+телефон) или ввод вручную; дополнительные данные,
 * паспорт/ID с прикреплением документа и служебная заметка. Тапбар:
 * «Сохранить» (40% до заполнения ФИО и телефона) + «Отменить».
 * Каноны экрана: поля активируются тапом (не скроллом), клавиатура не
 * прячет поле, Done прячет клавиатуру и снимает фокус.
 */
@Composable
fun NewTenantScreen(
    onBack: () -> Unit,
    viewModel: NewTenantViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isSaving by viewModel.isSaving.collectAsState()

    var fullName by remember { mutableStateOf(TextFieldValue()) }
    var phone by remember { mutableStateOf(TextFieldValue()) }
    var company by remember { mutableStateOf(TextFieldValue()) }
    var email by remember { mutableStateOf(TextFieldValue()) }
    var document by remember { mutableStateOf(TextFieldValue()) }
    var serviceInfo by remember { mutableStateOf(TextFieldValue()) }
    var showAttachDocSheet by remember { mutableStateOf(false) }

    val canSave = fullName.text.isNotBlank() && phone.text.isNotBlank() && !isSaving

    // ---- «Выбрать контакт»: системный пикер → ФИО + первый телефон ----
    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val resolver = context.contentResolver
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (!c.moveToFirst()) return@use
                val nameIdx = c.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                if (nameIdx >= 0) {
                    c.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let {
                        fullName = TextFieldValue(it, TextRange(it.length))
                    }
                }
                val hasPhoneIdx = c.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER)
                val idIdx = c.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                if (hasPhoneIdx >= 0 && idIdx >= 0 &&
                    c.getInt(hasPhoneIdx) > 0
                ) {
                    val id = c.getString(idIdx) ?: return@use
                    resolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id), null
                    )?.use { p ->
                        if (p.moveToFirst()) {
                            val numIdx = p.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numIdx >= 0) {
                                p.getString(numIdx)?.takeIf { it.isNotBlank() }?.let {
                                    phone = TextFieldValue(it, TextRange(it.length))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Канон: клавиатура не прячет поле ввода (как в редактировании) ----
    val contentScroll = rememberScrollState()
    val revealScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    var rootHeightPx by remember { mutableStateOf(0f) }
    var pendingReveal by remember { mutableStateOf<(() -> Float)?>(null) }
    suspend fun scrollFieldAboveKeyboard(imePx: Int, bottom: () -> Float) {
        if (rootHeightPx <= 0f || imePx <= 0) return
        val marginPx = with(density) { 24.dp.toPx() }
        val keyboardTopPx = rootHeightPx - imePx
        val need = bottom() - (keyboardTopPx - marginPx)
        if (need > 0) contentScroll.scrollBy(need)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { imeInsets.getBottom(density) }.collect { imePx ->
            if (imePx == 0) {
                pendingReveal = null
            } else {
                pendingReveal?.let { scrollFieldAboveKeyboard(imePx, it) }
            }
        }
    }
    fun revealField(bottom: () -> Float) {
        val imeNow = imeInsets.getBottom(density)
        if (imeNow > 0 && rootHeightPx > 0) {
            revealScope.launch { scrollFieldAboveKeyboard(imeNow, bottom) }
        } else {
            pendingReveal = bottom
        }
    }

    // Архитектура 1:1 с экраном редактирования: БЕЗ Scaffold — корень на всё
    // окно (rootHeightPx честный для докрутки), статус-инсет на шапке, нижний
    // инсет только на тапбаре (один раз, без двойного)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .onGloballyPositioned { rootHeightPx = it.size.height.toFloat() }
    ) {
        Column(Modifier.statusBarsPadding()) {
            Spacer(Modifier.height(27.dp))
            // ---- Шапка: КАНОН — заголовок всегда нажимаем = назад (стрелка
            // и текст в одной кликабельной строке, как в редактировании) ----
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
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_landlord_back),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Новый арендатор",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = Graphite,
                    letterSpacing = (-0.3).sp
                )
            }
        }

            // ---- Контент ----
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(contentScroll)
                    .padding(horizontal = 20.dp)
                    .windowInsetsPadding(WindowInsets.ime),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Основная информация", fontSize = 18.sp, lineHeight = 21.8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, color = Graphite)

                // «Выбрать контакт»: контурная 48 r100, иконка 24 + 6 + текст
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .border(1.dp, Graphite, RoundedCornerShape(100.dp))
                        .clickable { contactPicker.launch(null) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_contact_card),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Выбрать контакт",
                        fontSize = 15.sp,
                        lineHeight = 18.2.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.4).sp,
                        color = Graphite
                    )
                }

                Text(
                    "или ввести вручную",
                    fontSize = 13.sp,
                    lineHeight = 15.7.sp,
                    letterSpacing = (-0.4).sp,
                    color = GreyText,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                NewTenantField(caption = "ФИО*", value = fullName, onValue = { fullName = it }, onFocused = { revealField(it) })
                // 20 до заголовка секции: bottom 8 на поле + spacedBy 12
                // (Spacer между детьми дал бы 12+8+12=32)
                NewTenantField(
                    caption = "Номер телефона*",
                    value = phone,
                    onValue = { phone = it },
                    keyboardType = KeyboardType.Phone,
                    modifier = Modifier.padding(bottom = 8.dp),
                    onFocused = { revealField(it) }
                )
                Text("Дополнительные данные", fontSize = 18.sp, lineHeight = 21.8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, color = Graphite)
                NewTenantField(caption = "Название компании", value = company, onValue = { company = it }, onFocused = { revealField(it) })
                NewTenantField(
                    caption = "Электронная почта",
                    value = email,
                    onValue = { email = it },
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.padding(bottom = 8.dp),
                    onFocused = { revealField(it) }
                )
                Text("Паспорт / ID", fontSize = 18.sp, lineHeight = 21.8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, color = Graphite)
                NewTenantField(
                    caption = "Номер документа",
                    value = document,
                    onValue = { document = it },
                    keyboardType = KeyboardType.Number,
                    onFocused = { revealField(it) }
                )

                // «Прикрепить документ» (как в редактировании: скрепка +
                // подчёркнутый текст 13/500 #212121@0.85)
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
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.4).sp,
                            color = Graphite.copy(alpha = 0.85f),
                            textDecoration = TextDecoration.Underline
                        )
                    }
                }

                // ---- Служебная информация: баян, закрыт изначально ----
                var serviceExpanded by remember { mutableStateOf(false) }
                // Якорь на НИЗ формы (правило редактирования): при раскрытии
                // низ остаётся на месте, экран докручивается на высоту прироста
                val serviceScope = rememberCoroutineScope()
                var serviceBottomPx by remember { mutableStateOf(0f) }
                var serviceAnchorBottom by remember { mutableStateOf<Float?>(null) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Зазор 20 до тапбара (3695:33337 → 3695:33309) при
                        // максимально прокрученном списке — ДО clip/background,
                        // иначе серая карточка съедает зазор (белым его не видно)
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NewCardGrey)
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
                                    "Эта информация видна только вам",
                                    fontSize = 13.sp,
                                    lineHeight = 15.7.sp,
                                    letterSpacing = (-0.4).sp,
                                    color = GreyText
                                )
                            }
                        }
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ic_card_chevron),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer { rotationZ = if (serviceExpanded) 180f else 0f }
                        )
                    }
                    if (serviceExpanded) {
                        Spacer(Modifier.height(6.dp))
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
            }

            // ---- Тапбар: Сохранить + Отменить. Как в редактировании: корень
            // без Scaffold, нижний инсет ОДИН раз здесь — тапбар упирается
            // прямо в системную панель (3695:33309) ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(NewTabBarGrey)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .navigationBarsPadding()
            ) {
                BlackCtaButton(
                    text = if (isSaving) "Сохранение…" else "Сохранить",
                    enabled = canSave,
                    onClick = {
                        viewModel.save(
                            TenantDto(
                                id = "",
                                fullName = fullName.text.trim(),
                                phone = PhoneUtils.normalize(phone.text) ?: phone.text.trim(),
                                companyName = company.text.trim().ifBlank { null },
                                email = email.text.trim().ifBlank { null },
                                passportData = document.text.filter { it.isDigit() }.ifBlank { null },
                                serviceInfo = serviceInfo.text.ifBlank { null }
                            )
                        ) { ok ->
                            if (ok) onBack()
                            else Toast.makeText(context, "Не удалось сохранить арендатора", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                Spacer(Modifier.height(6.dp))
                OutlineCtaButton(
                    text = "Отменить",
                    borderColor = Graphite,
                    onClick = onBack
                )
            }
    }

    // ---- Шит источников документа (общий с редактированием) ----
    if (showAttachDocSheet) {
        val camContext = LocalContext.current
        var camUri by remember { mutableStateOf<android.net.Uri?>(null) }
        val cameraLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { }
        val galleryLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { }
        val fileLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { }
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
}

/**
 * Поле «Нового арендатора» 64 r20 #EFEFEF: пустое — подпись 15/600 #727272
 * по центру (3695:33307); после первого тапа — подпись 13/400 + ввод 15/600.
 * Тап по всему полю активирует и ставит курсор в конец; скролл поле не
 * активирует (ожидание отпускания); Done прячет клавиатуру.
 */
@Composable
private fun NewTenantField(
    caption: String,
    value: TextFieldValue,
    onValue: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onFocused: (() -> Float) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var fieldActive by remember { mutableStateOf(false) }
    var fieldBottomPx by remember { mutableStateOf(0f) }
    LaunchedEffect(fieldActive) {
        if (fieldActive) runCatching { focusRequester.requestFocus() }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(NewCardGrey)
            .onGloballyPositioned { fieldBottomPx = it.positionInRoot().y + it.size.height }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!fieldActive && value.text.isEmpty()) {
            // Пустое поле до первого тапа: подпись 15/600 серым по центру
            Text(
                caption,
                fontSize = 15.sp,
                lineHeight = 18.2.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = GreyText,
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            val up = waitForUpOrCancellation()
                            if (up != null) {
                                fieldActive = true
                                onFocused { fieldBottomPx }
                                up.consume()
                            }
                        }
                    }
            )
        } else {
            Column(Modifier.weight(1f)) {
                Text(caption, fontSize = 13.sp, lineHeight = 15.7.sp, letterSpacing = (-0.4).sp, color = GreyText)
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
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }
        }
    }
}
