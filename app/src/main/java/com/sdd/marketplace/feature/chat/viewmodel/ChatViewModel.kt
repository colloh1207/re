package com.sdd.marketplace.feature.chat.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.core.util.SanitizationUtils
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun stringToReportCategory(s: String): ReportCategory = when {
    s.contains("spam", ignoreCase = true) || s.contains("scam", ignoreCase = true) -> ReportCategory.SPAM
    s.contains("fake", ignoreCase = true) -> ReportCategory.FAKE_LISTING
    s.contains("harassment", ignoreCase = true) || s.contains("bully", ignoreCase = true) -> ReportCategory.HARASSMENT
    s.contains("contact", ignoreCase = true) || s.contains("phone", ignoreCase = true) || s.contains("link", ignoreCase = true) -> ReportCategory.INAPPROPRIATE_CONTENT
    else -> ReportCategory.OTHER
}

data class InboxUiState(
    val chats: List<Chat> = emptyList(),
    val filteredChats: List<Chat> = emptyList(),
    val userSearchResults: List<User> = emptyList(),
    val unreadCount: Int = 0,
    val searchQuery: String = "",
    val selectedFilter: String = "All",
    val isLoading: Boolean = true,
    val error: String? = null
)

data class ChatUiState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val currentUserId: String = "",
    val messageText: String = "",
    val editingMessage: Message? = null,
    val replyingTo: Message? = null,
    val isTyping: Boolean = false,
    val partnerTyping: Boolean = false,
    val partnerOnline: Boolean = false,
    val isLoading: Boolean = true,
    val violationWarning: String? = null,
    val error: String? = null
)

