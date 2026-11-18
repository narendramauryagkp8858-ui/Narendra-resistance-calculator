package com.example

import com.example.domain.CalculationResult
import com.example.domain.ResistanceFormatter
import com.example.domain.ResistorCalculator
import com.example.model.BandCount
import com.example.model.ResistorColor
import com.example.ui.ResistorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ResistorCalculatorTest {

    @Test
    fun `three band brown black red calculates 1k ohm with 20 percent tolerance`() {
        val result = ResistorCalculator.calculate(
            BandCount.THREE,
            listOf(ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.RED)
        )
        assertTrue(result is CalculationResult.Valid)
        val valid = result as CalculationResult.Valid
        assertEquals(0, valid.ohms.compareTo(BigDecimal("1000")))
        assertEquals(0, valid.kiloOhms.compareTo(BigDecimal("1")))
        assertEquals(0, valid.megaOhms.compareTo(BigDecimal("0.001")))
        assertEquals(0, valid.tolerancePercent.compareTo(BigDecimal("20")))
        assertEquals(0, valid.minOhms.compareTo(BigDecimal("800")))
        assertEquals(0, valid.maxOhms.compareTo(BigDecimal("1200")))
        assertNull(valid.tempCoeffPpm)
    }

    @Test
    fun `four band yellow violet red gold calculates 4_7k ohm 5 percent`() {
        val result = ResistorCalculator.calculate(
            BandCount.FOUR,
            listOf(ResistorColor.YELLOW, ResistorColor.VIOLET, ResistorColor.RED, ResistorColor.GOLD)
        ) as CalculationResult.Valid

        assertEquals(0, result.ohms.compareTo(BigDecimal("4700")))
        assertEquals("4.7 KΩ", ResistanceFormatter.formatAuto(result.ohms))
        assertEquals(0, result.minOhms.compareTo(BigDecimal("4465")))
        assertEquals(0, result.maxOhms.compareTo(BigDecimal("4935")))
    }

    @Test
    fun `five band precision resistor calculates accurately`() {
        val result = ResistorCalculator.calculate(
            BandCount.FIVE,
            listOf(
                ResistorColor.ORANGE,
                ResistorColor.ORANGE,
                ResistorColor.BLACK,
                ResistorColor.ORANGE,
                ResistorColor.BROWN
            )
        ) as CalculationResult.Valid

        assertEquals(0, result.ohms.compareTo(BigDecimal("330000")))
        assertEquals("330 KΩ", ResistanceFormatter.formatAuto(result.ohms))
        assertEquals(0, result.tolerancePercent.compareTo(BigDecimal("1")))
        assertEquals(0, result.minOhms.compareTo(BigDecimal("326700")))
        assertEquals(0, result.maxOhms.compareTo(BigDecimal("333300")))
    }

    @Test
    fun `six band includes temperature coefficient in ppm per kelvin`() {
        val result = ResistorCalculator.calculate(
            BandCount.SIX,
            listOf(
                ResistorColor.RED,
                ResistorColor.RED,
                ResistorColor.BLACK,
                ResistorColor.BROWN,
                ResistorColor.BROWN,
                ResistorColor.RED
            )
        ) as CalculationResult.Valid

        assertEquals(0, result.ohms.compareTo(BigDecimal("2200")))
        assertEquals(50, result.tempCoeffPpm)
    }

    @Test
    fun `sub ohm silver multiplier calculates 0_22 ohm accurately`() {
        val result = ResistorCalculator.calculate(
            BandCount.FOUR,
            listOf(ResistorColor.RED, ResistorColor.RED, ResistorColor.SILVER, ResistorColor.GOLD)
        ) as CalculationResult.Valid

        assertEquals("0.22 Ω", ResistanceFormatter.formatAuto(result.ohms))
        assertEquals("0.22 Ω", ResistanceFormatter.formatValueWithUnit(result.ohms, "Ω"))
    }

    @Test
    fun `invalid digit gold returns invalid calculation result`() {
        val result = ResistorCalculator.calculate(
            BandCount.FOUR,
            listOf(ResistorColor.GOLD, ResistorColor.BLACK, ResistorColor.RED, ResistorColor.GOLD)
        )
        assertTrue(result is CalculationResult.Invalid)
        assertEquals(0, (result as CalculationResult.Invalid).invalidBandIndex)
    }

    @Test
    fun `invalid tolerance black returns invalid calculation result`() {
        val result = ResistorCalculator.calculate(
            BandCount.FOUR,
            listOf(ResistorColor.BROWN, ResistorColor.BLACK, ResistorColor.RED, ResistorColor.BLACK)
        )
        assertTrue(result is CalculationResult.Invalid)
        assertEquals(3, (result as CalculationResult.Invalid).invalidBandIndex)
    }

    @Test
    fun `viewModel rejects invalid color selection without corrupting state and resets cleanly`() {
        val vm = ResistorViewModel()
        vm.selectActiveBand(0)
        vm.selectColorForActiveBand(ResistorColor.SILVER)

        assertTrue(vm.uiState.value.invalidAttemptMessage != null)
        assertEquals(ResistorColor.BROWN, vm.uiState.value.bands[0])

        vm.selectColorForActiveBand(ResistorColor.GREEN)
        assertNull(vm.uiState.value.invalidAttemptMessage)
        assertEquals(ResistorColor.GREEN, vm.uiState.value.bands[0])

        vm.reset()
        assertEquals(BandCount.FOUR.defaultBands, vm.uiState.value.bands)
    }
}
