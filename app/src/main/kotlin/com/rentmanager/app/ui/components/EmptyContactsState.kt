package com.rentmanager.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.theme.Graphite

/**
 * Пустое состояние списков «Арендаторы»/«Арендодатели» (канвас «13», 3108:56847/56859):
 * иллюстрация empty-contacts 135, заголовок 20/600 −0.3, подпись 15/600 −0.4 (обе #212121,
 * по центру), зазоры 20/12/20, чёрная CTA 372×55 с иконкой «человек+плюс» (зазор 6).
 *
 * Блок центрируется по всей высоте экрана — в макете карточка лежит на (917−325)/2 = 296
 * от верха кадра, т.е. по центру полного фрейма, а не области между шапкой и таббаром.
 */
@Composable
fun EmptyContactsState(
    title: String,
    subtitle: String,
    ctaText: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // PNG 675px рисуется в ~425 физических пикселей: без High (мипмапы)
            // билинейная минификация даёт лестницу на кривых
            val illustration = ImageBitmap.imageResource(R.drawable.ic_empty_contacts)
            Image(
                bitmap = illustration,
                contentDescription = null,
                modifier = Modifier.size(135.dp),
                filterQuality = FilterQuality.High
            )
            Spacer(Modifier.height(20.dp))
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = Graphite,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(
                subtitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
                color = Graphite,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            BlackCtaButton(
                text = ctaText,
                iconRes = R.drawable.ic_person_plus,
                iconSpacing = 6.dp,
                onClick = onCtaClick
            )
        }
    }
}
