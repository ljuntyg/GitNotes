package com.example.gitnotes

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class LoginViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GitLoginInputFragment.newInstance("GitHub Username and Password Login", "Login using your GitHub username and password via HTTPS.", "GitHub Username", "Password", "Login")
            else -> GitLoginInputFragment.newInstance("GitHub PAT (Personal Access Token) Login", "Login using a Personal Access Token via HTTPS.", "Local profile name", "Personal Access Token", "Login")
        }
    }
}