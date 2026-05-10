package dev.mslalith.focuslauncher.screens.aiscreen

import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

enum class PixState { IDLE, LISTENING, THINKING, RESPONDING }

data class AiScreenState(
    val messages: ImmutableList<ChatMessage> = persistentListOf(),
    val pixState: PixState = PixState.IDLE,
    val inputText: String = "",
    val isThinking: Boolean = false,
    val eventSink: (AiScreenUiEvent) -> Unit = {}
) : CircuitUiState

sealed interface AiScreenUiEvent : CircuitUiEvent {
    data class UpdateInput(val text: String) : AiScreenUiEvent
    data object SendMessage : AiScreenUiEvent
    data object NavigateBack : AiScreenUiEvent
}
