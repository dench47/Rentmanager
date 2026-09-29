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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.ui.landlord.createproperty.BlackCtaButton
import com.rentmanager.app.ui.theme.Graphite

/**
 * Пустое состояние списков «Арендаторы»/«Арендодатели» (канвас 13).
 * Карточка 412×313 r30 центрируется по всей высоте кадра; внутри —
 * иллюстрация 140, заголовок 20/600, подпись 13/500 (обе #212121, по центру),
 * зазоры 12/6/20 и чёрная CTA 372×55 r100 (иконка в CTA — по состоянию).
 *
 * Иллюстрации в макете РАЗНЫЕ: «списка нет» — empty-contacts (люди+плюс),
 * «ничего не найдено» (фильтр/поиск) — contacts-not-found (лупа с крестиком).
 */
@Composable
fun EmptyContactsState(
    title: String,
    subtitle: String,
    ctaText: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
    illustration: Painter = BitmapPainter(
        ImageBitmap.imageResource(R.drawable.ic_empty_contacts),
        filterQuality = FilterQuality.High
    ),
    ctaIconRes: Int? = R.drawable.ic_person_plus
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
            Image(
                painter = illustration,
                contentDescription = null,
                modifier = Modifier.size(140.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = Graphite,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.4).sp,
                color = Graphite,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            BlackCtaButton(
                text = ctaText,
                iconRes = ctaIconRes,
                iconSpacing = 6.dp,
                onClick = onCtaClick
            )
        }
    }
}
