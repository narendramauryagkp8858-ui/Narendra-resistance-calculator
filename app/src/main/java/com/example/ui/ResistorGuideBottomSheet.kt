package com.example.ui

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.R
import com.example.databinding.BottomSheetGuideBinding
import com.example.databinding.ItemGuideColorRowBinding
import com.example.model.ResistorColor
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors

class ResistorGuideBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetGuideBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetGuideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        binding.btnCloseGuide.setOnClickListener { dismiss() }

        val inflater = LayoutInflater.from(requireContext())
        val container = binding.guideTableContainer
        container.removeAllViews()

        val altSurface = MaterialColors.getColor(
            view,
            com.google.android.material.R.attr.colorSurfaceContainer,
            0
        )

        ResistorColor.entries.forEachIndexed { index, color ->
            val rowBinding = ItemGuideColorRowBinding.inflate(inflater, container, false)
            if (index % 2 == 1 && altSurface != 0) {
                rowBinding.root.setBackgroundColor(altSurface)
            }

            val dotDrawable = rowBinding.viewGuideColorDot.background.mutate() as? GradientDrawable
            dotDrawable?.setColor(color.colorInt)

            rowBinding.tvGuideColorName.text = getString(color.nameRes)
            rowBinding.tvGuideDigit.text = color.digit?.toString() ?: "—"
            rowBinding.tvGuideMultiplier.text = color.multiplierShortLabel
            rowBinding.tvGuideTolerance.text = color.tolerancePercent?.let {
                "±${it.stripTrailingZeros().toPlainString()}%"
            } ?: "—"
            rowBinding.tvGuideTcr.text = color.tempCoeffPpm?.toString() ?: "—"

            container.addView(rowBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ResistorGuideBottomSheet"
    }
}
