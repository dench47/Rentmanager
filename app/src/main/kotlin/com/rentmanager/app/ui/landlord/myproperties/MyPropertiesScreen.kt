package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Notifications

data class PropertyItem(
    val id: String,
    val name: String,
    val address: String,
    val status: String,
    val isOverdue: Boolean = false
)

// Mock data — empty ArrayList for now (will be replaced by backend later)
val mockProperties: MutableList<PropertyItem> = ArrayList()

@Composable
fun MyPropertiesScreen(
    onPropertyClick: (String) -> Unit,
    onCreateProperty: () -> Unit,
    onBack: () -> Unit,
    onFinanceClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    // Auto-navigate to create screen if no properties exist
    LaunchedEffect(Unit) {
        if (mockProperties.isEmpty()) {
            onCreateProperty()
        }
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.clickable { onBack() }.padding(end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_arrow_left),
                        contentDescription = "Назад",
                        modifier = Modifier.size(24.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        "Моя недвижимость",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121),
                        letterSpacing = (-0.3).sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = "Финансы",
                        modifier = Modifier.size(44.dp).clickable { onFinanceClick() }.padding(10.dp),
                        tint = Color(0xFF212121)
                    )
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Уведомления",
                        modifier = Modifier.size(44.dp).clickable { onNotificationsClick() }.padding(10.dp),
                        tint = Color(0xFF212121)
                    )
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить",
                        modifier = Modifier.size(44.dp).clickable { onCreateProperty() }.padding(10.dp),
                        tint = Color(0xFF212121)
                    )
                }
            }

            if (mockProperties.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Нет объектов недвижимости",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF212121),
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Создайте первый объект, нажав +",
                        fontSize = 14.sp,
                        color = Color(0x993C3C43),
                        letterSpacing = (-0.4).sp
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(mockProperties) { property ->
                        PropertyCard(
                            property = property,
                            onClick = { onPropertyClick(property.id) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 1.dp,
                            color = Color.Black.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PropertyCard(property: PropertyItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                property.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                letterSpacing = (-0.3).sp
            )
            Text(
                property.address,
                fontSize = 13.sp,
                color = Color(0x993C3C43),
                letterSpacing = (-0.4).sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    property.status,
                    fontSize = 13.sp,
                    color = if (property.isOverdue) Color(0xFFE53935) else Color(0xFF7AB66A),
                    letterSpacing = (-0.4).sp
                )
                if (property.isOverdue) {
                    Text(
                        "Просрочка",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE53935),
                        letterSpacing = (-0.4).sp
                    )
                }
            }
        }
    }
}