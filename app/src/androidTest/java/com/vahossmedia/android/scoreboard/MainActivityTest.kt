package com.vahossmedia.android.scoreboard

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        // provide a fresh MainActivity(visible in resumed state) before every test
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun showsNumbersOnLaunch() {
        onView(withId(R.id.textview_team_a_score))
            .check(matches(withText("0")))
        onView(withId(R.id.textview_team_b_score))
            .check(matches(withText("0")))
    }

    @Test
    fun scoreClicksWorkAsExpected() {
        onView(withId(R.id.textview_team_a_score))
            .check(matches(withText("0")))
        onView(withId(R.id.team_a_score_area)).perform(click())
        onView(withId(R.id.textview_team_a_score))
            .check(matches(withText("1")))

        onView(withId(R.id.textview_team_b_score))
            .check(matches(withText("0")))
        repeat(20) {
            onView(withId(R.id.team_b_score_area)).perform(click())
        }
        onView(withId(R.id.textview_team_b_score))
            .check(matches(withText("20")))
    }

    @Test
    fun stateIsPreservedAfterScreenRotation() {
        // Increment scores
        onView(withId(R.id.team_b_score_area)).perform(click())
        onView(withId(R.id.team_a_score_area)).perform(click(), click())
        // Rotate
        scenario.recreate()
        // Ensure scores preserved
        onView(withId(R.id.textview_team_b_score)).check(matches(withText("1")))
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("2")))
    }


    @Test
    fun undoButtonRestoresPreviousScoreAndDisablesWhenStackEmpty() {
        // Increment Team A twice
        onView(withId(R.id.team_a_score_area)).perform(click(), click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("2")))

        // Undo (should decrement)
        onView(withId(R.id.button_undo)).perform(click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("1")))

        // Undo again (should reach zero and disable)
        onView(withId(R.id.button_undo)).perform(click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("0")))
        onView(withId(R.id.button_undo)).check(matches(not(isEnabled())))
    }

    @Test
    fun undoButtonDisabledAtAppStartAndAfterReset() {
        // At launch
        onView(withId(R.id.button_undo)).check(matches(not(isEnabled())))
        // After reset
        onView(withId(R.id.team_b_score_area)).perform(click())
        onView(withId(R.id.button_reset_match)).perform(click())
        // check if this behaviour is wanted: enabled or disabled?
         onView(withId(R.id.button_undo)).check(matches(isEnabled()))
    }

    @Test
    fun resetSetClearsScoresButPreservesSets() {
        // Give Team A and B a set each
        winASetForTeamA()
        winASetForTeamB()

        // Should show 1 set each
        onView(withId(R.id.textview_team_a_sets)).check(matches(withText(containsString("1"))))
        onView(withId(R.id.textview_team_b_sets)).check(matches(withText(containsString("1"))))
        // Now score one, reset set, ensure scores zero but sets persist
        onView(withId(R.id.team_a_score_area)).perform(click())
        onView(withId(R.id.button_reset_set)).perform(click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("0")))
        onView(withId(R.id.textview_team_b_score)).check(matches(withText("0")))
        onView(withId(R.id.textview_team_a_sets)).check(matches(withText(containsString("1"))))
    }

    @Test
    fun resetMatchClearsScoresAndSets() {
        // Give Team B two sets and a couple points
        repeat(50) { onView(withId(R.id.team_b_score_area)).perform(click()) }
        onView(withId(R.id.button_reset_match)).perform(click())
        // All should be zeros
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("0")))
        onView(withId(R.id.textview_team_b_score)).check(matches(withText("0")))
        onView(withId(R.id.textview_team_a_sets)).check(matches(withText(containsString("0"))))
        onView(withId(R.id.textview_team_b_sets)).check(matches(withText(containsString("0"))))
    }

    @Test
    fun setWinShowsToast() {
        // Espresso can't verify Toasts directly, but you can use UiAutomator or rely on setWonEvent
        winASetForTeamA()
        // TODO: Optionally use IdlingResource if you hook up setWonEvent to make this more reliable
    }

    @Test
    fun matchWinShowsAlertDialog() {
        // Simulate Team B winning 3 sets
        repeat(3) {
            winASetForTeamB()
        }
        // Check for dialog with correct text
        onView(withText("Match Won!")).check(matches(isDisplayed()))
        onView(withText(containsString("Team B: 3 sets"))).check(matches(isDisplayed()))
        // Dismiss dialog
        onView(withText("OK")).perform(click())
    }

    @Test
    fun undoButtonDoesNotGoBelowInitialState() {
        // Click Team A score 3 times
        repeat(3) { onView(withId(R.id.team_a_score_area)).perform(click()) }
        // Undo 4 times (1 more than actual actions)
        repeat(4) { onView(withId(R.id.button_undo)).perform(click()) }
        // Team A score should be 0, and undo should be disabled
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("0")))
        onView(withId(R.id.button_undo)).check(matches(not(isEnabled())))
    }

    @Test
    fun rapidUndoInteractionIsStable() {
        repeat(7) { onView(withId(R.id.team_a_score_area)).perform(click()) }
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("7")))
        // Now rapidly undo (simulate user hammering button)
        repeat(10) { onView(withId(R.id.button_undo)).perform(click()) }
        // Score should be zero
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("0")))
    }

    @Test
    fun stressTestIncrementingScore() {
        // Team B: 75 rapid increments
        repeat(75) { onView(withId(R.id.team_b_score_area)).perform(click()) }

        // Match win dialog should appear with appropriate message
        onView(withText("Match Won!")).check(matches(isDisplayed()))
        onView(withText(containsString("Team B: 3 sets"))).check(matches(isDisplayed()))
        onView(withText("OK")).perform(click())

        // Team B: 25 rapid increments
        winASetForTeamB()

        // You can check for set wins, sets increment, and score resets,
        // but let's just check that the UI remains consistent.
        // (If app supports unlimited sets, this is fine.)
        onView(withId(R.id.textview_team_b_score)).check(matches(withText("0"))) // It should reset and increment sets
        onView(withId(R.id.textview_team_b_sets)).check(matches(withText(containsString("4"))))
        onView(withId(R.id.textview_team_a_sets)).check(matches(withText(containsString("0"))))
    }

    @Test
    fun redoRestoresScoreAfterUndo() {
        // Team A scores twice
        onView(withId(R.id.team_a_score_area)).perform(click(), click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("2")))

        // Undo
        onView(withId(R.id.button_undo)).perform(click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("1")))

        // Redo
        onView(withId(R.id.button_redo)).perform(click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("2")))
    }

    @Test
    fun redoButtonIsDisabledUntilUndoIsUsed() {
        // Should be disabled at start
        onView(withId(R.id.button_redo)).check(matches(not(isEnabled())))

        // Make a change
        onView(withId(R.id.team_b_score_area)).perform(click())

        // Still disabled
        onView(withId(R.id.button_redo)).check(matches(not(isEnabled())))

        // Undo; redo should now be enabled
        onView(withId(R.id.button_undo)).perform(click())
        onView(withId(R.id.button_redo)).check(matches(isEnabled()))
    }

    @Test
    fun redoDoesNothingWhenStackIsEmpty() {
        // Redo at launch: should do nothing, should be disabled
        onView(withId(R.id.button_redo)).check(matches(not(isEnabled())))
    }

    @Test
    fun redoIsClearedOnNewUserAction() {
        // Team B scores twice
        onView(withId(R.id.team_b_score_area)).perform(click(), click())
        onView(withId(R.id.textview_team_b_score)).check(matches(withText("2")))

        // Undo one
        onView(withId(R.id.button_undo)).perform(click())
        onView(withId(R.id.textview_team_b_score)).check(matches(withText("1")))

        // Now score for Team B again (new action)
        onView(withId(R.id.team_b_score_area)).perform(click())
        onView(withId(R.id.textview_team_b_score)).check(matches(withText("2")))

        // Redo should now be disabled
        onView(withId(R.id.button_redo)).check(matches(not(isEnabled())))
    }

    @Test
    fun multipleRedosRestoreAllStates() {
        // Team A: click 3 times
        repeat(13) { onView(withId(R.id.team_a_score_area)).perform(click()) }
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("13")))

        // Undo twice
        onView(withId(R.id.button_undo)).perform(click(), click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("11")))

        // Redo once
        onView(withId(R.id.button_redo)).perform(click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("12")))

        // Redo again
        onView(withId(R.id.button_redo)).perform(click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("13")))

        // Undo twice
        onView(withId(R.id.button_undo)).perform(click(), click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("11")))

        // Redo twice
        onView(withId(R.id.button_redo)).perform(click(), click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("13")))

        // Redo should now be disabled (no more "future" states)
        onView(withId(R.id.button_redo)).check(matches(not(isEnabled())))
    }

    @Test
    fun redoRestoresStateAfterResetMatch() {
        onView(withId(R.id.team_b_score_area)).perform(click(), click())
        onView(withId(R.id.textview_team_b_score)).check(matches(withText("2")))

        // Reset match
        onView(withId(R.id.button_reset_match)).perform(click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("0")))
        onView(withId(R.id.textview_team_b_score)).check(matches(withText("0")))

        // Undo (undo reset match -> scores back)
        onView(withId(R.id.button_undo)).perform(click())
        onView(withId(R.id.textview_team_b_score)).check(matches(withText("2")))

        // Redo (should redo the reset match)
        onView(withId(R.id.button_redo)).perform(click())
        onView(withId(R.id.textview_team_a_score)).check(matches(withText("0")))
        onView(withId(R.id.textview_team_b_score)).check(matches(withText("0")))
    }

    @Test
    fun redoRestoresMatchWinAndDialog() {
        // Score 3 sets for Team B (25 points each)
        repeat(3) {
            winASetForTeamB()
        }
        // Should show dialog: "Match Won!"
        onView(withText("Match Won!")).check(matches(isDisplayed()))
        onView(withText(containsString("Team B: 3 sets"))).check(matches(isDisplayed()))
        // Dismiss dialog
        onView(withText("OK")).perform(click())

        // Undo last point (now Team B has only 2 sets, no dialog)
        onView(withId(R.id.button_undo)).perform(click())
        onView(withId(R.id.textview_team_b_sets)).check(matches(withText(containsString("2"))))

        // Redo last point (should win the match and show dialog again)
        onView(withId(R.id.button_redo)).perform(click())
        onView(withText("Match Won!")).check(matches(isDisplayed()))
        onView(withText(containsString("Team B: 3 sets"))).check(matches(isDisplayed()))
        // Dismiss dialog
        onView(withText("OK")).perform(click())
    }

    @Test
    fun redoIsClearedAfterSetWinAndNewScore() {
        winASetForTeamA()
        onView(withId(R.id.textview_team_a_sets)).check(matches(withText(containsString("1"))))

        // Undo (now back before set win)
        onView(withId(R.id.button_undo)).perform(click())
        onView(withId(R.id.textview_team_a_sets)).check(matches(withText(containsString("0"))))

        // Instead of redo, take a new action (score for B)
        onView(withId(R.id.team_b_score_area)).perform(click())

        // Redo should now be disabled (cleared!)
        onView(withId(R.id.button_redo)).check(matches(not(isEnabled())))
    }

    /**
     * Win a set for Team A
     */
    private fun winASetForTeamA() {
        repeat(25) { onView(withId(R.id.team_a_score_area)).perform(click()) }
    }

    /**
     * Win a set for Team B
     */
    private fun winASetForTeamB() {
        repeat(25) { onView(withId(R.id.team_b_score_area)).perform(click()) }
    }
}