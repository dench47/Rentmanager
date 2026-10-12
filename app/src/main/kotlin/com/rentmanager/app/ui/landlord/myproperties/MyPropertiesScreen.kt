package com.rentmanager.app.ui.landlord.myproperties

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import coil.request.ImageRequest
import com.rentmanager.app.R
import com.rentmanager.app.ui.components.IconNotificationDialog
import com.rentmanager.app.ui.components.ScheduleDialog
import com.rentmanager.app.util.propertyTitle
import java.time.LocalDate
import java.time.YearMonth

// Р СџР В°Р В»Р С‘РЎвЂљРЎР‚Р В° Р С”Р В°Р Р…Р Р†Р В°РЎРѓР В° Р’В«Р СљР С•РЎРЏ Р Р…Р ВµР Т‘Р Р†Р С‘Р В¶Р С‘Р СР С•РЎРѓРЎвЂљРЎРЉР’В» 2 Р Р†Р В°РЎР‚Р С‘Р В°Р Р…РЎвЂљ (2949:45807)
private val White = Color.White
private val TextPrimary = Color(0xFF212121)
private val TextGray = Color(0xFF717171)
private val DividerGrey = Color(0xFFDBDBDB)
private val CellFree = Color(0xFFEFEFEF)
private val CellBusy = Color(0xFFE5F2E7)
private val BusyGreen = Color(0xFF2F7D4D)
private val OverdueRed = Color(0xFFFF4249)


