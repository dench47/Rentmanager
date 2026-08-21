package com.rentmanager.app.ui.landlord.createproperty

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rentmanager.app.R
import com.rentmanager.app.ui.theme.CardBackground
import com.rentmanager.app.ui.theme.FieldTextStyle
import com.rentmanager.app.ui.theme.ToolbarTitleStyle

// Toolbar экрана с заголовком и стрелкой «назад»
@Composable
fun ScreenToolbar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.clickable { onBack() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = "Назад",
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.size(8.dp))
            Text(title, style = ToolbarTitleStyle)
        }
    }
}

// Карточка выбора (тип недвижимости / вариант сдачи)
@Composable
fun ChoiceCard(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(CardBackground)
            .clickable { onClick() }
            .padding(horizontal = 31.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_property_type),
            contentDescription = null,
            modifier = Modifier.size(50.dp)
        )
        Text(label, style = FieldTextStyle)
    }
}
