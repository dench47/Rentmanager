package com.rentmanager.app.ui.landlord.createproperty

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.AddressMapPicker
import com.rentmanager.app.ui.components.BlackButtonWithIcon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.navigation.compose.hiltViewModel

private val GradientBackground = Brush.verticalGradient(
    colors = listOf(Color.White, Color(0xFFF5F7FA))
)

@Composable
fun CreatePropertyScreen(
    propertyId: String? = null,
    onBack: () -> Unit,
    onCreated: () -> Unit = {},
    onPaymentSchedule: (String) -> Unit = {},
    viewModel: CreatePropertyViewModel = hiltViewModel()
) {
    val isEdit = propertyId != null
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf(TextFieldValue("")) }
    var addressFocused by remember { mutableStateOf(false) }
    var area by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tenantInfo by remember { mutableStateOf("") }
    var serviceInfo by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var rulesText by remember { mutableStateOf("") }

    // Photos
    var photoUris by remember { mutableStateOf(listOf<String>()) }
    var showPhotoMenuIndex by remember { mutableIntStateOf(-1) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            photoUris = photoUris + uris.map { it.toString() }
        }
    }

    var tenantInfoExpanded by remember { mutableStateOf(false) }
    var serviceInfoExpanded by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    // Подстановка адреса после обратного геокодинга (тап по карте)
    LaunchedEffect(uiState.addressToSet) {
        uiState.addressToSet?.let { text ->
            address = TextFieldValue(text, TextRange(0))
            viewModel.consumeAddressToSet()
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GradientBackground)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onBack() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_arrow_left),
                            contentDescription = "Назад",
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            if (isEdit) "Изменить объект" else "Новый объект",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1D1F),
                            letterSpacing = (-0.3).sp
                        )
                    }
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .pointerInput(Unit) {
                            detectTapGestures { focusManager.clearFocus() }
                        }
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. Photos section
                    if (photoUris.isEmpty()) {
                        // Компактный значок добавления фото
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5F5F5))
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                null,
                                Modifier.size(32.dp),
                                tint = Color(0xFF007AFF)
                            )
                        }
                    } else {
                        // Photos grid — Figma: 88×88dp, cornerRadius 8, gap 8dp
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(photoUris.size) { index ->
                            @OptIn(ExperimentalFoundationApi::class)
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFD3D3D3))
                                    .combinedClickable(
                                        onClick = { },
                                        onLongClick = { showPhotoMenuIndex = index }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = photoUris[index],
                                    contentDescription = "Фото ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                if (index == 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color(0x99000000)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Основное",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showPhotoMenuIndex == index,
                                    onDismissRequest = { showPhotoMenuIndex = -1 }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Сделать основным") },
                                        onClick = {
                                            if (index > 0) {
                                                photoUris = listOf(photoUris[index]) + photoUris.filterIndexed { i, _ -> i != index }
                                            }
                                            showPhotoMenuIndex = -1
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Удалить", color = Color(0xFFE53935)) },
                                        onClick = {
                                            photoUris = photoUris.filterIndexed { i, _ -> i != index }
                                            showPhotoMenuIndex = -1
                                        }
                                    )
                                }
                            }
                            }
                            // Add more photos button
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF5F5F5))
                                        .clickable { galleryLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        null,
                                        Modifier.size(32.dp),
                                        tint = Color(0xFF007AFF)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Название
                    PremiumTextField(value = name, onValueChange = { name = it }, placeholder = "Название")

                    // 3. Адрес
                    AddressTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            viewModel.suggestAddress(it.text)
                        },
                        placeholder = "Адрес",
                        onFocusChanged = { focused ->
                            addressFocused = focused
                            if (!focused) {
                                viewModel.commitAddress(address.text)
                            }
                        },
                        onDone = {
                            viewModel.commitAddress(address.text)
                        }
                    )

                    // Подсказки адреса (Photon / OpenStreetMap)
                    if (uiState.addressSuggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                uiState.addressSuggestions.forEachIndexed { index, suggestion ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                address = TextFieldValue(" " + suggestion.displayName, TextRange(0))
                                                viewModel.selectAddress(suggestion)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            suggestion.displayName,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1D1D1F),
                                            letterSpacing = (-0.4).sp
                                        )
                                    }
                                    if (index < uiState.addressSuggestions.lastIndex) {
                                        HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
                                    }
                                }
                            }
                        }
                    }

                    // Карта с выбранной меткой
                    if (addressFocused && uiState.selectedLatitude != null && uiState.selectedLongitude != null) {
                        AddressMapPicker(
                            latitude = uiState.selectedLatitude!!,
                            longitude = uiState.selectedLongitude!!,
                            onLocationSelected = { lat, lon -> viewModel.onMapTapped(lat, lon) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(top = 12.dp)
                        )
                    }

                    // 4. Площадь
                    PremiumTextField(value = area, onValueChange = { area = it }, placeholder = "Площадь (м²)", keyboardType = KeyboardType.Decimal)

                    // 5. Цена
                    PremiumTextField(value = price, onValueChange = { price = it }, placeholder = "Цена (₽/мес)", keyboardType = KeyboardType.Decimal)

                    // 5. Информация об объекте (accordion)
                    PremiumAccordionCard(
                        title = "Информация об объекте",
                        subtitle = "Эта информация будет видна арендатору",
                        expanded = tenantInfoExpanded,
                        onToggle = { tenantInfoExpanded = !tenantInfoExpanded }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PremiumTextField(
                                value = description,
                                onValueChange = { description = it },
                                placeholder = "Описание",
                                singleLine = false
                            )

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Phone, null, Modifier.size(18.dp), tint = Color(0xFF007AFF))
                                Text("Номер телефона", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D1D1F), letterSpacing = (-0.4).sp)
                            }
                            PremiumTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, placeholder = "+7 (899) 99-99-99")

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Wifi, null, Modifier.size(18.dp), tint = Color(0xFF007AFF))
                                Text("Пароль WiFi", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D1D1F), letterSpacing = (-0.4).sp)
                            }
                            PremiumTextField(value = wifiPassword, onValueChange = { wifiPassword = it }, placeholder = "Rsjuff6749")

                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    null,
                                    Modifier.size(18.dp),
                                    tint = Color(0xFF007AFF)
                                )
                                Text("Правила объекта", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D1D1F), letterSpacing = (-0.4).sp)
                            }
                            PremiumTextField(
                                value = rulesText,
                                onValueChange = { rulesText = it },
                                placeholder = "Использовать помещение исключительно в целях, указанных в договоре",
                                singleLine = false
                            )
                        }
                    }

                    // 6. Служебная информация (accordion)
                    PremiumAccordionCard(
                        title = "Служебная информация",
                        subtitle = "Эта информация будет видна только вам",
                        expanded = serviceInfoExpanded,
                        onToggle = { serviceInfoExpanded = !serviceInfoExpanded }
                    ) {
                        PremiumTextField(
                            value = serviceInfo,
                            onValueChange = { serviceInfo = it },
                            placeholder = "",
                            singleLine = false
                        )
                    }
                }

                // Bottom buttons (fixed)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.95f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isEdit) {
                        BlackButtonWithIcon(
                            text = "Добавить счетчики",
                            iconRes = R.drawable.ic_plus_circle,
                            onClick = { },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (isEdit) {
                        BlackButtonWithIcon(
                            text = "График платежей и реквизиты",
                            iconRes = R.drawable.ic_calendar_edit,
                            onClick = { onPaymentSchedule(propertyId!!) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Сохранить / Создать
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(100.dp))
                            .background(if (name.isNotBlank() && address.text.isNotBlank()) Color(0xFF212121) else Color(0xFF212121).copy(alpha = 0.5f))
                    .clickable(enabled = name.isNotBlank() && address.text.isNotBlank() && !uiState.isCreating) {
                        if (isEdit) {
                            onCreated()
                        } else {
                            viewModel.createProperty(
                                name = name,
                                address = address.text,
                                area = area,
                                rentAmount = price,
                                description = description,
                                photoUris = photoUris,
                                serviceInfo = serviceInfo,
                                phone = phoneNumber,
                                wifiPassword = wifiPassword,
                                houseRules = rulesText,
                                latitude = uiState.selectedLatitude,
                                longitude = uiState.selectedLongitude,
                                onSuccess = onCreated
                            )
                        }
                    },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when {
                                uiState.isCreating -> "Загрузка..."
                                isEdit -> "Сохранить"
                                else -> "Создать"
                            },
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.4).sp
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun AddressTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    onFocusChanged: (Boolean) -> Unit,
    onDone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = Color(0xFF1D1D1F),
                letterSpacing = (-0.4).sp
            ),
            cursorBrush = SolidColor(Color(0xFF1D1D1F)),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { innerTextField ->
                Box {
                    if (value.text.isEmpty()) {
                        Text(
                            placeholder,
                            fontSize = 15.sp,
                            color = Color(0xFF8E8E93),
                            letterSpacing = (-0.4).sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = Color(0xFF1D1D1F),
                letterSpacing = (-0.4).sp
            ),
            cursorBrush = SolidColor(Color(0xFF1D1D1F)),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontSize = 15.sp,
                            color = Color(0xFF8E8E93),
                            letterSpacing = (-0.4).sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun PremiumAccordionCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1D1D1F),
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF8E8E93),
                        letterSpacing = (-0.4).sp
                    )
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null,
                    Modifier.size(18.dp),
                    tint = Color(0xFF1D1D1F)
                )
            }
            AnimatedVisibility(visible = expanded) {
                content()
            }
        }
    }
}