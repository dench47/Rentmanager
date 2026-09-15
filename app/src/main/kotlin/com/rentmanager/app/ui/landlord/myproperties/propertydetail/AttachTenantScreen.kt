package com.rentmanager.app.ui.landlord.myproperties.propertydetail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.DatePickerSheet
import com.rentmanager.app.ui.components.DesignWidthDialog
import com.rentmanager.app.ui.components.IconNotificationDialog
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.landlord.createproperty.ScreenToolbar
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.ErrorRed
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.ui.theme.SectionTitleStyle
import com.rentmanager.app.ui.theme.ToolbarTitleStyle
import com.rentmanager.app.util.shortAddress
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val DdMmYyyy = DateTimeFormatter.ofPattern("dd.MM.yyyy")

/** Контакт из телефонной книги для шита «Выбрать контакт». */
data class PhoneContact(val name: String, val phone: String)

/**
 * Черновик прикрепления (Figma 2935:40049): выход с частично заполненным
 * экраном запоминается по объекту, при повторном входе предлагаем
 * «Продолжить» или «Начать заново». Хранится в SharedPreferences —
 * переживает выгрузку процесса (MIUI убивает фон) и перезапуск приложения;
 * полностью пустой ввод черновиком не считается.
 */
object AttachTenantDraftHolder {
    data class Draft(
        val startDate: LocalDate? = null,
        val endDate: LocalDate? = null,
        val fullName: String = "",
        val phone: String = ""
    )

    // Gson не сериализует java.time надёжно — даты строками ISO
    private data class DraftDto(
        val startDate: String? = null,
        val endDate: String? = null,
        val fullName: String = "",
        val phone: String = ""
    )

    private val gson = Gson()
    private const val PREFS = "attach_tenant_drafts"

    fun get(context: Context, propertyId: String): Draft? = runCatching {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(propertyId, null) ?: return null
        val dto = gson.fromJson(json, DraftDto::class.java)
        Draft(
            startDate = dto.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            endDate = dto.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            fullName = dto.fullName,
            phone = dto.phone
        )
    }.getOrNull()

    fun save(context: Context, propertyId: String, draft: Draft) {
        val blank = draft.startDate == null && draft.endDate == null &&
            draft.fullName.isBlank() && draft.phone.isBlank()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (blank) {
            prefs.edit { remove(propertyId) }
        } else {
            runCatching {
                val dto = DraftDto(
                    startDate = draft.startDate?.toString(),
                    endDate = draft.endDate?.toString(),
                    fullName = draft.fullName,
                    phone = draft.phone
                )
                prefs.edit { putString(propertyId, gson.toJson(dto)) }
            }
        }
    }

    fun clear(context: Context, propertyId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { remove(propertyId) }
    }
}

/**
 * Предложение добавить вручную введённого арендатора в телефонную книгу:
 * экран прикрепления кладёт сюда контакт при успешном прикреплении, если
 * контакт НЕ был выбран из книги; карточка объекта после возврата
 * показывает диалог «Добавить в контакты?» и очищает предложение.
 */
object ContactSuggestionHolder {
    var pending: PhoneContact? = null
}

/**
 * Экран «Добавить арендатора» (канвас Figma «Добавить арендатора»,
 * файл KyNooQwZuHP9Fz3qShY2Lg): карточка объекта, период аренды
 * (Начало/Окончание — календарные шиты), арендатор (контакты или вручную),
 * CTA «Прикрепить арендатора»; ошибки — красная рамка + подпись
 * «Выберете значение»; сбой сети — окно с глобусом.
 */
