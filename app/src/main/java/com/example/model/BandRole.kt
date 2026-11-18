package com.example.model

import androidx.annotation.StringRes
import com.example.R

enum class BandRole(@StringRes val labelRes: Int) {
    DIGIT_1(R.string.role_digit_1),
    DIGIT_2(R.string.role_digit_2),
    DIGIT_3(R.string.role_digit_3),
    MULTIPLIER(R.string.role_multiplier),
    TOLERANCE(R.string.role_tolerance),
    TEMP_COEFF(R.string.role_temp_coeff);

    fun isValidColor(color: ResistorColor): Boolean = when (this) {
        DIGIT_1, DIGIT_2, DIGIT_3 -> color.digit != null
        MULTIPLIER -> color.multiplier != null
        TOLERANCE -> color.tolerancePercent != null
        TEMP_COEFF -> color.tempCoeffPpm != null
    }

    fun valueLabel(color: ResistorColor): String? = when (this) {
        DIGIT_1, DIGIT_2, DIGIT_3 -> color.digit?.toString()
        MULTIPLIER -> color.multiplierShortLabel
        TOLERANCE -> color.tolerancePercent?.let { "±${it.stripTrailingZeros().toPlainString()}%" }
        TEMP_COEFF -> color.tempCoeffPpm?.let { "$it ppm/K" }
    }
}
