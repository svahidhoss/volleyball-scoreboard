package com.vahossmedia.android.scoreboard

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.vahossmedia.android.scoreboard.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: ScoreboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up click listeners
        binding.teamAScoreArea.setOnClickListener { viewModel.incrementScore("A") }
        binding.teamBScoreArea.setOnClickListener { viewModel.incrementScore("B") }

        binding.buttonDecrementScoreA.setOnClickListener { viewModel.decrementScore("A") }
        binding.buttonDecrementScoreB.setOnClickListener { viewModel.decrementScore("B") }

        binding.buttonResetSet.setOnClickListener { viewModel.resetSet() }
        binding.buttonResetMatch.setOnClickListener { viewModel.resetMatch() }

        lifecycleScope.launch {
            viewModel.scoreboardState.collectLatest {
                updateUI(it)
            }
        }
    }

    private fun updateUI(state: ScoreboardState) {
        binding.textviewTeamAScore.text = "${state.teamAScore}"
        binding.textviewTeamBScore.text = "${state.teamBScore}"
        binding.textviewTeamASets.text = "Team A Sets: ${state.teamASets}"
        binding.textviewTeamBSets.text = "Team B Sets: ${state.teamBSets}"
    }
}
