package com.example.model

import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import com.example.R
import java.math.BigDecimal

enum class ResistorColor(
    @StringRes val nameRes: Int,
    @ColorInt val colorInt: Int,
    val isLightColor: Boolean,
    val digit: Int?,
    val multiplier: BigDecimal?,
    val multiplierShortLabel: String,
    val tolerancePercent: BigDecimal?,
    val tempCoeffPpm: Int?
) {
    BLACK(
        nameRes = R.string.color_black,
        colorInt = 0xFF18181B.toInt(),
        isLightColor = false,
        digit = 0,
        multiplier = BigDecimal("1"),
        multiplierShortLabel = "×1 Ω",
        tolerancePercent = null,
        tempCoeffPpm = 250
    ),
    BROWN(
        nameRes = R.string.color_brown,
        colorInt = 0xFF78350F.toInt(),
        isLightColor = false,
        digit = 1,
        multiplier = BigDecimal("10"),
        multiplierShortLabel = "×10 Ω",
        tolerancePercent = BigDecimal("1"),
        tempCoeffPpm = 100
    ),
    RED(
        nameRes = R.string.color_red,
        colorInt = 0xFFDC2626.toInt(),
        isLightColor = false,
        digit = 2,
        multiplier = BigDecimal("100"),
        multiplierShortLabel = "×100 Ω",
        tolerancePercent = BigDecimal("2"),
        tempCoeffPpm = 50
    ),
    ORANGE(
        nameRes = R.string.color_orange,
        colorInt = 0xFFEA580C.toInt(),
        isLightColor = false,
        digit = 3,
        multiplier = BigDecimal("1000"),
        multiplierShortLabel = "×1 KΩ",
        tolerancePercent = BigDecimal("0.05"),
        tempCoeffPpm = 15
    ),
    YELLOW(
        nameRes = R.string.color_yellow,
        colorInt = 0xFFEAB308.toInt(),
        isLightColor = true,
        digit = 4,
        multiplier = BigDecimal("10000"),
        multiplierShortLabel = "×10 KΩ",
        tolerancePercent = BigDecimal("0.02"),
        tempCoeffPpm = 25
    ),
    GREEN(
        nameRes = R.string.color_green,
        colorInt = 0xFF16A34A.toInt(),
        isLightColor = false,
        digit = 5,
        multiplier = BigDecimal("100000"),
        multiplierShortLabel = "×100 KΩ",
        tolerancePercent = BigDecimal("0.5"),
        tempCoeffPpm = 20
    ),
    BLUE(
        nameRes = R.string.color_blue,
        colorInt = 0xFF2563EB.toInt(),
        isLightColor = false,
        digit = 6,
        multiplier = BigDecimal("1000000"),
        multiplierShortLabel = "×1 MΩ",
        tolerancePercent = BigDecimal("0.25"),
        tempCoeffPpm = 10
    ),
    VIOLET(
        nameRes = R.string.color_violet,
        colorInt = 0xFF7C3AED.toInt(),
        isLightColor = false,
        digit = 7,
        multiplier = BigDecimal("10000000"),
        multiplierShortLabel = "×10 MΩ",
        tolerancePercent = BigDecimal("0.1"),
        tempCoeffPpm = 5
    ),
    GREY(
        nameRes = R.string.color_grey,
        colorInt = 0xFF64748B.toInt(),
        isLightColor = false,
        digit = 8,
        multiplier = BigDecimal("100000000"),
        multiplierShortLabel = "×100 MΩ",
        tolerancePercent = BigDecimal("0.01"),
        tempCoeffPpm = 1
    ),
    WHITE(
        nameRes = R.string.color_white,
        colorInt = 0xFFF8FAFC.toInt(),
        isLightColor = true,
        digit = 9,
        multiplier = BigDecimal("1000000000"),
        multiplierShortLabel = "×1 GΩ",
        tolerancePercent = null,
        tempCoeffPpm = null
    ),
    GOLD(
        nameRes = R.string.color_gold,
        colorInt = 0xFFD4AF37.toInt(),
        isLightColor = true,
        digit = null,
        multiplier = BigDecimal("0.1"),
        multiplierShortLabel = "×0.1 Ω",
        tolerancePercent = BigDecimal("5"),
        tempCoeffPpm = null
    ),
    SILVER(
        nameRes = R.string.color_silver,
        colorInt = 0xFFC0C9D4.toInt(),
        isLightColor = true,
        digit = null,
        multiplier = BigDecimal("0.01"),
        multiplierShortLabel = "×0.01 Ω",
        tolerancePercent = BigDecimal("10"),
        tempCoeffPpm = null
    )
}
