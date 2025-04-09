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
import kotlin.math.abs

private const val TAG = "VolleyballViewModel"
const val CURRENT_BOARD_STATE_KEY = "CURRENT_BOARD_STATE_KEY"

// Volleyball default
private const val WINNING_SCORE = 25

class ScoreboardViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _scoreboardState = MutableStateFlow(
        savedStateHandle[CURRENT_BOARD_STATE_KEY] ?: ScoreboardState()
    )
    val scoreboardState: StateFlow<ScoreboardState> = _scoreboardState.asStateFlow()

    private val _setWonEvent = MutableSharedFlow<String>(replay = 1)
    val setWonEvent: SharedFlow<String> = _setWonEvent.asSharedFlow()

    private val _matchWonEvent = MutableStateFlow("")
    val matchWonEvent: StateFlow<String> = _matchWonEvent


    init {
        Log.d(TAG, "ViewModel instance created")
    }

    fun incrementScore(team: String) {
        viewModelScope.launch {
            val currentState = _scoreboardState.value
            if (team == "A") {
                _scoreboardState.value = currentState.copy(teamAScore = currentState.teamAScore + 1)
            } else if (team == "B") {
                _scoreboardState.value = currentState.copy(teamBScore = currentState.teamBScore + 1)
            }
            savedStateHandle[CURRENT_BOARD_STATE_KEY] = _scoreboardState.value
            checkSetWinner()
        }
    }

    fun decrementScore(team: String) {
        viewModelScope.launch {
            val currentState = _scoreboardState.value
            if (team == "A" && _scoreboardState.value.teamAScore > 0) {
                _scoreboardState.value = currentState.copy(teamAScore = currentState.teamAScore - 1)
            } else if (team == "B" && _scoreboardState.value.teamBScore > 0) {
                _scoreboardState.value = currentState.copy(teamBScore = currentState.teamBScore - 1)
            }
        }
        savedStateHandle[CURRENT_BOARD_STATE_KEY] = _scoreboardState.value
    }

    private fun checkSetWinner() {
        val currentState = _scoreboardState.value
        if ((currentState.teamAScore >= WINNING_SCORE || currentState.teamBScore >= WINNING_SCORE) &&
            abs(currentState.teamAScore - currentState.teamBScore) >= 2
        ) {
            if (currentState.teamAScore > currentState.teamBScore) {
                _scoreboardState.value = currentState.copy(teamASets = currentState.teamASets + 1)
                viewModelScope.launch {
                    _setWonEvent.emit("Team A wins the set!")
                }
            } else {
                _scoreboardState.value = currentState.copy(teamBSets = currentState.teamBSets + 1)
                viewModelScope.launch {
                    _setWonEvent.emit("Team B wins the set!")
                }
            }
            resetSet()
        }

        // Check for match win
        if (currentState.teamASets >= 3 || currentState.teamBSets >= 3) {
            if (currentState.teamASets > currentState.teamBSets) {
                viewModelScope.launch {
                    _matchWonEvent.emit("Team A wins the match!")
                }
            } else {
                viewModelScope.launch {
                    _matchWonEvent.emit("Team B wins the match!")
                }
            }

        }
    }

    fun resetSet() {
        _scoreboardState.value = _scoreboardState.value.copy(teamAScore = 0, teamBScore = 0)
    }

    fun resetMatch() {
        _scoreboardState.value = ScoreboardState()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel instance about to be destroyed")
    }
}
