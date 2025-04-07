package com.vahossmedia.android.scoreboard

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.vahossmedia.android.scoreboard.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var teamAScore = 0
    private var teamBScore = 0
    private var teamASets = 0
    private var teamBSets = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            teamAScore++
            checkSetWinner()
            binding.textviewTeamAScore.text = "$teamAScore"
        } else if (team == "B") {
            teamBScore++
            checkSetWinner()
            binding.textviewTeamBScore.text = "$teamBScore"
        }
    }

    private fun decrementScore(team: String) {
        if (team == "A") {
            if (teamAScore <= 0) return
            teamAScore--
            checkSetWinner()
            binding.textviewTeamAScore.text = "$teamAScore"
        } else if (team == "B") {
            if (teamBScore <= 0) return
            teamBScore--
            checkSetWinner()
            binding.textviewTeamBScore.text = "$teamBScore"
        }
    }

    private fun checkSetWinner() {
        val winningScore = 25 // Volleyball default
        if ((teamAScore >= winningScore || teamBScore >= winningScore) &&
            Math.abs(teamAScore - teamBScore) >= 2
        ) {
            if (teamAScore > teamBScore) {
                teamASets++
                binding.textviewTeamASets.text = "Team A Sets: $teamASets"
            } else {
                teamBSets++
                binding.textviewTeamBSets.text = "Team B Sets: $teamBSets"
            }
            resetSet()
        }
    }

    private fun resetSet() {
        teamAScore = 0
        teamBScore = 0
        binding.textviewTeamAScore.text = "$teamAScore"
        binding.textviewTeamBScore.text = "$teamBScore"
    }

    private fun resetMatch() {
        resetSet()
        teamASets = 0
        teamBSets = 0
        binding.textviewTeamASets.text = "Team A Sets: $teamASets"
        binding.textviewTeamBSets.text = "Team B Sets: $teamBSets"
    }
}
