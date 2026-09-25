package com.rentmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.data.api.VersionResponse

@Composable
fun UpdateDialog(
    info: VersionResponse,
    isDownloading: Boolean,
    progress: Float,
    errorMessage: String?,
    onDownload: () -> Unit,
    onLater: () -> Unit,
    onRetry: () -> Unit
) {
    // Канонический диалог: тап вне не закрывает — реагируем только на кнопки
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* только кнопки */ }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 20.dp)
                .clickable(enabled = false) {}
        ) {
            Column(Modifier.padding(horizontal = 10.dp)) {
                Text(
                    if (isDownloading) "Загрузка обновления..." else "Доступно обновление v${info.versionName}",
                    fontSize = 20.sp,
                    lineHeight = 24.2.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    color = Color(0xFF212121)
                )
                Spacer(Modifier.height(6.dp))
                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color(0xFF007AFF),
                        trackColor = Color(0xFFE0E0E0),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0x993C3C43)
                    )
                } else if (errorMessage != null) {
                    Text(
                        errorMessage,
                        fontSize = 15.sp,
                        color = Color(0xFFFF4249)
                    )
                } else {
                    Text(
                        "Новая версия доступна для скачивания.",
                        fontSize = 15.sp,
                        color = Color(0x993C3C43)
                    )
                    if (info.releaseNotes.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            info.releaseNotes,
                            fontSize = 13.sp,
                            color = Color(0x993C3C43)
                        )
                    }
                }
            }
            if (!isDownloading) {
                Spacer(Modifier.height(20.dp))
                CanonicalDialogButton(
                    text = if (errorMessage != null) "Повторить" else "Обновить",
                    container = Color(0xFF212121),
                    textColor = Color.White,
                    onClick = if (errorMessage != null) onRetry else onDownload
                )
                if (errorMessage == null) {
                    Spacer(Modifier.height(6.dp))
                    CanonicalDialogButton(
                        text = "Позже",
                        stroke = Color(0xFF212121),
                        textColor = Color(0xFF212121),
                        onClick = onLater
                    )
                }
            }
        }
    }
}

@Composable
fun ForcedUpdateScreen(
    info: VersionResponse,
    isDownloading: Boolean,
    progress: Float,
    errorMessage: String?,
    onDownload: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* блокируем взаимодействие с приложением позади */ }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Требуется обновление",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Доступна версия ${info.versionName}. Для продолжения работы обновите приложение.",
                fontSize = 16.sp,
                color = Color(0x993C3C43),
                textAlign = TextAlign.Center
            )
            if (info.releaseNotes.isNotBlank() && !isDownloading && errorMessage == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    info.releaseNotes,
                    fontSize = 13.sp,
                    color = Color(0x993C3C43),
                    textAlign = TextAlign.Center
                )
            }
            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    errorMessage,
                    fontSize = 15.sp,
                    color = Color(0xFFFF4249),
                    textAlign = TextAlign.Center
                )
            }
            if (isDownloading) {
                Spacer(Modifier.height(24.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF007AFF),
                    trackColor = Color(0xFFE0E0E0)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${(progress * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0x993C3C43)
                )
            } else {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .background(Color(0xFF212121), RoundedCornerShape(100.dp))
                        .clickable { if (errorMessage != null) onRetry() else onDownload() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (errorMessage != null) "Повторить" else "Обновить",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}