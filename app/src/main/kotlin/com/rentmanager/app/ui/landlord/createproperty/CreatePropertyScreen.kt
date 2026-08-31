package com.rentmanager.app.ui.landlord.createproperty

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.CardSubtitleStyle
import com.rentmanager.app.ui.theme.ErrorRed
import com.rentmanager.app.ui.theme.FieldTextStyle
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobPlaceholderStyle
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.ToolbarTitleStyle
import com.rentmanager.app.ui.components.DesignWidthDialog

// Опции дропдаунов и чипов (локально, в бэкенд уходят как строки)
private val SleepingOptions = listOf("1", "2", "3", "4", "5", "6+")
private val FloorOptions = listOf("Подвал", "Цоколь", "-2", "-1") + (1..100).map { it.toString() }
private val FloorsInHouseOptions = (1..100).map { it.toString() }

// Шаг 4: карточка объекта (Figma 2533:18104, пример для «Квартира + посуточно»).
// Адрес уже выбран на шаге 3 — здесь он только отображается (без карты).
@Composable
fun CreatePropertyScreen(
    propertyId: String? = null,
    editPropertyId: String? = null,
    propertyType: String = "Квартира",
    rentType: String = "посуточно",
    initialAddress: String = "",
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    onBack: () -> Unit,
    onCreated: (String) -> Unit = {},
    onPaymentSchedule: (String) -> Unit = {},
    onAddCounter: () -> Unit = {},
    viewModel: CreatePropertyViewModel = hiltViewModel()
) {
    val isEditMode = editPropertyId != null
    val uiState by viewModel.uiState.collectAsState()
    val editProperty = uiState.editProperty

    if (isEditMode) {
        // Загружаем редактируемый объект: мгновенно из кэша, затем с сервера
        LaunchedEffect(editPropertyId) { viewModel.loadForEdit(editPropertyId!!) }
        // Правки объекта не должны портить черновик флоу создания:
        // сохраняем его снимок на входе и восстанавливаем при выходе с экрана
        val draftSnapshot = remember { CreateDraftHolder.snapshot() }
        DisposableEffect(Unit) {
            onDispose {
                CreateDraftHolder.restore(draftSnapshot)
                // Вышли с редактирования — фиксируем черновик в DataStore
                CreateDraftHolder.persist()
            }
        }
    } else {
        // Выход из шага 4 создания (назад/крестик) — фиксируем черновик,
        // чтобы он пережил перезапуск приложения
        DisposableEffect(Unit) {
            onDispose { CreateDraftHolder.persist() }
        }
    }

    // Пока объект для редактирования не загружен — показываем лоадер
    if (isEditMode && editProperty == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Graphite)
        }
        return
    }

    // В режиме редактирования поля предзаполняются данными объекта
    val editKey = editProperty?.id
    var name by remember(editKey) {
        mutableStateOf(if (isEditMode) editProperty?.name.orEmpty() else CreateDraftHolder.name)
    }
    var area by remember(editKey) {
        mutableStateOf(if (isEditMode) editProperty?.area?.toFieldText().orEmpty() else CreateDraftHolder.area)
    }
    var price by remember(editKey) {
        mutableStateOf(if (isEditMode) editProperty?.rentAmount?.toFieldText().orEmpty() else CreateDraftHolder.price)
    }
    var description by remember(editKey) {
        mutableStateOf(if (isEditMode) editProperty?.description.orEmpty() else CreateDraftHolder.description)
    }

    var rooms by remember(editKey) {
        mutableStateOf(if (isEditMode) editProperty?.rooms else CreateDraftHolder.rooms)
    }
    var sleepingPlaces by remember(editKey) {
        mutableStateOf(if (isEditMode) editProperty?.sleepingPlaces else CreateDraftHolder.sleepingPlaces)
    }
    var floor by remember(editKey) {
        mutableStateOf(if (isEditMode) editProperty?.floor else CreateDraftHolder.floor)
    }
    var floorsInHouse by remember(editKey) {
        mutableStateOf(if (isEditMode) editProperty?.floorsInHouse else CreateDraftHolder.floorsInHouse)
    }

    var phoneNumber by remember(editKey) {
        mutableStateOf(if (isEditMode) editProperty?.phone.orEmpty().removePrefix("+") else CreateDraftHolder.phoneNumber)
    }
    var wifiPassword by remember(editKey) {
        mutableStateOf(if (isEditMode) editProperty?.wifiPassword.orEmpty() else CreateDraftHolder.wifiPassword)
    }
    var rulesText by remember(editKey) {
        mutableStateOf(if (isEditMode) editProperty?.houseRules.orEmpty() else CreateDraftHolder.rulesText)
    }
    var serviceInfo by remember(editKey) {
        mutableStateOf(if (isEditMode) editProperty?.serviceInfo.orEmpty() else CreateDraftHolder.serviceInfo)
    }
    // Оба аккордеона изначально свёрнуты
    var tenantInfoExpanded by remember { mutableStateOf(false) }
    var serviceInfoExpanded by remember { mutableStateOf(false) }

    // Ошибки валидации обязательных полей (подсвечиваются при нажатии «Создать объект»)
    var nameError by remember { mutableStateOf(false) }
    var roomsError by remember { mutableStateOf(false) }
    var addressError by remember { mutableStateOf(false) }
    var areaError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var sleepingError by remember { mutableStateOf(false) }

    // Черновые счётчики (шаг 4): обновляются при возврате с экрана «Добавить счетчик»
    var draftMeters by remember(editKey) { mutableStateOf(CreateDraftHolder.meters) }
    val metersLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(metersLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                draftMeters = CreateDraftHolder.meters
            }
        }
        metersLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { metersLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var photoUris by remember(editKey) {
        mutableStateOf(
            if (isEditMode) editProperty?.photos.orEmpty().map { it.url } else CreateDraftHolder.photoUris
        )
    }
    var showPhotoMenuIndex by remember { mutableIntStateOf(-1) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            photoUris = uris.map { it.toString() } + photoUris
            CreateDraftHolder.photoUris = photoUris
        }
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    // В режиме редактирования тип/аренда/адрес/координаты берём из объекта
    val effPropertyType = editProperty?.type ?: propertyType
    val effRentType = editProperty?.rentType ?: rentType
    val effAddress = editProperty?.address ?: initialAddress
    val effLatitude = editProperty?.latitude ?: initialLatitude
    val effLongitude = editProperty?.longitude ?: initialLongitude
    // id объекта для «Графика платежей» (в режиме правки — редактируемый объект)
    val schedulePropertyId = propertyId ?: editPropertyId

    val canCreate = name.isNotBlank() &&
        effAddress.isNotBlank() &&
        !uiState.isCreating

    // Режим редактирования: контроль несохранённых изменений (Figma 2677-26609)
    var showExitDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = isEditMode && editProperty?.let { p ->
        name != p.name.orEmpty() ||
            area != p.area?.toFieldText().orEmpty() ||
            price != p.rentAmount?.toFieldText().orEmpty() ||
            description != p.description.orEmpty() ||
            rooms != p.rooms ||
            sleepingPlaces != p.sleepingPlaces ||
            floor != p.floor ||
            floorsInHouse != p.floorsInHouse ||
            phoneNumber != p.phone.orEmpty().removePrefix("+") ||
            wifiPassword != p.wifiPassword.orEmpty() ||
            rulesText != p.houseRules.orEmpty() ||
            serviceInfo != p.serviceInfo.orEmpty() ||
            photoUris != p.photos.orEmpty().map { it.url }
    } == true

    fun attemptSave() {
        nameError = name.isBlank()
        roomsError = rooms == null
        addressError = effAddress.isBlank()
        areaError = area.isBlank()
        priceError = price.isBlank()
        sleepingError = sleepingPlaces == null
        val hasErrors = nameError || roomsError || addressError || areaError || priceError || sleepingError
        if (!hasErrors) {
            viewModel.updateProperty(
                propertyId = editPropertyId ?: "",
                name = name,
                address = effAddress,
                area = area,
                rentAmount = price,
                description = description,
                photoUris = photoUris,
                serviceInfo = serviceInfo,
                // Значение хранится без «+» — добавляем при сохранении
                phone = phoneNumber.trim().let { if (it.isBlank()) "" else "+$it" },
                wifiPassword = wifiPassword,
                houseRules = rulesText,
                type = effPropertyType,
                rentType = effRentType,
                rooms = rooms,
                sleepingPlaces = sleepingPlaces,
                floor = floor,
                floorsInHouse = floorsInHouse,
                latitude = effLatitude,
                longitude = effLongitude
            ) { onCreated(it) }
        }
    }

    fun resetChanges() {
        val p = editProperty ?: return
        name = p.name.orEmpty()
        area = p.area?.toFieldText().orEmpty()
        price = p.rentAmount?.toFieldText().orEmpty()
        description = p.description.orEmpty()
        rooms = p.rooms
        sleepingPlaces = p.sleepingPlaces
        floor = p.floor
        floorsInHouse = p.floorsInHouse
        phoneNumber = p.phone.orEmpty().removePrefix("+")
        wifiPassword = p.wifiPassword.orEmpty()
        rulesText = p.houseRules.orEmpty()
        serviceInfo = p.serviceInfo.orEmpty()
        photoUris = p.photos.orEmpty().map { it.url }
        nameError = false
        roomsError = false
        addressError = false
        areaError = false
        priceError = false
    }

    // Системная кнопка «назад» при несохранённых правках — через диалог (Figma 2677-26836)
    BackHandler(enabled = isEditMode && hasUnsavedChanges && !uiState.isCreating) {
        showExitDialog = true
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                // Клавиатура поднимает весь контент (скролл + нижние CTA),
                // а не перекрывает его; работает вместе с adjustResize в манифесте
                .imePadding()
        ) {
            ScreenToolbar(
                title = if (isEditMode) "Редактировать объект" else "Новый объект",
                onBack = {
                    // Несохранённые правки — сначала диалог «Сохранить изменения?»
                    if (isEditMode && hasUnsavedChanges && !uiState.isCreating) {
                        showExitDialog = true
                    } else {
                        onBack()
                    }
                },
                showClose = false
            )
            // Прогресс-бар — только во флоу создания (в редактировании его нет, Figma 2677-26609)
            if (!isEditMode) {
                CreationProgressBar(currentStep = 4)
            }
            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
                // Боковой паддинг НЕ здесь: каждая секция несёт свой (20dp),
                // а жёлтая панель «Счетчики» — full-bleed во всю ширину экрана
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Фото (Figma: плитка 183x130, r30, камера 50, подпись Grey/Text)
                if (photoUris.isEmpty()) {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        AddPhotoTile { galleryLauncher.launch("image/*") }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        itemsIndexed(photoUris) { index, uri ->
                            Box {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(183.dp)
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(30.dp))
                                        .clickable { showPhotoMenuIndex = index }
                                )
                                DropdownMenu(
                                    expanded = showPhotoMenuIndex == index,
                                    onDismissRequest = { showPhotoMenuIndex = -1 },
                                    containerColor = Color.White
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Сделать основным") },
                                        onClick = {
                                            if (index > 0) {
                                                photoUris = listOf(photoUris[index]) + photoUris.filterIndexed { i, _ -> i != index }
                                                CreateDraftHolder.photoUris = photoUris
                                            }
                                            showPhotoMenuIndex = -1
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Удалить", color = ErrorRed) },
                                        onClick = {
                                            photoUris = photoUris.filterIndexed { i, _ -> i != index }
                                            CreateDraftHolder.photoUris = photoUris
                                            showPhotoMenuIndex = -1
                                        }
                                    )
                                }
                            }
                        }
                        item {
                            AddPhotoTile { galleryLauncher.launch("image/*") }
                        }
                    }
                }

                // 2. О квартире (Figma: заголовок + 12, чипы комнат r30, gap 6)
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowSectionTitle("О квартире")
                    RoomsChips(
                        selected = rooms,
                        onSelect = {
                            rooms = it
                            roomsError = false
                            CreateDraftHolder.rooms = it
                        },
                        isError = roomsError
                    )
                }

                // 3. Поля (Figma: gap 6; Название/Адрес 64, сетка 2x2 по 65)
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CardInput(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = false
                            CreateDraftHolder.name = it
                        },
                        placeholder = "Название",
                        isError = nameError,
                        errorHint = "Это поле обязательно для заполнения"
                    )
                    // Адрес перенесён на шаг 3 — здесь только отображение
                    CardAddressDisplay(address = effAddress, isError = addressError)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(65.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CardInput(
                            value = area,
                            onValueChange = {
                                area = it
                                areaError = false
                                CreateDraftHolder.area = it
                            },
                            placeholder = "Площадь, м2*",
                            keyboardType = KeyboardType.Decimal,
                            // После ввода рядом со значением показываем единицы
                            suffix = "м²",
                            modifier = Modifier.weight(1f),
                            isError = areaError,
                            errorHint = "Это поле обязательно для заполнения"
                        )
                        CardDropdown(
                            selected = sleepingPlaces,
                            options = SleepingOptions,
                            placeholder = "Спальные места*",
                            onSelect = {
                                sleepingPlaces = it
                                sleepingError = false
                                CreateDraftHolder.sleepingPlaces = it
                            },
                            modifier = Modifier.weight(1f),
                            isError = sleepingError
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(65.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CardDropdown(
                            selected = floor,
                            options = FloorOptions,
                            placeholder = "Этаж",
                            onSelect = {
                                floor = it
                                CreateDraftHolder.floor = it
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CardDropdown(
                            selected = floorsInHouse,
                            options = FloorsInHouseOptions,
                            placeholder = "Этажей в доме",
                            onSelect = {
                                floorsInHouse = it
                                CreateDraftHolder.floorsInHouse = it
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Описание объявления (Figma 5: блок полей → описание — зазор 16, карточка 155)
                Column(
                    // Внешний зазор секций 20 → сдвигаем блок на 4dp вверх, чтобы получить 16
                    modifier = Modifier.offset(y = (-4).dp).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowSectionTitle("Описание объявления")
                    DescriptionCard(
                        value = description,
                        onValueChange = {
                            description = it
                            CreateDraftHolder.description = it
                        }
                    )
                }

                // 5. Стоимость (Figma: заголовок + 12, карточка 64)
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowSectionTitle("Стоимость")
                    CardInput(
                        value = price,
                        onValueChange = {
                            price = it
                            priceError = false
                            CreateDraftHolder.price = it
                        },
                        placeholder = if (effRentType == "длительно") "Цена за месяц, ₽*" else "Цена за сутки, ₽*",
                        keyboardType = KeyboardType.Decimal,
                        isError = priceError,
                        errorHint = "Это поле обязательно для заполнения"
                    )
                }

                // 6. Дополнительно (Figma 5: только «График платежей и реквизиты»)
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FlowSectionTitle("Дополнительно")
                    OutlineCtaButton(
                        text = "График платежей и реквизиты",
                        iconRes = R.drawable.ic_payment_schedule,
                        borderColor = Color(0xD9212121)
                    ) {
                        if (schedulePropertyId != null) {
                            onPaymentSchedule(schedulePropertyId)
                        } else if (canCreate) {
                            submitCreate(
                                viewModel, name, effAddress, area, price, description,
                                photoUris, serviceInfo, phoneNumber, wifiPassword, rulesText,
                                effPropertyType, effRentType, rooms, sleepingPlaces, floor,
                                floorsInHouse, effLatitude, effLongitude
                            ) { newId -> onPaymentSchedule(newId) }
                        }
                    }
                }


                // 7. Аккордеоны (Figma: карточки 64, r20, подзаголовок Text 1 mob, шеврон 40)
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InfoAccordionCard(
                        title = "Информация об объекте",
                        subtitle = "Эта информация будет видна арендатору",
                        expanded = tenantInfoExpanded,
                        onToggle = { tenantInfoExpanded = !tenantInfoExpanded }
                    ) {
                        LabeledField(
                            icon = Icons.Filled.Phone,
                            caption = "Номер телефона",
                            value = phoneNumber,
                            onValueChange = {
                                phoneNumber = it
                                CreateDraftHolder.phoneNumber = it
                            },
                            // «+» стоит в поле всегда, плейсхолдер — «7» (любая страна),
                            // поле цифровое; «+» добавляется при сохранении
                            placeholder = "7",
                            keyboardType = KeyboardType.Phone,
                            prefix = "+"
                        )
                        LabeledField(
                            icon = Icons.Filled.Wifi,
                            caption = "Пароль WiFi",
                            value = wifiPassword,
                            onValueChange = {
                                wifiPassword = it
                                CreateDraftHolder.wifiPassword = it
                            },
                            placeholder = "Rsjuff6749"
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(start = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Правила объекта", style = Headline2MobStyle)
                            // Прозрачный многострочный ввод (Figma 2677-26576: без белой карточки)
                            TransparentNoteField(
                                value = rulesText,
                                onValueChange = {
                                    rulesText = it
                                    CreateDraftHolder.rulesText = it
                                },
                                placeholder = "Использовать помещение исключительно в целях, указанных в договоре"
                            )
                        }
                    }
                    InfoAccordionCard(
                        title = "Служебная информация",
                        subtitle = "Эта информация будет видна только вам",
                        expanded = serviceInfoExpanded,
                        onToggle = { serviceInfoExpanded = !serviceInfoExpanded }
                    ) {
                        // Зона заметки прямо на карточке (Figma 2677-26172): 144dp, без фона
                        TransparentNoteField(
                            value = serviceInfo,
                            onValueChange = {
                                serviceInfo = it
                                CreateDraftHolder.serviceInfo = it
                            },
                            placeholder = "",
                            minHeight = 144.dp
                        )
                    }
                }

                // 7b. Счётчики — жёлтая full-bleed панель (Figma 5: 2751-36069 / 2751-35798).
                // Скролл-колонка без бокового паддинга — панель естественно во всю ширину
                if (!isEditMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFFF1CF))
                            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(if (draftMeters.isEmpty()) 24.dp else 40.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Счетчики", style = ToolbarTitleStyle)
                            // Карандаш появляется, когда есть счётчики (Figma 2751-35798);
                            // редактирование списка пока не определено дизайном — TODO
                            if (draftMeters.isNotEmpty()) {
                                Image(
                                    painter = painterResource(R.drawable.ic_edit_pencil_white),
                                    contentDescription = "Редактировать счетчики",
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        // Контурная кнопка на жёлтом: белая заливка, рамка #212121
                        OutlineCtaButton(
                            text = "Добавить счетчики",
                            iconRes = R.drawable.ic_plus_circle_graphite,
                            borderColor = Graphite,
                            onClick = onAddCounter
                        )
                        if (draftMeters.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                draftMeters.forEach { meter -> DraftMeterCard(meter) }
                            }
                        }
                    }
                }

                // 8. Кнопки (создание: «Создать и опубликовать» + «Создать объект»;
                // редактирование: чёрная «Сохранить изменения» + контурная «Сбросить изменения», Figma 2677-26609)
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isEditMode) {
                        BlackCtaButton(
                            text = "Сохранить изменения",
                            enabled = !uiState.isCreating,
                            onClick = { attemptSave() }
                        )
                        OutlineCtaButton(
                            text = "Сбросить изменения",
                            borderColor = Graphite,
                            enabled = !uiState.isCreating
                        ) { showResetDialog = true }
                    } else {
                        // Валидация + создание (общая для обеих кнопок)
                        fun validateAndCreate() {
                            nameError = name.isBlank()
                            roomsError = rooms == null
                            addressError = effAddress.isBlank()
                            areaError = area.isBlank()
                            priceError = price.isBlank()
                            sleepingError = sleepingPlaces == null
                            val hasErrors = nameError || roomsError || addressError || areaError || priceError || sleepingError
                            if (!hasErrors) {
                                submitCreate(
                                    viewModel, name, effAddress, area, price, description,
                                    photoUris, serviceInfo, phoneNumber, wifiPassword, rulesText,
                                    effPropertyType, effRentType, rooms, sleepingPlaces, floor,
                                    floorsInHouse, effLatitude, effLongitude
                                ) { newId ->
                                    // Черновые счётчики шага 4 — в API после создания объекта
                                    viewModel.createDraftMeters(newId, CreateDraftHolder.meters)
                                    onCreated(newId)
                                }
                            }
                        }
                        // Figma 5: градиентная «Создать и опубликовать» первой, затем контурная
                        GradientCtaButton(text = "Создать и опубликовать") { validateAndCreate() }
                        OutlineCtaButton(text = "Создать объект") { validateAndCreate() }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // Диалог «Сохранить изменения?» при выходе с несохранёнными правками (Figma 2677-26836)
    if (isEditMode && showExitDialog) {
        SaveBeforeExitDialog(
            onSaveAndExit = {
                showExitDialog = false
                attemptSave()
            },
            onExitWithoutSaving = {
                showExitDialog = false
                onBack()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    // Диалог «Сбросить изменения?» (Figma 2677-26796)
    if (isEditMode && showResetDialog) {
        ResetChangesDialog(
            onCancel = { showResetDialog = false },
            onReset = {
                showResetDialog = false
                resetChanges()
            },
            onDismiss = { showResetDialog = false }
        )
    }
}

// Диалог выхода при несохранённых изменениях (Figma 2677-26836):
// «Сохранить изменения?» — [Сохранить и выйти] чёрная / [Выйти без сохранения] контурная.
// Закрытие по тапу вне карточки отключено — только явный выбор.
@Composable
private fun SaveBeforeExitDialog(
    onSaveAndExit: () -> Unit,
    onExitWithoutSaving: () -> Unit,
    onDismiss: () -> Unit
) {
    DesignWidthDialog(
        onDismissRequest = onDismiss,
        dismissOnClickOutside = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Сохранить изменения?", style = ToolbarTitleStyle)
            Text(
                "Внесённые изменения ещё не сохранены. Если выйти сейчас, они будут потеряны.",
                style = Headline2MobStyle.copy(color = GreyText)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BlackCtaButton(text = "Сохранить и выйти", onClick = onSaveAndExit)
                OutlineCtaButton(
                    text = "Выйти без сохранения",
                    borderColor = Graphite,
                    onClick = onExitWithoutSaving
                )
            }
        }
    }
}

// Диалог подтверждения сброса изменений (Figma 2677-26796):
// «Сбросить изменения?» — [Отмена] контурная / [Сбросить изменения] чёрная.
// Закрытие по тапу вне карточки отключено — только явный выбор.
@Composable
private fun ResetChangesDialog(
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    DesignWidthDialog(
        onDismissRequest = onDismiss,
        dismissOnClickOutside = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Сбросить изменения?", style = ToolbarTitleStyle)
            Text(
                "Все несохранённые изменения будут отменены, данные объекта вернутся к последней сохранённой версии.",
                style = Headline2MobStyle.copy(color = GreyText)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlineCtaButton(text = "Отмена", borderColor = Graphite, onClick = onCancel)
                BlackCtaButton(text = "Сбросить изменения", onClick = onReset)
            }
        }
    }
}

private fun submitCreate(
    viewModel: CreatePropertyViewModel,
    name: String,
    address: String,
    area: String,
    price: String,
    description: String,
    photoUris: List<String>,
    serviceInfo: String,
    phone: String,
    wifiPassword: String,
    houseRules: String,
    type: String,
    rentType: String,
    rooms: String?,
    sleepingPlaces: String?,
    floor: String?,
    floorsInHouse: String?,
    latitude: Double?,
    longitude: Double?,
    onSuccess: (String) -> Unit
) {
    viewModel.createProperty(
        name = name,
        address = address,
        area = area,
        rentAmount = price,
        description = description,
        photoUris = photoUris,
        serviceInfo = serviceInfo,
        phone = phone,
        wifiPassword = wifiPassword,
        houseRules = houseRules,
        type = type,
        rentType = rentType,
        rooms = rooms,
        sleepingPlaces = sleepingPlaces,
        floor = floor,
        floorsInHouse = floorsInHouse,
        latitude = latitude,
        longitude = longitude,
        onSuccess = onSuccess
    )
}

// 55.0 → «55», 55.5 → «55.5» — предзаполнение числовых полей в режиме редактирования
private fun Double.toFieldText(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()

// Карточка чернового счётчика на жёлтой панели шага 4 (Figma 2751-35798):
// белая 372×146 r20 pad12 — как карточка счётчика в карточке объекта
@Composable
private fun DraftMeterCard(meter: MeterDraft) {
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
            Text(meter.typeLabel, style = Headline2MobStyle)
            Text(
                "№${meter.factoryNumber}",
                style = Headline2MobStyle.copy(color = GreyText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MeterDraftInfoRow("Текущие показания:", formatDraftMeterValue(meter.currentValue, meter.unit))
            MeterDraftInfoRow("Последнее изменение:", "—")
            MeterDraftInfoRow("Дата следующей проверки:", formatDraftDate(meter.nextVerificationDate))
        }
        Text("Внести новые показания", style = Headline2MobStyle.copy(color = Color(0xD9212121)))
    }
}

@Composable
private fun MeterDraftInfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = CardSubtitleStyle.copy(color = Graphite))
        Text(value, style = CardSubtitleStyle.copy(fontWeight = FontWeight.Medium))
    }
}

// 456.0 → «456 кВт·ч»; дробные — как есть
private fun formatDraftMeterValue(value: Double, unit: String): String {
    val v = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    return if (unit.isBlank()) v else "$v $unit"
}

// yyyy-MM-dd → dd.MM.yyyy; пусто — «—»
private fun formatDraftDate(raw: String): String {
    if (raw.isBlank()) return "—"
    val parts = raw.split("-")
    return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else raw.ifBlank { "—" }
}

// Плитка «Добавить фото» (Figma: 183x130, r30, камера 50, подпись 15 SemiBold Grey/Text)
@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(183.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(CardBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_add_photo_camera),
                contentDescription = "Добавить фото",
                modifier = Modifier.size(50.dp),
                colorFilter = ColorFilter.tint(GreyText)
            )
            Text("Добавить фото", style = Headline2MobStyle.copy(color = GreyText))
        }
    }
}

