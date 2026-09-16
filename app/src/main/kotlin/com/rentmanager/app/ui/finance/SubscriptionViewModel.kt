package com.rentmanager.app.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rentmanager.app.data.api.FinanceApi
import com.rentmanager.app.data.api.PromoApplyRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val balance: Double = 0.0,
    val objects: Int = 0,
    val baseRate: Double = 10.0,
    val rate: Double = 10.0,
    val dailyCharge: Double = 0.0,
    val appliedPromo: String? = null,
    val isLoading: Boolean = true,
    /** Промокод проверяется на сервере — кнопка «Проверяем» */
    val promoChecking: Boolean = false,
    /** Сервер не принял промокод — красная рамка и подсказка */
    val promoFailed: Boolean = false,
    /** Промокод применён в ТЕКУЩЕЙ сессии — блок промокода скрыт;
     *  серверный promo при новом входе тариф пересчитывает, но поле
     *  не подсвечивает */
    val promoAppliedThisSession: Boolean = false
)

/**
 * Подписка: баланс/тариф/промокод приходят с сервера; пополнение —
 * демо-эндпоинт (+30 ₽), реальная оплата через ЮKassa подключится позже.
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val financeApi: FinanceApi,
    private val subscriptionEvents: com.rentmanager.app.data.local.SubscriptionEvents
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        load()
        // Списание в фоне → сервер прислал subscription_changed → перечитываем
        viewModelScope.launch {
            subscriptionEvents.refreshTick.collect { if (it > 0) load() }
        }
    }

    fun load() {
        viewModelScope.launch {
            try {
                val body = financeApi.subscriptionState().body()
                if (body != null) {
                    _uiState.update {
                        it.copy(
                            balance = body.balance,
                            objects = body.objects,
                            baseRate = body.baseRate,
                            rate = body.rate,
                            dailyCharge = body.dailyCharge,
                            appliedPromo = body.promo?.code,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Демо-пополнение: сервер зачисляет 30 ₽; после — перечитываем
     *  состояние целиком (параллельный тик списания мог уже изменить баланс) */
    fun topUp() {
        viewModelScope.launch {
            try {
                financeApi.topUp()
            } catch (_: Exception) { }
            load()
        }
    }

    /** Правка ввода после ошибки — убираем красную рамку */
    fun resetPromoFailure() {
        _uiState.update { it.copy(promoFailed = false) }
    }

    /** Проверить и применить промокод на сервере */
    fun applyPromo(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(promoChecking = true, promoFailed = false) }
            try {
                val resp = financeApi.applyPromo(PromoApplyRequest(trimmed))
                if (resp.isSuccessful) {
                    // Перечитываем состояние: тариф/списание пересчитает сервер
                    load()
                    _uiState.update {
                        it.copy(promoChecking = false, promoAppliedThisSession = true)
                    }
                } else {
                    _uiState.update { it.copy(promoChecking = false, promoFailed = true) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(promoChecking = false, promoFailed = true) }
            }
        }
    }
}
