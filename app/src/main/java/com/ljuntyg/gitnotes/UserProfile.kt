package com.ljuntyg.gitnotes

import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    var profileName: String = "",

    @Ignore
    var repositories: MutableList<Repository> = mutableListOf()
) : Parcelable

@Parcelize
@Entity(tableName = "repositories")
data class Repository(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    @ColumnInfo(name = "profile_name")
    var profileName: String, // This connects the Repository to a UserProfile

    @ColumnInfo(name = "name")
    var name: String,

    @ColumnInfo(name = "https_link")
    var httpsLink: String
) : Parcelable

/**
 * Room database related members
 */

// WARNING: Only access DAO through the repository, not directly
@Dao
interface UserProfilesDao {
    @Query("SELECT * FROM user_profiles")
    fun getAllUserProfiles(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE profileName = :name")
    fun getUserProfile(name: String): Flow<UserProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(userProfile: UserProfile): Long

    @Update
    fun update(userProfile: UserProfile)

    @Delete
    fun delete(userProfile: UserProfile)
}

// WARNING: Only access DAO through the repository, not directly
@Dao
interface RepositoriesDao {
    @Query("SELECT * FROM repositories")
    fun getAllRepositories(): Flow<List<Repository>>

    @Query("SELECT * FROM repositories WHERE profile_name = :profileName")
    fun getRepositoriesForUser(profileName: String): Flow<List<Repository>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(repository: Repository)

    @Update
    fun update(repository: Repository)

    @Delete
    fun delete(repository: Repository)
}

// Single Database for both entities
@Database(entities = [UserProfile::class, Repository::class], version = 1)
abstract class ProfilesReposDatabase : RoomDatabase() {
    abstract fun userProfilesDao(): UserProfilesDao
    abstract fun repositoriesDao(): RepositoriesDao

    companion object {
        private var INSTANCE: ProfilesReposDatabase? = null
        fun getDatabase(context: Context): ProfilesReposDatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = Room.databaseBuilder(
                        context,
                        ProfilesReposDatabase::class.java, "profiles_repos_database"
                    ).build()
                }
            }
            return INSTANCE!!
        }
    }
}

class ProfilesReposRepository(
    private val userProfilesDao: UserProfilesDao,
    private val repositoriesDao: RepositoriesDao
) {

    fun getAllUserProfiles(): Flow<List<UserProfile>> =
        userProfilesDao.getAllUserProfiles()

    fun getRepositoriesForUser(profileName: String): Flow<List<Repository>> =
        repositoriesDao.getRepositoriesForUser(profileName)

    fun getAllRepositories(): Flow<List<Repository>> =
        repositoriesDao.getAllRepositories()

    suspend fun getUserProfile(name: String): UserProfile? {
        return withContext(Dispatchers.IO) {
            val userProfile = userProfilesDao.getUserProfile(name).firstOrNull()
            userProfile?.repositories =
                repositoriesDao.getRepositoriesForUser(name).first().toMutableList()
            userProfile
        }
    }

    suspend fun insert(userProfile: UserProfile) {
        withContext(Dispatchers.IO) {
            userProfilesDao.insert(userProfile)
            userProfile.repositories.forEach { repositoriesDao.insert(it) }
        }
    }

    suspend fun update(userProfile: UserProfile) {
        withContext(Dispatchers.IO) {
            userProfilesDao.update(userProfile)
            userProfile.repositories.forEach { repositoriesDao.update(it) }
        }
    }

    suspend fun delete(userProfile: UserProfile) {
        withContext(Dispatchers.IO) {
            userProfile.repositories.forEach { repositoriesDao.delete(it) }
            userProfilesDao.delete(userProfile)
        }
    }

    suspend fun insertRepoForUser(repo: Repository, userProfile: UserProfile): Boolean {
        return withContext(Dispatchers.IO) {
            val userExists = getUserProfile(userProfile.profileName) != null
            if (!userExists) {
                Log.d("MYLOG", "ERROR: Attempt to add repo to non-existent user")
                false
            } else {
                repositoriesDao.insert(repo)
                true
            }
        }
    }

    suspend fun updateRepoForUser(repo: Repository, userProfile: UserProfile): Boolean {
        return withContext(Dispatchers.IO) {
            val userExists = getUserProfile(userProfile.profileName) != null
            if (!userExists) {
                Log.d("MYLOG", "ERROR: Attempt to update repo for non-existent user")
                false
            } else {
                repositoriesDao.update(repo)
                true
            }
        }
    }

    suspend fun deleteRepoForUser(repo: Repository, userProfile: UserProfile): Boolean {
        return withContext(Dispatchers.IO) {
            val userHasRepo = getRepositoriesForUser(userProfile.profileName).first().contains(repo)
            if (!userHasRepo) {
                Log.d("MYLOG", "ERROR: Attempt to delete repo not belonging to given user")
                false
            } else {
                repositoriesDao.delete(repo)
                true
            }
        }
    }
}

