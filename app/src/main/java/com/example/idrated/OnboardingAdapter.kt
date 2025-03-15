package com.example.idrated

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return 5 // Total onboarding steps
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UsernameInputFragment()
            1 -> AgeInputFragment()
            2 -> GenderInputFragment()
            3 -> ActivityLevelFragment()
            else -> HydrationTipFragment() // Last step
        }
    }
}