@Composable
fun AttachTenantScreen(
    propertyId: String,
    onDismiss: () -> Unit,
    onAttached: () -> Unit,
    // Пресет периода из шахматки «Моя недвижимость» (аннотация дизайнера:
    // выбранная в шахматке дата автоматически подставляется здесь)
    presetStart: LocalDate? = null,
    presetEnd: LocalDate? = null,
    viewModel: AttachTenantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val property = uiState.property
    val context = LocalContext.current
    LaunchedEffect(propertyId) { viewModel.load(propertyId) }

    var startDate by remember { mutableStateOf<LocalDate?>(presetStart) }
    var endDate by remember { mutableStateOf<LocalDate?>(presetEnd) }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Черновик прошлого незавершённого ввода: на входе предлагаем продолжить
    // (Figma 2935:40049); после успешного прикрепления черновик не нужен.
    // Сохраняем при каждом изменении полей — переживает даже убийство
    // процесса прямо на экране, не только выход из него
    var attached by remember { mutableStateOf(false) }
    fun saveDraft() {
        if (attached) return
        AttachTenantDraftHolder.save(
            context, propertyId,
            AttachTenantDraftHolder.Draft(startDate, endDate, fullName, phone)
        )
    }
    val storedDraft = remember(propertyId) { AttachTenantDraftHolder.get(context, propertyId) }
    var showResumeDialog by remember { mutableStateOf(storedDraft != null) }
    // Контакт выбран из книги — предложение «добавить в контакты» не нужно
    var pickedFromBook by remember { mutableStateOf(false) }
    DisposableEffect(propertyId) {
        onDispose {
            if (attached) AttachTenantDraftHolder.clear(context, propertyId) else saveDraft()
        }
    }

    // Ошибки валидации (Figma Error-экраны): рамка + «Выберете значение»
    var startError by remember { mutableStateOf(false) }
    var endError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    // null | «Обязательное поле» | «Проверьте правильность заполнения»
    var phoneError by remember { mutableStateOf<String?>(null) }

    var showStartSheet by remember { mutableStateOf(false) }
    var showEndSheet by remember { mutableStateOf(false) }
    var showContactsSheet by remember { mutableStateOf(false) }

    fun tryAttach() {
        startError = startDate == null
        endError = endDate == null
        nameError = fullName.isBlank()
        // Полное значение — ровно 10 цифр; иностранный/неполный — ошибка
        phoneError = when {
            phone.isBlank() -> "Обязательное поле"
            phone.any { !it.isDigit() } || phone.length != 10 -> "Проверьте правильность заполнения"
            else -> null
        }
        if (startError || endError || nameError || phoneError != null) return
        val end = endDate ?: return
        val start = startDate ?: return
        // Период включает последний выбранный день (вопрос дизайнера решён так)
        viewModel.attach(propertyId, fullName.trim(), phone, start, end) {
            attached = true
            // Ручной ввод: предложить добавить контакт в телефонную книгу —
            // диалог покажет карточка объекта после возврата
            if (!pickedFromBook) {
                ContactSuggestionHolder.pending =
                    PhoneContact(fullName.trim(), formatRuPhone(phone))
            }
            onAttached()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            ScreenToolbar(title = "Добавить арендатора", onBack = onDismiss)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // ---- Карточка объекта (фото 40 + название + короткий адрес) ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .height(64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = property?.photos?.firstOrNull()?.url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEFEFEF)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Column {
                        Text(
                            property?.name?.ifBlank { "Без названия" } ?: "",
                            style = Headline2MobStyle
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            property?.address?.shortAddress().orEmpty(),
                            style = CardSubtitleStyle
                        )
                    }
                }

                // ---- Период аренды ----
                Text("Период аренды", style = SectionTitleStyle)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateValueField(
                        caption = "Начало",
                        value = startDate?.format(DdMmYyyy),
                        error = startError,
                        modifier = Modifier.weight(1f),
                        onClick = { showStartSheet = true }
                    )
                    DateValueField(
                        caption = "Окончание",
                        value = endDate?.format(DdMmYyyy),
                        error = endError,
                        modifier = Modifier.weight(1f),
                        onClick = { showEndSheet = true }
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ---- Арендатор ----
                Text("Арендатор", style = SectionTitleStyle)
                Spacer(Modifier.height(12.dp))
                OutlineCtaButton(
                    text = "Выбрать из контактов",
                    iconRes = R.drawable.ic_user_outline,
                    // Figma «10» 2935-37006: иконка и текст через 6 (не дефолтные 10)
                    iconSpacing = 6.dp,
                    borderColor = Graphite,
                    onClick = { showContactsSheet = true }
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "или ввести вручную",
                    style = CardSubtitleStyle,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                TextValueField(
                    caption = "ФИО",
                    value = fullName,
                    error = nameError,
                    onValueChange = {
                        fullName = it
                        nameError = false
                        saveDraft()
                    }
                )
                Spacer(Modifier.height(12.dp))
                PhoneValueField(
                    value = phone,
                    error = phoneError,
                    onValueChange = {
                        phone = it
                        phoneError = null
                        saveDraft()
                    }
                )

                Spacer(Modifier.height(20.dp))
                BlackCtaButton(
                    text = if (uiState.isAttaching) "Прикрепление…" else "Прикрепить арендатора",
                    onClick = { tryAttach() }
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // ---- Календарные шиты (структура = DatePickerSheet, 2935:36390/36573) ----
    if (showStartSheet) {
        DatePickerSheet(
            title = "Начало аренды",
            initialDate = startDate ?: LocalDate.now(),
            onDone = {
                startDate = it
                startError = false
                // Начало сдвинулось за окончание — окончание сбрасываем
                if (endDate != null && it.isAfter(endDate)) {
                    endDate = null
                    endError = false
                }
                showStartSheet = false
                saveDraft()
            },
            onDismiss = { showStartSheet = false }
        )
    }
    if (showEndSheet) {
        DatePickerSheet(
            title = "Окончание аренды",
            initialDate = endDate ?: startDate ?: LocalDate.now(),
            minDate = startDate,
            onDone = {
                endDate = it
                endError = false
                showEndSheet = false
                saveDraft()
            },
            onDismiss = { showEndSheet = false }
        )
    }
    if (showContactsSheet) {
        ContactsPickerSheet(
            onPick = {
                fullName = it.name
                phone = contactPhoneValue(it.phone)
                nameError = false
                phoneError = null
                pickedFromBook = true
                showContactsSheet = false
                saveDraft()
            },
            onDismiss = { showContactsSheet = false }
        )
    }

    // Сбой сети — окно с глобусом (Figma 2935:40038)
    if (uiState.failed) {
        IconNotificationDialog(
            iconRes = R.drawable.ic_globe_warning_vec,
            text = "Не\u00A0удалось добавить арендатора. \nПовторите еще\u00A0раз",
            onDismiss = { viewModel.clearFailure() }
        )
    }

    // Незавершённый ввод при прошлом визите — «Продолжить»/«Начать заново»
    // (Figma 2935:40049): карточка 380 r20 pad20, заголовок 20/600 + 6 +
    // тело 15/600 серым, +20 → чёрная CTA + 6 + контурная
    if (showResumeDialog && storedDraft != null) {
        DesignWidthDialog(onDismissRequest = { showResumeDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Добавить арендатора", style = ToolbarTitleStyle)
                    Text(
                        "Вы начали добавлять арендатора. Хотите продолжить?",
                        style = Headline2MobStyle.copy(color = GreyText)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BlackCtaButton(text = "Продолжить") {
                        startDate = storedDraft.startDate
                        endDate = storedDraft.endDate
                        fullName = storedDraft.fullName
                        // Старые черновики могли хранить маску «+7-…» — приводим к цифрам
                        phone = contactPhoneValue(storedDraft.phone)
                        showResumeDialog = false
                    }
                    OutlineCtaButton(
                        text = "Начать заново",
                        borderColor = Graphite,
                        onClick = {
                            AttachTenantDraftHolder.clear(context, propertyId)
                            showResumeDialog = false
                        }
                    )
                }
            }
        }
    }
}

/** Поле-карточка даты (серое 64dp r20): подпись, значение, календарь 24
 *  в зоне 40; ошибка — красная рамка + подпись под полем (Figma Error). */
@Composable
private fun DateValueField(
    caption: String,
    value: String?,
    error: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFEFEFEF))
                .then(
                    if (error) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp))
                    else Modifier
                )
                .clickable { onClick() }
                .padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (value != null || error) {
                // Заполнено (подпись+значение) или ошибка пустого поля: красная
                // подпись 13/400 сверху (Figma 2935:39168), НЕ жирный плейсхолдер
                Column(Modifier.weight(1f)) {
                    Text(
                        caption,
                        style = CardSubtitleStyle.copy(color = if (error) ErrorRed else GreyText)
                    )
                    if (value != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(value, style = Headline2MobStyle)
                    }
                }
            } else {
                // Пустое поле: жирный плейсхолдер слева на 20 (Default-макет)
                Text(
                    caption,
                    style = Headline2MobStyle.copy(color = GreyText),
                    modifier = Modifier.weight(1f)
                )
            }
            Box(
                modifier = Modifier.size(40.dp).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_calendar_payment),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (error) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Выберете значение",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = InterFontFamily,
                color = ErrorRed,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}

