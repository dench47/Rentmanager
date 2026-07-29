package com.rentmanager.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rentmanager.app.ui.components.IconCircleButton
import com.rentmanager.app.ui.components.RoleButton

@Composable
fun HomeScreen(
    onLandlordSelected: () -> Unit,
    onTenantSelected: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color(0xFFF0F0F0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF0F0F0))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Hero image section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(392.dp)
                        .background(Color(0xFFBDBDBD)), // placeholder for image
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Top bar: settings (left) + avatar+name (right) — over image
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 53.dp, start = 20.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Settings icon (left) — Figma: Frame 85, 40x40dp, white/transparent, blur
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            IconCircleButton(onClick = onSettingsClick)
                        }

                        // Avatar + name (right) — Figma: Frame 2087329451
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            // Avatar placeholder
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFBDBDBD)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Placeholder initials
                                Text(
                                    text = uiState.userName.firstOrNull()?.toString() ?: "A",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = uiState.userName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Black.copy(alpha = 0.9f),
                                letterSpacing = (-0.4).sp
                            )
                        }
                    }

                    // Welcome greeting — Figma: Frame 2087329448, bottom-left
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, bottom = 20.dp)
                    ) {
                        Text(
                            text = "Добро пожаловать,",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = uiState.userName,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            letterSpacing = (-0.4).sp
                        )
                    }
                }

                // Role selection section — Figma: Frame 2087329449
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Выберите свою роль",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF212121),
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Role buttons row
                    Row(
                        modifier = Modifier.width(353.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoleButton(
                            text = "Сдаю",
                            isSelected = uiState.selectedRole == UserRole.LANDLORD,
                            onClick = {
                                viewModel.selectRole(UserRole.LANDLORD)
                                onLandlordSelected()
                            },
                            modifier = Modifier.width(172.5.dp)
                        )
                        RoleButton(
                            text = "Арендую",
                            isSelected = uiState.selectedRole == UserRole.TENANT,
                            onClick = {
                                viewModel.selectRole(UserRole.TENANT)
                                onTenantSelected()
                            },
                            modifier = Modifier.width(172.5.dp)
                        )
                    }
                }
            }

            // Bottom tab bar with home indicator — Figma: 4. Tab Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(33.dp)
                    .background(Color(0xFFF0F0F0).copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(139.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.Black)
                )
            }
        }
    }
}