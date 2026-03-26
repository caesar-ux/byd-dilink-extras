package com.byd.dilink.extras.hazard.model

enum class HazardType(val label: String, val iconName: String, val colorLong: Long) {
    POTHOLE("Pothole", "warning", 0xFFFF6D00),
    SPEED_BUMP("Speed Bump", "signal_cellular_alt", 0xFFFFC107),
    CHECKPOINT("Checkpoint", "shield", 0xFFFF5252),
    CONSTRUCTION("Construction", "construction", 0xFFFF9800),
    BAD_ROAD("Bad Road", "dangerous", 0xFF795548),
    FLOOD("Flood", "water", 0xFF42A5F5),
    ANIMAL("Animal", "pets", 0xFF8D6E63),
    POLICE("Police", "local_police", 0xFF2196F3),
    SPEED_CAMERA("Speed Camera", "camera", 0xFFE91E63),
    ACCIDENT("Accident", "car_crash", 0xFFD32F2F),
    OTHER("Other", "place", 0xFF9E9E9E);

    companion object {
        fun fromString(name: String): HazardType {
            return entries.firstOrNull { it.name == name } ?: OTHER
        }

        /** The four quick-add types shown on the main driving screen */
        val quickAdd = listOf(POTHOLE, SPEED_BUMP, CHECKPOINT, CONSTRUCTION)

        /** All remaining types shown in the "More" expanded grid */
        val moreTypes = entries.filter { it !in quickAdd }
    }
}
