package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.domain.CalculationResult
import com.example.domain.ResistorCalculator
import com.example.model.BandCount
import com.example.model.BandRole
import com.example.model.ResistorColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class ResistorUiState(
    val bandCount: BandCount = BandCount.FOUR,
    val bands: List<ResistorColor> = BandCount.FOUR.defaultBands,
    val activeBandIndex: Int = 0,
    val result: CalculationResult = ResistorCalculator.calculate(
        BandCount.FOUR,
        BandCount.FOUR.defaultBands
    ),
    val invalidAttemptMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val activeRole: BandRole
        get() = bandCount.roles.getOrElse(activeBandIndex) { BandRole.DIGIT_1 }

    val activeColor: ResistorColor
        get() = bands.getOrElse(activeBandIndex) { ResistorColor.BROWN }
}

class ResistorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ResistorUiState())
    val uiState: StateFlow<ResistorUiState> = _uiState.asStateFlow()

    fun selectBandCount(newCount: BandCount) {
        _uiState.update { current ->
            if (current.bandCount == newCount) return@update current
            val mappedBands = remapBands(current.bandCount, current.bands, newCount)
            val safeActiveIndex = current.activeBandIndex.coerceIn(0, newCount.count - 1)
            current.copy(
                bandCount = newCount,
                bands = mappedBands,
                activeBandIndex = safeActiveIndex,
                result = ResistorCalculator.calculate(newCount, mappedBands),
                invalidAttemptMessage = null
            )
        }
    }

    fun selectActiveBand(index: Int) {
        _uiState.update { current ->
            val clamped = index.coerceIn(0, current.bandCount.count - 1)
            current.copy(
                activeBandIndex = clamped,
                invalidAttemptMessage = null
            )
        }
    }

    fun selectColorForActiveBand(color: ResistorColor) {
        selectColorForBand(_uiState.value.activeBandIndex, color)
    }

    fun selectColorForBand(bandIndex: Int, color: ResistorColor) {
        _uiState.update { current ->
            if (bandIndex !in 0 until current.bandCount.count) return@update current
            val role = current.bandCount.roles[bandIndex]
            if (!role.isValidColor(color)) {
                val colorName = color.name.lowercase().replaceFirstChar { it.uppercase() }
                val roleName = when (role) {
                    BandRole.DIGIT_1 -> "1st Digit"
                    BandRole.DIGIT_2 -> "2nd Digit"
                    BandRole.DIGIT_3 -> "3rd Digit"
                    BandRole.MULTIPLIER -> "Multiplier"
                    BandRole.TOLERANCE -> "Tolerance"
                    BandRole.TEMP_COEFF -> "Temperature Coefficient"
                }
                return@update current.copy(
                    activeBandIndex = bandIndex,
                    invalidAttemptMessage = "$colorName is not valid for the $roleName band."
                )
            }

            val updated = current.bands.toMutableList().apply {
                this[bandIndex] = color
            }
            current.copy(
                bands = updated,
                activeBandIndex = bandIndex,
                result = ResistorCalculator.calculate(current.bandCount, updated),
                invalidAttemptMessage = null
            )
        }
    }

    fun reset() {
        _uiState.update { current ->
            val defaults = current.bandCount.defaultBands
            current.copy(
                bands = defaults,
                activeBandIndex = 0,
                result = ResistorCalculator.calculate(current.bandCount, defaults),
                invalidAttemptMessage = null
            )
        }
    }

    fun cycleThemeMode() {
        _uiState.update { current ->
            val next = when (current.themeMode) {
                ThemeMode.SYSTEM -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.SYSTEM
            }
            current.copy(themeMode = next)
        }
    }

    private fun remapBands(
        oldCount: BandCount,
        oldBands: List<ResistorColor>,
        newCount: BandCount
    ): List<ResistorColor> {
        val d1 = oldBands.getOrElse(0) { ResistorColor.BROWN }
        val d2 = oldBands.getOrElse(1) { ResistorColor.BLACK }
        val d3 = when (oldCount) {
            BandCount.FIVE, BandCount.SIX -> oldBands.getOrElse(2) { ResistorColor.BLACK }
            else -> ResistorColor.BLACK
        }
        val mult = when (oldCount) {
            BandCount.THREE, BandCount.FOUR -> oldBands.getOrElse(2) { ResistorColor.RED }
            BandCount.FIVE, BandCount.SIX -> oldBands.getOrElse(3) { ResistorColor.BROWN }
        }
        val tol = when (oldCount) {
            BandCount.THREE -> ResistorColor.GOLD
            BandCount.FOUR -> oldBands.getOrElse(3) { ResistorColor.GOLD }
            BandCount.FIVE, BandCount.SIX -> oldBands.getOrElse(4) { ResistorColor.BROWN }
        }
        val tcr = when (oldCount) {
            BandCount.SIX -> oldBands.getOrElse(5) { ResistorColor.RED }
            else -> ResistorColor.RED
        }

        return when (newCount) {
            BandCount.THREE -> listOf(d1, d2, mult)
            BandCount.FOUR -> listOf(d1, d2, mult, tol)
            BandCount.FIVE -> listOf(d1, d2, d3, mult, tol)
            BandCount.SIX -> listOf(d1, d2, d3, mult, tol, tcr)
        }
    }
}
