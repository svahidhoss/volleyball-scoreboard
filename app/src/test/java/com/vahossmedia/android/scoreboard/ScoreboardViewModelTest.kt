package com.vahossmedia.android.scoreboard

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

@ExperimentalCoroutinesApi
class ScoreboardViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        // Reset the Main dispatcher after tests
        Dispatchers.resetMain()
    }

    @Test
    fun providesExpectedScoresOnInitiation() {
        val savedStateHandle = SavedStateHandle()
        val scoreboardViewModel = ScoreboardViewModel(savedStateHandle)
        assertEquals(0, scoreboardViewModel.scoreboardState.value.teamAScore)
        assertEquals(0, scoreboardViewModel.scoreboardState.value.teamBScore)
        assertEquals(0, scoreboardViewModel.scoreboardState.value.teamASets)
        assertEquals(0, scoreboardViewModel.scoreboardState.value.teamBSets)
    }

    @Test
    fun testIncrementScoreAndCheckSetValues() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Increment Team A's score to 25 to win a set
        repeat(25) { viewModel.incrementScore("A") }

        // Check that Team A has won 1 set
        assertEquals(1, viewModel.scoreboardState.value.teamASets)
        assertEquals(0, viewModel.scoreboardState.value.teamBSets)

        // Check that scores are reset after winning a set
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(0, viewModel.scoreboardState.value.teamBScore)
    }

    @Test
    fun testIncrementScoreAndCheckSetValuesForTeamB() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Increment Team B's score to 25 to win a set
        repeat(25) { viewModel.incrementScore("B") }

        // Check that Team B has won 1 set
        assertEquals(0, viewModel.scoreboardState.value.teamASets)
        assertEquals(1, viewModel.scoreboardState.value.teamBSets)

        // Check that scores are reset after winning a set
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(0, viewModel.scoreboardState.value.teamBScore)
    }

    @Test
    fun testIncrementScoreAndCheckGameWin() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Simulate a game where Team A wins 3 sets
        repeat(3) {
            repeat(25) { viewModel.incrementScore("A") }
        }

        // Check that Team A has won the game
        assertEquals(3, viewModel.scoreboardState.value.teamASets)
        assertEquals(0, viewModel.scoreboardState.value.teamBSets)
    }

    @Test
    fun testResetGame() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Simulate a game where Team A wins 3 sets
        repeat(3) {
            repeat(25) { viewModel.incrementScore("A") }
        }

        // Reset the game
        viewModel.resetMatch()

        // Check that all values are reset
        assertEquals(0, viewModel.scoreboardState.value.teamASets)
        assertEquals(0, viewModel.scoreboardState.value.teamBSets)
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(0, viewModel.scoreboardState.value.teamBScore)
    }
}
