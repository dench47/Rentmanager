package com.rentmanager.app.data.repository

import com.rentmanager.app.data.api.AddPhotoRequest
import com.rentmanager.app.data.api.PropertyApi
import com.rentmanager.app.data.model.MeterDto
import com.rentmanager.app.data.model.PropertyDto
import com.rentmanager.app.data.model.SubmitReadingRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PropertyRepository @Inject constructor(
    private val propertyApi: PropertyApi
) {
    suspend fun getProperties() = propertyApi.getProperties()
    suspend fun getProperty(id: String) = propertyApi.getProperty(id)
    suspend fun createProperty(property: PropertyDto) = propertyApi.createProperty(property)
    suspend fun updateProperty(id: String, property: PropertyDto) = propertyApi.updateProperty(id, property)
    suspend fun deleteProperty(id: String) = propertyApi.deleteProperty(id)
    suspend fun detachTenant(id: String) = propertyApi.detachTenant(id)
    suspend fun publishProperty(id: String) = propertyApi.publishProperty(id)
    suspend fun unpublishProperty(id: String) = propertyApi.unpublishProperty(id)
    suspend fun addPhoto(id: String, url: String) = propertyApi.addPhoto(id, AddPhotoRequest(url))
    suspend fun deletePhoto(photoId: String) = propertyApi.deletePhoto(photoId)
    suspend fun getMeters(id: String) = propertyApi.getMeters(id)
    suspend fun createMeter(id: String, meter: MeterDto) = propertyApi.createMeter(id, meter)
    suspend fun updateMeter(meterId: String, meter: MeterDto) = propertyApi.updateMeter(meterId, meter)
    suspend fun deleteMeter(meterId: String) = propertyApi.deleteMeter(meterId)
    suspend fun listReadings(meterId: String) = propertyApi.listReadings(meterId)
    suspend fun submitReading(meterId: String, value: Double) =
        propertyApi.submitReading(meterId, SubmitReadingRequest(value))
}