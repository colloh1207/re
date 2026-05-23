package com.sdd.marketplace.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.core.util.ErrorHandler
import com.sdd.marketplace.domain.model.Block
import com.sdd.marketplace.domain.repository.BlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedUsersUiState(
    val isLoading: Boolean = true,
    val blockedUsers: List<Block> = emptyList(),
    val unblockingIds: Set<String> = emptySet(),
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val blockRepository: BlockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

    init {
        loadBlockedUsers()
    }

    private fun loadBlockedUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            blockRepository.getBlockedUsers()
                .catch { e ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = ErrorHandler.friendlyMessage(e)
                    )}
                }
                .collect { blocks ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        blockedUsers = blocks
                    )}
                }
        }
    }

    fun unblockUser(userId: String) = viewModelScope.launch {
        _uiState.update { it.copy(unblockingIds = it.unblockingIds + userId) }
        blockRepository.unblockUser(userId)
            .onSuccess {
                _uiState.update { s ->
                    s.copy(
                        unblockingIds = s.unblockingIds - userId,
                        blockedUsers = s.blockedUsers.filter { it.blockedId != userId },
                        message = "User unblocked successfully"
                    )
                }
            }
            .onFailure { e ->
                _uiState.update { s ->
                    s.copy(
                        unblockingIds = s.unblockingIds - userId,
                        error = ErrorHandler.friendlyMessage(e)
                    )
                }
            }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
