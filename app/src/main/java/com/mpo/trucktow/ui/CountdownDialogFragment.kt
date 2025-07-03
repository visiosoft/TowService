package com.mpo.trucktow.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.mpo.trucktow.R

class CountdownDialogFragment : DialogFragment() {

    private var timeTextView: TextView? = null
    private var progressIndicator: CircularProgressIndicator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_countdown, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        timeTextView = view.findViewById(R.id.countdownTimeText)
        progressIndicator = view.findViewById(R.id.countdownProgress)
        
        // Set initial time
        updateTime("02:30")
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full screen with transparent background
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    fun updateTime(timeText: String) {
        timeTextView?.text = timeText
    }
    
    fun updateProgress(current: Int, total: Int) {
        val progress = ((total - current) * 100 / total).coerceIn(0, 100)
        progressIndicator?.progress = progress
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timeTextView = null
        progressIndicator = null
    }
} 