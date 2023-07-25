package com.example.gitnotes

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

fun View.showShortSnackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()
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