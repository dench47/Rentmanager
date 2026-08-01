package com.rentmanager.app.ui.landlord.tenants

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R

data class TenantItem(
    val id: String,
    val name: String,
    val company: String,
    @DrawableRes val avatarRes: Int? = null
)

@Composable
fun TenantsListScreen(
    onTenantClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val tenants = listOf(
        TenantItem("1", "Кузнецов Андрей", "Vertex Studio", R.drawable.mock_avatar_kuznetsov),
        TenantItem("2", "Новиков Тихон", "Nexus Dynamics", R.drawable.mock_avatar_novikov),
        TenantItem("3", "Лебедев Павел", ""),
        TenantItem("4", "Морозова София", "Neural", R.drawable.mock_avatar_morozova)
    )

    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 12.dp, bottom = 12.dp, end = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(end = 20.dp),
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
                        text = "Арендаторы",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121),
                        letterSpacing = (-0.3).sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = "Поиск",
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Box(
                        modifier = Modifier.size(44.dp).clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_sort),
                            contentDescription = "Сортировка",
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tenants) { tenant ->
                    TenantCard(
                        name = tenant.name,
                        company = tenant.company,
                        avatarRes = tenant.avatarRes,
                        onClick = { onTenantClick(tenant.id) },
                        onEmailClick = { },
                        onCallClick = { }
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

@Composable
private fun TenantCard(
    name: String,
    company: String,
    @DrawableRes avatarRes: Int?,
    onClick: () -> Unit,
    onEmailClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar — Figma: circle, 40×40
            Image(
                painter = painterResource(avatarRes ?: R.drawable.ic_default_avatar),
                contentDescription = name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    letterSpacing = (-0.3).sp
                )
                if (company.isNotEmpty()) {
                    Text(
                        text = company,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0x993C3C43),
                        letterSpacing = (-0.4).sp
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF6F6F6))
                    .clickable { onEmailClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_email),
                    contentDescription = "Email",
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF6F6F6))
                    .clickable { onCallClick() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_call),
                    contentDescription = "Позвонить",
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}