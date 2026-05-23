package com.sdd.marketplace.feature.telegram.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import timber.log.Timber
import javax.inject.Inject

enum class TelegramConnectionState { DISCONNECTED, VALIDATING, CONNECTED }

data class TelegramUiState(
    val connectionState: TelegramConnectionState = TelegramConnectionState.DISCONNECTED,
    val botUsername: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TelegramBotViewModel @Inject constructor(
    private val auth: Auth,
    private val functions: Functions,
    private val postgrest: Postgrest
) : ViewModel() {

    private val _uiState = MutableStateFlow(TelegramUiState())
    val uiState: StateFlow<TelegramUiState> = _uiState.asStateFlow()

    init {
        checkExistingConnection()
    }

    private fun checkExistingConnection() = viewModelScope.launch {
        try {
            val userId = auth.currentUserOrNull()?.id ?: return@launch
            val result = postgrest["telegram_connections"].select {
                filter { eq("user_id", userId) }
            }.decodeSingleOrNull<TelegramConnectionDto>()
            if (result != null) {
                _uiState.update {
                    it.copy(
                        connectionState = TelegramConnectionState.CONNECTED,
                        botUsername = result.botUsername
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking Telegram connection")
        }
    }

    fun connectBot(token: String) = viewModelScope.launch {
        if (token.isBlank() || !token.contains(":")) {
            _uiState.update { it.copy(error = "Please enter a valid bot token") }
            return@launch
        }
        _uiState.update { it.copy(connectionState = TelegramConnectionState.VALIDATING, isLoading = true, error = null) }
        try {
            val response = functions.invoke(
                function = "telegram-bot",
                body = buildJsonObject {
                    put("action", "connect")
                    put("token", token)
                }
            )
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            val success = body["success"]?.jsonPrimitive?.boolean ?: false
            if (success) {
                val botUsername = body["botUsername"]?.jsonPrimitive?.contentOrNull
                _uiState.update {
                    it.copy(
                        connectionState = TelegramConnectionState.CONNECTED,
                        botUsername = botUsername,
                        isLoading = false,
                        error = null
                    )
                }
            } else {
                val error = body["error"]?.jsonPrimitive?.contentOrNull ?: "Invalid bot token. Please check and try again."
                _uiState.update {
                    it.copy(
                        connectionState = TelegramConnectionState.DISCONNECTED,
                        isLoading = false,
                        error = error
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error connecting Telegram bot")
            _uiState.update {
                it.copy(
                    connectionState = TelegramConnectionState.DISCONNECTED,
                    isLoading = false,
                    error = "Connection failed: ${e.message ?: "Please try again."}"
                )
            }
        }
    }

    fun disconnectBot() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            functions.invoke(
                function = "telegram-bot",
                body = buildJsonObject { put("action", "disconnect") }
            )
            _uiState.update {
                it.copy(
                    connectionState = TelegramConnectionState.DISCONNECTED,
                    botUsername = null,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error disconnecting Telegram bot")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Disconnection failed: ${e.message ?: "Please try again."}"
                )
            }
        }
    }
}

@kotlinx.serialization.Serializable
private data class TelegramConnectionDto(
    @kotlinx.serialization.SerialName("bot_username") val botUsername: String?
)

private fun buildJsonObject(builder: JsonObjectBuilder.() -> Unit) = kotlinx.serialization.json.buildJsonObject(builder)
