package com.vahossmedia.android.scoreboard

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private var teamAScore = 0
    private var teamBScore = 0
    private var teamASets = 0
    private var teamBSets = 0

    private lateinit var teamAScoreTextView: TextView
    private lateinit var teamBScoreTextView: TextView
    private lateinit var teamASetsTextView: TextView
    private lateinit var teamBSetsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        teamAScoreTextView = findViewById(R.id.team_a_score)
        teamBScoreTextView = findViewById(R.id.team_b_score)
        teamASetsTextView = findViewById(R.id.team_a_sets)
        teamBSetsTextView = findViewById(R.id.team_b_sets)

        // Set up click listeners
        findViewById<View>(R.id.team_a_score_area).setOnClickListener { incrementScore("A") }
        findViewById<View>(R.id.team_b_score_area).setOnClickListener { incrementScore("B") }

        findViewById<Button>(R.id.reset_set_button).setOnClickListener { resetSet() }
        findViewById<Button>(R.id.reset_match_button).setOnClickListener { resetMatch() }
    }

    private fun incrementScore(team: String) {
        if (team == "A") {
            teamAScore++
            checkSetWinner()
            teamAScoreTextView.text = "$teamAScore"
        } else if (team == "B") {
            teamBScore++
            checkSetWinner()
            teamBScoreTextView.text = "$teamBScore"
        }
    }

    private fun checkSetWinner() {
        val winningScore = 25 // Volleyball default
        if ((teamAScore >= winningScore || teamBScore >= winningScore) &&
            Math.abs(teamAScore - teamBScore) >= 2
        ) {
            if (teamAScore > teamBScore) {
                teamASets++
                teamASetsTextView.text = "Team A Sets: $teamASets"
            } else {
                teamBSets++
                teamBSetsTextView.text = "Team B Sets: $teamBSets"
            }
            resetSet()
        }
    }

    private fun resetSet() {
        teamAScore = 0
        teamBScore = 0
        teamAScoreTextView.text = "$teamAScore"
        teamBScoreTextView.text = "$teamBScore"
    }

    private fun resetMatch() {
        resetSet()
        teamASets = 0
        teamBSets = 0
        teamASetsTextView.text = "Team A Sets: $teamASets"
        teamBSetsTextView.text = "Team B Sets: $teamBSets"
    }
}
