package com.byd.dilink.extras.prayer.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byd.dilink.extras.data.dao.TasbeehDao
import com.byd.dilink.extras.data.dao.TasbeehSession
import com.byd.dilink.extras.data.preferences.PrayerPrefsKeys
import com.byd.dilink.extras.data.preferences.prayerPrefs
import com.byd.dilink.extras.prayer.ui.Dhikr
import com.byd.dilink.extras.prayer.ui.PRESET_DHIKR
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TasbeehViewModel @Inject constructor(
    private val tasbeehDao: TasbeehDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter.asStateFlow()

    private val _currentDhikr = MutableStateFlow(PRESET_DHIKR[0])
    val currentDhikr: StateFlow<Dhikr> = _currentDhikr.asStateFlow()

    private val _goal = MutableStateFlow(33)
    val goal: StateFlow<Int> = _goal.asStateFlow()

    private val _loopMode = MutableStateFlow(false)
    val loopMode: StateFlow<Boolean> = _loopMode.asStateFlow()

    // Which dhikr index in the loop sequence (0=SubhanAllah, 1=Alhamdulillah, 2=AllahuAkbar)
    private var loopIndex = 0

    // Vibration & sound settings (read from prefs)
    val vibrationEnabled: StateFlow<Boolean> = context.prayerPrefs.data
        .map { it[PrayerPrefsKeys.TASBEEH_VIBRATION] ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val soundEnabled: StateFlow<Boolean> = context.prayerPrefs.data
        .map { it[PrayerPrefsKeys.TASBEEH_SOUND] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Daily total
    private val todayTimestamp: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val dailyTotal: StateFlow<Int> = tasbeehDao.getDailyTotal(todayTimestamp)
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Load default goal from prefs
        viewModelScope.launch {
            context.prayerPrefs.data.collect { prefs ->
                val g = prefs[PrayerPrefsKeys.DEFAULT_TASBEEH_GOAL] ?: 33
                if (_goal.value == 33) _goal.value = g
            }
        }
    }

    fun increment() {
        _counter.value++

        // Check if goal reached
        if (_counter.value >= _goal.value) {
            saveCurrentSession()

            if (_loopMode.value) {
                // Advance to next dhikr in sequence
                loopIndex++
                if (loopIndex >= 3) {
                    // Completed all 3: SubhanAllah, Alhamdulillah, AllahuAkbar
                    loopIndex = 0
                    _counter.value = 0
                    _currentDhikr.value = PRESET_DHIKR[0]
                } else {
                    _counter.value = 0
                    _currentDhikr.value = PRESET_DHIKR[loopIndex]
                }
            }
            // If not loop mode, counter stays at goal — user can manually reset
        }
    }

    fun reset() {
        if (_counter.value > 0) {
            saveCurrentSession()
        }
        _counter.value = 0
        loopIndex = 0
    }

    fun setDhikr(dhikr: Dhikr) {
        if (_counter.value > 0) {
            saveCurrentSession()
        }
        _currentDhikr.value = dhikr
        _counter.value = 0
        loopIndex = PRESET_DHIKR.indexOfFirst { it.arabic == dhikr.arabic }.coerceAtLeast(0)
    }

    fun setGoal(newGoal: Int) {
        _goal.value = newGoal
    }

    fun toggleLoopMode() {
        _loopMode.value = !_loopMode.value
        if (_loopMode.value) {
            loopIndex = 0
            _currentDhikr.value = PRESET_DHIKR[0]
            _goal.value = 33
            _counter.value = 0
        }
    }

    private fun saveCurrentSession() {
        val count = _counter.value
        if (count == 0) return

        viewModelScope.launch {
            tasbeehDao.upsert(
                TasbeehSession(
                    date = todayTimestamp,
                    dhikrText = _currentDhikr.value.arabic,
                    count = count,
                    goal = _goal.value
                )
            )
        }
    }
}
