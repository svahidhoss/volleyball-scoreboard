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
        // TODO check this behaviour is wanted
        // onView(withId(R.id.button_undo)).check(matches(not(isEnabled())))
    }

    @Test
    fun resetSetClearsScoresButPreservesSets() {
        // Give Team A and B a set each
        repeat(25) { onView(withId(R.id.team_a_score_area)).perform(click()) }
        repeat(25) { onView(withId(R.id.team_b_score_area)).perform(click()) }
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
        repeat(25) { onView(withId(R.id.team_a_score_area)).perform(click()) }
        // TODO: Optionally use IdlingResource if you hook up setWonEvent to make this more reliable
    }

    @Test
    fun matchWinShowsAlertDialog() {
        // Simulate Team B winning 3 sets
        repeat(3) {
            repeat(25) { onView(withId(R.id.team_b_score_area)).perform(click()) }
        }
        // Check for dialog with correct text
        onView(withText("Match Won!")).check(matches(isDisplayed()))
        onView(withText(containsString("Team B: 3 sets"))).check(matches(isDisplayed()))
        // Dismiss dialog
        onView(withText("OK")).perform(click())
    }
}