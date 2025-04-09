package com.vahossmedia.android.scoreboard

import androidx.lifecycle.SavedStateHandle
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class ScoreboardViewModelTest {

    @Test
    fun providesExpectedScoresOnInitiation() {
        val savedStateHandle = SavedStateHandle()
        val quizViewModel = ScoreboardViewModel(savedStateHandle)
        assertEquals(0, quizViewModel.scoreboardState.value.teamAScore)
        assertEquals(0, quizViewModel.scoreboardState.value.teamBScore)
        assertEquals(0, quizViewModel.scoreboardState.value.teamASets)
        assertEquals(0, quizViewModel.scoreboardState.value.teamBSets)
    }
}