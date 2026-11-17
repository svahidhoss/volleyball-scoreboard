package com.vahossmedia.android.scoreboard

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

private const val TAG = "VolleyballViewModel"
const val CURRENT_BOARD_STATE_KEY = "CURRENT_BOARD_STATE_KEY"


class ScoreboardViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _scoreboardState = MutableStateFlow(
        savedStateHandle[CURRENT_BOARD_STATE_KEY] ?: ScoreboardState()
    )
    val scoreboardState: StateFlow<ScoreboardState> = _scoreboardState.asStateFlow()

    // One-time events: each is delivered once and never replayed, so a recreated Activity
    // (e.g. after a rotation) doesn't show the toast/dialog or play the sound again.
    // Buffered, so an event sent while no screen is collecting waits for the next one.
    private val _setWonEvent = Channel<String>(Channel.BUFFERED)
    val setWonEvent: Flow<String> = _setWonEvent.receiveAsFlow()

    private val _matchWonEvent = Channel<String>(Channel.BUFFERED)
    val matchWonEvent: Flow<String> = _matchWonEvent.receiveAsFlow()

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
        // The match is over: ignore taps until Undo or Reset Match
        if (_scoreboardState.value.matchWinner != null) return

        clearRedoStack()
        // Save current state for undo
        saveStateForUndo()

        val currentState = _scoreboardState.value
        val incrementedState = when (team) {
            "A" -> currentState.copy(teamAScore = currentState.teamAScore + 1)
            "B" -> currentState.copy(teamBScore = currentState.teamBScore + 1)
            else -> currentState
        }

        val (newState, setWinMsg, matchWinMsg) = checkSetAndMatchWinner(incrementedState)
        _scoreboardState.value = newState
        savedStateHandle[CURRENT_BOARD_STATE_KEY] = newState
        // Update undo availability
        _canUndo.value = undoStack.isNotEmpty()

        // Buffered channels accept events without waiting, so trySend works here without a coroutine.
        // (It only fails if 64 events are already waiting unread, which can't happen in this app.)
        setWinMsg?.let { _setWonEvent.trySend(it) }
        matchWinMsg?.let { _matchWonEvent.trySend(it) }
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
        if (redoStack.isNotEmpty()) {
            redoStack.clear()
            _canRedo.value = false
        }
    }

    private data class SetCheckResult(
        val state: ScoreboardState,
        val setWinMsg: String?,
        val matchWinMsg: String?
    )

    private fun checkSetAndMatchWinner(state: ScoreboardState): SetCheckResult {
        val pointsToWin = if (state.isDecidingSet) POINTS_TO_WIN_DECIDING_SET else POINTS_TO_WIN_SET

        val aWins = state.teamAScore >= pointsToWin && state.teamAScore - state.teamBScore >= 2
        val bWins = state.teamBScore >= pointsToWin && state.teamBScore - state.teamAScore >= 2

        var updatedState = state
        var setWinMsg: String? = null
        var matchWinMsg: String? = null

        if (aWins || bWins) {
            setWinMsg = if (aWins) "Team A wins the set!" else "Team B wins the set!"
            updatedState = if (aWins) {
                updatedState.copy(teamAScore = 0, teamBScore = 0, teamASets = updatedState.teamASets + 1)
            } else {
                updatedState.copy(teamAScore = 0, teamBScore = 0, teamBSets = updatedState.teamBSets + 1)
            }
        }

        if (updatedState.matchWinner == null) {
            val matchWinner = when {
                updatedState.teamASets >= SETS_TO_WIN_MATCH -> "A"
                updatedState.teamBSets >= SETS_TO_WIN_MATCH -> "B"
                else -> null
            }
            // Only update if a team just won
            if (matchWinner != null) {
                updatedState = updatedState.copy(matchWinner = matchWinner)
                matchWinMsg = "Team $matchWinner wins the match!"
            }
        }

        return SetCheckResult(updatedState, setWinMsg, matchWinMsg)
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
