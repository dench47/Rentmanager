package com.rentmanager.app.ui.auth.verify

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.ui.components.ScaledModalBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerDialog(
    sheetState: SheetState,
    selectedCountry: CountryPhone,
    onCountrySelected: (CountryPhone) -> Unit,
    onDismiss: () -> Unit
) {
    ScaledModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Выберите страну",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF151515),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            CountryPhone.availableCountries.forEachIndexed { index, country ->
                val isSelected = country.countryCode == selectedCountry.countryCode

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCountrySelected(country) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = country.flagEmoji,
                        fontSize = 22.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = country.displayName,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = Color(0xFF151515),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = country.phonePrefix,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFF007AFF) else Color(0xFF8E8E93)
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✓",
                            fontSize = 16.sp,
                            color = Color(0xFF007AFF)
                        )
                    }
                }

                if (index < CountryPhone.availableCountries.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
                }
            }
        }
    }
}