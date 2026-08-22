package com.rentmanager.app.ui.landlord.createproperty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rentmanager.app.ui.theme.ScreenBackground
import com.rentmanager.app.ui.theme.ToolbarTitleStyle

@Composable
fun ChooseRentTypeScreen(
    propertyType: String = "Квартира",
    onBack: () -> Unit,
    onRentTypeSelected: (String) -> Unit
) {
    Scaffold(containerColor = ScreenBackground) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .padding(paddingValues)
        ) {
            ScreenToolbar(title = "Новый объект", onBack = onBack)
            CreationProgressBar(currentStep = 2)
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Вариант сдачи",
                    style = ToolbarTitleStyle,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                ChoiceCard("Сдать длительно") { onRentTypeSelected("длительно") }
                ChoiceCard("Сдать посуточно") { onRentTypeSelected("посуточно") }
            }
        }
    }
}
