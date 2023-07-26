package com.ljuntyg.gitnotes

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class LoginViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GitLoginInputFragment.newInstanceRemoteLogin(
                "Clone remote GitNotes repository with PAT",
                "Login using a PAT (Personal Access Token) and clone a given remote GitNotes repository via an HTTPS link.",
                "Local profile name",
                "Personal Access Token",
                "Repository HTTPS link",
                "Clone Remote Repository"
            )

            else -> GitLoginInputFragment.newInstanceNewLocal(
                "Create new local GitNotes repository",
                "Create a new local GitNotes repository. An HTTPS link to a remote repository can be added for the local repository later.",
                "Local profile name",
                "Local repository name",
                "Create Local Repository"
            )
        }
    }
}