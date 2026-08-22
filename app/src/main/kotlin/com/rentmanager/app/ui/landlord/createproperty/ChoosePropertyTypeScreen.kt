package com.rentmanager.app.ui.landlord.createproperty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentmanager.app.ui.theme.ScreenBackground
import com.rentmanager.app.ui.theme.ToolbarTitleStyle

@Composable
fun ChoosePropertyTypeScreen(
    onBack: () -> Unit,
    onApartmentSelected: () -> Unit
) {
    var showStubAlert by remember { mutableStateOf(false) }

    Scaffold(containerColor = ScreenBackground) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .padding(paddingValues)
        ) {
            ScreenToolbar(title = "Новый объект", onBack = onBack)
            CreationProgressBar(currentStep = 1)
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Выберите тип недвижимости",
                    style = ToolbarTitleStyle,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                ChoiceCard("Квартира") { onApartmentSelected() }
                ChoiceCard("Комната") { showStubAlert = true }
                ChoiceCard("Дом") { showStubAlert = true }
                ChoiceCard("Гараж") { showStubAlert = true }
                ChoiceCard("Коммерческая") { showStubAlert = true }
            }
        }
    }

    if (showStubAlert) {
        AlertDialog(
            onDismissRequest = { showStubAlert = false },
            title = { Text("Экран еще не готов") },
            text = { Text("Ждем Вику") },
            confirmButton = {
                TextButton(onClick = { showStubAlert = false }) { Text("ОК") }
            }
        )
    }
}
