package com.rentmanager.app.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * ModalBottomSheet с пробросом дизайнерской плотности внутрь.
 * Шит открывается в отдельном окне Dialog, куда переопределение LocalDensity
 * из темы не доходит — плотность захватывается снаружи (уже масштабированная
 * под дизайн-ширину 412dp) и вручную предоставляется контенту, чтобы шит
 * масштабировался вместе со всем приложением.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaledModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    containerColor: Color = Color.White,
    dragHandle: (@Composable () -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit
) {
    val designDensity = LocalDensity.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        shape = shape,
        containerColor = containerColor,
        dragHandle = dragHandle
    ) {
        CompositionLocalProvider(LocalDensity provides designDensity) {
            content()
        }
    }
}
