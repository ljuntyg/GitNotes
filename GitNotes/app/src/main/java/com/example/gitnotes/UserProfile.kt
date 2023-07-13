package com.example.gitnotes

data class UserProfile(
    // Set automatically to username if username/password login, set manually if login with token
    var profileName: String,
    // Local repositories will have a name, but an empty HTTPS link
    var repoNameHTTPSMaps: List<Map<String, String>>
)
