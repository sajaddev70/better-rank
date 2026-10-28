package com.marvelous.betterrank.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.marvelous.betterrank.fragment.homeguide.Section1Fragment
import com.marvelous.betterrank.fragment.homeguide.Section2Fragment
import com.marvelous.betterrank.fragment.homeguide.Section3Fragment
import com.marvelous.betterrank.fragment.homeguide.Section4Fragment
import com.marvelous.betterrank.fragment.homeguide.Section5Fragment
import com.marvelous.betterrank.fragment.homeguide.Section6Fragment
import com.marvelous.betterrank.fragment.homeguide.Section7Fragment
import com.marvelous.betterrank.fragment.homeguide.Section8Fragment
import com.marvelous.betterrank.fragment.homeguide.Section9Fragment

class HomeGuidePagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 9

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> Section1Fragment()
            1 -> Section2Fragment()
            2 -> Section3Fragment()
            3 -> Section4Fragment()
            4 -> Section5Fragment()
            5 -> Section6Fragment()
            6 -> Section7Fragment()
            7 -> Section8Fragment()
            8 -> Section9Fragment()
            else -> Section1Fragment()
        }
    }
}