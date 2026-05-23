package com.sdd.marketplace.feature.followers.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.domain.model.User
import com.sdd.marketplace.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FollowersUiState(
    val followers: List<User> = emptyList(),
    val following: List<User> = emptyList(),
    val followingIds: Set<String> = emptySet(),
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val userName: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class FollowersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: String = savedStateHandle["userId"] ?: ""
    private val _uiState = MutableStateFlow(FollowersUiState())
    val uiState: StateFlow<FollowersUiState> = _uiState.asStateFlow()

    init {
        loadFollowers()
        viewModelScope.launch {
            userRepository.getUserProfile(userId).collect { user ->
                _uiState.update { it.copy(
                    userName = user?.fullName ?: "",
                    followerCount = user?.followerCount ?: 0,
                    followingCount = user?.followingCount ?: 0
                )}
            }
        }
    }

    fun loadFollowers() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        userRepository.getFollowers(userId).collect { users ->
            _uiState.update { it.copy(followers = users, isLoading = false) }
        }
    }

    fun loadFollowing() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        userRepository.getFollowing(userId).collect { users ->
            _uiState.update { it.copy(following = users, isLoading = false) }
        }
        userRepository.getFollowingIds().collect { ids ->
            _uiState.update { it.copy(followingIds = ids) }
        }
    }

    fun toggleFollow(targetUserId: String) = viewModelScope.launch {
        val isFollowing = _uiState.value.followingIds.contains(targetUserId)
        if (isFollowing) {
            userRepository.unfollowUser(targetUserId)
            _uiState.update { it.copy(followingIds = it.followingIds - targetUserId) }
        } else {
            userRepository.followUser(targetUserId)
            _uiState.update { it.copy(followingIds = it.followingIds + targetUserId) }
        }
    }
}
