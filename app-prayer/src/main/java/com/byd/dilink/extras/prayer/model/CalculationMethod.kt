package com.byd.dilink.extras.prayer.model

enum class CalculationMethod(
    val displayName: String,
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaMinutes: Int?
) {
    MWL("Muslim World League", 18.0, 17.0, null),
    UMM_AL_QURA("Umm Al-Qura (Makkah)", 18.5, null, 90),
    EGYPTIAN("Egyptian Authority", 19.5, 17.5, null),
    ISNA("ISNA", 15.0, 15.0, null)
}
