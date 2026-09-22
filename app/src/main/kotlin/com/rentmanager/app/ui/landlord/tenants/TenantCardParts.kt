package com.rentmanager.app.ui.landlord.tenants

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.rentmanager.app.R
import com.rentmanager.app.ui.landlord.createproperty.OutlineCtaButton
import com.rentmanager.app.ui.theme.Graphite
import com.rentmanager.app.ui.theme.GreyText
import com.rentmanager.app.ui.theme.Headline2MobStyle
import com.rentmanager.app.ui.theme.SectionTitleStyle

private val CardGrey = Color(0xFFEFEFEF)

/** Шапка «Карточка арендатора»: назад + название + вертикальное меню. */
@Composable
fun TenantCardToolbar(
    onBack: () -> Unit,
    onMenu: () -> Unit
) {
    androidx.compose.foundation.layout.Column {
        Spacer(Modifier.statusBarsPadding())
        // Канон AppScreenHeader: статус-инсет → 27dp → строка (стрелка+8+текст) → 13dp
        Spacer(Modifier.height(27.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_landlord_back),
                    contentDescription = "Назад",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Карточка арендатора",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = com.rentmanager.app.ui.theme.InterFontFamily,
                    color = Graphite,
                    letterSpacing = (-0.3).sp
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_more_vertical),
                contentDescription = "Меню",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onMenu() }
            )
        }
    }
}

/** Карточка-строка «подпись 13/400 + значение 15/600» на сером r20. */
@Composable
fun InfoCard(caption: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardGrey)
            .padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(caption, fontSize = 13.sp, lineHeight = 15.7.sp, letterSpacing = (-0.4).sp, color = GreyText)
            Spacer(Modifier.height(4.dp))
            Text(value, style = Headline2MobStyle.copy(lineHeight = 18.2.sp))
        }
    }
}

/** Строка блока аренды: дата периода с календарём 18 + карточка объекта. */
@Composable
fun BookingEntry(
    period: String,
    propertyName: String,
    propertyAddress: String,
    photoUrl: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardGrey)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_calendar_payment),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                period,
                fontSize = 13.sp,
                lineHeight = 15.7.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.4).sp,
                color = Graphite
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Column {
                Text(propertyName, style = Headline2MobStyle.copy(lineHeight = 18.2.sp))
                Spacer(Modifier.height(4.dp))
                Text(propertyAddress, fontSize = 13.sp, lineHeight = 15.7.sp, letterSpacing = (-0.4).sp, color = GreyText)
            }
        }
    }
}
