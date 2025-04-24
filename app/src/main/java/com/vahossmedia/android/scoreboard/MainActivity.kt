package com.vahossmedia.android.scoreboard

import android.media.MediaPlayer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.vahossmedia.android.scoreboard.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: ScoreboardViewModel by viewModels()

    private lateinit var setWinPlayer: MediaPlayer
    private lateinit var matchWinPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setWinPlayer = MediaPlayer.create(this, R.raw.marimba_win_sound)
        matchWinPlayer = MediaPlayer.create(this, R.raw.marimba_win_sound3)

        // Set up click listeners
        binding.teamAScoreArea.setOnClickListener { viewModel.incrementScore("A") }
        binding.teamBScoreArea.setOnClickListener { viewModel.incrementScore("B") }

        binding.buttonUndo.setOnClickListener { viewModel.undo() }
        binding.buttonRedo.setOnClickListener { viewModel.redo() }

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

        // Observe redo availability
        lifecycleScope.launch {
            viewModel.canRedo.collectLatest {
                binding.buttonRedo.isEnabled = it
            }
        }

        lifecycleScope.launch {
            viewModel.matchWonEvent.collect { message ->
                if (message.isNotEmpty()) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Match Won!")
                        .setMessage("Results:\nTeam A: ${viewModel.scoreboardState.value.teamASets} sets, Team B: ${viewModel.scoreboardState.value.teamBSets} sets")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                    // Play sound effect here
                    matchWinPlayer.start()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.setWonEvent.collect { message ->
                if (message.isNotEmpty()) {
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    // Play sound effect here
                    setWinPlayer.start()
                }
            }
        }
    }

    private fun updateUI(state: ScoreboardState) {
        binding.textviewTeamAScore.text = "${state.teamAScore}"
        binding.textviewTeamBScore.text = "${state.teamBScore}"
        binding.textviewTeamASets.text = "Team A Sets: ${state.teamASets}"
        binding.textviewTeamBSets.text = "Team B Sets: ${state.teamBSets}"

        if (state.matchWinner != null) {
            binding.textviewWinner.text =
                if (state.matchWinner == "A") "Winner: Team A!" else "Winner: Team B!"
            binding.textviewWinner.visibility = View.VISIBLE
        } else {
            binding.textviewWinner.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        setWinPlayer.release()
        matchWinPlayer.release()
    }
}
