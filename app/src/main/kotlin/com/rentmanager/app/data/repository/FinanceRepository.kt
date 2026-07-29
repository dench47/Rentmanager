package com.rentmanager.app.data.repository

import com.rentmanager.app.data.api.FinanceApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FinanceRepository @Inject constructor(
    private val financeApi: FinanceApi
) {
    suspend fun getReport(propertyId: String? = null, from: String? = null, to: String? = null) =
        financeApi.getReport(propertyId, from, to)
}