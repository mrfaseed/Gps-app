package com.example.gpsmapcameraapp.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.gpsmapcameraapp.MainActivity
import com.example.gpsmapcameraapp.R
import com.example.gpsmapcameraapp.models.StampTemplate
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TemplateSelectorBottomSheetDialog : BottomSheetDialogFragment() {

    private var currentTemplateId = 1
    var onTemplateSelectedListener: ((StampTemplate) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_template_selector, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("TemplatePreferences", Context.MODE_PRIVATE)
        currentTemplateId = prefs.getInt("selected_template_id", 1)

        val cards = listOf(
            view.findViewById<LinearLayout>(R.id.card_template_1),
            view.findViewById<LinearLayout>(R.id.card_template_2),
            view.findViewById<LinearLayout>(R.id.card_template_3),
            view.findViewById<LinearLayout>(R.id.card_template_4),
            view.findViewById<LinearLayout>(R.id.card_template_5)
        )

        val checks = listOf(
            view.findViewById<ImageView>(R.id.check_template_1),
            view.findViewById<ImageView>(R.id.check_template_2),
            view.findViewById<ImageView>(R.id.check_template_3),
            view.findViewById<ImageView>(R.id.check_template_4),
            view.findViewById<ImageView>(R.id.check_template_5)
        )

        fun updateSelection(selectedId: Int) {
            currentTemplateId = selectedId
            prefs.edit().putInt("selected_template_id", selectedId).apply()

            for (i in 0 until 5) {
                val isSelected = (i + 1) == selectedId
                cards[i].setBackgroundResource(
                    if (isSelected) R.drawable.bg_template_card_selected
                    else R.drawable.bg_template_card_unselected
                )
                checks[i].visibility = if (isSelected) View.VISIBLE else View.GONE
            }
        }

        updateSelection(currentTemplateId)

        cards.forEachIndexed { index, card ->
            val templateId = index + 1
            card.setOnClickListener {
                updateSelection(templateId)
                val template = StampTemplate.fromId(templateId)
                onTemplateSelectedListener?.invoke(template)
                (activity as? MainActivity)?.applyTemplateSelection(template)
                dismiss()
            }
        }

        val card6 = view.findViewById<View>(R.id.card_template_6)
        card6?.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "Custom template is coming soon!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val TAG = "TemplateSelectorBottomSheetDialog"

        fun newInstance(listener: ((StampTemplate) -> Unit)? = null): TemplateSelectorBottomSheetDialog {
            val dialog = TemplateSelectorBottomSheetDialog()
            dialog.onTemplateSelectedListener = listener
            return dialog
        }
    }
}
