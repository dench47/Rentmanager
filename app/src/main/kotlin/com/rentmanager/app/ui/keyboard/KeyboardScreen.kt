package com.rentmanager.app.ui.keyboard

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun KeyboardScreen(
    onContinue: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: KeyboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color(0xFFFEFFBB)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFEFFBB))
        ) {
            // Top section with phone number display
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 53.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Введите номер телефона",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    letterSpacing = (-0.4).sp
                )
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = uiState.phoneNumber.ifEmpty { "+7 " },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    letterSpacing = 2.sp
                )
            }

            // Keyboard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color(0xFFD4D6DC))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Row 1: 1 2 3
                KeyboardRow(
                    keys = listOf("1", "2", "3"),
                    onKeyPress = { viewModel.onKeyPress(it) }
                )
                // Row 2: 4 5 6
                KeyboardRow(
                    keys = listOf("4", "5", "6"),
                    onKeyPress = { viewModel.onKeyPress(it) }
                )
                // Row 3: 7 8 9
                KeyboardRow(
                    keys = listOf("7", "8", "9"),
                    onKeyPress = { viewModel.onKeyPress(it) }
                )
                // Row 4: empty, 0, backspace
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Empty space (same as key size)
                    Spacer(modifier = Modifier.size(72.dp, 48.dp))

                    // 0 key
                    KeyboardKey(
                        label = "0",
                        onClick = { viewModel.onKeyPress("0") }
                    )

                    // Backspace
                    Box(
                        modifier = Modifier
                            .size(72.dp, 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.onBackspace() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Backspace,
                            contentDescription = "Удалить",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardRow(
    keys: List<String>,
    onKeyPress: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        keys.forEach { key ->
            KeyboardKey(
                label = key,
                onClick = { onKeyPress(key) }
            )
        }
    }
}

@Composable
private fun KeyboardKey(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp, 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}