sealed class ChatDetailEvent {
    data class ShowSnackbar(val message: String) : ChatDetailEvent()
    object ScrollToBottom : ChatDetailEvent()
}

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val blockRepository: BlockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init { loadChats() }

    private fun loadChats() = viewModelScope.launch {
        chatRepository.getChats().collect { chats ->
            _uiState.update { it.copy(chats = chats, filteredChats = applyFilter(chats, it.selectedFilter, it.searchQuery), isLoading = false) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            if (query.length >= 2) {
                viewModelScope.launch {
                    chatRepository.searchUsers(query).collect { users ->
                        _uiState.update { it.copy(userSearchResults = users) }
                    }
                }
            } else {
                _uiState.update { it.copy(userSearchResults = emptyList()) }
            }
            val filtered = if (query.isBlank()) state.chats
            else state.chats.filter { chat ->
                chat.participants.any { it.fullName.contains(query, ignoreCase = true) } ||
                chat.lastMessage?.content?.contains(query, ignoreCase = true) == true
            }
            state.copy(searchQuery = query, filteredChats = filtered)
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { state ->
            state.copy(selectedFilter = filter, filteredChats = applyFilter(state.chats, filter, state.searchQuery))
        }
    }

    private fun applyFilter(chats: List<Chat>, filter: String, query: String): List<Chat> {
        val filtered = when (filter) {
            "Unread" -> chats.filter { it.unreadCount > 0 }
            "Orders" -> chats.filter { it.productId != null }
            else -> chats
        }
        return if (query.isBlank()) filtered
        else filtered.filter { chat ->
            chat.participants.any { it.fullName.contains(query, ignoreCase = true) } ||
            chat.lastMessage?.content?.contains(query, ignoreCase = true) == true
        }
    }

    fun blockUser(userId: String) = viewModelScope.launch {
        blockRepository.blockUser(userId, null).onSuccess { loadChats() }
    }

    fun reportUser(userId: String, categoryStr: String, description: String) = viewModelScope.launch {
        val cat = stringToReportCategory(categoryStr)
        blockRepository.reportUser(userId, cat, description, emptyList())
    }
}

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val blockRepository: BlockRepository,
    private val moderationRepository: ModerationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val chatId: String = savedStateHandle["chatId"] ?: ""

    private val _uiState = MutableStateFlow(ChatUiState(currentUserId = authRepository.getCurrentUserId() ?: ""))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatDetailEvent>()
    val events: SharedFlow<ChatDetailEvent> = _events.asSharedFlow()

    private val _typingDebounce = MutableStateFlow(false)

    init {
        if (chatId.isNotBlank()) {
            loadMessages()
            loadChat()
            observeTyping()
            markRead()
            observePartnerOnline()
        }
        observeTypingDebounce()
    }

    private fun loadChat() = viewModelScope.launch {
        chatRepository.getChat(chatId).collect { chat ->
            _uiState.update { it.copy(chat = chat) }
            // Observe partner online status
            val partnerId = chat.participants.firstOrNull { it.id != _uiState.value.currentUserId }?.id
            partnerId?.let { observePartnerOnlineById(it) }
        }
    }

    private fun loadMessages() = viewModelScope.launch {
        chatRepository.getMessages(chatId).collect { messages ->
            _uiState.update { it.copy(messages = messages, isLoading = false) }
            _events.emit(ChatDetailEvent.ScrollToBottom)
        }
    }

    private fun observeTyping() = viewModelScope.launch {
        chatRepository.observeTypingStatus(chatId).collect { typingUserId ->
            val isPartnerTyping = typingUserId != null && typingUserId != _uiState.value.currentUserId
            _uiState.update { it.copy(partnerTyping = isPartnerTyping) }
        }
    }

    private fun observePartnerOnline() {}

    private fun observePartnerOnlineById(partnerId: String) = viewModelScope.launch {
        chatRepository.observeOnlineStatus(partnerId).collect { isOnline ->
            _uiState.update { it.copy(partnerOnline = isOnline) }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeTypingDebounce() = viewModelScope.launch {
        _typingDebounce.debounce(1000).collect { isTyping ->
            if (!isTyping) chatRepository.sendTypingIndicator(chatId, false)
        }
    }

    private fun markRead() = viewModelScope.launch { chatRepository.markChatRead(chatId) }

    fun onMessageTextChanged(text: String) {
        _uiState.update { it.copy(messageText = text, violationWarning = null) }
        viewModelScope.launch {
            chatRepository.sendTypingIndicator(chatId, text.isNotBlank())
            _typingDebounce.value = text.isNotBlank()
            if (text.length > 5) {
                val check = moderationRepository.checkMessageForViolations(text)
                if (check.hasViolation) {
                    val warn = when (check.violationType) {
                        ViolationType.LINK -> "Sharing links is not allowed. This may result in a warning."
                        ViolationType.PHONE_NUMBER -> "Sharing phone numbers is not allowed. Use the app to communicate."
                        ViolationType.CARD_NUMBER -> "Sharing payment details is strictly prohibited."
                        else -> "This content is not allowed on our platform."
                    }
                    _uiState.update { it.copy(violationWarning = warn) }
                }
            }
        }
    }

    fun sendMessage() = viewModelScope.launch {
        val text = SanitizationUtils.sanitizeChatMessage(_uiState.value.messageText)
        if (text.isBlank()) return@launch
        val check = moderationRepository.checkMessageForViolations(text)
        if (check.hasViolation) {
            _events.emit(ChatDetailEvent.ShowSnackbar("Message blocked: ${check.violationType?.name}. Repeated violations will result in suspension."))
            moderationRepository.issueWarning(_uiState.value.currentUserId, "Attempted to send ${check.violationType?.name}", text)
            return@launch
        }
        val editingMsg = _uiState.value.editingMessage
        val replyToId = _uiState.value.replyingTo?.id
        if (editingMsg != null) {
            _uiState.update { it.copy(messageText = "", editingMessage = null) }
            chatRepository.editMessage(editingMsg.id, text)
                .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
        } else {
            _uiState.update { it.copy(messageText = "", replyingTo = null) }
            chatRepository.sendMessage(chatId, text, replyToId = replyToId)
                .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
        }
    }

    fun startEditing(message: Message) {
        _uiState.update { it.copy(editingMessage = message, messageText = message.content, replyingTo = null) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(editingMessage = null, messageText = "") }
    }

    fun startReplying(message: Message) {
        _uiState.update { it.copy(replyingTo = message, editingMessage = null) }
    }

    fun cancelReplying() {
        _uiState.update { it.copy(replyingTo = null) }
    }

    fun deleteMessage(messageId: String) = viewModelScope.launch {
        chatRepository.deleteMessage(messageId)
            .onFailure { _events.emit(ChatDetailEvent.ShowSnackbar("Failed to delete")) }
    }

    fun unsendMessage(messageId: String) = viewModelScope.launch {
        chatRepository.unsendMessage(messageId)
            .onSuccess { _events.emit(ChatDetailEvent.ShowSnackbar("Message unsent")) }
            .onFailure { _events.emit(ChatDetailEvent.ShowSnackbar("Failed to unsend")) }
    }

    fun sendLocation(lat: Double, lng: Double, address: String) = viewModelScope.launch {
        chatRepository.sendLocationMessage(chatId, lat, lng, address)
            .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
    }

    fun sendImage() = viewModelScope.launch {}

    fun blockUser(userId: String) = viewModelScope.launch { blockRepository.blockUser(userId, null) }

    fun reportUser(userId: String, categoryStr: String, description: String) = viewModelScope.launch {
        val cat = stringToReportCategory(categoryStr)
        blockRepository.reportUser(userId, cat, description, emptyList())
    }

    fun dismissViolationWarning() = _uiState.update { it.copy(violationWarning = null) }
}
