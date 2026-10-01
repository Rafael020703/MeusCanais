package com.meuscanais.core.domain.interactor

import com.meuscanais.data.repository.FirebaseRepository
import com.meuscanais.data.repository.SettingsRepository
import com.meuscanais.domain.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileManager @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val firebaseRepository: FirebaseRepository
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val profiles: StateFlow<List<UserProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<UserProfile?>(null)
    val activeProfile: StateFlow<UserProfile?> = _activeProfile.asStateFlow()

    private val _profileImageUrl = MutableStateFlow<String?>(null)
    val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    init {
        managerScope.launch {
            settingsRepository.settingsFlow.map { it.activeProfileId }.distinctUntilChanged().collect { id ->
                loadProfiles(id)
            }
        }
        
        managerScope.launch {
            firebaseRepository.getProfilePicture().collect { url ->
                _profileImageUrl.value = url
            }
        }
    }

    private suspend fun loadProfiles(activeId: String) {
        val fetched = firebaseRepository.getProfiles()
        if (fetched.isEmpty()) {
            val defaultProfile = UserProfile("default", "Usuário 1")
            firebaseRepository.saveProfile(defaultProfile)
            _profiles.value = listOf(defaultProfile)
        } else {
            _profiles.value = fetched
        }
        _activeProfile.value = _profiles.value.find { it.id == activeId } ?: _profiles.value.firstOrNull()
    }

    fun selectProfile(profile: UserProfile) {
        managerScope.launch {
            settingsRepository.updateActiveProfileId(profile.id)
            _activeProfile.value = profile
        }
    }

    fun addProfile(name: String, iconUrl: String? = null, isChild: Boolean = false) {
        managerScope.launch {
            val newProfile = UserProfile(UUID.randomUUID().toString(), name, iconUrl, isChild)
            firebaseRepository.saveProfile(newProfile)
            settingsRepository.settingsFlow.first().activeProfileId.let { loadProfiles(it) }
        }
    }

    fun deleteProfile(profileId: String) {
        if (profileId == "default") return
        managerScope.launch {
            firebaseRepository.deleteProfile(profileId)
            settingsRepository.settingsFlow.first().activeProfileId.let { loadProfiles(it) }
        }
    }

    fun updateProfilePicture(url: String) {
        managerScope.launch {
            firebaseRepository.updateProfilePicture(url)
            _profileImageUrl.value = url
        }
    }
}
