package com.ljuntyg.gitnotes

import android.app.Application
import android.util.Log
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Pass application to then get application context to pass to creation of SelectedUserPrefs singleton
class UserProfilesViewModel(
    application: Application,
    private val repository: ProfilesReposRepository
) : ViewModel() {
    // Use a backing property to hide the mutable LiveData from the UI
    private val _allUserProfiles = MutableLiveData<List<UserProfile>>()
    val allUserProfiles: LiveData<List<UserProfile>> get() = _allUserProfiles

    private val _selectedUserProfile = MutableLiveData<UserProfile>()
    val selectedUserProfile: LiveData<UserProfile> get() = _selectedUserProfile

    private val _selectedUserRepositories = MutableLiveData<List<Repository>>()
    val selectedUserRepositories: LiveData<List<Repository>> get() = _selectedUserRepositories

    private val _selectedRepository = MutableLiveData<Repository>()
    val selectedRepository: LiveData<Repository> get() = _selectedRepository

    // TODO: Check for stray repositories on launch (repositories with profileName of non-existent user)?
    private val _allRepositories = MutableLiveData<List<Repository>>()
    val allRepositories: LiveData<List<Repository>> get() = _allRepositories

    private val _loggedIn = MutableLiveData<Boolean>()
    val loggedIn: LiveData<Boolean> get() = _loggedIn

    val selectedUserPrefs: SelectedUserPrefs = SelectedUserPrefs.getInstance(application)

    private fun getOrCreatePlaceholderRepo(userProfile: UserProfile): Repository {
        return selectedUserRepositories.value?.find { repo ->
            repo.profileName == userProfile.profileName
                    && repo.name == "NEW REPOSITORY"
                    && repo.httpsLink == ""
        } ?: Repository(
            profileName = userProfile.profileName,
            name = "NEW REPOSITORY",
            httpsLink = ""
        )
    }


    fun logInAsUser(userProfile: UserProfile, token: String?) {
        insert(userProfile)
        setSelectedUserProfile(userProfile)
        selectedUserPrefs.insertOrReplace(userProfile.profileName, token ?: "")
        setSelectedRepository(getOrCreatePlaceholderRepo(userProfile))
        setLoggedIn(true)
    }

    init {
        // Automatically updates the list of user profiles/all repositories when the database changes
        viewModelScope.launch {
            launch {
                repository.getAllUserProfiles().collect { userProfilesList ->
                    _allUserProfiles.value =
                        userProfilesList.mapNotNull { repository.getUserProfile(it.profileName) }
                    Log.d(
                        "MYLOG",
                        "Size of allUserProfiles: " + allUserProfiles.value?.size.toString()
                    )
                }
            }

            launch {
                repository.getAllRepositories().collect { repositoriesList ->
                    _allRepositories.value = repositoriesList
                }
            }
        }
    }

    // TODO: Use postValue?
    private var selectedUserRepositoriesCollectJob: Job? = null
    private fun setSelectedUserProfile(userProfile: UserProfile) {
        _selectedUserProfile.value = userProfile

        selectedUserRepositoriesCollectJob?.cancel()

        selectedUserRepositoriesCollectJob = viewModelScope.launch {
            repository.getRepositoriesForUser(userProfile.profileName).collect { repositoriesList ->
                _selectedUserRepositories.value = repositoriesList
            }
        }
    }

    // TODO: Use postValue?
    fun setSelectedRepository(repo: Repository) {
        _selectedRepository.value = repo
    }

    fun setLoggedIn(value: Boolean) {
        _loggedIn.postValue(value)
    }

    fun getUserProfileAsync(name: String): Deferred<UserProfile?> {
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

    fun insertRepoForUserAsync(repo: Repository, userProfile: UserProfile): Deferred<Boolean> {
        return viewModelScope.async {
            repository.insertRepoForUser(repo, userProfile)
        }
    }

    fun updateRepoForUserAsync(repo: Repository, userProfile: UserProfile): Deferred<Boolean> {
        return viewModelScope.async {
            repository.updateRepoForUser(repo, userProfile)
        }
    }

    fun deleteRepoForUserAsync(repo: Repository, userProfile: UserProfile): Deferred<Boolean> {
        return viewModelScope.async {
            repository.deleteRepoForUser(repo, userProfile)
        }
    }
}

// For ViewModel classes with non-empty constructor, ViewModelProvider
// requires class implementing the ViewModelProvider.Factory interface
class UserProfilesViewModelFactory(
    private val application: Application,
    private val repository: ProfilesReposRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserProfilesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserProfilesViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}