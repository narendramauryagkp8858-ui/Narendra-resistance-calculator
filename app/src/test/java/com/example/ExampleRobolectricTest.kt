package com.example

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app_name matches Narendra Resistance Calculator`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Narendra Resistance Calculator", appName)
    }

    @Test
    fun `MainActivity launches and switches band counts accurately`() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val heroResistance = activity.findViewById<TextView>(R.id.tvHeroResistance)
        val ohmsValue = activity.findViewById<TextView>(R.id.tvOhmsValue)
        val kiloOhmsValue = activity.findViewById<TextView>(R.id.tvKiloOhmsValue)
        val megaOhmsValue = activity.findViewById<TextView>(R.id.tvMegaOhmsValue)
        val toleranceBadge = activity.findViewById<TextView>(R.id.tvToleranceBadge)
        val tcrBadge = activity.findViewById<TextView>(R.id.tvTcrBadge)
        val minResistance = activity.findViewById<TextView>(R.id.tvMinResistance)
        val maxResistance = activity.findViewById<TextView>(R.id.tvMaxResistance)
        val toggleGroup = activity.findViewById<MaterialButtonToggleGroup>(R.id.bandCountToggleGroup)
        val resetButton = activity.findViewById<MaterialButton>(R.id.btnReset)

        assertNotNull(heroResistance)
        assertEquals("1 KΩ", heroResistance.text.toString())
        assertEquals("1,000 Ω", ohmsValue.text.toString())
        assertEquals("1 KΩ", kiloOhmsValue.text.toString())
        assertEquals("0.001 MΩ", megaOhmsValue.text.toString())
        assertEquals("±5%", toleranceBadge.text.toString())
        assertEquals("950 Ω", minResistance.text.toString())
        assertEquals("1.05 KΩ", maxResistance.text.toString())
        assertEquals(View.GONE, tcrBadge.visibility)

        toggleGroup.check(R.id.btnBand6)
        assertEquals(View.VISIBLE, tcrBadge.visibility)
        assertEquals("50 ppm/K", tcrBadge.text.toString())

        resetButton.performClick()
        assertEquals("1 KΩ", heroResistance.text.toString())
        assertEquals("50 ppm/K", tcrBadge.text.toString())
    }
}
