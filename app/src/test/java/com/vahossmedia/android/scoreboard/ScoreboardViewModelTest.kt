package com.vahossmedia.android.scoreboard

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
    fun testIncrementScoreAndCheckSetValuesForTeamA() = runTest(testDispatcher) {
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
        val setWonMessage = viewModel.setWonEvent.first()
        assertEquals("Team A wins the set!", setWonMessage)
    }

    @Test
    fun testIncrementScoreAndCheckSetValuesForTeamB() = runTest(testDispatcher) {
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
        val setWonMessage = viewModel.setWonEvent.first()
        assertEquals("Team B wins the set!", setWonMessage)
    }

    @Test
    fun testIncrementScoreAndCheckGameWin() = runTest(testDispatcher) {
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

        assertEquals("Team A wins the match!", viewModel.matchWonEvent.first())
    }

    @Test
    fun testResetGame() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val viewModel = ScoreboardViewModel(savedStateHandle)

        // Simulate a game where Team B wins 3 sets
        repeat(3) {
            repeat(25) { viewModel.incrementScore("B") }
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Team B wins the match!", viewModel.matchWonEvent.first())

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

    /**
     * The match-won event is a one-time event: once a screen has received it, a new screen
     * (e.g. the Activity recreated after a rotation) must not receive it again, so the
     * "Match Won!" dialog and sound are not repeated.
     */
    @Test
    fun matchWonEventIsNotDeliveredAgainToANewCollector() = runTest(testDispatcher) {
        val viewModel = ScoreboardViewModel(SavedStateHandle())

        // The first Activity sees the match win once
        val firstScreen = mutableListOf<String>()
        val firstCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.matchWonEvent.collect { firstScreen.add(it) }
        }
        winMatch(viewModel, "A")
        advanceUntilIdle()
        assertEquals(listOf("Team A wins the match!"), firstScreen)

        // Rotation: the old Activity stops collecting and a new one starts
        firstCollector.cancel()
        val secondScreen = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.matchWonEvent.collect { secondScreen.add(it) }
        }
        advanceUntilIdle()

        assertEquals(emptyList<String>(), secondScreen)
    }

    /**
     * The set-won event is a one-time event: after a rotation, the recreated Activity must not
     * receive it again, so the "wins the set" toast and sound are not repeated.
     */
    @Test
    fun setWonEventIsNotDeliveredAgainToANewCollector() = runTest(testDispatcher) {
        val viewModel = ScoreboardViewModel(SavedStateHandle())

        val firstScreen = mutableListOf<String>()
        val firstCollector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.setWonEvent.collect { firstScreen.add(it) }
        }
        winSet(viewModel, "B")
        advanceUntilIdle()
        assertEquals(listOf("Team B wins the set!"), firstScreen)

        firstCollector.cancel()
        val secondScreen = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.setWonEvent.collect { secondScreen.add(it) }
        }
        advanceUntilIdle()

        assertEquals(emptyList<String>(), secondScreen)
    }

    /**
     * Once a team has won the match, tapping either team's score area changes nothing:
     * scores, sets and the winner stay exactly as they were.
     */
    @Test
    fun scoringIsIgnoredAfterMatchIsWon() {
        val viewModel = ScoreboardViewModel(SavedStateHandle())
        winMatch(viewModel, "A")
        testDispatcher.scheduler.advanceUntilIdle()
        val wonState = viewModel.scoreboardState.value

        viewModel.incrementScore("A")
        viewModel.incrementScore("B")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(wonState, viewModel.scoreboardState.value)
        assertEquals("A", viewModel.scoreboardState.value.matchWinner)
    }

    /**
     * A tap that is ignored because the match is over must not push an undo step: a single
     * Undo goes straight back to just before the winning point (24–0 in the third set).
     */
    @Test
    fun ignoredTapAfterMatchDoesNotAddAnUndoStep() {
        val viewModel = ScoreboardViewModel(SavedStateHandle())
        winMatch(viewModel, "A")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.incrementScore("A") // ignored
        viewModel.undo()

        // A single undo goes straight back to just before the winning point
        assertEquals(24, viewModel.scoreboardState.value.teamAScore)
        assertEquals(2, viewModel.scoreboardState.value.teamASets)
        assertNull(viewModel.scoreboardState.value.matchWinner)
    }

    /**
     * Undoing the winning point clears the match winner, so scoring works again from
     * 24–0 in the third set.
     */
    @Test
    fun undoingTheWinningPointAllowsScoringAgain() {
        val viewModel = ScoreboardViewModel(SavedStateHandle())
        winMatch(viewModel, "A")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.undo()
        viewModel.incrementScore("B")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(24, viewModel.scoreboardState.value.teamAScore)
        assertEquals(1, viewModel.scoreboardState.value.teamBScore)
        assertNull(viewModel.scoreboardState.value.matchWinner)
    }

    /**
     * Reset Match after a win starts a fresh match: the winner and sets are cleared and
     * the next tap scores normally.
     */
    @Test
    fun resetMatchAfterWinAllowsScoringAgain() {
        val viewModel = ScoreboardViewModel(SavedStateHandle())
        winMatch(viewModel, "B")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetMatch()
        viewModel.incrementScore("A")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.scoreboardState.value.teamAScore)
        assertEquals(0, viewModel.scoreboardState.value.teamBSets)
        assertNull(viewModel.scoreboardState.value.matchWinner)
    }

    /**
     * In the deciding 5th set (2–2 in sets) the target is 15, not 25: 15–13 wins the set
     * and the match.
     */
    @Test
    fun decidingSetIsWonAt15WithATwoPointLead() {
        val viewModel = ScoreboardViewModel(SavedStateHandle())
        reachDecidingSet(viewModel)

        addPoints(viewModel, "B", 13)
        addPoints(viewModel, "A", 15)

        assertEquals(3, viewModel.scoreboardState.value.teamASets)
        assertEquals(2, viewModel.scoreboardState.value.teamBSets)
        assertEquals("A", viewModel.scoreboardState.value.matchWinner)
    }

    /**
     * The deciding set still needs a 2-point lead: 15–14 doesn't win it, 16–14 does.
     */
    @Test
    fun decidingSetNeedsATwoPointLead() {
        val viewModel = ScoreboardViewModel(SavedStateHandle())
        reachDecidingSet(viewModel)

        addPoints(viewModel, "A", 14)
        addPoints(viewModel, "B", 14)
        addPoints(viewModel, "A", 1)
        assertEquals(15, viewModel.scoreboardState.value.teamAScore)
        assertNull(viewModel.scoreboardState.value.matchWinner)

        addPoints(viewModel, "A", 1)
        assertEquals("A", viewModel.scoreboardState.value.matchWinner)
        assertEquals(3, viewModel.scoreboardState.value.teamASets)
    }

    /**
     * Sets before the deciding set still go to 25: at 2–1 in sets, reaching 15 points
     * does not end the set.
     */
    @Test
    fun setsBeforeTheDecidingSetStillGoTo25() {
        val viewModel = ScoreboardViewModel(SavedStateHandle())
        winSet(viewModel, "A")
        winSet(viewModel, "B")
        winSet(viewModel, "A")

        addPoints(viewModel, "B", 15)

        assertEquals(15, viewModel.scoreboardState.value.teamBScore)
        assertEquals(1, viewModel.scoreboardState.value.teamBSets)
        assertNull(viewModel.scoreboardState.value.matchWinner)
    }

    /**
     * Undo after winning the match 15–13 in the deciding set goes back to 14–13, still 2–2
     * in sets, with no winner.
     */
    @Test
    fun undoAfterDecidingSetWinGoesBackToTheLastPoint() {
        val viewModel = ScoreboardViewModel(SavedStateHandle())
        reachDecidingSet(viewModel)
        addPoints(viewModel, "B", 13)
        addPoints(viewModel, "A", 15)

        viewModel.undo()

        assertEquals(14, viewModel.scoreboardState.value.teamAScore)
        assertEquals(13, viewModel.scoreboardState.value.teamBScore)
        assertEquals(2, viewModel.scoreboardState.value.teamASets)
        assertEquals(2, viewModel.scoreboardState.value.teamBSets)
        assertNull(viewModel.scoreboardState.value.matchWinner)
    }

    /**
     * isDecidingSet is only true at 2–2 in sets: not at the start of the match and not at 2–1.
     */
    @Test
    fun isDecidingSetOnlyAtTwoSetsAll() {
        val viewModel = ScoreboardViewModel(SavedStateHandle())
        assertFalse(viewModel.scoreboardState.value.isDecidingSet)

        winSet(viewModel, "A")
        winSet(viewModel, "B")
        winSet(viewModel, "A")
        assertFalse(viewModel.scoreboardState.value.isDecidingSet)

        winSet(viewModel, "B")
        assertTrue(viewModel.scoreboardState.value.isDecidingSet)
    }

    /**
     * Winning the deciding set ends it (3–2, so no longer deciding); undoing the winning point
     * makes it the deciding set again.
     */
    @Test
    fun decidingSetEndsWhenWonAndReturnsOnUndo() {
        val viewModel = ScoreboardViewModel(SavedStateHandle())
        reachDecidingSet(viewModel)
        addPoints(viewModel, "B", 13)
        addPoints(viewModel, "A", 15)
        assertFalse(viewModel.scoreboardState.value.isDecidingSet)

        viewModel.undo()

        assertTrue(viewModel.scoreboardState.value.isDecidingSet)
    }

    /** Scores [points] for [team], one point at a time. */
    private fun addPoints(viewModel: ScoreboardViewModel, team: String, points: Int) =
        repeat(points) { viewModel.incrementScore(team) }

    /** Wins two sets each (A, B, A, B), so the next set is the deciding 5th set. */
    private fun reachDecidingSet(viewModel: ScoreboardViewModel) {
        winSet(viewModel, "A")
        winSet(viewModel, "B")
        winSet(viewModel, "A")
        winSet(viewModel, "B")
    }

    /** Scores 25 straight points for [team], winning one set 25–0. */
    private fun winSet(viewModel: ScoreboardViewModel, team: String) =
        repeat(25) { viewModel.incrementScore(team) }

    /** Wins three straight sets for [team], which wins the match. */
    private fun winMatch(viewModel: ScoreboardViewModel, team: String) =
        repeat(3) { winSet(viewModel, team) }
}
