package com.ljuntyg.gitnotes

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class LoginViewPagerAdapter(private val fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GitLoginInputFragment.newInstanceRemoteLogin(
                fragmentActivity.getString(R.string.clone_remote_title),
                fragmentActivity.getString(R.string.clone_remote_body),
                fragmentActivity.getString(R.string.local_profile_name),
                fragmentActivity.getString(R.string.personal_access_token),
                fragmentActivity.getString(R.string.repo_link),
                fragmentActivity.getString(R.string.clone_remote_button)
            )

            else -> GitLoginInputFragment.newInstanceNewLocal(
                fragmentActivity.getString(R.string.create_local_title),
                fragmentActivity.getString(R.string.create_local_body),
                fragmentActivity.getString(R.string.local_profile_name),
                fragmentActivity.getString(R.string.local_repo_name),
                fragmentActivity.getString(R.string.create_local_button)
            )
        }
    }
}