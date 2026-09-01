package com.rentmanager.app.ui.landlord.propertycard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.toMutableStateList
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rentmanager.app.R
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.ErrorRed
import com.rentmanager.app.ui.theme.FieldTextStyle
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobPlaceholderStyle
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.ToolbarTitleStyle

// #79747E — drag handle шита (как в PropertyActionsSheet)
private val SheetHandleGrey = Color(0xFF79747E)
// #CAC4D0 — разделитель списков в шите
private val DividerGrey = Color(0xFFCAC4D0)
// #EFEFEF — заливка плитки «Добавить фото» и кружка удаления (Grey/Icon)
private val GreyIcon = Color(0xFFEFEFEF)
// 40% от Graphite #212121 — разделитель в шите фотографий
private val TrackGrey = Color(0x66212121)

// Опции дропдаунов шита «Об объекте» (те же значения, что на экране создания)
private val RoomsSheetOptions = listOf("Студия", "1", "2", "3", "4", "5", "6+ комнат")
private val SleepingSheetOptions = listOf("1", "2", "3", "4", "5", "6+")
private val FloorSheetOptions = listOf("Подвал", "Цоколь", "-2", "-1") + (1..100).map { it.toString() }
private val FloorsInHouseSheetOptions = (1..100).map { it.toString() }

// 55.0 → «55», 55.5 → «55.5» — предзаполнение числовых полей шитов
private fun Double?.toFieldText(): String =
    if (this == null) "" else if (this == toLong().toDouble()) toLong().toString() else toString()

// Группировка разрядов пробелом: «25000» → «25 000» (макеты 2700-23453/23258).
// Оставляем только цифры и один десятичный разделитель; пробелы игнорируются при вводе
fun formatAmount(raw: String): String {
    var seenDot = false
    val cleaned = buildString {
        for (c in raw) {
            when {
                c.isDigit() -> append(c)
                (c == '.' || c == ',') && !seenDot && isNotEmpty() -> {
                    append('.')
                    seenDot = true
                }
            }
        }
    }
    if (cleaned.isEmpty()) return ""
    val intPart = cleaned.substringBefore('.')
    val frac = if ('.' in cleaned) cleaned.substringAfter('.') else ""
    val grouped = StringBuilder()
    val n = intPart.length
    intPart.forEachIndexed { i, c ->
        if (i > 0 && (n - i) % 3 == 0) grouped.append(' ')
        grouped.append(c)
    }
    return grouped.toString() + if (frac.isNotEmpty()) ".$frac" else ""
}

// Договор в одном поле: «№45 от 14.02.2025» (собирается из реальных данных;
// используется и в шите, и в секции карточки «Арендатор и договор»)
fun contractDisplayText(number: String?, date: String?): String {
    val num = number?.takeIf { it.isNotBlank() }
        ?.let { if (it.startsWith("№")) it else "№$it" } ?: ""
    val d = date?.takeIf { it.isNotBlank() } ?: ""
    return when {
        num.isBlank() && d.isBlank() -> ""
        num.isBlank() -> d
        d.isBlank() -> num
        else -> "$num от $d"
    }
}

// ---------- Шиты редактирования секций карточки ----------

// Шит «Аренда и платежи» (Figma 2677-26495): арендная плата + дата окончания аренды.
@Composable
fun RentEditSheet(
    property: PropertyDto,
    isSaving: Boolean,
    onSave: (rentAmount: String, rentEndDate: String) -> Unit,
    onDismiss: () -> Unit
) {
    var rentAmount by remember(property.id) { mutableStateOf(formatAmount(property.rentAmount.toFieldText())) }
    var rentEndDate by remember(property.id) { mutableStateOf(property.rentEndDate.orEmpty()) }
    var dateErrorHint by remember(property.id) { mutableStateOf<String?>(null) }
    EditSheetScaffold(title = "Аренда и платежи", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SheetCaptionField(
                caption = "Арендная плата",
                value = rentAmount,
                onValueChange = { rentAmount = formatAmount(it) },
                keyboardType = KeyboardType.Decimal,
                // Суффикс по типу аренды (Figma 2700-23463: «25 000 ₽ / сутки»)
                suffix = " ₽ / " + if (property.rentType == "длительно") "месяц" else "сутки"
            )
            // «Арендовано»: подпись сверху, снизу «до» и ввод даты прямо в поле (дд.мм.гггг)
            RentedUntilField(
                date = rentEndDate,
                onDateChange = {
                    rentEndDate = it
                    if (dateErrorHint != null) dateErrorHint = validateRentDate(it)
                },
                errorHint = dateErrorHint
            )
        }
        // 12dp от spacedBy + 8dp паддинга = 20dp до кнопки (Figma: Content itemSpacing 20)
        BlackCtaButton(
            modifier = Modifier.padding(top = 8.dp),
            text = "Сохранить изменения",
            enabled = !isSaving,
            onClick = {
                // Дата окончания аренды не может быть в прошлом (сегодня — можно)
                val hint = validateRentDate(rentEndDate)
                dateErrorHint = hint
                if (hint == null) onSave(rentAmount, rentEndDate)
            }
        )
    }
}

