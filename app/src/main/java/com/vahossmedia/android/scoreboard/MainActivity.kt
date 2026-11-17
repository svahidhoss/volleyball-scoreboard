package com.vahossmedia.android.scoreboard

import android.graphics.Color
import android.media.MediaPlayer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.vahossmedia.android.scoreboard.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: ScoreboardViewModel by viewModels()

    private lateinit var setWinPlayer: MediaPlayer
    private lateinit var matchWinPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars. Light icons read best on the team colors.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()

        setWinPlayer = MediaPlayer.create(this, R.raw.marimba_win_sound)
        matchWinPlayer = MediaPlayer.create(this, R.raw.marimba_win_sound3)

        // Set up click listeners
        binding.teamAScoreArea.setOnClickListener { viewModel.incrementScore("A") }
        binding.teamBScoreArea.setOnClickListener { viewModel.incrementScore("B") }

        binding.buttonUndo.setOnClickListener { viewModel.undo() }
        binding.buttonRedo.setOnClickListener { viewModel.redo() }

        binding.buttonResetSet.setOnClickListener { viewModel.resetSet() }

        binding.textviewDecidingSet.text =
            getString(R.string.deciding_set_hint, POINTS_TO_WIN_DECIDING_SET)
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

    /**
     * The team colors run edge to edge; this keeps the set labels and buttons clear of the
     * system bars and the display cutout. Works for both the portrait and landscape layouts:
     * the top controls hang from guideline_top, which is moved below the status bar.
     */
    private fun applyWindowInsets() {
        val labelPadding = (12 * resources.displayMetrics.density).roundToInt()
        val buttonMargin = (binding.buttonUndo.layoutParams as ViewGroup.MarginLayoutParams).leftMargin

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.guidelineTop.setGuidelineBegin(insets.top)
            binding.textviewTeamASets.updatePadding(
                left = labelPadding + insets.left, right = labelPadding + insets.right
            )
            binding.textviewTeamBSets.updatePadding(
                left = labelPadding + insets.left, right = labelPadding + insets.right
            )
            binding.buttonUndo.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = buttonMargin + insets.left
            }
            binding.buttonRedo.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                rightMargin = buttonMargin + insets.right
            }
            // Only the outer edge of each reset button moves, so they still meet at the middle
            binding.buttonResetSet.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = buttonMargin + insets.left
                bottomMargin = buttonMargin + insets.bottom
            }
            binding.buttonResetMatch.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                rightMargin = buttonMargin + insets.right
                bottomMargin = buttonMargin + insets.bottom
            }
            windowInsets
        }
    }

    private fun updateUI(state: ScoreboardState) {
        binding.textviewTeamAScore.text = "${state.teamAScore}"
        binding.textviewTeamBScore.text = "${state.teamBScore}"
        binding.textviewTeamASets.text = "Team A Sets: ${state.teamASets}"
        binding.textviewTeamBSets.text = "Team B Sets: ${state.teamBSets}"

        if (state.matchWinner != null) {
            binding.textviewWinner.text =
                if (state.matchWinner == "A") "Winner:\nTeam A!" else "Winner:\nTeam B!"
            binding.textviewWinner.visibility = View.VISIBLE
        } else {
            binding.textviewWinner.visibility = View.GONE
        }

        binding.textviewDecidingSet.visibility =
            if (state.isDecidingSet) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        setWinPlayer.release()
        matchWinPlayer.release()
    }
}
