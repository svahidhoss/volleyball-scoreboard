package com.vahossmedia.android.scoreboard

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScoreboardState(
    val teamAScore: Int = 0,
    val teamBScore: Int = 0,
    val teamASets: Int = 0,
    val teamBSets: Int = 0
) : Parcelable