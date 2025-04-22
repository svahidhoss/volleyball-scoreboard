package com.vahossmedia.android.scoreboard

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}