// Проверка даты окончания аренды: null = всё хорошо, иначе текст подсказки
private fun validateRentDate(date: String): String? {
    if (date.isBlank()) return null
    if (date.length != 10) return "Введите дату в формате дд.мм.гггг"
    return try {
        val parsed = java.time.LocalDate.of(
            date.substring(6).toInt(),
            date.substring(3, 5).toInt(),
            date.substring(0, 2).toInt()
        )
        if (parsed.isBefore(java.time.LocalDate.now())) "Дата не может быть раньше сегодняшней" else null
    } catch (_: Exception) {
        "Введите корректную дату"
    }
}

// Поле «Арендовано»: подпись «Арендовано» сверху тонко, снизу постоянное «до»
// и ввод даты dd.MM.yyyy сразу в поле (точки вставляются автоматически).
// TextFieldValue с курсором в конце — иначе после вставки точки курсор прыгал назад
@Composable
private fun RentedUntilField(
    date: String,
    onDateChange: (String) -> Unit,
    errorHint: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBackground)
                .then(
                    if (errorHint != null) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp))
                    else Modifier
                )
                .padding(start = 20.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var dateValue by remember(date) {
                mutableStateOf(TextFieldValue(date, TextRange(date.length)))
            }
            BasicTextField(
                value = dateValue,
                onValueChange = { incoming ->
                    val masked = formatDateMask(incoming.text)
                    // Курсор всегда в конце отформатированной даты
                    dateValue = TextFieldValue(masked, TextRange(masked.length))
                    if (masked != date) onDateChange(masked)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = Headline2MobStyle,
                cursorBrush = SolidColor(Graphite),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                decorationBox = { innerTextField ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Арендовано", style = CardSubtitleStyle)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // «до» — постоянный префикс; дата вводится после него
                            Text("до", style = Headline2MobStyle)
                            Box {
                                if (date.isEmpty()) {
                                    Text("дд.мм.гггг", style = Headline2MobStyle.copy(color = GreyText, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal))
                                }
                                innerTextField()
                            }
                        }
                    }
                }
            )
        }
        if (errorHint != null) {
            Text(
                errorHint,
                fontSize = 9.5.sp,
                lineHeight = 11.5.sp,
                letterSpacing = (-0.2).sp,
                color = ErrorRed,
                modifier = Modifier.padding(start = 20.dp)
            )
        }
    }
}

// Маска даты: пропускаем только цифры, точки после дд и мм, максимум 10 символов
private fun formatDateMask(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(8)
    return buildString {
        digits.forEachIndexed { i, c ->
            when (i) {
                2, 4 -> append('.').append(c)
                else -> append(c)
            }
        }
    }
}

