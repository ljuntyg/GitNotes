package com.ljuntyg.gitnotes

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun View.showShortSnackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()
}

fun View.showLongSnackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).show()
}

fun View.showIndefiniteSnackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_INDEFINITE).show()
}

fun String.getRepoNameFromUrl(): String {
    val parts = this.split("/")
    val fullName = parts.last()

    return fullName.removeSuffix(".git")
}

// Regular expression courtesy of https://gist.github.com/magnetikonline/073afe7909ffdd6f10ef06a00bc3bc88
fun String.isPersonalAccessToken(): Boolean {
    val pattern = "^ghp_[a-zA-Z0-9]{36}$".toRegex()
    return this.trim().matches(pattern)
}

fun TextInputLayout.validateToken(): Boolean {
    val input = this.editText!!.text.toString()

    return if (!input.isPersonalAccessToken() && input.isNotEmpty()) {
        this.error = context.getString(R.string.invalid_token)
        false
    } else {
        this.error = null
        this.isErrorEnabled = false
        true
    }
}

// Regular expression courtesy of https://stackoverflow.com/questions/2514859/regular-expression-for-git-repository
fun String.isValidHTTPSlink(): Boolean {
    val pattern =
        "((git|ssh|http(s)?)|(git@[\\w\\.]+))(:(//)?)([\\w\\.@\\:/\\-~]+)(\\.git)(/)?".toRegex()
    return this.trim().matches(pattern)
}

fun TextInputLayout.validateLink(): Boolean {
    val input = this.editText!!.text.toString()

    return if (!input.isValidHTTPSlink() && input.isNotEmpty()) {
        this.error = context.getString(R.string.invalid_link)
        false
    } else {
        this.error = null
        this.isErrorEnabled = false
        true
    }
}

fun TextInputLayout.validateProfileName(allUserProfiles: List<UserProfile>): Boolean {
    val inputName = this.editText!!.text.toString().trim()
    val allProfileNames = allUserProfiles.map { userProfile -> userProfile.profileName.trim() }

    return if (allProfileNames.contains(inputName) && inputName.isNotEmpty()) {
        this.error = context.getString(R.string.user_already_exists, inputName)
        false
    } else {
        this.error = null
        this.isErrorEnabled = false
        true
    }
}

fun Long.toFormattedDateString(): String {
    val date = Date(this)
    val format = SimpleDateFormat("MMMM dd, HH:mm", Locale.getDefault())
    return format.format(date)
}
