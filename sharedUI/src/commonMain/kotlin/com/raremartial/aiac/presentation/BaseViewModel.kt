package com.raremartial.aiac.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public abstract class BaseViewModel<State : Any, Action, Event>(initialState: State) : ViewModel(), KoinComponent {

    private val _viewStates = MutableStateFlow(initialState)

    private val _viewActions = MutableSharedFlow<Action?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun viewStates(): StateFlow<State> = _viewStates.asStateFlow()

    fun viewActions(): SharedFlow<Action?> = _viewActions.asSharedFlow()

    internal var viewState: State
        get() = _viewStates.value
        set(value) {
            _viewStates.value = value
        }

    internal var viewAction: Action?
        get() = _viewActions.replayCache.last()
        set(value) {
            _viewActions.tryEmit(value)
        }

    public abstract fun obtainEvent(viewEvent: Event)

    /**
     * Convenient method to perform work in [viewModelScope] scope.
     */
    internal fun withViewModelScope(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(block = block)
    }

    fun clearAction() {
        viewAction = null
    }

    override fun onCleared() {
        super.onCleared()
        clearAction()
    }
}