/** Поле ввода (серое 64dp r20): подпись сверху, ввод снизу (Figma Filled). */
@Composable
private fun TextValueField(
    caption: String,
    value: String,
    error: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFEFEFEF))
                .then(
                    if (error) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp))
                    else Modifier
                )
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    // Enter = «Готово»: скрываем клавиатуру, фокус и мигающий
                    // курсор остаются в поле (единое поведение полей приложения)
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    // Курсор исчезает: поле отдаёт фокус (стандартное «Готово»)
                    focusManager.clearFocus()
                }),
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = Graphite
                ),
                cursorBrush = SolidColor(Graphite),
                decorationBox = { inner ->
                    if (value.isNotEmpty() || error) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                caption,
                                style = CardSubtitleStyle.copy(color = if (error) ErrorRed else GreyText)
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.fillMaxWidth()) { inner() }
                        }
                    } else {
                        // Пустое поле: жирный плейсхолдер слева (Default-макет)
                        Box(Modifier.fillMaxWidth()) {
                            Text(caption, style = Headline2MobStyle.copy(color = GreyText))
                            Box(Modifier.fillMaxWidth()) { inner() }
                        }
                    }
                }
            )
        }
        if (error) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Обязательное поле",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = InterFontFamily,
                color = ErrorRed,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}

