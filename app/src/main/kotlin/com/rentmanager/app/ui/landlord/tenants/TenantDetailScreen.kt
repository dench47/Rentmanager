package com.rentmanager.app.ui.landlord.tenants

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.ui.theme.InterFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantDetailScreen(
    tenantId: String,
    onBack: () -> Unit,
    onPropertyClick: (String) -> Unit,
    viewModel: TenantDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(tenantId) { viewModel.load(tenantId) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Арендатор", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = InterFontFamily, modifier = Modifier.clickable(onClickLabel = "Назад") { onBack() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color(0xFF212121))
                    }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.Call, "Позвонить", tint = Color(0xFF212121)) }
                    IconButton(onClick = { }) { Icon(Icons.Default.Email, "Написать", tint = Color(0xFF212121)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF212121),
                    navigationIconContentColor = Color(0xFF212121),
                    actionIconContentColor = Color(0xFF212121)
                )
            )
        }
    ) { padding ->
        val tenant = uiState.tenant

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEFEFEF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (tenant?.fullName ?: "?").take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8E8E93),
                        fontFamily = InterFontFamily
                    )
                }
                Text(
                    tenant?.fullName ?: "…",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    fontFamily = InterFontFamily
                )
            }

            HorizontalDivider(color = Color(0x1A000000))

            InfoField("Полное имя", tenant?.fullName)
            InfoField("Название компании", tenant?.companyName)
            InfoField("Номер паспорта", tenant?.passportData)

            // Прикрепить документы
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .clickable { }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Attachment, null, Modifier.size(20.dp), tint = Color(0xFF727272))
                Text("Прикрепить документы", fontSize = 14.sp, color = Color(0xFF727272), fontFamily = InterFontFamily)
            }

            InfoField("Электронная почта", tenant?.email)
            InfoField("Номер телефона", tenant?.phone)

            // Служебная информация
            Text(
                "Служебная информация",
                fontSize = 13.sp,
                color = Color(0xFF8E8E93),
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFF3F3F3))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        tenant?.serviceInfo ?: "—",
                        fontSize = 14.sp,
                        color = Color(0xFF151515),
                        fontFamily = InterFontFamily,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.Edit, null, Modifier.size(20.dp), tint = Color(0xFF212121))
                }
            }
            Text(
                "Видна только арендодателю",
                fontSize = 13.sp,
                color = Color(0x993C3C43),
                fontFamily = InterFontFamily,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 16.dp)
            )

            uiState.errorMessage?.let {
                Text(it, color = Color(0xFFE53935), fontSize = 13.sp, fontFamily = InterFontFamily, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun InfoField(label: String, value: String?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, fontSize = 13.sp, color = Color(0xFF8E8E93), fontFamily = InterFontFamily)
        Text(value.orEmpty().ifBlank { "—" }, fontSize = 16.sp, color = Color(0xFF1D1D1F), fontFamily = InterFontFamily)
    }
}