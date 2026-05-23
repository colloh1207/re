package com.sdd.marketplace.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.domain.model.Coupon
import com.sdd.marketplace.domain.repository.CouponRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CouponUiState(
    val allCoupons: List<Coupon> = emptyList(),
    val validCoupons: List<Coupon> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CouponViewModel @Inject constructor(
    private val couponRepository: CouponRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CouponUiState())
    val uiState: StateFlow<CouponUiState> = _uiState.asStateFlow()

    init {
        loadCoupons()
    }

    private fun loadCoupons() = viewModelScope.launch {
        combine(
            couponRepository.getMyCoupons(),
            couponRepository.getValidCoupons()
        ) { all, valid -> Pair(all, valid) }.collect { (all, valid) ->
            _uiState.update { it.copy(allCoupons = all, validCoupons = valid, isLoading = false) }
        }
    }
}
