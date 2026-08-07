package com.rentmanager.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rentmanager.app.R
import com.rentmanager.app.ui.auth.name.EnterNameDialog
import com.rentmanager.app.ui.components.RoleButton
import com.rentmanager.app.ui.role.UserRole

@Composable
fun HomeScreen(
    onRoleSelected: (UserRole) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userName) {
        if (uiState.userName.isEmpty() || uiState.userName == "Пользователь") {
            showNameDialog = true
        }
    }

    if (showNameDialog) {
        EnterNameDialog(
            onDismiss = { showNameDialog = false },
            onSaved = { name ->
                viewModel.updateUserName(name)
                showNameDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0))
    ) {
        // ============================================================
        // 1. Здание (Figma: 127-4572, y=3576, w=393, h=392)
        // ============================================================
        Image(
            painter = painterResource(R.drawable.home_building),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            contentScale = ContentScale.FillWidth
        )

        // ============================================================
        // 2. Основной контент Column
        // ============================================================
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // System Bar (Figma: y=3104-3157, h=53)
            Spacer(modifier = Modifier.height(53.dp))

            // Gap System Bar → Top Bar: 3177-3157=20dp
            Spacer(modifier = Modifier.height(20.dp))

            // Top Bar (Figma: y=3177): аватар + имя слева ... шестерёнка справа
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Лево: аватар (Figma: 131-1099, 40×40 IMAGE) + «Андрей»
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_default_avatar),
                        contentDescription = "Аватар",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = uiState.userName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black.copy(alpha = 0.9f),
                        letterSpacing = (-0.4).sp
                    )
                }

                // Право: шестерёнка (Figma: 127-5156, Frame 85, 40×40)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = "Настройки",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Отступ top bar → приветствие: 3277-(3177+40)=60dp
            Spacer(modifier = Modifier.height(60.dp))

            // Приветствие (Figma: 127-4663, y=3277, w=253, центрирован, itemSpacing=4)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Добро пожаловать,",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    letterSpacing = (-0.4).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.userName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    letterSpacing = (-0.4).sp,
                    textAlign = TextAlign.Center
                )
            }

            // Отступ приветствие → кнопки: 3469-(3277+72)=120dp
            Spacer(modifier = Modifier.height(120.dp))

            // Кнопки выбора роли (Figma: 127-4666, y=3469, h=95, itemSpacing=16)
            Column(
                modifier = Modifier.fillMaxWidth(),
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

                Row(
                    modifier = Modifier.width(353.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoleButton(
                        text = "Сдаю",
                        isSelected = uiState.selectedRole == UserRole.LANDLORD,
                        onClick = {
                            viewModel.selectRole(UserRole.LANDLORD)
                            onRoleSelected(UserRole.LANDLORD)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    RoleButton(
                        text = "Арендую",
                        isSelected = uiState.selectedRole == UserRole.TENANT,
                        onClick = {
                            viewModel.selectRole(UserRole.TENANT)
                            onRoleSelected(UserRole.TENANT)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Отступ кнопки → здание: 3576-3564=12dp
            Spacer(modifier = Modifier.height(12.dp))
        }

    }
}
