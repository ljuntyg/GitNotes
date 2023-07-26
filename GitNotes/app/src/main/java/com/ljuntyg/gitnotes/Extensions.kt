package com.ljuntyg.gitnotes

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

fun View.showShortSnackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()
}

// Regular expression courtesy of https://gist.github.com/magnetikonline/073afe7909ffdd6f10ef06a00bc3bc88
fun String.isPersonalAccessToken(): Boolean {
    val pattern = "^ghp_[a-zA-Z0-9]{36}$".toRegex()
    return this.trim().matches(pattern)
}

fun TextInputLayout.validateToken(): Boolean {
    val input = this.editText!!.text.toString()

    return if (!input.isPersonalAccessToken() && input.isNotEmpty()) {
        this.error = "Not a valid Token"
        false
    } else {
        this.error = null
        this.isErrorEnabled = false
        true
    }
}

// Regular expression courtesy of https://stackoverflow.com/questions/2514859/regular-expression-for-git-repository
fun String.isValidHTTPSlink(): Boolean {
    val pattern = "((git|ssh|http(s)?)|(git@[\\w\\.]+))(:(//)?)([\\w\\.@\\:/\\-~]+)(\\.git)(/)?".toRegex()
    return this.trim().matches(pattern)
}

fun TextInputLayout.validateLink(): Boolean {
    val input = this.editText!!.text.toString()

    return if (!input.isValidHTTPSlink() && input.isNotEmpty()) {
        this.error = "Not a valid HTTPS link"
        false
    } else {
        this.error = null
        this.isErrorEnabled = false
        true
    }
}

// Used to handle getParcelable(String key) deprecation (deprecated in API level 33)
// in favor of the new getParcelable(String key, Class class) if version >= API 33
inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}