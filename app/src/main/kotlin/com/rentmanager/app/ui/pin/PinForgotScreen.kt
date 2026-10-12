package com.rentmanager.app.ui.pin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.AppScreenHeader
import com.rentmanager.app.ui.components.CanonicalDialogButton
import com.rentmanager.app.ui.theme.InterFontFamily

/**
 * «Не помните код доступа?» — по макету 18 (ветка «после правок», 4252:36045)
 * это ОТДЕЛЬНЫЙ ЭКРАН, а не диалог: полноэкранный фрейм 412×917 с шапкой-стрелкой
 * и блоком, прижатым к низу.
 *
 * Замеры: щит 140×140, заголовок 20/600, пояснение 13/500 (ширина 372 → 2 строки,
 * gap 6), зазор до кнопок 175, кнопки 372×55 r100 (тёмная + контурная, gap 6),
 * снизу 20 до навигационного инсета. «Карточка» в макете без заливки и тени.
 */
@Composable
fun PinForgotScreen(onBack: () -> Unit, onLogin: () -> Unit) {
    BackHandler { onBack() }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Канон шапки проекта (Денис: «канон»); на этом экране заголовка нет — только стрелка
        AppScreenHeader(title = "", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_pin_shield_large),
                contentDescription = null,
                modifier = Modifier.size(140.dp)
            )
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Не помните код доступа?",
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = InterFontFamily,
                    color = PinInk,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Войдите в аккаунт повторно. После этого вы сможете создать новый код доступа",
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = InterFontFamily,
                    color = PinInk,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(175.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CanonicalDialogButton(
                    text = "Войти в аккаунт",
                    container = PinInk,
                    textColor = Color.White
                ) { onLogin() }
                CanonicalDialogButton(
                    text = "Отменить",
                    stroke = PinInk,
                    textColor = PinInk
                ) { onBack() }
            }
        }
    }
}
