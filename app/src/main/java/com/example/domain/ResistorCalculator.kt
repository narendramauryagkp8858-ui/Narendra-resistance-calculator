package com.example.domain

import com.example.model.BandCount
import com.example.model.BandRole
import com.example.model.ResistorColor
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

sealed interface CalculationResult {
    data class Valid(
        val ohms: BigDecimal,
        val kiloOhms: BigDecimal,
        val megaOhms: BigDecimal,
        val tolerancePercent: BigDecimal,
        val minOhms: BigDecimal,
        val maxOhms: BigDecimal,
        val tempCoeffPpm: Int?,
        val warningMessage: String? = null
    ) : CalculationResult

    data class Invalid(
        val errorMessage: String,
        val invalidBandIndex: Int
    ) : CalculationResult
}

object ResistorCalculator {

    private val THOUSAND = BigDecimal("1000")
    private val MILLION = BigDecimal("1000000")
    private val HUNDRED = BigDecimal("100")
    private val DEFAULT_THREE_BAND_TOLERANCE = BigDecimal("20")

    fun calculate(bandCount: BandCount, bands: List<ResistorColor>): CalculationResult {
        if (bands.size < bandCount.count) {
            return CalculationResult.Invalid(
                errorMessage = "Expected ${bandCount.count} bands, received ${bands.size}.",
                invalidBandIndex = bands.size
            )
        }

        val activeBands = bands.take(bandCount.count)
        bandCount.roles.forEachIndexed { index, role ->
            val color = activeBands[index]
            if (!role.isValidColor(color)) {
                val colorName = color.name.lowercase().replaceFirstChar { it.uppercase() }
                val roleLabel = when (role) {
                    BandRole.DIGIT_1 -> "1st Digit"
                    BandRole.DIGIT_2 -> "2nd Digit"
                    BandRole.DIGIT_3 -> "3rd Digit"
                    BandRole.MULTIPLIER -> "Multiplier"
                    BandRole.TOLERANCE -> "Tolerance"
                    BandRole.TEMP_COEFF -> "Temperature Coefficient"
                }
                return CalculationResult.Invalid(
                    errorMessage = "$colorName cannot be used for the $roleLabel band.",
                    invalidBandIndex = index
                )
            }
        }

        val significantValue: BigDecimal = when (bandCount) {
            BandCount.THREE, BandCount.FOUR -> {
                val d1 = activeBands[0].digit!!
                val d2 = activeBands[1].digit!!
                BigDecimal(d1 * 10 + d2)
            }
            BandCount.FIVE, BandCount.SIX -> {
                val d1 = activeBands[0].digit!!
                val d2 = activeBands[1].digit!!
                val d3 = activeBands[2].digit!!
                BigDecimal(d1 * 100 + d2 * 10 + d3)
            }
        }

        val multiplier = when (bandCount) {
            BandCount.THREE, BandCount.FOUR -> activeBands[2].multiplier!!
            BandCount.FIVE, BandCount.SIX -> activeBands[3].multiplier!!
        }

        val tolerancePercent = when (bandCount) {
            BandCount.THREE -> DEFAULT_THREE_BAND_TOLERANCE
            BandCount.FOUR -> activeBands[3].tolerancePercent!!
            BandCount.FIVE, BandCount.SIX -> activeBands[4].tolerancePercent!!
        }

        val tempCoeffPpm = when (bandCount) {
            BandCount.SIX -> activeBands[5].tempCoeffPpm!!
            else -> null
        }

        val ohms = significantValue.multiply(multiplier)
        val kiloOhms = ohms.divide(THOUSAND)
        val megaOhms = ohms.divide(MILLION)

        val toleranceFraction = tolerancePercent.divide(HUNDRED, 10, RoundingMode.HALF_UP)
        val delta = ohms.multiply(toleranceFraction)
        val minOhms = ohms.subtract(delta).max(BigDecimal.ZERO)
        val maxOhms = ohms.add(delta)

        val warning = if (activeBands[0] == ResistorColor.BLACK) {
            "Leading Black (0) is non-standard; 1st band is normally Brown through White."
        } else {
            null
        }

        return CalculationResult.Valid(
            ohms = ohms,
            kiloOhms = kiloOhms,
            megaOhms = megaOhms,
            tolerancePercent = tolerancePercent,
            minOhms = minOhms,
            maxOhms = maxOhms,
            tempCoeffPpm = tempCoeffPpm,
            warningMessage = warning
        )
    }
}

object ResistanceFormatter {

    private val symbols = DecimalFormatSymbols(Locale.US)
    private val thousand = BigDecimal("1000")
    private val million = BigDecimal("1000000")
    private val billion = BigDecimal("1000000000")

    fun formatAuto(ohms: BigDecimal): String {
        val clean = normalize(ohms)
        return when {
            clean >= billion -> "${formatNumber(clean.divide(billion))} GΩ"
            clean >= million -> "${formatNumber(clean.divide(million))} MΩ"
            clean >= thousand -> "${formatNumber(clean.divide(thousand))} KΩ"
            else -> "${formatNumber(clean)} Ω"
        }
    }

    fun formatValueWithUnit(value: BigDecimal, unit: String): String {
        return "${formatNumber(value)} $unit"
    }

    fun formatNumber(value: BigDecimal): String {
        val clean = normalize(value)
        if (clean.compareTo(BigDecimal.ZERO) == 0) return "0"
        val scale = clean.scale()
        return if (scale <= 0 && clean < BigDecimal("1000000000000")) {
            val df = DecimalFormat("#,##0", symbols)
            df.format(clean)
        } else if (clean >= BigDecimal("1")) {
            val df = DecimalFormat("#,##0.######", symbols)
            df.format(clean)
        } else {
            clean.toPlainString()
        }
    }

    fun formatTolerance(tolerance: BigDecimal): String {
        return "±${normalize(tolerance).toPlainString()}%"
    }

    private fun normalize(value: BigDecimal): BigDecimal {
        val stripped = value.stripTrailingZeros()
        return if (stripped.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else stripped
    }
}