// Шит «Арендатор и договор» (Figma 2677-26531): ровно три поля —
// арендатор, договор («№… от …»), номер телефона арендатора
@Composable
fun TenantContractEditSheet(
    property: PropertyDto,
    isSaving: Boolean,
    onSave: (tenantInfo: String, contractText: String, phone: String) -> Unit,
    onDismiss: () -> Unit
) {
    var tenantInfo by remember(property.id) { mutableStateOf(property.tenantInfo.orEmpty()) }
    var contractText by remember(property.id) {
        mutableStateOf(contractDisplayText(property.contractNumber, property.contractDate))
    }
    var phone by remember(property.id) { mutableStateOf(property.phone.orEmpty()) }
    EditSheetScaffold(title = "Арендатор и договор", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SheetCaptionField(
                caption = "Арендатор",
                value = tenantInfo,
                onValueChange = { tenantInfo = it }
            )
            SheetCaptionField(
                caption = "Договор",
                value = contractText,
                onValueChange = { contractText = it }
            )
            SheetIconField(
                iconRes = R.drawable.ic_call_phone,
                caption = "Номер телефона арендатора",
                value = phone,
                onValueChange = { phone = it },
                placeholder = "+7"
            )
        }
        // 12dp от spacedBy + 8dp паддинга = 20dp до кнопки (Figma: Content itemSpacing 20)
        BlackCtaButton(
            modifier = Modifier.padding(top = 8.dp),
            text = "Сохранить изменения",
            enabled = !isSaving,
            onClick = { onSave(tenantInfo, contractText, phone) }
        )
    }
}

// Шит «Информация об объекте» удалён: редактирование стало инлайн-режимом
// раскрытого аккордеона (Figma 2677-26576) — см. ObjectInfoEditContent в PropertyCardScreen

// Шит «Об объекте» удалён: редактирование вынесено на отдельный экран
// AboutPropertyEditScreen (Figma 2726-33827)

// Шит «Счетчики» (Figma 2678-27396): заголовок «Редактировать счетчики»,
// строки типов счётчиков (номера в макете — примеры, не переносятся),
// без кнопки сохранения
@Composable
fun MetersSheet(onDismiss: () -> Unit) {
    val meters = listOf("Электроэнергия", "Холодная вода", "Горячая вода", "Отопление")
    EditSheetScaffold(title = "Редактировать счетчики", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            meters.forEach { meter ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .padding(start = 20.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        text = meter,
                        style = Headline2MobStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_card_chevron),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

// Шит редактирования фотографий (Figma 2523-27049): сетка 2 в ряд,
// на каждой фотографии крестик удаления, последняя плитка «Добавить фото»,
// ниже разделитель и кнопка «Сохранить изменения».
// Крестик удаляет фото сразу: из списка и из БД/S3 (onDeletePhoto)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditSheet(
    initialPhotoUris: List<String>,
    isSaving: Boolean,
    onSave: (photoUris: List<String>) -> Unit,
    onDeletePhoto: (uri: String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val photoUris = remember(initialPhotoUris) { initialPhotoUris.toMutableStateList() }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            photoUris.addAll(0, uris.map { it.toString() })
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Ручка 32×4 по центру: 16dp сверху, 16dp до контента (Figma: Header padding 16)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(TrackGrey)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 36.dp)
            ) {
                // Заголовок «Фотографии» в контенте шита (Figma 2523-27049): слева, над сеткой
                Text("Фотографии", style = ToolbarTitleStyle)
                Spacer(Modifier.height(12.dp))
            // null — маркер плитки «Добавить фото» (всегда последняя)
            val rows = (photoUris.toList() + listOf<String?>(null)).chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { uri ->
                            if (uri == null) {
                                AddPhotoSheetTile(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                PhotoSheetTile(
                                    uri = uri,
                                    onRemove = {
                                        photoUris.remove(uri)
                                        onDeletePhoto(uri)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = TrackGrey, thickness = 1.dp)
            Spacer(Modifier.height(20.dp))
            BlackCtaButton(
                text = "Сохранить изменения",
                enabled = !isSaving,
                onClick = { onSave(photoUris.toList()) }
            )
            }
        }
    }
}

// Плитка загруженного фото с крестиком удаления (Figma 2700-23638: без рамки,
// кружок #EFEFEF 40×40 с крестиком, отступы 6 сверху / 11 справа)
@Composable
private fun PhotoSheetTile(
    uri: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(183f / 130f)
            .clip(RoundedCornerShape(30.dp))
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 11.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(GreyIcon)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_toolbar_close),
                contentDescription = "Удалить фото",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Плитка «Добавить фото» (серая, r30, камера + подпись — как на экране создания)
@Composable
private fun AddPhotoSheetTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(183f / 130f)
            .clip(RoundedCornerShape(30.dp))
            .background(GreyIcon)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_add_photo_camera),
            contentDescription = null,
            modifier = Modifier.size(50.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("Добавить фото", style = Headline2MobStyle.copy(color = GreyText))
    }
}

// ---------- внутренние компоненты шитов ----------

// Каркас шита редактирования секции:
// ручка 32×4 рисуется вручную (16dp сверху, 16dp до заголовка), контент 20/20/36
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSheetScaffold(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Ручка 32×4 по центру: 16dp сверху, 16dp до заголовка (Figma: Header padding 16)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(TrackGrey)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 36.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, style = ToolbarTitleStyle)
                content()
            }
        }
    }
}

