package com.ljuntyg.gitnotes

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.ljuntyg.gitnotes.databinding.FragmentWarningDialogBinding

class WarningDialogFragment : DialogFragment() {
    private var _binding: FragmentWarningDialogBinding? = null
    private val binding get() = _binding!!

    private var positiveButtonListener: (() -> Unit)? = null
    private var negativeButtonListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarningDialogBinding.inflate(inflater, container, false)

        arguments?.let { args ->
            binding.title.text = args.getString("title")
            binding.message.text = args.getString("message")
            binding.warning.text = args.getString("warning")
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // This ensures that the custom white background with rounded corners is what's visible
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        if (binding.warning.text == "") {
            binding.cardView.visibility = View.GONE
        }

        binding.buttonYes.setOnClickListener {
            positiveButtonListener?.invoke()
            dismiss() // Close the dialog
        }

        binding.buttonNo.setOnClickListener {
            negativeButtonListener?.invoke()
            dismiss() // Close the dialog
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setPositiveButtonListener(listener: () -> Unit) {
        positiveButtonListener = listener
    }

    fun setNegativeButtonListener(listener: () -> Unit) {
        negativeButtonListener = listener
    }

    companion object {
        fun newInstance(title: String, message: String, warning: String?) = WarningDialogFragment().apply {
            arguments = Bundle().apply {
                putString("title", title)
                putString("message", message)
                putString("warning", warning ?: "")
            }
        }
    }
}


