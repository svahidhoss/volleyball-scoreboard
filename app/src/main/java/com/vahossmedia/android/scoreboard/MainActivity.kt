package com.vahossmedia.android.scoreboard

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vahossmedia.android.scoreboard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var state = ScoreboardState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize UI with initial state
        updateUI()

        // Set up click listeners
        binding.teamAScoreArea.setOnClickListener { incrementScore("A") }
        binding.teamBScoreArea.setOnClickListener { incrementScore("B") }

        binding.buttonDecrementScoreA.setOnClickListener { decrementScore("A") }
        binding.buttonDecrementScoreB.setOnClickListener { decrementScore("B") }

        binding.buttonResetSet.setOnClickListener { resetSet() }
        binding.buttonResetMatch.setOnClickListener { resetMatch() }
    }

    private fun incrementScore(team: String) {
        if (team == "A") {
            state = state.copy(teamAScore = state.teamAScore + 1)
        } else if (team == "B") {
            state = state.copy(teamBScore = state.teamBScore + 1)
        }
        checkSetWinner()
        updateUI()
    }

    private fun decrementScore(team: String) {
        if (team == "A") {
            state = state.copy(teamAScore = state.teamAScore - 1)
        } else if (team == "B") {
            state = state.copy(teamBScore = state.teamBScore - 1)
        }
        checkSetWinner()
        updateUI()
    }

    private fun checkSetWinner() {
        val winningScore = 25 // Volleyball default
        if ((state.teamAScore >= winningScore || state.teamBScore >= winningScore) &&
            Math.abs(state.teamAScore - state.teamBScore) >= 2
        ) {
            state =
                if (state.teamAScore > state.teamBScore) state.copy(teamASets = state.teamASets + 1)
                else state.copy(teamBSets = state.teamBSets + 1)

            resetSet()
        }
    }

    private fun resetSet() {
        state = state.copy(teamAScore = 0, teamBScore = 0)
        updateUI()
    }

    private fun resetMatch() {
        state = ScoreboardState()
        updateUI()
    }

    private fun updateUI() {
        binding.textviewTeamAScore.text = "${state.teamAScore}"
        binding.textviewTeamBScore.text = "${state.teamBScore}"
        binding.textviewTeamASets.text = "Team A Sets: ${state.teamASets}"
        binding.textviewTeamBSets.text = "Team B Sets: ${state.teamBSets}"
    }
}
