package com.vahossmedia.android.scoreboard

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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

        binding.buttonUndo.setOnClickListener { viewModel.undo() }

        binding.buttonResetSet.setOnClickListener { viewModel.resetSet() }
        binding.buttonResetMatch.setOnClickListener { viewModel.resetMatch() }

        // Observe state changes
        lifecycleScope.launch {
            viewModel.scoreboardState.collectLatest {
                updateUI(it)
            }
        }

        // Observe undo availability
        lifecycleScope.launch {
            viewModel.canUndo.collectLatest { canUndo ->
                binding.buttonUndo.isEnabled = canUndo
            }
        }

        lifecycleScope.launch {
            viewModel.matchWonEvent.collect { message ->
                if (message.isNotEmpty()) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Match Won!")
                        .setMessage("Results:\nTeam A: ${viewModel.scoreboardState.value.teamASets} sets, Team B: ${viewModel.scoreboardState.value.teamBSets} sets")
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                    // TODO: Play sound effect here
                }
            }
        }

        lifecycleScope.launch {
            viewModel.setWonEvent.collect { message ->
                if (message.isNotEmpty()) {
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    // TODO: Play sound effect here
                }
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
