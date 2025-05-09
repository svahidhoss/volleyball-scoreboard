package com.vahossmedia.android.scoreboard

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "VolleyballViewModel"
const val CURRENT_BOARD_STATE_KEY = "CURRENT_BOARD_STATE_KEY"

// Volleyball default
private const val WINNING_SCORE = 25

class ScoreboardViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _scoreboardState = MutableStateFlow(
        savedStateHandle[CURRENT_BOARD_STATE_KEY] ?: ScoreboardState()
    )
    val scoreboardState: StateFlow<ScoreboardState> = _scoreboardState.asStateFlow()

    // Events
    private val _setWonEvent = MutableSharedFlow<String>(replay = 1)
    val setWonEvent: SharedFlow<String> = _setWonEvent.asSharedFlow()

    private val _matchWonEvent = MutableSharedFlow<String>(replay = 1)
    val matchWonEvent: SharedFlow<String> = _matchWonEvent.asSharedFlow()

    // Undo and Redo stacks
    private val undoStack = ArrayDeque<ScoreboardState>(10)
    private val redoStack = ArrayDeque<ScoreboardState>(10)

    // States to expose redo/undo availability
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    init {
        Log.d(TAG, "ViewModel instance created")
    }

    fun incrementScore(team: String) {
        clearRedoStack()
        // Save current state for undo
        saveStateForUndo()

        viewModelScope.launch {
            val currentState = _scoreboardState.value
            val incrementedState = when (team) {
                "A" -> currentState.copy(teamAScore = currentState.teamAScore + 1)
                "B" -> currentState.copy(teamBScore = currentState.teamBScore + 1)
                else -> currentState
            }

            val afterSetAndMatchCheck = checkSetAndMatchWinner(incrementedState)
            _scoreboardState.value = afterSetAndMatchCheck
            savedStateHandle[CURRENT_BOARD_STATE_KEY] = afterSetAndMatchCheck

            // Update undo availability
            _canUndo.value = undoStack.isNotEmpty()
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            // Save current state for redo
            saveStateForRedo()
            // Restore previous state
            val previousState = undoStack.removeLast()
            _scoreboardState.value = previousState

            // Save to SavedStateHandle
            savedStateHandle[CURRENT_BOARD_STATE_KEY] = previousState

            // Update availability
            _canRedo.value = redoStack.isNotEmpty()
            _canUndo.value = undoStack.isNotEmpty()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            saveStateForUndo()
            // Restore next state
            val nextState = redoStack.removeLast()
            _scoreboardState.value = nextState

            // Save to SavedStateHandle
            savedStateHandle[CURRENT_BOARD_STATE_KEY] = nextState

            // Update availability
            _canRedo.value = redoStack.isNotEmpty()
            _canUndo.value = undoStack.isNotEmpty()
        }
    }

    /**
     * Save current state to undo stack, maintain size limit
     */
    private fun saveStateForUndo() {
        if (undoStack.size >= 10) undoStack.removeFirst()
        undoStack.add(_scoreboardState.value)
    }

    /**
     * Save current state to redo stack, maintain size limit
     */
    private fun saveStateForRedo() {
        if (redoStack.size >= 10) redoStack.removeFirst()
        redoStack.add(_scoreboardState.value)
    }

    /**
     * Call clearRedoStack() at the beginning of any action
     * that changes state in a “new” way:
     * like incrementScore, resetSet or resetMatch
     */
    private fun clearRedoStack() {
        redoStack.clear()
        _canRedo.value = false
    }

    private fun checkSetAndMatchWinner(state: ScoreboardState): ScoreboardState {
        val aWins = state.teamAScore >= WINNING_SCORE && state.teamAScore - state.teamBScore >= 2
        val bWins = state.teamBScore >= WINNING_SCORE && state.teamBScore - state.teamAScore >= 2

        var updatedState = state

        // Check for set win
        if (aWins || bWins) {
            updatedState = if (aWins) {
                viewModelScope.launch { _setWonEvent.emit("Team A wins the set!") }
                updatedState.copy(
                    teamAScore = 0,
                    teamBScore = 0,
                    teamASets = updatedState.teamASets + 1
                )
            } else {
                viewModelScope.launch { _setWonEvent.emit("Team B wins the set!") }
                updatedState.copy(
                    teamAScore = 0,
                    teamBScore = 0,
                    teamBSets = updatedState.teamBSets + 1
                )
            }
        }

        // Check for match win
        if (updatedState.matchWinner == null) {
            val matchWinner = when {
                updatedState.teamASets >= 3 -> "A"
                updatedState.teamBSets >= 3 -> "B"
                else -> null
            }
            // Only update if a team just won
            if (matchWinner != null) {
                updatedState = updatedState.copy(matchWinner = matchWinner)
                viewModelScope.launch {
                    _matchWonEvent.emit("Team $matchWinner wins the match!")
                }
            }
        }

        return updatedState
    }

    fun resetSet(saveState: Boolean = true) {
        clearRedoStack()
        if (saveState) saveStateForUndo()
        _scoreboardState.value = _scoreboardState.value.copy(teamAScore = 0, teamBScore = 0)

        // Update stored state and undo availability
        savedStateHandle[CURRENT_BOARD_STATE_KEY] = _scoreboardState.value
        _canUndo.value = undoStack.isNotEmpty()
    }

    fun resetMatch() {
        clearRedoStack()
        saveStateForUndo()
        _scoreboardState.value = ScoreboardState()

        // Update stored state and undo availability
        savedStateHandle[CURRENT_BOARD_STATE_KEY] = _scoreboardState.value
        _canUndo.value = undoStack.isNotEmpty()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel instance about to be destroyed")
    }
}