/**
 * Поле «Телефон» с российской маской +7-900-000-00-08 (Figma 2935:39179):
 * как бы номер ни был записан (+7/8/7), показываем всегда в этом формате.
 * Две ошибки: «Обязательное поле» и «Проверьте правильность заполнения».
 */
@Composable
private fun PhoneValueField(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFEFEFEF))
                .then(
                    if (error != null) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp))
                    else Modifier
                )
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusManager = LocalFocusManager.current
            // Ввод начали с +7/7/8 — префикс маски уже набран: «+7» так и
            // записываем, «8» перерисовываем в «+7»; пустой ввод сбрасывает
            var prefixTyped by remember { mutableStateOf(false) }
            BasicTextField(
                value = value,
                // Значение — только цифры номера (без префикса), как в поле
                // телефона при регистрации: маска — VisualTransformation,
                // значение не переписывается, курсор и тапы ведут себя обычно
                onValueChange = { raw ->
                    val s = raw.trim()
                    prefixTyped = when {
                        s.isEmpty() -> false
                        s.startsWith("+") || s.firstOrNull() == '7' || s.firstOrNull() == '8' -> true
                        else -> prefixTyped
                    }
                    onValueChange(normalizePhoneInput(s))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    // Курсор исчезает: поле отдаёт фокус (стандартное «Готово»)
                    focusManager.clearFocus()
                }),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (value.isNotEmpty() && value.any { !it.isDigit() }) {
                    // Иностранный номер из книги — без русской маски, как записан
                    VisualTransformation.None
                } else {
                    RuPhoneVisualTransformation(prefixWhenEmpty = prefixTyped)
                },
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = Graphite
                ),
                cursorBrush = SolidColor(Graphite),
                decorationBox = { inner ->
                    // prefixTyped: «+7»/«8» уже набраны — цифр ещё нет, но
                    // поле больше не пустое: плейсхолдер уходит, виден префикс
                    if (value.isNotEmpty() || prefixTyped || error != null) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                "Телефон",
                                style = CardSubtitleStyle.copy(color = if (error != null) ErrorRed else GreyText)
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.fillMaxWidth()) { inner() }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth()) {
                            Text("Телефон", style = Headline2MobStyle.copy(color = GreyText))
                            Box(Modifier.fillMaxWidth()) { inner() }
                        }
                    }
                }
            )
        }
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                error,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = InterFontFamily,
                color = ErrorRed,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}

