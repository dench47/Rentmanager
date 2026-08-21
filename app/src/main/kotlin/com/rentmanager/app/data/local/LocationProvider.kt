package com.rentmanager.app.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Отдаёт последнее известное местоположение устройства (для приоритизации подсказок). */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun lastKnownLocation(): Pair<Double, Double>? {
        return try {
            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
                return null
            }
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            location?.let { it.latitude to it.longitude }
        } catch (_: Exception) {
            null
        }
    }
}