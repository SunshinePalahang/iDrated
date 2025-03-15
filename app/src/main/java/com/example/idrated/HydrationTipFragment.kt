package com.example.idrated

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class HydrationTipFragment : Fragment(R.layout.fragment_hydration_tip) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val hydrationTipText: TextView = view.findViewById(R.id.hydrationTipText)

        // Set hydration tip text
        hydrationTipText.text = "Stay hydrated! Drink at least 8 cups of water a day."
    }
}
