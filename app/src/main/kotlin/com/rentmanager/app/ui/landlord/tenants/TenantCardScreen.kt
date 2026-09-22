package com.rentmanager.app.ui.landlord.tenants

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
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
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardGrey = Color(0xFFEFEFEF)
private val DividerGrey = Color(0xFFDBDBDB)
private val RuDate = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale("ru"))

/**
 * «Карточка арендатора» — режим просмотра (канвас «13», 2983:42399/42356/50881/51020):
 * шапка с вертикальным меню, аватар+имя+карандаш, инфо-карточки, Позвонить/Написать,
 * паспорт с маскировкой (глаз), «Документы · N файлов», служебная информация и
 * история аренды (аккордеоны), текущая аренда.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantCardScreen(
    tenantId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    onAttach: () -> Unit = {},
    onDelete: () -> Unit = {},
    viewModel: TenantCardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showActionsSheet by remember { mutableStateOf(false) }

    // Загрузка стартует в init ViewModel (вместе с посевом кеша в первый кадр);
    // ON_RESUME-триггер не нужен — экран самодостаточен и не мигает

    val tenant = state.tenant
    val passport = if (state.passportVisible) passportPlain(tenant?.passportData)
    else passportMasked(tenant?.passportData)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TenantCardToolbar(onBack = onBack, onMenu = { showActionsSheet = true })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ---- Аватар + имя + карандаш (Figma: 80, pad v10, аватар 60, зазор 8) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(CardGrey),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ава с аккаунта арендатора (по телефону), иначе плейсхолдер
                        val avatarUrl = tenant?.avatarUrl
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(100.dp))
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_avatar_placeholder),
                                contentDescription = null,
                                modifier = Modifier.size(width = 42.dp, height = 38.dp),
                                tint = Color.Unspecified
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    tenant?.fullName.orEmpty(),
                    style = SectionTitleStyle,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(17.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(CardGrey)
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pencil_circle),
                        contentDescription = "Редактировать",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // ---- Инфо-карточки + Позвонить/Написать (sp6 между карточками и кнопками) ----
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoCard("Название компании", tenant?.companyName ?: "—")
                    InfoCard("Электронная почта", tenant?.email ?: "—")
                    InfoCard("Номер телефона", formatTenantPhone(tenant?.phone))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlineCtaButton(
                        text = "Позвонить",
                        iconRes = R.drawable.ic_call_phone,
                        iconSpacing = 4.dp,
                        borderColor = Graphite,
                        onClick = {
                            tenant?.phone?.filter(Char::isDigit)?.let { digits ->
                                val normalized = if (digits.startsWith("8")) "7" + digits.drop(1) else digits
                                if (normalized.length == 11) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:+$normalized"))
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OutlineCtaButton(
                        text = "Написать",
                        iconRes = R.drawable.ic_chat_message,
                        iconSpacing = 4.dp,
                        borderColor = Graphite,
                        onClick = { },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ---- Паспорт: заполнен → 2 поля с маской + документы; пустой → 3428:65920 ----
            if (passport != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PassportField(
                            caption = "Серия паспорта",
                            value = passport.first,
                            onClick = { viewModel.togglePassport() },
                            modifier = Modifier.weight(1f)
                        )
                        PassportField(
                            caption = "Номер паспорта",
                            value = passport.second,
                            onClick = { viewModel.togglePassport() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlineCtaButton(
                        text = "Документы · 2 файла",
                        iconRes = R.drawable.ic_doc_folder,
                        iconSpacing = 6.dp,
                        borderColor = Graphite,
                        onClick = { }
                    )
                }
            } else {
                // Новый арендатор после прикрепления — данных паспорта ещё нет
                // (Figma 3428:65920): заголовок 18/600, пустые поля 15/600 #727272
                // с глазками, ссылка «Прикрепить документ» (скрепка 20 + 6 + 13/500)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Паспортные данные", style = SectionTitleStyle)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EmptyPassportField(caption = "Серия паспорта", modifier = Modifier.weight(1f))
                        EmptyPassportField(caption = "Номер паспорта", modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_attach_doc),
                            contentDescription = "Прикрепить документ",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Прикрепить документ",
                            fontSize = 13.sp,
                            lineHeight = 15.7.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.4).sp,
                            color = Graphite
                        )
                    }
                }
            }

            // ---- Служебная информация (аккордеон: 64 + раскрытое содержимое) ----
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardGrey)
                        .clickable { viewModel.toggleServiceInfo() }
                        .padding(start = 20.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Служебная информация", style = Headline2MobStyle.copy(lineHeight = 18.2.sp))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Эта информация видна только вам",
                            fontSize = 13.sp,
                            lineHeight = 15.7.sp,
                            letterSpacing = (-0.4).sp,
                            color = GreyText
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
                    )
                }
                if (state.serviceInfoExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardGrey)
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            tenant?.serviceInfo?.takeIf { it.isNotBlank() }
                                ?: "Здесь пока ничего нет",
                            fontSize = 13.sp,
                            lineHeight = 15.7.sp,
                            letterSpacing = (-0.4).sp,
                            color = Graphite
                        )
                    }
                }
            }

            // ---- Арендует (текущая/будущая) + История аренды ----
            val currentBooking = state.currentBooking
            if (currentBooking != null || state.pastBookings.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (currentBooking != null) {
                        Text("Арендует", style = SectionTitleStyle)
                        BookingEntry(
                            period = "с ${currentBooking.startDate.format(RuDate)} " +
                                "до ${currentBooking.endDate.format(RuDate)}",
                            propertyName = currentBooking.propertyName,
                            propertyAddress = currentBooking.propertyAddress,
                            photoUrl = currentBooking.propertyPhoto
                        )
                    }
                    if (state.pastBookings.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "История аренды",
                                style = SectionTitleStyle,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = "История аренды",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { viewModel.toggleHistory() },
                                tint = Color.Unspecified
                            )
                        }
                        if (state.historyExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                state.pastBookings.forEach { b ->
                                    BookingEntry(
                                        period = "с ${b.startDate.format(RuDate)} до ${b.endDate.format(RuDate)}",
                                        propertyName = b.propertyName,
                                        propertyAddress = b.propertyAddress,
                                        photoUrl = b.propertyPhoto
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    // ---- Шит «Действия с арендатором» (2983:51850) ----
    if (showActionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = Color.White
        ) {
            TenantActionsSheet(
                onEdit = { showActionsSheet = false; onEdit() },
                onAttach = { showActionsSheet = false; onAttach() },
                onDelete = { showActionsSheet = false; onDelete() },
                onDismiss = { showActionsSheet = false }
            )
        }
    }
}

/** Телефон из DTO (+7900…/8900…) → отображение +7-900-000-00-03. */
private fun formatTenantPhone(raw: String?): String {
    var digits = raw?.filter { it.isDigit() } ?: return "—"
    if (digits.startsWith("8")) digits = "7" + digits.drop(1)
    if (digits.length < 11) return raw ?: "—"
    val d = digits
    return buildString {
        append("+").append(d.take(1))
        append("-").append(d.substring(1, 4))
        append("-").append(d.substring(4, 7))
        append("-").append(d.substring(7, 9))
        append("-").append(d.substring(9, 11))
    }
}

