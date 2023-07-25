package com.example.gitnotes

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun View.showShortSnackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()
}