/** Маска +7-900-000-00-08 из любого ввода (+7 / 8 / 7 / без префикса). */
private fun formatRuPhone(input: String): String {
    var digits = input.filter { it.isDigit() }
    if (digits.startsWith("8")) digits = "7" + digits.drop(1)
    if (digits.isEmpty()) return ""
    if (digits.first() != '7') digits = "7$digits"
    digits = digits.take(11)
    val body = digits.drop(1)
    return buildString {
        append("+7")
        if (body.isNotEmpty()) append("-").append(body.take(3))
        if (body.length > 3) append("-").append(body.substring(3, minOf(6, body.length)))
        if (body.length > 6) append("-").append(body.substring(6, minOf(8, body.length)))
        if (body.length > 8) append("-").append(body.substring(8))
    }
}

/**
 * Российская маска +7-900-000-00-08 как VisualTransformation: значение поля —
 * только 10 цифр без префикса (механика поля телефона при регистрации),
 * форматирование — только отображение, курсор живёт в координатах цифр.
 * prefixWhenEmpty — пользователь уже набрал «+7»/«8»: при пустом номере
 * показываем префикс «+7» (курсор после него), а не плейсхолдер.
 */
private class RuPhoneVisualTransformation(
    private val prefixWhenEmpty: Boolean = false
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = when {
            digits.isNotEmpty() -> formatRuPhone(digits)
            prefixWhenEmpty -> "+7"
            else -> ""
        }
        // Позиции цифр значения внутри отформатированной строки
        // (префикс «+7» не считаем — это не часть значения)
        val digitPositions = formatted.drop(2).mapIndexedNotNull { i, c ->
            if (c.isDigit()) i + 2 else null
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                // Пустой номер с набранным префиксом — курсор после «+7»
                digits.isEmpty() -> formatted.length
                offset <= 0 -> digitPositions.firstOrNull() ?: formatted.length
                else -> digitPositions.getOrNull(offset - 1)?.plus(1) ?: formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                var count = 0
                for (i in 0 until offset.coerceAtMost(formatted.length)) {
                    if (i >= 2 && formatted[i].isDigit()) count++
                }
                return count.coerceAtMost(digits.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

/**
 * Значение поля «Телефон» из номера контакта: русские (+7/8) → 10 цифр
 * без семёрки (под маску), иностранные → как записан (без маски).
 */
private fun contactPhoneValue(raw: String): String {
    val digits = raw.filter(Char::isDigit)
    return if (digits.startsWith("7") || digits.startsWith("8")) digits.drop(1).take(10)
    else raw.trim()
}

/**
 * Ручной ввод телефона: 10 цифр — это сам номер, БЕЗ учёта ведущей 8 или +7.
 * Если ввод начали с 8/7 — это префикс, убираем сразу, чтобы маска
 * не ломалась на промежуточном вводе (8905… → 905…).
 */
private fun normalizePhoneInput(raw: String): String {
    var d = raw.filter(Char::isDigit)
    if (d.firstOrNull() == '8' || d.firstOrNull() == '7') d = d.drop(1)
    return d.take(10)
}

/**
 * Телефон контакта из книги: русская маска применяется ТОЛЬКО к номерам,
 * начинающимся с +7 или 8; остальные (например +49…) показываются
 * как записаны — семёрку никому не дописываем.
 */
private fun formatContactPhone(raw: String): String {
    val digits = raw.filter(Char::isDigit)
    return if (digits.startsWith("7") || digits.startsWith("8")) formatRuPhone(raw)
    else raw.trim()
}

/**
 * Шит «Выбрать контакт» (Figma 2935:36528): заголовок + крестик,
 * поиск «Имя или телефон», список секциями по первой букве (аватар 40,
 * имя + телефон, разделители #DBDBDB).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsPickerSheet(
    onPick: (PhoneContact) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
    var permissionDenied by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && contacts.isEmpty()) {
            contacts = readPhoneContacts(context)
        } else if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    // Ранжированный поиск: СНАЧАЛА все имена, начинающиеся с запроса
    // (внутри блока — алфавит), затем совпадения по фамилии (любое слово
    // имени), затем телефон по цифрам. Подстрока из середины слова
    // («мЭри» по «Э») не матчится никогда.
    val filtered = remember(query, contacts) {
        val q = query.trim()
        if (q.isEmpty()) {
            contacts
        } else {
            val digits = q.filter(Char::isDigit)
            val byName = mutableListOf<PhoneContact>()
            val byWord = mutableListOf<PhoneContact>()
            val byPhone = mutableListOf<PhoneContact>()
            contacts.forEach { c ->
                val n = c.name.trim()
                when {
                    n.startsWith(q, ignoreCase = true) -> byName += c
                    n.split(' ', ' ').any { it.isNotBlank() && it.startsWith(q, ignoreCase = true) } -> byWord += c
                    digits.isNotEmpty() && c.phone.filter(Char::isDigit).contains(digits) -> byPhone += c
                }
            }
            byName + byWord + byPhone
        }
    }
    val grouped = remember(filtered) {
        filtered.groupBy { it.name.trim().firstOrNull()?.uppercase() ?: "#" }
            .toSortedMap()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        // Шит фиксированного размера 632 (Figma «10», 2935-36528):
        // ручка 36 (16+4+16) → заголовок 24 → 20 → поиск 55 → 20 → список
        // (буква 18 → 6 → контакт 64 с разделителем, между контактами 12)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(632.dp)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            com.rentmanager.app.ui.components.SheetDragHandle()
            Box(modifier = Modifier.fillMaxWidth()) {
                Text("Выбрать контакт", style = Headline2MobStyle.copy(fontSize = 20.sp))
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_close_graphite),
                    contentDescription = "Закрыть",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clickable { onDismiss() }
                )
            }
            Spacer(Modifier.height(20.dp))

            // Поиск (Figma «10», 2935-36538): лупа 20 на x+20, текст с x+46;
            // скругление 20 (не пилюля)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEFEFEF)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(20.dp))
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_search_lens),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = InterFontFamily,
                        color = Graphite
                    ),
                    cursorBrush = SolidColor(Graphite),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    "Имя или телефон",
                                    style = Headline2MobStyle.copy(color = GreyText)
                                )
                            }
                            inner()
                        }
                    }
                )
                Spacer(Modifier.width(20.dp))
            }
            Spacer(Modifier.height(20.dp))

            when {
                permissionDenied -> Text(
                    "Нет доступа к контактам",
                    style = CardSubtitleStyle,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                contacts.isEmpty() -> Text(
                    "Контакты не найдены",
                    style = CardSubtitleStyle,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                else -> LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    if (query.isBlank()) {
                        // Полный список — секциями по буквам
                        grouped.forEach { (letter, group) ->
                            item(key = "letter_$letter") {
                                Text(
                                    letter.toString(),
                                    style = Headline2MobStyle.copy(color = GreyText)
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                            items(group, key = { it.name + it.phone }) { contact ->
                                ContactRow(contact, onPick)
                            }
                        }
                    } else {
                        // Результаты поиска — ранжированный список без
                        // буквенных заголовков: сначала имена на запрос,
                        // ниже — фамильные совпадения, ещё ниже — телефоны
                        items(filtered, key = { it.name + it.phone }) { contact ->
                            ContactRow(contact, onPick)
                        }
                    }
                }
            }
        }
    }
}

/** Контакт шита (Figma «10», 2935-36541): аватар 40 с силуэтом, имя + телефон
 *  (зазор 4), разделитель внизу; высота строки 64, до следующего — 12. */
@Composable
private fun ContactRow(contact: PhoneContact, onPick: (PhoneContact) -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clickable { onPick(contact) }
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEFEFEF)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_avatar_placeholder),
                    contentDescription = null,
                    modifier = Modifier.size(width = 28.dp, height = 26.dp)
                )
            }
            Column {
                Text(contact.name, style = Headline2MobStyle)
                Spacer(Modifier.height(4.dp))
                Text(
                    formatContactPhone(contact.phone),
                    style = CardSubtitleStyle
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFDBDBDB))
        )
        Spacer(Modifier.height(12.dp))
    }
}

/** Чтение контактов телефонной книги (имя + первый телефон). */
private fun readPhoneContacts(context: Context): List<PhoneContact> = try {
    val list = mutableListOf<PhoneContact>()
    context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null,
        null,
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )?.use { cursor ->
        while (cursor.moveToNext()) {
            val name = cursor.getString(0) ?: continue
            val phone = cursor.getString(1) ?: ""
            if (name.isNotBlank() && phone.isNotBlank()) {
                list.add(PhoneContact(name, phone))
            }
        }
    }
    list.distinctBy { it.name to it.phone }
} catch (_: Exception) {
    emptyList()
}