/** Поле паспорта 64: подпись + значение + иконка глаза 24 в зоне 40. */
@Composable
private fun PassportField(
    caption: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardGrey)
            .clickable { onClick() }
            .padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(caption, fontSize = 13.sp, lineHeight = 15.7.sp, letterSpacing = (-0.4).sp, color = GreyText)
            Spacer(Modifier.height(4.dp))
            Text(value, style = Headline2MobStyle.copy(lineHeight = 18.2.sp))
        }
        Icon(
            painter = painterResource(R.drawable.ic_eye_slash),
            contentDescription = "Показать паспорт",
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Пустое поле паспорта (нет данных, 3428:65920): подпись 15/600 #727272 + глаз. */
@Composable
private fun EmptyPassportField(caption: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardGrey)
            .padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            caption,
            fontSize = 15.sp,
            lineHeight = 18.2.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            color = GreyText,
            modifier = Modifier.weight(1f)
        )
        Icon(
            painter = painterResource(R.drawable.ic_eye_slash),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Шит «Действия с арендатором»: редактировать / прикрепить / удалить. */
@Composable
private fun TenantActionsSheet(
    onEdit: () -> Unit,
    onAttach: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp)
    ) {
        Text(
            "Действия с арендатором",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            color = Graphite,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        OutlineCtaButton(
            text = "Редактировать карточку",
            iconRes = R.drawable.ic_action_edit,
            borderColor = Color(0xD9212121),
            onClick = onEdit
        )
        Spacer(Modifier.height(6.dp))
        OutlineCtaButton(
            text = "Прикрепить к объекту",
            iconRes = R.drawable.ic_user_outline,
            borderColor = Color(0xD9212121),
            onClick = onAttach
        )
        Spacer(Modifier.height(6.dp))
        OutlineCtaButton(
            text = "Удалить карточку арендатора",
            iconRes = R.drawable.ic_action_delete,
            borderColor = com.rentmanager.app.ui.theme.ErrorRed,
            textColor = com.rentmanager.app.ui.theme.ErrorRed,
            onClick = onDelete
        )
    }
}