/**
 * Р’В«Р СљР С•РЎРЏ Р Р…Р ВµР Т‘Р Р†Р С‘Р В¶Р С‘Р СР С•РЎРѓРЎвЂљРЎРЉР’В», 2 Р Р†Р В°РЎР‚Р С‘Р В°Р Р…РЎвЂљ РЎР‚Р ВµР Т‘Р С‘Р В·Р В°Р в„–Р Р…Р В° (Р С”Р В°Р Р…Р Р†Р В°РЎРѓ 2949:45807):
 * РЎвЂљР В°Р В±РЎвЂ№ Р’В«Р С›Р В±РЎР‰Р ВµР С”РЎвЂљРЎвЂ№ | Р РЃР В°РЎвЂ¦Р СР В°РЎвЂљР С”Р В°Р’В», Р С”Р В°РЎР‚РЎвЂљР С•РЎвЂЎР С”Р С‘-Р С•Р В±РЎР‰Р ВµР С”РЎвЂљРЎвЂ№ РЎРѓ Р В»Р ВµР Р…РЎвЂљР С•Р в„– Р В±Р В»Р С‘Р В¶Р В°Р в„–РЎв‚¬Р ВµР в„– Р В·Р В°Р С–РЎР‚РЎС“Р В·Р С”Р С‘
 * Р С‘ РЎРѓРЎвЂљРЎР‚Р С•Р С”Р С•Р в„– Р В°РЎР‚Р ВµР Р…Р Т‘Р В°РЎвЂљР С•РЎР‚Р В°, РЎв‚¬Р В°РЎвЂ¦Р СР В°РЎвЂљР С”Р В°-РЎвЂљР В°Р В±Р В»Р С‘РЎвЂ Р В° РЎРѓ Р В·Р В°Р С”РЎР‚Р ВµР С—Р В»РЎвЂР Р…Р Р…Р С•Р в„– Р С”Р С•Р В»Р С•Р Р…Р С”Р С•Р в„– Р С•Р В±РЎР‰Р ВµР С”РЎвЂљР С•Р Р†,
 * Р С”Р С•Р СР С—Р В°Р С”РЎвЂљР Р…РЎвЂ№Р в„– Р Р…Р С‘Р В¶Р Р…Р С‘Р в„– РЎвЂљР В°Р В±Р В±Р В°РЎР‚ (Р В¤Р С‘Р Р…Р В°Р Р…РЎРѓРЎвЂ№ / Р РЋР С•Р В·Р Т‘Р В°РЎвЂљРЎРЉ Р С•Р В±РЎР‰Р ВµР С”РЎвЂљ / Р СњР В° Р С–Р В»Р В°Р Р†Р Р…РЎС“РЎР‹).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPropertiesScreen(
    onPropertyClick: (String) -> Unit,
    onCreateProperty: () -> Unit,
    onBack: () -> Unit,
    onFinanceClick: () -> Unit,
    onWriteClick: () -> Unit,
    onAttachTenant: (propertyId: String, start: LocalDate?, end: LocalDate?) -> Unit = { _, _, _ -> },
    viewModel: MyPropertiesViewModel = viewModel()
) {
    val propertiesRaw by viewModel.properties.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val lastEdit by viewModel.lastEdit.collectAsState()

    // Р РЋР Р…Р ВµР С”Р В±Р В°РЎР‚ Р С—РЎР‚Р В°Р Р†Р С”Р С‘ РЎв‚¬Р В°РЎвЂ¦Р СР В°РЎвЂљР С”Р С‘: Р’В«07.10.2026 РІР‚вЂќ РЎРѓР Р…РЎРЏРЎвЂљР С• Р’В· Р С›РЎвЂљР СР ВµР Р…Р С‘РЎвЂљРЎРЉР’В». Р РЋР В»РЎС“РЎвЂЎР В°Р в„–Р Р…РЎвЂ№Р в„– РЎвЂљР В°Р С—
    // Р С—Р С• Р Т‘Р Р…РЎР‹ Р В»Р ВµРЎвЂЎР С‘РЎвЂљРЎРѓРЎРЏ Р С•Р Т‘Р Р…Р С‘Р С РЎвЂљР В°Р С—Р С•Р С (Р Р† РЎв‚¬Р В°РЎвЂ¦Р СР В°РЎвЂљР С”Р Вµ Р Р†РЎвЂљР С•РЎР‚Р С•Р в„– РЎвЂљР В°Р С— РЎРѓРЎР‚Р В°Р В·РЎС“ Р С—РЎР‚Р С‘Р СР ВµР Р…РЎРЏР ВµРЎвЂљ Р С—РЎР‚Р В°Р Р†Р С”РЎС“)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(lastEdit) {
        val edit = lastEdit ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = edit.label, duration = SnackbarDuration.Long)
        viewModel.dismissEdit()
    }

    // Р РЃР С‘РЎвЂљ Р’В«Р С™Р В°Р С” РЎС“РЎРѓРЎвЂљРЎР‚Р С•Р ВµР Р… Р С”Р В°Р В»Р ВµР Р…Р Т‘Р В°РЎР‚РЎРЉР’В» (Р С‘Р С”Р С•Р Р…Р С”Р В° Р’В«?Р’В» Р Р† РЎвЂљРЎС“Р В»Р В±Р В°РЎР‚Р Вµ) Р С‘ РЎвЂћР С‘Р В»РЎРЉРЎвЂљРЎР‚ Р С•Р В±РЎР‰Р ВµР С”РЎвЂљР С•Р Р†
    var showChessHelp by remember { mutableStateOf(false) }
    var showObjectsFilter by remember { mutableStateOf(false) }

    // Р С›Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…Р С‘Р Вµ РЎРѓР С—Р С‘РЎРѓР С”Р В° Р С—РЎР‚Р С‘ Р Р†Р С•Р В·Р Р†РЎР‚Р В°РЎвЂљР Вµ Р Р…Р В° РЎРЊР С”РЎР‚Р В°Р Р… / Р С‘Р В· РЎвЂћР С•Р Р…Р В°
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Р СџР ВµРЎР‚Р С‘Р С•Р Т‘ Р С”Р В°Р В»Р ВµР Р…Р Т‘Р В°РЎР‚Р ВµР в„–: Р С—РЎР‚Р С‘Р СР ВµР Р…РЎвЂР Р…Р Р…РЎвЂ№Р в„– (Р Р†РЎРѓР Вµ Р С”Р В°РЎР‚РЎвЂљР С•РЎвЂЎР С”Р С‘) Р С‘ РЎвЂЎР ВµРЎР‚Р Р…Р С•Р Р†Р С‘Р С” Р Р† РЎв‚¬Р С‘РЎвЂљР Вµ
    val today = remember { LocalDate.now() }
    var selectedYear by remember { mutableIntStateOf(today.year) }
    var selectedMonth by remember { mutableIntStateOf(today.monthValue) }
    var draftPeriod by remember { mutableStateOf(YearMonth.of(today.year, today.monthValue)) }
    var showPeriodSheet by remember { mutableStateOf(false) }
    var objectsFilter by remember { mutableStateOf(ObjectsFilter.ALL) }

    // Р вЂ™РЎвЂ№Р В±Р С•РЎР‚ Р Р…Р С•Р Р†Р С•Р С–Р С• Р С—Р ВµРЎР‚Р С‘Р С•Р Т‘Р В° Р Р† Р С”Р В°Р В»Р ВµР Р…Р Т‘Р В°РЎР‚Р Вµ Р С”Р В°РЎР‚РЎвЂљР С•РЎвЂЎР С”Р С‘ (РЎвЂЎРЎвЂРЎР‚Р Р…РЎвЂ№Р Вµ РЎРЏРЎвЂЎР ВµР в„–Р С”Р С‘) Р С‘ Р С—Р В°Р Р…Р ВµР В»РЎРЉ card_inf
    var selectionPropertyId by remember { mutableStateOf<String?>(null) }
    var selectionStart by remember { mutableStateOf<LocalDate?>(null) }
    var selectionEnd by remember { mutableStateOf<LocalDate?>(null) }
    var infoPropertyId by remember { mutableStateOf<String?>(null) }
    var infoRange by remember { mutableStateOf<ClosedRange<LocalDate>?>(null) }

    // Р вЂќР С•Р В»Р С–Р С•Р Вµ Р Р…Р В°Р В¶Р В°РЎвЂљР С‘Р Вµ Р Р…Р В° Р С”РЎР‚Р В°РЎРѓР Р…Р С•Р в„– РЎРЏРЎвЂЎР ВµР в„–Р С”Р Вµ РІР‚вЂќ РЎР‚РЎС“РЎвЂЎР Р…Р С•Р Вµ Р С–Р В°РЎв‚¬Р ВµР Р…Р С‘Р Вµ Р С—РЎР‚Р С•РЎРѓРЎР‚Р С•РЎвЂЎР С”Р С‘
    var confirmPaidPropertyId by remember { mutableStateOf<String?>(null) }

    // Р В¤Р С‘Р В»РЎРЉРЎвЂљРЎР‚ Р’В«Р вЂ™РЎРѓР Вµ Р С•Р В±РЎР‰Р ВµР С”РЎвЂљРЎвЂ№ / Р СџР С•РЎРѓРЎС“РЎвЂљР С•РЎвЂЎР Р…Р С• / Р СџР С•Р СР ВµРЎРѓРЎРЏРЎвЂЎР Р…Р С•Р’В» Р С‘Р В· Р С—Р С‘Р В»РЎР‹Р В»Р С‘ CalendarToolbar
    val properties = remember(propertiesRaw, objectsFilter) {
        propertiesRaw.filter { objectsFilter.matches(it.rentType) }
    }

    // Р СџРЎР‚Р С•Р С–РЎР‚Р ВµР Р† Р С”РЎРЊРЎв‚¬Р В° РЎвЂћР С•РЎвЂљР С• 40dp РІР‚вЂќ Р С”Р В°РЎР‚РЎвЂљР С•РЎвЂЎР С”Р С‘ Р Р…Р Вµ Р СР С‘Р С–Р В°РЎР‹РЎвЂљ Р С—РЎС“РЎРѓРЎвЂљРЎвЂ№Р С Р С”Р Р†Р В°Р Т‘РЎР‚Р В°РЎвЂљР С•Р С Р С—РЎР‚Р С‘ РЎРѓР С”РЎР‚Р С•Р В»Р В»Р Вµ
    val prefetchContext = LocalContext.current
    val prefetchDensity = LocalDensity.current
    LaunchedEffect(properties) {
        val photoSizePx = with(prefetchDensity) { 40.dp.roundToPx() }
        properties.forEach { property ->
            property.photoUrl?.let { url ->
                prefetchContext.imageLoader.enqueue(
                    ImageRequest.Builder(prefetchContext)
                        .data(url)
                        .size(photoSizePx)
                        .build()
                )
            }
        }
    }

    /** Р вЂњРЎР‚Р В°Р Р…Р С‘РЎвЂ РЎвЂ№ Р С—Р ВµРЎР‚Р С‘Р С•Р Т‘Р В° РЎвЂљР С•РЎвЂЎР С”Р С‘: Р С—Р С•РЎРѓРЎС“РЎвЂљР С•РЎвЂЎР Р…Р С• РІР‚вЂќ Р С•Р Т‘Р С‘Р Р… Р Т‘Р ВµР Р…РЎРЉ, Р С—Р С•Р СР ВµРЎРѓРЎРЏРЎвЂЎР Р…Р С• РІР‚вЂќ Р Р†Р ВµРЎРѓРЎРЉ Р СР ВµРЎРѓРЎРЏРЎвЂ . */
    fun periodBounds(property: MyPropertyItem, date: LocalDate): ClosedRange<LocalDate> {
        if (property.rentType == "Р С—Р С•РЎРѓРЎС“РЎвЂљР С•РЎвЂЎР Р…Р С•") return date..date
        val month = YearMonth.from(date)
        return month.atDay(1)..month.atEndOfMonth()
    }

    /**
     * Р СћР В°Р С— Р С—Р С• Р Т‘Р В°РЎвЂљР Вµ: РЎРѓР Р†Р С•Р В±Р С•Р Т‘Р Р…Р В°РЎРЏ РІР‚вЂќ Р Р…Р В°РЎвЂЎР В°Р В»Р С•/Р С”Р С•Р Р…Р ВµРЎвЂ  Р Р…Р С•Р Р†Р С•Р С–Р С• Р С—Р ВµРЎР‚Р С‘Р С•Р Т‘Р В° (Р Р†РЎвЂљР С•РЎР‚Р С•Р в„– РЎвЂљР В°Р С— РЎРѓРЎР‚Р В°Р В·РЎС“
     * Р С—РЎР‚Р С‘Р СР ВµР Р…РЎРЏР ВµРЎвЂљ Р С—Р С•Р С”РЎР‚Р В°РЎРѓР С”РЎС“), Р В·Р В°Р Р…РЎРЏРЎвЂљР В°РЎРЏ РІР‚вЂќ Р С—Р В°Р Р…Р ВµР В»РЎРЉ `card_inf` РЎРѓ Р В°РЎР‚Р ВµР Р…Р Т‘Р В°РЎвЂљР С•РЎР‚Р С•Р С Р С‘ Р Т‘Р С•Р В»Р С–Р С•Р С.
     */
    fun onCalendarDayTap(property: MyPropertyItem, date: LocalDate) {
        if (property.calStateAt(date) != CalState.FREE) {
            val range = property.bookings
                .firstOrNull { !date.isBefore(it.start) && !date.isAfter(it.end) }
            infoPropertyId = property.id
            infoRange = range?.let { it.start..it.end }
            return
        }
        val start = selectionStart
        if (selectionPropertyId != property.id || start == null) {
            selectionPropertyId = property.id
            selectionStart = periodBounds(property, date).start
            selectionEnd = null
            infoPropertyId = null
            infoRange = null
            return
        }
        val bounds = periodBounds(property, date)
        val lo = minOf(start, bounds.start)
        val hi = maxOf(start, bounds.endInclusive)
        selectionEnd = hi
        // Р СџР С•Р В»Р Р…Р С•РЎРѓРЎвЂљРЎРЉРЎР‹ Р В·Р В°Р Р…РЎРЏРЎвЂљРЎвЂ№Р в„– Р Т‘Р С‘Р В°Р С—Р В°Р В·Р С•Р Р… РІР‚вЂќ РЎРѓР Р…РЎРЏРЎвЂљР С‘Р Вµ Р С—Р С•Р С”РЎР‚Р В°РЎРѓР С”Р С‘, Р С‘Р Р…Р В°РЎвЂЎР Вµ Р Р…Р С•Р Р†Р В°РЎРЏ Р В°РЎР‚Р ВµР Р…Р Т‘Р В°
        val fullyBusy = !lo.isAfter(hi) && generateSequence(lo) { it.plusDays(1) }
            .takeWhile { !it.isAfter(hi) }
            .all { property.calStateAt(it) != CalState.FREE }
        if (fullyBusy) viewModel.deleteBookings(property.id, lo, hi)
        else viewModel.saveBooking(property.id, lo, hi)
        viewModel.clearAttachSuggestion()
    }

    /** Р вЂќР С•Р В»Р С–Р С‘Р в„– РЎвЂљР В°Р С—: Р С—РЎР‚Р С•РЎРѓРЎР‚Р С•РЎвЂЎР С”Р В° РІР‚вЂќ Р С–Р В°РЎв‚¬Р ВµР Р…Р С‘Р Вµ, Р В·Р В°Р Р…РЎРЏРЎвЂљР В°РЎРЏ Р Т‘Р В°РЎвЂљР В° РІР‚вЂќ Р Р…Р С•Р Р†РЎвЂ№Р в„– Р С—Р ВµРЎР‚Р С‘Р С•Р Т‘ РЎРѓ РЎРЊРЎвЂљР С•Р в„– Р Т‘Р В°РЎвЂљРЎвЂ№. */
    fun onCalendarDayLongTap(property: MyPropertyItem, date: LocalDate) {
        when (property.calStateAt(date)) {
            CalState.OVERDUE -> confirmPaidPropertyId = property.id
            CalState.BUSY, CalState.PLANNED -> onAttachTenant(property.id, date, null)
            else -> Unit
        }
    }

    Scaffold(
        containerColor = White,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                UndoSnackbar(
                    message = data.visuals.message,
                    onUndo = {
                        data.dismiss()
                        viewModel.undoLastEdit()
                    }
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(White)
        ) {
            Spacer(Modifier.height(27.dp))
            // ---- Р СћРЎС“Р В»Р В±Р В°РЎР‚: Р Р…Р В°Р В·Р В°Р Т‘ + Р В·Р В°Р С–Р С•Р В»Р С•Р Р†Р С•Р С” + РЎРѓР С•РЎР‚РЎвЂљР С‘РЎР‚Р С•Р Р†Р С”Р В° ----
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
                        painter = painterResource(R.drawable.ic_arrow_left),
                        contentDescription = "Р СњР В°Р В·Р В°Р Т‘",
                        modifier = Modifier.size(24.dp),
                        tint = TextPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Р СљР С•РЎРЏ Р Р…Р ВµР Т‘Р Р†Р С‘Р В¶Р С‘Р СР С•РЎРѓРЎвЂљРЎРЉ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                        color = TextPrimary
                    )
                }
                // Р С™Р В°Р Р…Р Р†Р В°РЎРѓ 19: Р Р† РЎвЂљРЎС“Р В»Р В±Р В°РЎР‚Р Вµ Р Т‘Р Р†Р Вµ Р С‘Р С”Р С•Р Р…Р С”Р С‘ 24Р“вЂ”24 РЎРѓ Р В·Р В°Р В·Р С•РЎР‚Р С•Р С 8 РІР‚вЂќ Р С—Р С•Р С‘РЎРѓР С” Р С‘ РЎРѓР С—РЎР‚Р В°Р Р†Р С”Р В°
                Icon(
                    painter = painterResource(R.drawable.ic_myprops_search),
                    contentDescription = "Р СџР С•Р С‘РЎРѓР С”",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { showObjectsFilter = true },
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_myprops_help),
                    contentDescription = "Р С™Р В°Р С” РЎС“РЎРѓРЎвЂљРЎР‚Р С•Р ВµР Р… Р С”Р В°Р В»Р ВµР Р…Р Т‘Р В°РЎР‚РЎРЉ",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { showChessHelp = true },
                    tint = Color.Unspecified
                )
            }

            // ---- Р СџР В°Р Р…Р ВµР В»РЎРЉ Р С”Р В°Р В»Р ВµР Р…Р Т‘Р В°РЎР‚РЎРЏ: Р’В«Р вЂ™РЎвЂ№Р В±РЎР‚Р В°РЎвЂљРЎРЉ Р С—Р ВµРЎР‚Р С‘Р С•Р Т‘Р’В» + РЎвЂћР С‘Р В»РЎРЉРЎвЂљРЎР‚ Р С•Р В±РЎР‰Р ВµР С”РЎвЂљР С•Р Р† (Р С”Р В°Р Р…Р Р†Р В°РЎРѓ 19) ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                CalendarToolbar(
                    filter = objectsFilter,
                    onPeriodClick = {
                        draftPeriod = YearMonth.of(selectedYear, selectedMonth)
                        showPeriodSheet = true
                    },
                    onFilterClick = { showObjectsFilter = true }
                )
            }

            if (properties.isEmpty()) {
                Spacer(Modifier.height(24.dp))
                PropertiesStub(
                    filter = objectsFilter,
                    hasProperties = propertiesRaw.isNotEmpty(),
                    onShowAll = { objectsFilter = ObjectsFilter.ALL },
                    onCreateProperty = onCreateProperty,
                    onManageSubscription = onFinanceClick
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 6.dp,
                            bottom = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(properties, key = { it.id }) { property ->
                            val start = selectionStart
                            val selection = if (selectionPropertyId == property.id && start != null) {
                                start..(selectionEnd ?: start)
                            } else null
                            CalendarObjectCard(
                                property = property,
                                anchor = YearMonth.of(selectedYear, selectedMonth),
                                selection = selection,
                                onDayClick = { date -> onCalendarDayTap(property, date) },
                                onDayLongClick = { date -> onCalendarDayLongTap(property, date) },
                                onOpenProperty = { onPropertyClick(property.id) }
                            )
                        }
                    }

                    // Р СњР С‘Р В¶Р Р…Р С‘Р Вµ Р С—Р В°Р Р…Р ВµР В»Р С‘ Р С—Р С•Р Р†Р ВµРЎР‚РЎвЂ¦ РЎРѓР С—Р С‘РЎРѓР С”Р В°: card_inf Р С—РЎР‚Р С‘Р С•РЎР‚Р С‘РЎвЂљР ВµРЎвЂљР Р…Р ВµР Вµ Р Р†РЎвЂ№Р В±Р С•РЎР‚Р В°
                    val infoProperty = infoPropertyId?.let { id -> properties.find { it.id == id } }
                    val infoDates = infoRange
                    val selProperty = selectionPropertyId?.let { id -> properties.find { it.id == id } }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 13.dp)
                    ) {
                        when {
                            infoProperty != null && infoDates != null -> PeriodInfoPanel(
                                periodText = formatCalendarPeriod(
                                    infoDates.start,
                                    infoDates.endInclusive,
                                    infoProperty.rentType == "Р С—Р С•РЎРѓРЎС“РЎвЂљР С•РЎвЂЎР Р…Р С•"
                                ),
                                overdueText = if (infoProperty.overdue && infoProperty.overdueAmount > 0.0) {
                                    "Р вЂ”Р В°Р Т‘Р С•Р В»Р В¶Р ВµР Р…Р Р…Р С•РЎРѓРЎвЂљРЎРЉ " + String.format(
                                        java.util.Locale.US,
                                        "%,.0f РІвЂљР…",
                                        infoProperty.overdueAmount
                                    ).replace(',', ' ')
                                } else null,
                                tenantName = infoProperty.tenantInfo?.substringBefore(" Р’В·"),
                                onClose = {
                                    infoPropertyId = null
                                    infoRange = null
                                },
                                onEmail = { onWriteClick() },
                                onCall = { }
                            )

                            selProperty != null -> SelectionPanel(
                                propertyName = propertyTitle(selProperty.name, selProperty.address),
                                periodText = formatSelectionPeriod(
                                    selectionStart,
                                    selectionEnd,
                                    selProperty.rentType == "Р С—Р С•РЎРѓРЎС“РЎвЂљР С•РЎвЂЎР Р…Р С•"
                                ),
                                periodComplete = selectionEnd != null,
                                onAddTenant = {
                                    val startDate = selectionStart
                                    val endDate = selectionEnd ?: selectionStart
                                    selectionPropertyId = null
                                    selectionStart = null
                                    selectionEnd = null
                                    if (startDate != null && endDate != null) {
                                        onAttachTenant(selProperty.id, startDate, endDate)
                                    }
                                },
                                onCancel = {
                                    selectionPropertyId = null
                                    selectionStart = null
                                    selectionEnd = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Р С›РЎв‚¬Р С‘Р В±Р С”Р В° Р В·Р В°Р С–РЎР‚РЎС“Р В·Р С”Р С‘ РІР‚вЂќ Р С•Р С”Р Р…Р С•Р С-РЎС“Р Р†Р ВµР Т‘Р С•Р СР В»Р ВµР Р…Р С‘Р ВµР С, Р В·Р В°Р С”РЎР‚РЎвЂ№РЎвЂљР С‘Р Вµ РЎвЂљР В°Р С—Р С•Р С Р Р†Р Р…Р Вµ
    errorMessage?.let {
        IconNotificationDialog(
            iconRes = R.drawable.ic_globe_warning_vec,
            text = it,
            onDismiss = { viewModel.clearError() }
        )
    }

    // Р В РЎС“РЎвЂЎР Р…Р С•Р Вµ Р С–Р В°РЎв‚¬Р ВµР Р…Р С‘Р Вµ Р С—РЎР‚Р С•РЎРѓРЎР‚Р С•РЎвЂЎР С”Р С‘ (Р Т‘Р С•Р В»Р С–Р С•Р Вµ Р Р…Р В°Р В¶Р В°РЎвЂљР С‘Р Вµ Р Р…Р В° Р С”РЎР‚Р В°РЎРѓР Р…Р С•Р в„– РЎРЏРЎвЂЎР ВµР в„–Р С”Р Вµ РЎв‚¬Р В°РЎвЂ¦Р СР В°РЎвЂљР С”Р С‘)
    confirmPaidPropertyId?.let { id ->
        val item = properties.find { it.id == id }
        if (item != null) {
            ScheduleDialog(
                iconRes = null,
                title = "Р С›РЎвЂљР СР ВµРЎвЂљР С‘РЎвЂљРЎРЉ Р С—Р В»Р В°РЎвЂљРЎвЂР В¶ Р С—Р С•Р В»РЎС“РЎвЂЎР ВµР Р…Р Р…РЎвЂ№Р С?",
                body = "Р СџР В»Р В°РЎвЂљРЎвЂР В¶ " + String.format(java.util.Locale.US, "%,.0f РІвЂљР…", item.overdueAmount).replace(',', ' ') +
                    " Р В±РЎС“Р Т‘Р ВµРЎвЂљ Р С•РЎвЂљР СР ВµРЎвЂЎР ВµР Р… Р С”Р В°Р С” Р С—Р С•Р В»РЎС“РЎвЂЎР ВµР Р…Р Р…РЎвЂ№Р в„– РЎРѓР ВµР С–Р С•Р Т‘Р Р…РЎРЏ РІР‚вЂќ Р Р…Р В°Р С—РЎР‚Р С‘Р СР ВµРЎР‚, Р С•Р С—Р В»Р В°РЎвЂЎР ВµР Р…Р Р…РЎвЂ№Р в„– Р Р…Р В°Р В»Р С‘РЎвЂЎР Р…РЎвЂ№Р СР С‘",
                confirmText = "Р С›РЎвЂљР СР ВµРЎвЂљР С‘РЎвЂљРЎРЉ Р С•Р С—Р В»Р В°РЎвЂљРЎС“",
                onConfirm = {
                    confirmPaidPropertyId = null
                    viewModel.markOverduePaid(id)
                },
                onDismiss = { confirmPaidPropertyId = null }
            )
        }
    }

    if (showPeriodSheet) {
        CalendarPeriodSheet(
            draft = draftPeriod,
            onDraftChange = { draftPeriod = it },
            onApply = {
                selectedYear = draftPeriod.year
                selectedMonth = draftPeriod.monthValue
                showPeriodSheet = false
            },
            onCurrentMonth = {
                val now = YearMonth.now()
                selectedYear = now.year
                selectedMonth = now.monthValue
                draftPeriod = now
                showPeriodSheet = false
            },
            onDismiss = { showPeriodSheet = false }
        )
    }

    // Р РЃР С‘РЎвЂљ Р’В«Р СџР С•Р С”Р В°Р В·Р В°РЎвЂљРЎРЉ Р С•Р В±РЎР‰Р ВµР С”РЎвЂљРЎвЂ№Р’В» РІР‚вЂќ РЎвЂћР С‘Р В»РЎРЉРЎвЂљРЎР‚ РЎРѓР С—Р С‘РЎРѓР С”Р В° (Р С—РЎР‚Р С‘Р СР ВµР Р…РЎРЏР ВµРЎвЂљРЎРѓРЎРЏ РЎРѓРЎР‚Р В°Р В·РЎС“ Р С—Р С•РЎРѓР В»Р Вµ Р Р†РЎвЂ№Р В±Р С•РЎР‚Р В°)
    if (showObjectsFilter) {
        ObjectsFilterSheet(
            selected = objectsFilter,
            onSelect = { filter ->
                objectsFilter = filter
                showObjectsFilter = false
            },
            onDismiss = { showObjectsFilter = false }
        )
    }

    // Р РЃР С‘РЎвЂљ Р’В«Р С™Р В°Р С” РЎС“РЎРѓРЎвЂљРЎР‚Р С•Р ВµР Р… Р С”Р В°Р В»Р ВµР Р…Р Т‘Р В°РЎР‚РЎРЉР’В» РІР‚вЂќ Р В»Р ВµР С–Р ВµР Р…Р Т‘Р В° Р С‘Р В· 5 РЎРѓРЎвЂљРЎР‚Р С•Р С” Р С‘ Р С—Р С•Р Т‘РЎРѓР С”Р В°Р В·Р С”Р С‘
    if (showChessHelp) {
        CalendarLegendSheet(onDismiss = { showChessHelp = false })
    }
}

// ===================== Р С›Р В±РЎвЂ°Р С‘Р Вµ РЎв‚¬Р С‘РЎвЂљРЎвЂ№ =====================

/** Р РЋР Р…Р ВµР С”Р В±Р В°РЎР‚ Р С—РЎР‚Р В°Р Р†Р С”Р С‘ РЎв‚¬Р В°РЎвЂ¦Р СР В°РЎвЂљР С”Р С‘: РЎвЂљРЎвЂР СР Р…Р В°РЎРЏ Р С—Р В»Р В°РЎв‚¬Р С”Р В° + Р’В«Р С›РЎвЂљР СР ВµР Р…Р С‘РЎвЂљРЎРЉР’В» (РЎРѓР Р†Р С•РЎРЏ Р Р†РЎвЂРЎР‚РЎРѓРЎвЂљР С”Р В°). */
@Composable
private fun UndoSnackbar(message: String, onUndo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TextPrimary)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            message,
            fontSize = 13.sp,
            letterSpacing = (-0.4).sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "Р С›РЎвЂљР СР ВµР Р…Р С‘РЎвЂљРЎРЉ",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
            color = Color.White,
            modifier = Modifier
                .clickable { onUndo() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
