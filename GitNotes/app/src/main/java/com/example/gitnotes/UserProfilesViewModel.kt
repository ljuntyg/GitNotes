package com.example.gitnotes

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class UserProfilesViewModel(private val repository: ProfilesReposRepository) : ViewModel() {
    // Use a backing property to hide the mutable LiveData from the UI
    private val _allUserProfiles = MutableLiveData<List<UserProfile>>()
    val allUserProfiles: LiveData<List<UserProfile>> get() = _allUserProfiles

    var loggedIn = false
    lateinit var selectedUserProfile: UserProfile

    init {
        // Automatically updates the list of user profiles when the database changes
        viewModelScope.launch {
            repository.getAllUserProfiles().collect { userProfilesList ->
                _allUserProfiles.value = userProfilesList.map { repository.getUserProfile(it.profileName) }
                Log.d("MYLOG", "Size of allUserProfiles: " + allUserProfiles.value?.size.toString())
            }
        }
    }

    fun getUserProfileAsync(name: String): Deferred<UserProfile> {
        return viewModelScope.async {
            repository.getUserProfile(name)
        }
    }

    fun insert(userProfile: UserProfile) {
        viewModelScope.launch {
            repository.insert(userProfile)
        }
    }

    fun update(userProfile: UserProfile) {
        viewModelScope.launch {
            repository.update(userProfile)
        }
    }

    fun delete(userProfile: UserProfile) {
        viewModelScope.launch {
            repository.delete(userProfile)
        }
    }
}

// For ViewModel classes with non-empty constructor, ViewModelProvider
// requires class implementing the ViewModelProvider.Factory interface
class UserProfilesViewModelFactory(private val repository: ProfilesReposRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserProfilesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserProfilesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}