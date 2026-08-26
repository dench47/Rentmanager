package com.rentmanager.app.ui.landlord.createproperty

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.ToolbarTitleStyle

// Шаг 1: выбор типа недвижимости (Figma 2533:17735)
@Composable
fun ChoosePropertyTypeScreen(
    onBack: () -> Unit,
    onApartmentSelected: () -> Unit,
    onClose: () -> Unit,
    onContinueDraft: () -> Unit
) {
    var showStubAlert by remember { mutableStateOf(false) }
    var showContinueDialog by remember { mutableStateOf(false) }

    // Если новый вход во флоу, а черновик уже есть — предложить продолжить или начать заново
    LaunchedEffect(Unit) {
        if (CreateDraftHolder.consumeEntryRequested() && CreateDraftHolder.hasDraft()) {
            showContinueDialog = true
        }
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            ScreenToolbar(title = "Новый объект", onBack = onBack, showClose = true, onClose = onClose)
            CreationProgressBar(currentStep = 1)
            Spacer(Modifier.height(20.dp))

            Text(
                "Выберите тип недвижимости",
                style = ToolbarTitleStyle,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(30.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChoiceCard("Квартира", R.drawable.ic_pt_apartment) { onApartmentSelected() }
                ChoiceCard("Комната", R.drawable.ic_pt_room) { showStubAlert = true }
                ChoiceCard("Дом", R.drawable.ic_pt_house) { showStubAlert = true }
                ChoiceCard("Гараж", R.drawable.ic_pt_garage) { showStubAlert = true }
                ChoiceCard("Коммерческая", R.drawable.ic_pt_commercial) { showStubAlert = true }
            }
        }
    }

    if (showStubAlert) {
        AlertDialog(
            onDismissRequest = { showStubAlert = false },
            title = { Text("Экран еще не готов") },
            text = { Text("Пока реализован путь для квартиры") },
            confirmButton = {
                TextButton(onClick = { showStubAlert = false }) { Text("ОК") }
            }
        )
    }

    if (showContinueDialog) {
        ContinueDraftDialog(
            onContinue = {
                showContinueDialog = false
                onContinueDraft()
            },
            onStartOver = {
                CreateDraftHolder.clear()
                showContinueDialog = false
            },
            onDismiss = { showContinueDialog = false }
        )
    }
}

// Диалог продолжения создания объекта (Figma 2571:18704)
@Composable
private fun ContinueDraftDialog(
    onContinue: () -> Unit,
    onStartOver: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Шапка: заголовок + подзаголовок + крестик закрытия
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Добавление объекта", style = ToolbarTitleStyle)
                        Text(
                            "Вы начали добавлять объект. Хотите продолжить?",
                            style = Headline2MobStyle.copy(color = GreyText)
                        )
                    }
                    Image(
                        painter = painterResource(R.drawable.ic_toolbar_close),
                        contentDescription = "Закрыть",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(onClickLabel = "Закрыть") { onDismiss() },
                        contentScale = ContentScale.Fit
                    )
                }

                // Кнопки Button_CTA (Figma): чёрная «Продолжить» + контурная «Начать заново»
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BlackCtaButton(text = "Продолжить") { onContinue() }
                    OutlineCtaButton(text = "Начать заново", borderColor = Color(0xD9212121)) {
                        onStartOver()
                    }
                }
            }
        }
    }
}

