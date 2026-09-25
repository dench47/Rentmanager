package com.rentmanager.app.ui.landlord.createproperty

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.ErrorRed
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.SectionTitleStyle
import com.rentmanager.app.ui.theme.ToolbarTitleStyle
import com.rentmanager.app.ui.components.DesignWidthDialog

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
                style = SectionTitleStyle,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            // Заголовок → список: 12 (правка дизайнера, файл «8»)
            Spacer(Modifier.height(12.dp))

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
        com.rentmanager.app.ui.components.CanonicalDialog(
            onDismiss = { showStubAlert = false },
            title = "Экран еще не готов",
            text = "Пока реализован путь для квартиры"
        ) {
            com.rentmanager.app.ui.components.CanonicalDialogButton(
                text = "Понятно",
                container = Color(0xFF212121),
                textColor = Color.White,
                onClick = { showStubAlert = false }
            )
        }
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

// Диалог продолжения создания объекта (Figma 2698-22344): заголовок + текст,
// красная «Продолжить» и контурная «Начать заново», паддинг карточки 20
@Composable
private fun ContinueDraftDialog(
    onContinue: () -> Unit,
    onStartOver: () -> Unit,
    onDismiss: () -> Unit
) {
    com.rentmanager.app.ui.components.CanonicalDialog(
        onDismiss = onDismiss,
        title = "Добавление объекта",
        text = "Вы начали добавлять объект. Хотите продолжить?"
    ) {
        // Кнопки (Figma 2698-22344): «Продолжить» + контурная «Начать заново»
        com.rentmanager.app.ui.components.CanonicalDialogButton(
            text = "Продолжить",
            container = Color(0xFFFF4249),
            textColor = Color.White,
            onClick = { onContinue() }
        )
        com.rentmanager.app.ui.components.CanonicalDialogButton(
            text = "Начать заново",
            stroke = Color(0xFF212121),
            textColor = Color(0xFF212121),
            onClick = { onStartOver() }
        )
    }
}