// Чипы количества комнат (Figma: сетка 5 равных колонок, gap 6; «6+ комнат» — по ширине текста, ряд 2 пустые колонки справа)
@Composable
private fun RoomsChips(selected: String?, onSelect: (String) -> Unit, isError: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Студия", "1", "2", "3", "4").forEach { option ->
                RoomChip(
                    label = option,
                    selected = selected == option,
                    modifier = Modifier.weight(1f),
                    showError = false
                ) { onSelect(option) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoomChip(
                label = "5",
                selected = selected == "5",
                modifier = Modifier.width(69.6.dp),
                showError = false
            ) { onSelect("5") }
            RoomChip(
                label = "6+ комнат",
                selected = selected == "6+",
                showError = false
            ) { onSelect("6+") }
        }
    }
}

@Composable
private fun RoomChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    showError: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            // Выбранный чип — инверсия цвета (Figma 2533-20939): #212121 фон, белый текст
            .background(if (selected) Graphite else CardBackground)
            .then(
                if (!selected && showError) {
                    Modifier.border(1.dp, ErrorRed, RoundedCornerShape(30.dp))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = Headline2MobStyle.copy(
                color = when {
                    selected -> Color.White
                    showError -> ErrorRed
                    else -> Graphite
                }
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Текстовое поле в карточке card_inf (Figma: 64, r20, #EFEFEF, padding 20/10, плейсхолдер Headline 2 mob Grey/Text).
// suffix — единицы измерения после введённого значения («м²»).
// Ошибка (Figma 5): рамка #FF4249, подпись 13/400 красная, под полем подсказка 9.5sp
@Composable
private fun CardInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    suffix: String? = null,
    errorHint: String? = null
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBackground)
                .then(if (isError) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp)) else Modifier)
                .padding(start = 20.dp, end = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = Headline2MobStyle,
                cursorBrush = SolidColor(Graphite),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(
                                    placeholder,
                                    style = if (isError) CardSubtitleStyle.copy(color = ErrorRed)
                                    else Headline2MobPlaceholderStyle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                        // Единицы измерения — рядом с введённым значением
                        if (suffix != null && value.isNotEmpty()) {
                            Text(suffix, style = Headline2MobStyle)
                        }
                    }
                }
            )
        }
        if (isError && errorHint != null) {
            FieldErrorHint(errorHint)
        }
    }
}

