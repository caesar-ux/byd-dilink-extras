package com.byd.dilink.extras.fuelcost.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.byd.dilink.extras.data.dao.ChargeRecord
import com.byd.dilink.extras.data.dao.FuelRecord
import com.byd.dilink.extras.data.dao.OdometerEntry
import com.byd.dilink.extras.data.preferences.FuelCostPrefsKeys
import com.byd.dilink.extras.data.preferences.fuelCostPrefs
import com.byd.dilink.extras.data.repository.CostPerKmResult
import com.byd.dilink.extras.data.repository.FuelCostRepository
import com.byd.dilink.extras.data.repository.LifetimeTotals
import com.byd.dilink.extras.data.repository.MonthlyBreakdown
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FuelCostUiState(
    val costPerKm: CostPerKmResult = CostPerKmResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
    val evPercentage: Double = 0.0,
    val monthlySavings: Double = 0.0,
    val currentOdometer: Int = 0,
    val recentFuelRecords: List<FuelRecord> = emptyList(),
    val recentChargeRecords: List<ChargeRecord> = emptyList(),
    val allFuelRecords: List<FuelRecord> = emptyList(),
    val allChargeRecords: List<ChargeRecord> = emptyList(),
    val lifetimeTotals: LifetimeTotals = LifetimeTotals(0.0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0),
    val monthlyBreakdowns: List<MonthlyBreakdown> = emptyList(),
    // Settings
    val batteryCapacityKwh: Double = 18.3,
    val defaultFuelPriceIqd: Double = 750.0,
    val defaultElectricityPriceIqd: Double = 100.0,
    val benchmarkLPer100Km: Double = 7.0,
    val useUsd: Boolean = false,
    val usdExchangeRate: Double = 1460.0
)

