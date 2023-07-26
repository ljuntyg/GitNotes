package com.ljuntyg.gitnotes

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// WARNING: Only pass application context to this singleton
class SelectedUserPrefs private constructor(context: Context) {
    companion object {
        private var instance: SelectedUserPrefs? = null

        fun getInstance(context: Context): SelectedUserPrefs {
            return instance ?: synchronized(this) {
                val newInstance = SelectedUserPrefs(context.applicationContext)
                instance = newInstance
                newInstance
            }
        }
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun insertOrReplace(name: String, pwt: String) {
        with(sharedPreferences.edit()) {
            clear()
            putString("NAME_KEY", name)
            putString("PWT_KEY", pwt)
            apply()
        }
    }

    fun getCredentials(): Pair<String?, String?> {
        return Pair(
            sharedPreferences.getString("NAME_KEY", null),
            sharedPreferences.getString("PWT_KEY", null)
        )
    }
}

