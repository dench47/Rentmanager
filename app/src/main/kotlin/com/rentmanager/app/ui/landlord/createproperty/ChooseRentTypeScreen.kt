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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.SectionTitleStyle

// Шаг 2: выбор варианта сдачи (Figma 2533:17764)
@Composable
fun ChooseRentTypeScreen(
    propertyType: String = "Квартира",
    onBack: () -> Unit,
    onRentTypeSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            ScreenToolbar(title = "Новый объект", onBack = onBack, showClose = true, onClose = onClose)
            CreationProgressBar(currentStep = 2)
            Spacer(Modifier.height(20.dp))

            Text(
                "Вариант сдачи",
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
                ChoiceCard("Сдать длительно", R.drawable.ic_rent_long) { onRentTypeSelected("длительно") }
                ChoiceCard("Сдать посуточно", R.drawable.ic_rent_daily) { onRentTypeSelected("посуточно") }
            }
        }
    }
}

