package com.byd.dilink.extras.fuelcost.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FuelFormState(
    val odometer: String = "",
    val liters: String = "",
    val totalCost: String = "",
    val pricePerLiter: String = "750",
    val fuelType: String = "Regular",
    val isFullTank: Boolean = true,
    val stationName: String = "",
    val notes: String = ""
) {
    val isValid: Boolean get() =
        odometer.toIntOrNull() != null &&
                liters.toDoubleOrNull() != null && liters.toDoubleOrNull()!! > 0 &&
                totalCost.toDoubleOrNull() != null && totalCost.toDoubleOrNull()!! > 0
}

data class ChargeFormState(
    val odometer: String = "",
    val kwhCharged: String = "",
    val startSocPercent: String = "",
    val endSocPercent: String = "",
    val totalCost: String = "",
    val costPerKwh: String = "",
    val source: String = "Home",
    val durationMin: String = "",
    val notes: String = "",
    val batteryCapacityKwh: Double = 18.3
) {
    val isValid: Boolean get() =
        odometer.toIntOrNull() != null &&
                (kwhCharged.toDoubleOrNull() != null && kwhCharged.toDoubleOrNull()!! > 0 ||
                        startSocPercent.toIntOrNull() != null && endSocPercent.toIntOrNull() != null) &&
                totalCost.toDoubleOrNull() != null && totalCost.toDoubleOrNull()!! >= 0

    fun calculateKwh(): Double {
        val direct = kwhCharged.toDoubleOrNull()
        if (direct != null && direct > 0) return direct

        val startSoc = startSocPercent.toIntOrNull()
        val endSoc = endSocPercent.toIntOrNull()
        if (startSoc != null && endSoc != null && endSoc > startSoc) {
            return (endSoc - startSoc) / 100.0 * batteryCapacityKwh
        }
        return 0.0
    }
}

class LogFormViewModel : ViewModel() {
    private val _fuelForm = MutableStateFlow(FuelFormState())
    val fuelForm: StateFlow<FuelFormState> = _fuelForm.asStateFlow()

    private val _chargeForm = MutableStateFlow(ChargeFormState())
    val chargeForm: StateFlow<ChargeFormState> = _chargeForm.asStateFlow()

    // Fuel form
    fun updateFuelOdometer(value: String) {
        _fuelForm.update { it.copy(odometer = value) }
    }

    fun updateFuelLiters(value: String) {
        _fuelForm.update { form ->
            val liters = value.toDoubleOrNull()
            val price = form.pricePerLiter.toDoubleOrNull()
            val newCost = if (liters != null && price != null) {
                String.format("%.0f", liters * price)
            } else form.totalCost
            form.copy(liters = value, totalCost = newCost)
        }
    }

    fun updateFuelTotalCost(value: String) {
        _fuelForm.update { form ->
            val cost = value.toDoubleOrNull()
            val liters = form.liters.toDoubleOrNull()
            val newPrice = if (cost != null && liters != null && liters > 0) {
                String.format("%.0f", cost / liters)
            } else form.pricePerLiter
            form.copy(totalCost = value, pricePerLiter = newPrice)
        }
    }

    fun updateFuelPricePerLiter(value: String) {
        _fuelForm.update { form ->
            val price = value.toDoubleOrNull()
            val liters = form.liters.toDoubleOrNull()
            val newCost = if (price != null && liters != null) {
                String.format("%.0f", price * liters)
            } else form.totalCost
            form.copy(pricePerLiter = value, totalCost = newCost)
        }
    }

    fun updateFuelType(type: String) {
        _fuelForm.update { it.copy(fuelType = type) }
    }

    fun updateFullTank(isFullTank: Boolean) {
        _fuelForm.update { it.copy(isFullTank = isFullTank) }
    }

    fun updateFuelStation(station: String) {
        _fuelForm.update { it.copy(stationName = station) }
    }

    fun updateFuelNotes(notes: String) {
        _fuelForm.update { it.copy(notes = notes) }
    }

    fun resetFuelForm() {
        _fuelForm.value = FuelFormState()
    }

    // Charge form
    fun updateChargeOdometer(value: String) {
        _chargeForm.update { it.copy(odometer = value) }
    }

    fun updateChargeKwh(value: String) {
        _chargeForm.update { it.copy(kwhCharged = value) }
    }

    fun updateChargeStartSoc(value: String) {
        _chargeForm.update { form ->
            val newForm = form.copy(startSocPercent = value)
            val kwh = newForm.calculateKwh()
            if (kwh > 0 && form.kwhCharged.isEmpty()) {
                newForm.copy(kwhCharged = String.format("%.1f", kwh))
            } else newForm
        }
    }

    fun updateChargeEndSoc(value: String) {
        _chargeForm.update { form ->
            val newForm = form.copy(endSocPercent = value)
            val kwh = newForm.calculateKwh()
            if (kwh > 0) {
                newForm.copy(kwhCharged = String.format("%.1f", kwh))
            } else newForm
        }
    }

    fun updateChargeTotalCost(value: String) {
        _chargeForm.update { form ->
            val cost = value.toDoubleOrNull()
            val kwh = form.kwhCharged.toDoubleOrNull()
            val newCostPerKwh = if (cost != null && kwh != null && kwh > 0) {
                String.format("%.1f", cost / kwh)
            } else form.costPerKwh
            form.copy(totalCost = value, costPerKwh = newCostPerKwh)
        }
    }

    fun updateChargeCostPerKwh(value: String) {
        _chargeForm.update { form ->
            val cPerKwh = value.toDoubleOrNull()
            val kwh = form.kwhCharged.toDoubleOrNull()
            val newTotal = if (cPerKwh != null && kwh != null) {
                String.format("%.0f", cPerKwh * kwh)
            } else form.totalCost
            form.copy(costPerKwh = value, totalCost = newTotal)
        }
    }

    fun updateChargeSource(source: String) {
        _chargeForm.update { it.copy(source = source) }
    }

    fun updateChargeDuration(duration: String) {
        _chargeForm.update { it.copy(durationMin = duration) }
    }

    fun updateChargeNotes(notes: String) {
        _chargeForm.update { it.copy(notes = notes) }
    }

    fun setBatteryCapacity(kwh: Double) {
        _chargeForm.update { it.copy(batteryCapacityKwh = kwh) }
    }

    fun resetChargeForm() {
        val capacity = _chargeForm.value.batteryCapacityKwh
        _chargeForm.value = ChargeFormState(batteryCapacityKwh = capacity)
    }
}
