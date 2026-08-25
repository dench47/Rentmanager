package com.rentmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/**
 * Карта OpenStreetMap (osmdroid) с одной меткой.
 * Бесплатно, без API-ключей. Требует атрибуции © OpenStreetMap contributors.
 *
 * markerIconRes — кастомная иконка маркера (например, пин из макета); null — системная.
 */
@Composable
fun AddressMapPicker(
    latitude: Double,
    longitude: Double,
    onLocationSelected: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
    markerIconRes: Int? = null
) {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isFocusable = false
            isFocusableInTouchMode = false
            controller.setZoom(16.0)
        }
    }
    val marker = remember(markerIconRes) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Объект"
            markerIconRes?.let { resId ->
                icon = ContextCompat.getDrawable(context, resId)
            }
        }
    }

    // Тап по карте → перемещаем метку (обратный геокодинг делает вызывающая сторона)
    val currentOnLocationSelected by rememberUpdatedState(onLocationSelected)
    DisposableEffect(mapView) {
        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let { currentOnLocationSelected(it.latitude, it.longitude) }
                return false
            }
            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
        mapView.overlayManager.add(eventsOverlay)
        onDispose {
            mapView.overlayManager.remove(eventsOverlay)
        }
    }

    Box(modifier = modifier.clip(RoundedCornerShape(12.dp))) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val point = GeoPoint(latitude, longitude)
                view.controller.setCenter(point)
                marker.position = point
                if (marker !in view.overlays) {
                    view.overlayManager.add(marker)
                }
                view.invalidate()
            }
        )

        // Обязательная атрибуция OpenStreetMap (ODbL)
        Text(
            text = "© OpenStreetMap contributors",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.75f))
                .padding(horizontal = 5.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = Color(0xFF444444)
        )
    }
}
