package com.vahossmedia.android.scoreboard

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Volleyball rules (best of five): sets 1–4 go to 25, the deciding 5th set to 15, all won by 2
internal const val POINTS_TO_WIN_SET = 25
internal const val POINTS_TO_WIN_DECIDING_SET = 15
internal const val SETS_TO_WIN_MATCH = 3

@Parcelize
data class ScoreboardState(
    val teamAScore: Int = 0,
    val teamBScore: Int = 0,
    val teamASets: Int = 0,
    val teamBSets: Int = 0,
    val matchWinner: String? = null // "A", "B", or null
) : Parcelable {

    /**
     * True while the deciding 5th set is being played: both teams are one set away from
     * winning the match (2–2 in sets). Computed, so it isn't saved or parcelled.
     */
    val isDecidingSet: Boolean
        get() = teamASets == SETS_TO_WIN_MATCH - 1 && teamBSets == SETS_TO_WIN_MATCH - 1
}
