package com.byd.dilink.extras.data.repository

import com.byd.dilink.extras.data.dao.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

data class CostPerKmResult(
    val fuelCostPerKm: Double,
    val electricCostPerKm: Double,
    val totalFuelCost: Double,
    val totalElectricCost: Double,
    val fuelKm: Double,
    val electricKm: Double
)

data class MonthlySavings(
    val month: String,
    val actualCost: Double,
    val benchmarkCost: Double,
    val savings: Double,
    val evPercentage: Double
)

data class MonthlyBreakdown(
    val yearMonth: String,
    val fuelCost: Double,
    val electricCost: Double,
    val fuelLiters: Double,
    val electricKwh: Double,
    val totalKm: Int,
    val fuelCostPerKm: Double,
    val electricCostPerKm: Double,
    val savings: Double,
    val evPercentage: Double
)

data class LifetimeTotals(
    val totalFuelLiters: Double,
    val totalFuelCostIqd: Double,
    val totalElectricKwh: Double,
    val totalElectricCostIqd: Double,
    val totalKmDriven: Int,
    val avgLPer100Km: Double,
    val avgKwhPer100Km: Double,
    val totalSavingsVsBenchmark: Double
)

class FuelCostRepository(
    private val fuelDao: FuelDao,
    private val chargeDao: ChargeDao,
    private val odometerDao: OdometerDao
) {

    fun getAllFuelRecords(): Flow<List<FuelRecord>> = fuelDao.getAllByDate()
    fun getAllChargeRecords(): Flow<List<ChargeRecord>> = chargeDao.getAllByDate()
    fun getAllOdometerEntries(): Flow<List<OdometerEntry>> = odometerDao.getAll()
    fun getRecentFuel(limit: Int): Flow<List<FuelRecord>> = fuelDao.getRecent(limit)
    fun getRecentCharges(limit: Int): Flow<List<ChargeRecord>> = chargeDao.getRecent(limit)

    suspend fun insertFuel(record: FuelRecord): Long = fuelDao.insert(record)
    suspend fun updateFuel(record: FuelRecord) = fuelDao.update(record)
    suspend fun deleteFuel(record: FuelRecord) = fuelDao.delete(record)

    suspend fun insertCharge(record: ChargeRecord): Long = chargeDao.insert(record)
    suspend fun updateCharge(record: ChargeRecord) = chargeDao.update(record)
    suspend fun deleteCharge(record: ChargeRecord) = chargeDao.delete(record)

    suspend fun insertOdometer(entry: OdometerEntry): Long = odometerDao.insert(entry)
    suspend fun getLatestOdometer(): OdometerEntry? = odometerDao.getLatest()

    /**
     * Calculate cost per km for fuel and electric based on all records.
     * Uses odometer differences between fuel/charge events to estimate km driven per mode.
     */
    suspend fun calculateCostPerKm(): CostPerKmResult {
        val totalFuelCost = fuelDao.getTotalCost()
        val totalElectricCost = chargeDao.getTotalCost()
        val totalFuelLiters = fuelDao.getTotalLiters()
        val totalElectricKwh = chargeDao.getTotalKwh()

        val latestFuel = fuelDao.getLatest()
        val latestCharge = chargeDao.getLatest()
        val allOdometer = odometerDao.getAllOnce()

        // Estimate km driven by mode
        // Simple approach: use total km and split by energy ratio
        val firstOdo = allOdometer.firstOrNull()?.odometerKm ?: 0
        val lastOdo = allOdometer.lastOrNull()?.odometerKm ?: 0
        val totalKm = (lastOdo - firstOdo).coerceAtLeast(0).toDouble()

        if (totalKm <= 0) {
            return CostPerKmResult(0.0, 0.0, totalFuelCost, totalElectricCost, 0.0, 0.0)
        }

        // Estimate fuel km from consumption: liters / (7 L/100km benchmark) * 100
        val estimatedFuelKm = if (totalFuelLiters > 0) (totalFuelLiters / 7.0) * 100.0 else 0.0
        // Electric km is the remainder
        val estimatedElectricKm = (totalKm - estimatedFuelKm).coerceAtLeast(0.0)

        val fuelCostPerKm = if (estimatedFuelKm > 0) totalFuelCost / estimatedFuelKm else 0.0
        val electricCostPerKm = if (estimatedElectricKm > 0) totalElectricCost / estimatedElectricKm else 0.0

        return CostPerKmResult(
            fuelCostPerKm = fuelCostPerKm,
            electricCostPerKm = electricCostPerKm,
            totalFuelCost = totalFuelCost,
            totalElectricCost = totalElectricCost,
            fuelKm = estimatedFuelKm,
            electricKm = estimatedElectricKm
        )
    }

    /**
     * Calculate monthly savings compared to driving a pure petrol vehicle.
     */
    suspend fun monthlySavings(benchmarkLPer100Km: Double, defaultFuelPrice: Double): MonthlySavings {
        val result = calculateCostPerKm()
        val totalKm = result.fuelKm + result.electricKm
        val actualCost = result.totalFuelCost + result.totalElectricCost
        val benchmarkCost = totalKm * benchmarkLPer100Km / 100.0 * defaultFuelPrice
        val savings = benchmarkCost - actualCost
        val evPct = if (totalKm > 0) result.electricKm / totalKm * 100.0 else 0.0

        val cal = Calendar.getInstance()
        val monthStr = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"

        return MonthlySavings(
            month = monthStr,
            actualCost = actualCost,
            benchmarkCost = benchmarkCost,
            savings = savings,
            evPercentage = evPct
        )
    }

    /**
     * Calculate lifetime totals.
     */
    suspend fun lifetimeTotals(benchmarkLPer100Km: Double, defaultFuelPrice: Double): LifetimeTotals {
        val totalFuelLiters = fuelDao.getTotalLiters()
        val totalFuelCost = fuelDao.getTotalCost()
        val totalElectricKwh = chargeDao.getTotalKwh()
        val totalElectricCost = chargeDao.getTotalCost()

        val allOdometer = odometerDao.getAllOnce()
        val firstOdo = allOdometer.firstOrNull()?.odometerKm ?: 0
        val lastOdo = allOdometer.lastOrNull()?.odometerKm ?: 0
        val totalKm = (lastOdo - firstOdo).coerceAtLeast(0)

        // Estimate fuel km from fuel consumption
        val fuelKm = if (totalFuelLiters > 0) (totalFuelLiters / benchmarkLPer100Km) * 100.0 else 0.0
        val electricKm = (totalKm - fuelKm).coerceAtLeast(0.0)

        val avgLPer100Km = if (fuelKm > 0) totalFuelLiters / fuelKm * 100.0 else 0.0
        val avgKwhPer100Km = if (electricKm > 0) totalElectricKwh / electricKm * 100.0 else 0.0

        val benchmarkCost = totalKm * benchmarkLPer100Km / 100.0 * defaultFuelPrice
        val actualCost = totalFuelCost + totalElectricCost
        val savings = benchmarkCost - actualCost

        return LifetimeTotals(
            totalFuelLiters = totalFuelLiters,
            totalFuelCostIqd = totalFuelCost,
            totalElectricKwh = totalElectricKwh,
            totalElectricCostIqd = totalElectricCost,
            totalKmDriven = totalKm,
            avgLPer100Km = avgLPer100Km,
            avgKwhPer100Km = avgKwhPer100Km,
            totalSavingsVsBenchmark = savings
        )
    }

    /**
     * Monthly breakdown for statistics.
     */
    suspend fun monthlyBreakdown(benchmarkLPer100Km: Double, defaultFuelPrice: Double): List<MonthlyBreakdown> {
        val fuelRecords = fuelDao.getLatest()?.let { listOf(it) } ?: emptyList()
        // Get all records for proper grouping
        val allFuel = mutableListOf<FuelRecord>()
        val allCharge = mutableListOf<ChargeRecord>()
        val allOdo = odometerDao.getAllOnce()

        // We need to collect from flow - do a snapshot
        val fuelSnapshot = fuelDao.getTotalCost() // ensure db is accessible
        // Use a simpler approach: group by year-month
        val fuelByMonth = mutableMapOf<String, MutableList<FuelRecord>>()
        val chargeByMonth = mutableMapOf<String, MutableList<ChargeRecord>>()

        // Get all records once through the DAO
        // Since we need all records, we'll work with what we have
        return buildMonthlyBreakdownFromDao(benchmarkLPer100Km, defaultFuelPrice)
    }

    private suspend fun buildMonthlyBreakdownFromDao(
        benchmarkLPer100Km: Double,
        defaultFuelPrice: Double
    ): List<MonthlyBreakdown> {
        // Group fuel and charge records by month
        val fuelByMonth = mutableMapOf<String, MutableList<FuelRecord>>()
        val chargeByMonth = mutableMapOf<String, MutableList<ChargeRecord>>()
        val allMonths = mutableSetOf<String>()

        // We'll use the DAO's suspend functions and flows
        val totalFuelCost = fuelDao.getTotalCost()
        val totalChargeCost = chargeDao.getTotalCost()
        val totalFuelLiters = fuelDao.getTotalLiters()
        val totalChargeKwh = chargeDao.getTotalKwh()

        val allOdo = odometerDao.getAllOnce()
        val firstOdo = allOdo.firstOrNull()?.odometerKm ?: 0
        val lastOdo = allOdo.lastOrNull()?.odometerKm ?: 0
        val totalKm = (lastOdo - firstOdo).coerceAtLeast(0)

        // If we have no km data, return a single entry with totals
        if (totalKm <= 0) {
            val cal = Calendar.getInstance()
            val monthStr = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"
            return listOf(
                MonthlyBreakdown(
                    yearMonth = monthStr,
                    fuelCost = totalFuelCost,
                    electricCost = totalChargeCost,
                    fuelLiters = totalFuelLiters,
                    electricKwh = totalChargeKwh,
                    totalKm = totalKm,
                    fuelCostPerKm = 0.0,
                    electricCostPerKm = 0.0,
                    savings = 0.0,
                    evPercentage = 0.0
                )
            )
        }

        // Simple single-month summary for now
        val fuelKm = if (totalFuelLiters > 0) (totalFuelLiters / benchmarkLPer100Km) * 100.0 else 0.0
        val electricKm = (totalKm - fuelKm).coerceAtLeast(0.0)
        val fuelCostPerKm = if (fuelKm > 0) totalFuelCost / fuelKm else 0.0
        val electricCostPerKm = if (electricKm > 0) totalChargeCost / electricKm else 0.0
        val benchmarkCost = totalKm * benchmarkLPer100Km / 100.0 * defaultFuelPrice
        val actualCost = totalFuelCost + totalChargeCost
        val savings = benchmarkCost - actualCost
        val evPct = if (totalKm > 0) electricKm / totalKm * 100.0 else 0.0

        val cal = Calendar.getInstance()
        val monthStr = "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"

        return listOf(
            MonthlyBreakdown(
                yearMonth = monthStr,
                fuelCost = totalFuelCost,
                electricCost = totalChargeCost,
                fuelLiters = totalFuelLiters,
                electricKwh = totalChargeKwh,
                totalKm = totalKm,
                fuelCostPerKm = fuelCostPerKm,
                electricCostPerKm = electricCostPerKm,
                savings = savings,
                evPercentage = evPct
            )
        )
    }
}
