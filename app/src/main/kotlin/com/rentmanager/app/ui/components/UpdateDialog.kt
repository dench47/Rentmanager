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
    AlertDialog(
        onDismissRequest = { /* реагируем только на кнопки — тап вне окна не закрывает */ },
        title = {
            Text(
                if (isDownloading) "Загрузка обновления..." else "Доступно обновление v${info.versionName}",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF212121),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isDownloading) {
                    Spacer(Modifier.height(8.dp))
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
                        color = Color(0xFFFF4249),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        "Новая версия доступна для скачивания.",
                        fontSize = 16.sp,
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
        },
        confirmButton = {
            if (!isDownloading) {
                TextButton(onClick = if (errorMessage != null) onRetry else onDownload) {
                    Text(
                        if (errorMessage != null) "Повторить" else "Обновить",
                        color = Color(0xFF007AFF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            if (!isDownloading && errorMessage == null) {
                TextButton(onClick = onLater) {
                    Text("Позже", color = Color(0x993C3C43))
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
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