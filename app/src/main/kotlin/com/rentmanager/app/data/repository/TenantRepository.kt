package com.rentmanager.app.data.repository

import com.rentmanager.app.data.api.TenantApi
import com.rentmanager.app.data.model.TenantDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TenantRepository @Inject constructor(
    private val tenantApi: TenantApi
) {
    suspend fun getTenants() = tenantApi.getTenants()
    suspend fun getTenant(id: String) = tenantApi.getTenant(id)
    suspend fun createTenant(tenant: TenantDto) = tenantApi.createTenant(tenant)
    suspend fun deleteTenant(id: String) = tenantApi.deleteTenant(id)
}