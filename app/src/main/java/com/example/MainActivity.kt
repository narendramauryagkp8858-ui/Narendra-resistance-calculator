package com.example

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.databinding.ActivityMainBinding
import com.example.databinding.ItemBandTabBinding
import com.example.databinding.ItemColorSwatchBinding
import com.example.domain.CalculationResult
import com.example.domain.ResistanceFormatter
import com.example.model.BandCount
import com.example.model.BandRole
import com.example.model.ResistorColor
import com.example.ui.ResistorGuideBottomSheet
import com.example.ui.ResistorUiState
import com.example.ui.ResistorViewModel
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ResistorViewModel by viewModels()

    private val swatchBindings = mutableMapOf<ResistorColor, ItemColorSwatchBinding>()
    private val bandTabBindings = mutableListOf<ItemBandTabBinding>()
    private var renderedBandCount: BandCount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeToEdgeInsets()
        setupToolbar()
        setupBandCountToggle()
        setupResistorDiagram()
        setupColorSwatchGrid()
        setupActions()
        observeUiState()
    }

    private fun applyEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollContainer) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(
                left = bars.left,
                right = bars.right,
                bottom = bars.bottom
            )
            insets
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_guide -> {
                    openGuideSheet()
                    true
                }
                R.id.action_reset -> {
                    performReset()
                    true
                }
                R.id.action_theme -> {
                    cycleNightMode()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBandCountToggle() {
        binding.bandCountToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val target = when (checkedId) {
                R.id.btnBand3 -> BandCount.THREE
                R.id.btnBand4 -> BandCount.FOUR
                R.id.btnBand5 -> BandCount.FIVE
                R.id.btnBand6 -> BandCount.SIX
                else -> BandCount.FOUR
            }
            viewModel.selectBandCount(target)
        }
    }

    private fun setupResistorDiagram() {
        binding.resistorView.setOnBandSelectedListener { index ->
            viewModel.selectActiveBand(index)
        }
    }

    private fun setupColorSwatchGrid() {
        val inflater = LayoutInflater.from(this)
        binding.colorSwatchGrid.removeAllViews()
        swatchBindings.clear()

        ResistorColor.entries.forEach { resistorColor ->
            val swatchBinding = ItemColorSwatchBinding.inflate(
                inflater,
                binding.colorSwatchGrid,
                false
            )
            val chipBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * resources.displayMetrics.density
                setColor(resistorColor.colorInt)
                if (resistorColor == ResistorColor.WHITE || resistorColor == ResistorColor.SILVER) {
                    setStroke(
                        (1f * resources.displayMetrics.density).toInt(),
                        ContextCompat.getColor(this@MainActivity, R.color.md_theme_outline)
                    )
                }
            }
            swatchBinding.frameColorChip.background = chipBg
            swatchBinding.tvSwatchName.text = getString(resistorColor.nameRes)

            val checkTint = if (resistorColor.isLightColor) {
                0xFF0F172A.toInt()
            } else {
                0xFFFFFFFF.toInt()
            }
            swatchBinding.ivSelectedCheck.imageTintList = ColorStateList.valueOf(checkTint)

            swatchBinding.cardColorSwatch.setOnClickListener {
                viewModel.selectColorForActiveBand(resistorColor)
            }

            swatchBindings[resistorColor] = swatchBinding
            binding.colorSwatchGrid.addView(swatchBinding.root)
        }
    }

    private fun setupActions() {
        binding.btnReset.setOnClickListener { performReset() }
        binding.btnOpenGuide.setOnClickListener { openGuideSheet() }
    }

    private fun performReset() {
        viewModel.reset()
        Snackbar.make(binding.rootCoordinator, R.string.reset_toast, Snackbar.LENGTH_SHORT).show()
    }

    private fun openGuideSheet() {
        if (supportFragmentManager.findFragmentByTag(ResistorGuideBottomSheet.TAG) == null) {
            ResistorGuideBottomSheet().show(supportFragmentManager, ResistorGuideBottomSheet.TAG)
        }
    }

    private fun cycleNightMode() {
        val current = AppCompatDelegate.getDefaultNightMode()
        val next = when (current) {
            AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(next)
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: ResistorUiState) {
        // 1. Sync toggle button selection
        val expectedButtonId = when (state.bandCount) {
            BandCount.THREE -> R.id.btnBand3
            BandCount.FOUR -> R.id.btnBand4
            BandCount.FIVE -> R.id.btnBand5
            BandCount.SIX -> R.id.btnBand6
        }
        if (binding.bandCountToggleGroup.checkedButtonId != expectedButtonId) {
            binding.bandCountToggleGroup.check(expectedButtonId)
        }

        // 2. Resistor diagram & body badge
        binding.resistorView.updateState(state.bandCount, state.bands, state.activeBandIndex)
        binding.tvResistorTypeBadge.text = getString(
            if (state.bandCount == BandCount.FIVE || state.bandCount == BandCount.SIX) {
                R.string.resistor_body_precision
            } else {
                R.string.resistor_body_carbon
            }
        )
        binding.tvBandSummaryCode.text = state.bands
            .take(state.bandCount.count)
            .joinToString(" • ") { shortColorCode(it) }

        // 3. Band selector tabs (rebuild if bandCount changed)
        if (renderedBandCount != state.bandCount) {
            rebuildBandTabs(state.bandCount)
            renderedBandCount = state.bandCount
        }
        updateBandTabs(state)

        // 4. Color swatch grid for active band role
        val activeRole = state.activeRole
        val activeColor = state.activeColor
        binding.tvEditingBandTitle.text = getString(
            R.string.editing_band_format,
            getString(activeRole.labelRes)
        )
        updateColorSwatches(activeRole, activeColor)

        // 5. Calculation Result card
        renderResult(state.result, state.invalidAttemptMessage)
    }

    private fun rebuildBandTabs(bandCount: BandCount) {
        val inflater = LayoutInflater.from(this)
        binding.bandTabsContainer.removeAllViews()
        bandTabBindings.clear()

        bandCount.roles.forEachIndexed { index, role ->
            val tabBinding = ItemBandTabBinding.inflate(inflater, binding.bandTabsContainer, false)
            tabBinding.tvBandRoleLabel.text = getString(role.labelRes)
            tabBinding.cardBandTab.setOnClickListener {
                viewModel.selectActiveBand(index)
            }
            tabBinding.btnBandDropdown.setOnClickListener { anchor ->
                showBandDropdownMenu(anchor, index, role)
            }
            bandTabBindings.add(tabBinding)
            binding.bandTabsContainer.addView(tabBinding.root)
        }
    }

    private fun updateBandTabs(state: ResistorUiState) {
        val primaryColor = MaterialColors.getColor(
            binding.root,
            androidx.appcompat.R.attr.colorPrimary
        )
        val outlineColor = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorOutline
        )
        val primaryContainer = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorPrimaryContainer
        )
        val surfaceContainer = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorSurfaceContainer
        )

        val density = resources.displayMetrics.density
        bandTabBindings.forEachIndexed { index, tabBinding ->
            val color = state.bands.getOrElse(index) { ResistorColor.BROWN }
            val isSelected = index == state.activeBandIndex

            val dotDrawable = tabBinding.viewBandDot.background.mutate() as? GradientDrawable
            dotDrawable?.setColor(color.colorInt)
            tabBinding.tvBandColorName.text = getString(color.nameRes)

            tabBinding.cardBandTab.strokeColor = if (isSelected) primaryColor else outlineColor
            tabBinding.cardBandTab.strokeWidth = ((if (isSelected) 2f else 1f) * density).toInt()
            tabBinding.cardBandTab.setCardBackgroundColor(
                if (isSelected) primaryContainer else surfaceContainer
            )
        }
    }

    private fun showBandDropdownMenu(anchor: View, bandIndex: Int, role: BandRole) {
        viewModel.selectActiveBand(bandIndex)
        val popup = PopupMenu(this, anchor)
        ResistorColor.entries.forEachIndexed { idx, color ->
            val valueLabel = role.valueLabel(color)
            val title = if (valueLabel != null) {
                "${getString(color.nameRes)} ($valueLabel)"
            } else {
                "${getString(color.nameRes)} (${getString(R.string.not_applicable)})"
            }
            val item = popup.menu.add(Menu.NONE, idx, idx, title)
            item.isEnabled = role.isValidColor(color)
        }
        popup.setOnMenuItemClickListener { menuItem ->
            val chosenColor = ResistorColor.entries[menuItem.itemId]
            viewModel.selectColorForBand(bandIndex, chosenColor)
            true
        }
        popup.show()
    }

    private fun updateColorSwatches(activeRole: BandRole, activeColor: ResistorColor) {
        val primaryColor = MaterialColors.getColor(
            binding.root,
            androidx.appcompat.R.attr.colorPrimary
        )
        val outlineVariant = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorOutlineVariant
        )
        val primaryContainer = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorPrimaryContainer
        )
        val surfaceContainer = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorSurfaceContainer
        )
        val density = resources.displayMetrics.density

        ResistorColor.entries.forEach { color ->
            val swatchBinding = swatchBindings[color] ?: return@forEach
            val isValid = activeRole.isValidColor(color)
            val isSelected = color == activeColor

            swatchBinding.tvSwatchValue.text = activeRole.valueLabel(color) ?: getString(R.string.not_applicable)
            swatchBinding.ivSelectedCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

            swatchBinding.cardColorSwatch.alpha = if (isValid) 1f else 0.42f
            swatchBinding.cardColorSwatch.strokeColor = if (isSelected) primaryColor else outlineVariant
            swatchBinding.cardColorSwatch.strokeWidth = ((if (isSelected) 2f else 1f) * density).toInt()
            swatchBinding.cardColorSwatch.setCardBackgroundColor(
                if (isSelected) primaryContainer else surfaceContainer
            )
        }
    }

    private fun renderResult(result: CalculationResult, invalidAttemptMessage: String?) {
        when (result) {
            is CalculationResult.Valid -> {
                binding.tvHeroResistance.text = ResistanceFormatter.formatAuto(result.ohms)
                binding.tvToleranceBadge.text = ResistanceFormatter.formatTolerance(result.tolerancePercent)
                binding.tvToleranceBadge.visibility = View.VISIBLE

                if (result.tempCoeffPpm != null) {
                    binding.tvTcrBadge.text = getString(R.string.temp_coeff_unit_format, result.tempCoeffPpm)
                    binding.tvTcrBadge.visibility = View.VISIBLE
                } else {
                    binding.tvTcrBadge.visibility = View.GONE
                }

                binding.tvOhmsValue.text = ResistanceFormatter.formatValueWithUnit(result.ohms, "Ω")
                binding.tvKiloOhmsValue.text = ResistanceFormatter.formatValueWithUnit(result.kiloOhms, "KΩ")
                binding.tvMegaOhmsValue.text = ResistanceFormatter.formatValueWithUnit(result.megaOhms, "MΩ")

                binding.tvMinResistance.text = ResistanceFormatter.formatAuto(result.minOhms)
                binding.tvMaxResistance.text = ResistanceFormatter.formatAuto(result.maxOhms)

                val notice = invalidAttemptMessage ?: result.warningMessage
                if (notice != null) {
                    binding.layoutNoticeBanner.visibility = View.VISIBLE
                    binding.tvNoticeMessage.text = notice
                } else {
                    binding.layoutNoticeBanner.visibility = View.GONE
                }
            }
            is CalculationResult.Invalid -> {
                binding.tvHeroResistance.text = "—"
                binding.tvToleranceBadge.visibility = View.GONE
                binding.tvTcrBadge.visibility = View.GONE
                binding.tvOhmsValue.text = "—"
                binding.tvKiloOhmsValue.text = "—"
                binding.tvMegaOhmsValue.text = "—"
                binding.tvMinResistance.text = "—"
                binding.tvMaxResistance.text = "—"

                binding.layoutNoticeBanner.visibility = View.VISIBLE
                binding.tvNoticeMessage.text = invalidAttemptMessage ?: result.errorMessage
            }
        }
    }

    private fun shortColorCode(color: ResistorColor): String = when (color) {
        ResistorColor.BLACK -> "BLK"
        ResistorColor.BROWN -> "BRN"
        ResistorColor.RED -> "RED"
        ResistorColor.ORANGE -> "ORG"
        ResistorColor.YELLOW -> "YLW"
        ResistorColor.GREEN -> "GRN"
        ResistorColor.BLUE -> "BLU"
        ResistorColor.VIOLET -> "VLT"
        ResistorColor.GREY -> "GRY"
        ResistorColor.WHITE -> "WHT"
        ResistorColor.GOLD -> "GLD"
        ResistorColor.SILVER -> "SLV"
    }
}
