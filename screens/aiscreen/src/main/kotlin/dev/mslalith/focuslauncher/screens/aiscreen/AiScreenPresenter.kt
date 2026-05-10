package dev.mslalith.focuslauncher.screens.aiscreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.components.SingletonComponent
import dev.mslalith.focuslauncher.core.screens.AiScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val mockResponses = listOf(
    "Hola, soy Pix. ¿En qué puedo ayudarte hoy?",
    "Interesante pregunta. Déjame pensar un momento...",
    "Puedo ayudarte con eso. ¿Quieres que te explique más?",
    "¡Claro! Eso es algo que puedo hacer por ti.",
    "Entendido. Trabajando en ello..."
)

class AiScreenPresenter @AssistedInject constructor(
    @Assisted private val navigator: Navigator
) : Presenter<AiScreenState> {

    @CircuitInject(AiScreen::class, SingletonComponent::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): AiScreenPresenter
    }

    @Composable
    override fun present(): AiScreenState {
        var messages: ImmutableList<ChatMessage> by mutableStateOf(persistentListOf())
        var pixState by mutableStateOf(PixState.IDLE)
        var inputText by mutableStateOf("")
        var isThinking by mutableStateOf(false)
        val scope = rememberCoroutineScope()

        return AiScreenState(
            messages = messages,
            pixState = pixState,
            inputText = inputText,
            isThinking = isThinking,
            eventSink = { event ->
                when (event) {
                    is AiScreenUiEvent.UpdateInput -> inputText = event.text
                    is AiScreenUiEvent.SendMessage -> {
                        if (inputText.isBlank()) return@AiScreenState
                        val userMessage = ChatMessage(text = inputText.trim(), isUser = true)
                        messages = (messages + userMessage).toImmutableList()
                        inputText = ""
                        pixState = PixState.THINKING
                        isThinking = true
                        scope.launch {
                            delay(1500)
                            val reply = mockResponses.random()
                            messages = (messages + ChatMessage(text = reply, isUser = false)).toImmutableList()
                            pixState = PixState.RESPONDING
                            delay(800)
                            pixState = PixState.IDLE
                            isThinking = false
                        }
                    }
                    AiScreenUiEvent.NavigateBack -> navigator.pop()
                }
            }
        )
    }
}