// Подсказка ошибки под полем (Figma 5: 9.5/400 #FF4249, отступ слева 20)
@Composable
private fun FieldErrorHint(text: String) {
    Text(
        text,
        fontSize = 9.5.sp,
        lineHeight = 11.5.sp,
        letterSpacing = (-0.2).sp,
        color = ErrorRed,
        modifier = Modifier.padding(start = 20.dp)
    )
}

// Адрес — только отображение (карта на шаге 3); ошибка — рамка + подпись + подсказка (Figma 5)
@Composable
private fun CardAddressDisplay(
    address: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBackground)
                .then(if (isError) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp)) else Modifier)
                .padding(start = 20.dp, end = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (address.isBlank()) {
                Text(
                    "Адрес*",
                    style = if (isError) CardSubtitleStyle.copy(color = ErrorRed)
                    else Headline2MobPlaceholderStyle
                )
            } else {
                Text(
                    address,
                    style = Headline2MobStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isError) {
            FieldErrorHint("Это поле обязательно для заполнения")
        }
    }
}

// Дропдаун в карточке card_inf (ячейка 65, шеврон 40 справа, padding 20/10);
// ошибка (Figma 5): рамка + подпись 13/400 красная + подсказка «Выберите значение»
@Composable
private fun CardDropdown(
    selected: String?,
    options: List<String>,
    placeholder: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBackground)
                    .then(if (isError) Modifier.border(1.dp, ErrorRed, RoundedCornerShape(20.dp)) else Modifier)
                    .clickable { expanded = true }
                    .padding(start = 20.dp, end = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selected ?: placeholder,
                        style = when {
                            selected == null && isError -> CardSubtitleStyle.copy(color = ErrorRed)
                            selected == null -> Headline2MobPlaceholderStyle
                            else -> Headline2MobStyle
                        },
                        maxLines = 2,
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
        if (isError && selected == null) {
            FieldErrorHint("Выберите значение")
        }
    }
}

