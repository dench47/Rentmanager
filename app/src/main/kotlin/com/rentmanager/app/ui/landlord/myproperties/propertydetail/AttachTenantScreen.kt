package com.rentmanager.app.ui.landlord.myproperties.propertydetail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rentmanager.app.data.api.UserSearchResult
import com.rentmanager.app.ui.theme.InterFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachTenantScreen(
    propertyId: String,
    onDismiss: () -> Unit,
    onAttached: () -> Unit,
    viewModel: AttachTenantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedUser by remember { mutableStateOf<UserSearchResult?>(null) }
    var manualName by remember { mutableStateOf("") }
    var manualPhone by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            val (name, phone) = queryContact(context, uri)
            if (phone.isNotBlank()) {
                manualName = name
                manualPhone = phone
                searchQuery = phone
                viewModel.searchUsers(phone)
            }
        }
    }

    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted
        if (granted) contactPicker.launch(null)
    }

    val canAttach = selectedUser != null || (manualName.isNotBlank() && manualPhone.isNotBlank())

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = { Text("Добавить арендатора", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, modifier = Modifier.clickable(onClickLabel = "Назад") { onDismiss() }) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color(0xFF212121))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF212121),
                    navigationIconContentColor = Color(0xFF212121)
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                modifier = Modifier.navigationBarsPadding().imePadding()
            ) {
                Button(
                    onClick = {
                        val user = selectedUser
                        if (user != null) {
                            viewModel.attachUser(propertyId, user.id, onAttached)
                        } else {
                            viewModel.attachManual(propertyId, manualName.trim(), manualPhone.trim(), onAttached)
                        }
                    },
                    enabled = canAttach && !uiState.isAttaching,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(55.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121))
                ) {
                    Text(
                        if (uiState.isAttaching) "Прикрепление..." else "Прикрепить арендатора",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionCard(Icons.Default.Contacts, "Выбрать из контактов") {
                if (hasContactsPermission) contactPicker.launch(null)
                else permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }

            SectionDivider("или")

            Text("Найти по номеру телефона", fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, color = Color(0xFF212121))
            WhiteTextField(searchQuery, { searchQuery = it }, "Номер телефона", KeyboardType.Phone)
            Button(
                onClick = { viewModel.searchUsers(searchQuery) },
                enabled = searchQuery.isNotBlank() && !uiState.isSearching,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Найти", fontFamily = InterFontFamily)
            }

            if (uiState.isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (uiState.searchResults.isNotEmpty()) {
                uiState.searchResults.forEach { user ->
                    UserCard(user, selected = selectedUser?.id == user.id) {
                        selectedUser = if (selectedUser?.id == user.id) null else user
                    }
                }
            }

            SectionDivider("или")

            Text("Ввести вручную", fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, color = Color(0xFF212121))
            WhiteTextField(manualName, { manualName = it }, "Имя", KeyboardType.Text)
            WhiteTextField(manualPhone, { manualPhone = it }, "Телефон", KeyboardType.Phone)

            uiState.errorMessage?.let {
                Text(it, color = Color(0xFFE53935), fontSize = 13.sp, fontFamily = InterFontFamily)
            }
        }
    }
}

private fun queryContact(context: Context, uri: Uri): Pair<String, String> {
    var name = ""
    var phone = ""
    val contactId = uri.lastPathSegment
    context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
        arrayOf(contactId),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            name = cursor.getString(0) ?: ""
            phone = cursor.getString(1) ?: ""
        }
    }
    return name to phone
}

@Composable
private fun SectionDivider(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(Modifier.weight(1f), color = Color(0x1A000000))
        Text(text, fontSize = 13.sp, color = Color(0xFF8E8E93), fontFamily = InterFontFamily)
        HorizontalDivider(Modifier.weight(1f), color = Color(0x1A000000))
    }
}

@Composable
private fun ActionCard(icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, Modifier.size(24.dp), tint = Color(0xFF007AFF))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = InterFontFamily, color = Color(0xFF212121))
        }
    }
}

@Composable
private fun WhiteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType
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
            textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF1D1D1F), letterSpacing = (-0.4).sp),
            cursorBrush = SolidColor(Color(0xFF1D1D1F)),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 15.sp, color = Color(0xFF8E8E93), letterSpacing = (-0.4).sp)
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun UserCard(user: UserSearchResult, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFE8F0FE) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEFEFEF)),
                contentAlignment = Alignment.Center
            ) {
                if (user.avatarUrl != null) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        user.name.take(1).uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8E8E93),
                        fontFamily = InterFontFamily
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    user.name.ifBlank { "Без имени" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFontFamily,
                    color = Color(0xFF212121)
                )
                Text(user.phone, fontSize = 13.sp, color = Color(0xFF8E8E93), fontFamily = InterFontFamily)
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp), tint = Color(0xFF007AFF))
            }
        }
    }
}