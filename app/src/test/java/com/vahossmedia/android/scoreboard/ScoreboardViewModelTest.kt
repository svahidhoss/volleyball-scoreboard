package com.vahossmedia.android.scoreboard

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

@ExperimentalCoroutinesApi
class ScoreboardViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
    fun testIncrementScoreAndCheckSetValuesForTeamA() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Increment Team A's score step by step and verify each value
        for (expectedScore in 1..24) {
            viewModel.incrementScore("A")
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(expectedScore, viewModel.scoreboardState.value.teamAScore)
        }

        // Increment Team A's score to win a set
        viewModel.incrementScore("A")
        testDispatcher.scheduler.advanceUntilIdle()

        // Check that Team A has won 1 set
        assertEquals(1, viewModel.scoreboardState.value.teamASets)
        assertEquals(0, viewModel.scoreboardState.value.teamBSets)

        // Check that scores are reset after winning a set
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(0, viewModel.scoreboardState.value.teamBScore)

        // Check Won Event value
        val setWonMessage = viewModel.setWonEvent.replayCache.firstOrNull()
        assertEquals("Team A wins the set!", setWonMessage)
    }

    @Test
    fun testIncrementScoreAndCheckSetValuesForTeamB() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Increment Team B's score to 25 to win a set
        repeat(25) { viewModel.incrementScore("B") }
        testDispatcher.scheduler.advanceUntilIdle()

        // Check that Team B has won 1 set
        assertEquals(0, viewModel.scoreboardState.value.teamASets)
        assertEquals(1, viewModel.scoreboardState.value.teamBSets)

        // Check that scores are reset after winning a set
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(0, viewModel.scoreboardState.value.teamBScore)

        // Check Won Event value
        val setWonMessage = viewModel.setWonEvent.replayCache.firstOrNull()
        assertEquals("Team B wins the set!", setWonMessage)
    }

    @Test
    fun testIncrementScoreAndCheckGameWin() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Simulate a game where Team A wins 3 sets
        repeat(3) {
            repeat(25) { viewModel.incrementScore("A") }
        }

        testDispatcher.scheduler.advanceUntilIdle()

        // Check that Team A has won the game
        assertEquals(3, viewModel.scoreboardState.value.teamASets)
        assertEquals(0, viewModel.scoreboardState.value.teamBSets)

        assertEquals("Team A wins the match!", viewModel.matchWonEvent.replayCache.firstOrNull())
    }

    @Test
    fun testResetGame() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Simulate a game where Team B wins 3 sets
        repeat(3) {
            repeat(25) { viewModel.incrementScore("B") }
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Team B wins the match!", viewModel.matchWonEvent.replayCache.firstOrNull())

        // Reset the game
        viewModel.resetMatch()

        // Check that all values are reset
        assertEquals(0, viewModel.scoreboardState.value.teamASets)
        assertEquals(0, viewModel.scoreboardState.value.teamBSets)
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(0, viewModel.scoreboardState.value.teamBScore)
    }

    @Test
    fun testUndo() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Initial state - canUndo should be false
        assertEquals(false, viewModel.canUndo.value)

        // Increment score
        viewModel.incrementScore("A")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.scoreboardState.value.teamAScore)
        assertEquals(true, viewModel.canUndo.value)

        // Undo
        viewModel.undo()
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(false, viewModel.canUndo.value)
    }

    @Test
    fun testMultipleUndos() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Perform multiple actions
        repeat(5) {
            viewModel.incrementScore("A")
            testDispatcher.scheduler.advanceUntilIdle()
        }
        assertEquals(5, viewModel.scoreboardState.value.teamAScore)

        // Undo multiple times
        repeat(3) {
            viewModel.undo()
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.scoreboardState.value.teamAScore)
        assertEquals(true, viewModel.canUndo.value)

        // Undo remaining actions
        repeat(2) { viewModel.undo() }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(false, viewModel.canUndo.value)
    }

    @Test
    fun testUndoAndRedoFlow() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        repeat(3) {
            viewModel.incrementScore("A")
            testDispatcher.scheduler.advanceUntilIdle()
        }
        assertEquals(3, viewModel.scoreboardState.value.teamAScore)
        viewModel.undo()
        assertEquals(2, viewModel.scoreboardState.value.teamAScore)
        viewModel.redo()
        assertEquals(3, viewModel.scoreboardState.value.teamAScore)
    }

    @Test
    fun testRedoRestoresStateAfterUndo() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Take two actions
        viewModel.incrementScore("A")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.incrementScore("A")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.scoreboardState.value.teamAScore)

        // Undo twice
        viewModel.undo()
        viewModel.undo()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)

        // Redo once
        viewModel.redo()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.scoreboardState.value.teamAScore)

        // Redo again
        viewModel.redo()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.scoreboardState.value.teamAScore)
    }

    @Test
    fun testRedoClearedOnNewAction() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        viewModel.incrementScore("A")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.incrementScore("A")
        testDispatcher.scheduler.advanceUntilIdle()

        // Undo once
        viewModel.undo()
        assertEquals(1, viewModel.scoreboardState.value.teamAScore)

        // Now take a new action
        viewModel.incrementScore("A")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.scoreboardState.value.teamAScore)

        // Now redo should do nothing, because stack is cleared
        viewModel.redo()
        // Still 2, redo did nothing
        assertEquals(2, viewModel.scoreboardState.value.teamAScore)
    }

    @Test
    fun testUndoRedoWithResetSetAndResetMatch() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Increment, ResetSet, Increment again
        viewModel.incrementScore("A")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.resetSet()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.incrementScore("B")
        testDispatcher.scheduler.advanceUntilIdle()

        // Undo last increment
        viewModel.undo()
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(0, viewModel.scoreboardState.value.teamBScore)

        // Redo last increment
        viewModel.redo()
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(1, viewModel.scoreboardState.value.teamBScore)

        // Now resetMatch and undo/redo
        viewModel.resetMatch()
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(0, viewModel.scoreboardState.value.teamBScore)
        assertEquals(0, viewModel.scoreboardState.value.teamASets)
        assertEquals(0, viewModel.scoreboardState.value.teamBSets)

        // Undo resetMatch
        viewModel.undo()
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(1, viewModel.scoreboardState.value.teamBScore)

        // Redo resetMatch
        viewModel.redo()
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(0, viewModel.scoreboardState.value.teamBScore)
        assertEquals(0, viewModel.scoreboardState.value.teamASets)
        assertEquals(0, viewModel.scoreboardState.value.teamBSets)
    }

    @Test
    fun testCanRedoReflectsRedoStack() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Initially, canRedo should be false
        assertFalse(viewModel.canRedo.value)

        // Do something, then undo
        viewModel.incrementScore("A")
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.undo()

        // Now canRedo should be true
        assertTrue(viewModel.canRedo.value)

        // Redo, should now be false
        viewModel.redo()
        assertFalse(viewModel.canRedo.value)
    }

    @Test
    fun testRedoNoOpWhenStackEmpty() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        viewModel.redo()
        // Still initial state
        assertEquals(0, viewModel.scoreboardState.value.teamAScore)
        assertEquals(0, viewModel.scoreboardState.value.teamBScore)
    }

    @Test
    fun testMultipleUndosAndRedos() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Take 3 actions
        repeat(3) {
            viewModel.incrementScore("A")
            testDispatcher.scheduler.advanceUntilIdle()
        }
        assertEquals(3, viewModel.scoreboardState.value.teamAScore)

        // Undo twice
        viewModel.undo()
        viewModel.undo()
        assertEquals(1, viewModel.scoreboardState.value.teamAScore)

        // Redo once
        viewModel.redo()
        assertEquals(2, viewModel.scoreboardState.value.teamAScore)

        // Redo again
        viewModel.redo()
        assertEquals(3, viewModel.scoreboardState.value.teamAScore)
    }


}