@HiltViewModel
class FuelCostViewModel @Inject constructor(
    application: Application,
    private val repository: FuelCostRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FuelCostUiState())
    val uiState: StateFlow<FuelCostUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
        collectData()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            getApplication<Application>().fuelCostPrefs.data.collect { prefs ->
                _uiState.update { state ->
                    state.copy(
                        batteryCapacityKwh = prefs[FuelCostPrefsKeys.BATTERY_CAPACITY_KWH] ?: 18.3,
                        defaultFuelPriceIqd = prefs[FuelCostPrefsKeys.DEFAULT_FUEL_PRICE_IQD] ?: 750.0,
                        defaultElectricityPriceIqd = prefs[FuelCostPrefsKeys.DEFAULT_ELECTRICITY_PRICE_IQD] ?: 100.0,
                        benchmarkLPer100Km = prefs[FuelCostPrefsKeys.BENCHMARK_L_PER_100KM] ?: 7.0,
                        useUsd = prefs[FuelCostPrefsKeys.USE_USD] ?: false,
                        usdExchangeRate = prefs[FuelCostPrefsKeys.USD_EXCHANGE_RATE] ?: 1460.0
                    )
                }
                refreshCalculations()
            }
        }
    }

    private fun collectData() {
        viewModelScope.launch {
            repository.getRecentFuel(5).collect { records ->
                _uiState.update { it.copy(recentFuelRecords = records) }
            }
        }
        viewModelScope.launch {
            repository.getRecentCharges(5).collect { records ->
                _uiState.update { it.copy(recentChargeRecords = records) }
            }
        }
        viewModelScope.launch {
            repository.getAllFuelRecords().collect { records ->
                _uiState.update { it.copy(allFuelRecords = records) }
            }
        }
        viewModelScope.launch {
            repository.getAllChargeRecords().collect { records ->
                _uiState.update { it.copy(allChargeRecords = records) }
            }
        }
        viewModelScope.launch {
            val odo = repository.getLatestOdometer()
            _uiState.update { it.copy(currentOdometer = odo?.odometerKm ?: 0) }
            refreshCalculations()
        }
    }

    fun refreshCalculations() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val costPerKm = repository.calculateCostPerKm()
                val totalKm = costPerKm.fuelKm + costPerKm.electricKm
                val evPct = if (totalKm > 0) costPerKm.electricKm / totalKm * 100.0 else 0.0

                val savings = repository.monthlySavings(
                    state.benchmarkLPer100Km,
                    state.defaultFuelPriceIqd
                )

                val lifetime = repository.lifetimeTotals(
                    state.benchmarkLPer100Km,
                    state.defaultFuelPriceIqd
                )

                val monthly = repository.monthlyBreakdown(
                    state.benchmarkLPer100Km,
                    state.defaultFuelPriceIqd
                )

                _uiState.update {
                    it.copy(
                        costPerKm = costPerKm,
                        evPercentage = evPct,
                        monthlySavings = savings.savings,
                        lifetimeTotals = lifetime,
                        monthlyBreakdowns = monthly
                    )
                }
            } catch (_: Exception) { }
        }
    }

    fun insertFuelRecord(record: FuelRecord) {
        viewModelScope.launch {
            repository.insertFuel(record)
            refreshCalculations()
        }
    }

    fun insertChargeRecord(record: ChargeRecord) {
        viewModelScope.launch {
            repository.insertCharge(record)
            refreshCalculations()
        }
    }

    fun updateOdometer(km: Int) {
        viewModelScope.launch {
            repository.insertOdometer(OdometerEntry(date = System.currentTimeMillis(), odometerKm = km))
            _uiState.update { it.copy(currentOdometer = km) }
            refreshCalculations()
        }
    }

    fun deleteFuelRecord(record: FuelRecord) {
        viewModelScope.launch {
            repository.deleteFuel(record)
            refreshCalculations()
        }
    }

    fun deleteChargeRecord(record: ChargeRecord) {
        viewModelScope.launch {
            repository.deleteCharge(record)
            refreshCalculations()
        }
    }

    // Settings
    fun updateBatteryCapacity(kwh: Double) {
        viewModelScope.launch {
            getApplication<Application>().fuelCostPrefs.edit { prefs ->
                prefs[FuelCostPrefsKeys.BATTERY_CAPACITY_KWH] = kwh
            }
        }
    }

    fun updateDefaultFuelPrice(price: Double) {
        viewModelScope.launch {
            getApplication<Application>().fuelCostPrefs.edit { prefs ->
                prefs[FuelCostPrefsKeys.DEFAULT_FUEL_PRICE_IQD] = price
            }
        }
    }

    fun updateDefaultElectricityPrice(price: Double) {
        viewModelScope.launch {
            getApplication<Application>().fuelCostPrefs.edit { prefs ->
                prefs[FuelCostPrefsKeys.DEFAULT_ELECTRICITY_PRICE_IQD] = price
            }
        }
    }

    fun updateBenchmark(lPer100Km: Double) {
        viewModelScope.launch {
            getApplication<Application>().fuelCostPrefs.edit { prefs ->
                prefs[FuelCostPrefsKeys.BENCHMARK_L_PER_100KM] = lPer100Km
            }
        }
    }

    fun updateUseUsd(useUsd: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().fuelCostPrefs.edit { prefs ->
                prefs[FuelCostPrefsKeys.USE_USD] = useUsd
            }
        }
    }

    fun updateExchangeRate(rate: Double) {
        viewModelScope.launch {
            getApplication<Application>().fuelCostPrefs.edit { prefs ->
                prefs[FuelCostPrefsKeys.USD_EXCHANGE_RATE] = rate
            }
        }
    }

    fun formatCurrency(amountIqd: Double): String {
        val state = _uiState.value
        return if (state.useUsd && state.usdExchangeRate > 0) {
            String.format("$%.2f", amountIqd / state.usdExchangeRate)
        } else {
            String.format("%,.0f IQD", amountIqd)
        }
    }

    fun currencyLabel(): String {
        return if (_uiState.value.useUsd) "USD" else "IQD"
    }
}