// Описание объявления (Figma: карточка 155, r20, padding 10, плейсхолдер Text 1 mob)
@Composable
private fun DescriptionCard(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(155.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
            .padding(10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = CardSubtitleStyle.copy(color = Graphite),
            cursorBrush = SolidColor(Graphite),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text("Эта информация будет видна в объявлении", style = CardSubtitleStyle)
                    }
                    innerTextField()
                }
            }
        )
    }
}

// Аккордеон card_inf (Figma: 64, r20, заголовок 15 SemiBold + подзаголовок 13 Regular Grey/Text, шеврон 40)
@Composable
private fun InfoAccordionCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBackground)
    ) {
        // Без ripple-подсветки и анимации размера при раскрытии/закрытии
        val headerInteraction = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Раскрытый (Figma 2677-26576/26172): 10dp сверху + строка 40dp, только название;
                // свёрнутый — 64dp с подзаголовком
                .padding(top = if (expanded) 10.dp else 0.dp)
                .clickable(
                    interactionSource = headerInteraction,
                    indication = null,
                    onClick = onToggle
                )
                .padding(start = 20.dp, end = 10.dp)
                .height(if (expanded) 40.dp else 64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(title, style = Headline2MobStyle)
                if (!expanded) {
                    Text(subtitle, style = CardSubtitleStyle)
                }
            }
            Image(
                painter = painterResource(R.drawable.ic_card_chevron),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .rotate(if (expanded) 180f else 0f)
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Заголовок → поля 12, между полями 6, снизу 20 (Figma 2677-26576)
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content
            )
        }
    }
}