// Безрамочное поле с серой подписью (названием) сверху и значением снизу —
// как в макетах шитов (Figma 2677-26495/26531/26195).
// suffix — неизменяемый хвост значения (например «₽ / сутки»); подпись намеренно
// вне BasicTextField: intrinsic-ширина на самом поле ломает вертикальную раскладку
@Composable
private fun SheetCaptionField(
    caption: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    suffix: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .then(if (isError) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp)) else Modifier)
            .padding(start = 20.dp, end = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(caption, style = CardSubtitleStyle)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (suffix == null) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = Headline2MobStyle,
                        cursorBrush = SolidColor(Graphite),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next)
                    )
                } else {
                    // Box сжимается до ширины введённых цифр (min 2dp под курсор),
                    // суффикс идёт сразу за ними — «25 000 ₽ / сутки» одной строкой
                    Box(Modifier.widthIn(min = 2.dp).width(IntrinsicSize.Min)) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = Headline2MobStyle,
                            cursorBrush = SolidColor(Graphite),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next)
                        )
                    }
                    Text(suffix, style = Headline2MobStyle)
                }
            }
        }
    }
}

// Безрамочное поле с иконкой и подписью (телефон) — как LabeledField на экране создания
@Composable
private fun SheetIconField(
    iconRes: Int,
    caption: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            decorationBox = { innerTextField ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(caption, style = CardSubtitleStyle)
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, style = Headline2MobPlaceholderStyle)
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

// Раскрывающаяся карточка «Описание объявления» (Figma 2700-23258): как дропдаун —
// тёмный заголовок, серая подпись, шеврон; раскрытый ввод — как на экране создания
// (Figma 2533-18130: подпись становится плейсхолдером и исчезает при вводе)
@Composable
private fun SheetDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    subtitle: String
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
    ) {
        // Шеврон плавает справа по центру; текст получает всю ширину карточки —
        // субтитр помещается одной строкой даже на узких экранах (макет 2700-23258)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable { expanded = !expanded }
        ) {
            Image(
                painter = painterResource(R.drawable.ic_card_chevron),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(40.dp)
                    .graphicsLayer { rotationZ = if (expanded) 180f else 0f }
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp, end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Описание объявления", style = Headline2MobStyle)
                // В раскрытом состоянии подпись переезжает в поле ввода плейсхолдером
                if (!expanded) {
                    Text(
                        subtitle,
                        style = CardSubtitleStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (expanded) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                    .heightIn(min = 70.dp),
                textStyle = CardSubtitleStyle.copy(color = Graphite),
                cursorBrush = SolidColor(Graphite),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                subtitle,
                                style = CardSubtitleStyle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

// Безрамочный дропдаун с серой подписью и шевроном (как в макете «Об объекте»)
@Composable
private fun SheetCaptionDropdown(
    caption: String,
    selected: String?,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBackground)
                .clickable { expanded = true }
                .padding(start = 20.dp, end = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Подпись всегда одна строка: в половинных карточках («Спальные места»,
                    // «Этажей в доме») места ровно на одну, перенос недопустим (макет 2700-23258)
                    Text(caption, style = CardSubtitleStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = selected ?: "",
                        style = if (selected == null) Headline2MobPlaceholderStyle else Headline2MobStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Image(
                    painter = painterResource(R.drawable.ic_card_chevron),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 304.dp),
            containerColor = Color.White
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = FieldTextStyle) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}