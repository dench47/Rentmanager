package com.rentmanager.app.ui.landlord.otherproperties

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.data.api.UserSearchResult
import com.rentmanager.app.ui.theme.InterFontFamily
import com.rentmanager.app.ui.theme.RentManagerTheme

@Composable
fun LandlordsListScreen(
    onLandlordClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: LandlordsListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).background(Color.White)) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 12.dp, end = 0.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(Modifier.clickable { onBack() }.padding(end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(painter = painterResource(R.drawable.ic_arrow_left), contentDescription = "Назад", modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit)
                    Text("Арендодатели", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121), letterSpacing = (-0.3).sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(end = 4.dp)) {
                    Box(Modifier.size(44.dp).clickable { }, Alignment.Center) { Image(painter = painterResource(R.drawable.ic_search), contentDescription = "Поиск", modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit) }
                    Box(Modifier.size(44.dp).clickable { }, Alignment.Center) { Image(painter = painterResource(R.drawable.ic_sort), contentDescription = "Сортировка", modifier = Modifier.size(24.dp), contentScale = ContentScale.Fit) }
                }
            }
            uiState.errorMessage?.let {
                Text(it, color = Color(0xFFE53935), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Загрузка…", fontSize = 14.sp, color = Color(0xFF8E8E93))
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(uiState.landlords, key = { it.id }) { landlord ->
                        LandlordCard(landlord) { onLandlordClick(landlord.id) }
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 1.dp, color = Color.Black.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LandlordCard(landlord: UserSearchResult, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEFEFEF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                landlord.name.ifBlank { "?" }.take(1).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8E8E93),
                fontFamily = InterFontFamily
            )
        }
        Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(landlord.name.ifBlank { "Без имени" }, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.Black, letterSpacing = (-0.3).sp)
            Text(landlord.phone, fontSize = 13.sp, fontWeight = FontWeight.Normal, color = Color(0x993C3C43), letterSpacing = (-0.4).sp)
        }
    }
}

@Preview(showBackground = true, name = "Список арендодателей")
@Composable
private fun PreviewLandlordsList() {
    RentManagerTheme { LandlordsListScreen(onLandlordClick = {}, onBack = {}) }
}