package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelId: Int,
    val stars: Int = 0,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val bestMoves: Int = 0,
    val bestTimeSeconds: Long = 0L,
    val hintsUsed: Int = 0
)

@Entity(tableName = "daily_challenges")
data class DailyChallengeEntity(
    @PrimaryKey val dateKey: String, // e.g. "2026-11-10"
    val isCompleted: Boolean = false,
    val stars: Int = 0,
    val completedAtTimestamp: Long = 0L
)

@Entity(tableName = "trophies")
data class TrophyEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val progress: Int = 0,
    val maxProgress: Int = 1,
    val iconName: String = "trophy"
)