// Поле с иконкой (телефон/WiFi) внутри аккордеона — белая пилюля 64dp r20 (Figma 2677-26576)
@Composable
private fun LabeledField(
    icon: ImageVector,
    caption: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null
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
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Graphite
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Постоянный префикс («+»): всегда в начале, курсор после него
                        if (prefix != null) {
                            Text(prefix, style = FieldTextStyle)
                        }
                        Box(Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(placeholder, style = Headline2MobPlaceholderStyle)
                            }
                            innerTextField()
                        }
                    }
                }
            }
        )
    }
}

// Прозрачная зона заметки внутри аккордеона (Figma 2677-26172/26576):
// без фона и рамки, 13sp Regular #212121, плейсхолдер — только когда пусто
@Composable
private fun TransparentNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minHeight: Dp = 57.dp
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        textStyle = CardSubtitleStyle.copy(color = Graphite),
        cursorBrush = SolidColor(Graphite),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = CardSubtitleStyle)
                }
                innerTextField()
            }
        }
    )
}

// Многострочное поле внутри аккордеона — белая карточка
@Composable
private fun MultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            textStyle = CardSubtitleStyle.copy(color = Graphite),
            cursorBrush = SolidColor(Graphite),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, style = CardSubtitleStyle)
                    }
                    innerTextField()
                }
            }
        )
    }
}




