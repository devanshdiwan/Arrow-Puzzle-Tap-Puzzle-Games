package com.example.data

import com.example.gameengine.LevelCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class GameRepository(
    private val database: AppDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val levelDao = database.levelProgressDao()
    private val dailyDao = database.dailyChallengeDao()
    private val trophyDao = database.trophyDao()

    val allLevels: Flow<List<LevelProgressEntity>> = levelDao.getAllLevels()
    val allTrophies: Flow<List<TrophyEntity>> = trophyDao.getAllTrophies()
    val settingsFlow = preferencesRepository.settingsFlow

    suspend fun initializeIfNeeded() {
        // Initialize 100 levels if not yet seeded
        val existing = levelDao.getAllLevels().first()
        if (existing.isEmpty()) {
            val list = mutableListOf<LevelProgressEntity>()
            for (i in 1..LevelCatalog.TOTAL_LEVELS) {
                list.add(
                    LevelProgressEntity(
                        levelId = i,
                        stars = 0,
                        isUnlocked = (i == 1), // Level 1 unlocked
                        isCompleted = false
                    )
                )
            }
            levelDao.saveAll(list)
        }

        // Initialize trophies if empty
        val existingTrophies = trophyDao.getAllTrophies().first()
        if (existingTrophies.isEmpty()) {
            val trophies = listOf(
                TrophyEntity("first_escape", "First Escape", "Escape your very first arrow", isUnlocked = false, progress = 0, maxProgress = 1),
                TrophyEntity("tutorial_grad", "Tutorial Graduate", "Complete all 10 tutorial levels", isUnlocked = false, progress = 0, maxProgress = 10),
                TrophyEntity("easy_champ", "Easy Champion", "Complete all 30 easy levels", isUnlocked = false, progress = 0, maxProgress = 30),
                TrophyEntity("heart_breaker", "Heart of the Maze", "Complete Level 42 heart silhouette", isUnlocked = false, progress = 0, maxProgress = 1),
                TrophyEntity("flawless", "Flawless Aim", "Complete a level with 3 lives and 0 hints", isUnlocked = false, progress = 0, maxProgress = 1),
                TrophyEntity("star_50", "Star Collector", "Collect 50 total stars across levels", isUnlocked = false, progress = 0, maxProgress = 50),
                TrophyEntity("star_100", "Constellation", "Collect 100 total stars across levels", isUnlocked = false, progress = 0, maxProgress = 100),
                TrophyEntity("daily_streak_3", "Daily Habit", "Complete 3 Daily Challenges", isUnlocked = false, progress = 0, maxProgress = 3),
                TrophyEntity("daily_streak_7", "Weekly Dedication", "Complete 7 Daily Challenges", isUnlocked = false, progress = 0, maxProgress = 7),
                TrophyEntity("undo_tactician", "Tactical Rewind", "Use Undo during a puzzle", isUnlocked = false, progress = 0, maxProgress = 1),
                TrophyEntity("century_club", "Arrow Grandmaster", "Complete all 100 puzzle levels", isUnlocked = false, progress = 0, maxProgress = 100),
                TrophyEntity("speedster", "Swift Solver", "Clear a puzzle in under 25 seconds", isUnlocked = false, progress = 0, maxProgress = 1)
            )
            trophyDao.saveAll(trophies)
        }
    }

    suspend fun getLevel(levelId: Int): LevelProgressEntity? {
        return levelDao.getLevelSync(levelId)
    }

    suspend fun completeLevel(
        levelId: Int,
        stars: Int,
        moves: Int,
        timeSeconds: Long,
        hintsUsed: Int,
        livesRemaining: Int
    ) {
        val current = levelDao.getLevelSync(levelId) ?: LevelProgressEntity(levelId = levelId)
        val bestStars = maxOf(current.stars, stars)
        val bestMoves = if (current.bestMoves == 0) moves else minOf(current.bestMoves, moves)
        val bestTime = if (current.bestTimeSeconds == 0L) timeSeconds else minOf(current.bestTimeSeconds, timeSeconds)

        levelDao.saveLevel(
            current.copy(
                isCompleted = true,
                isUnlocked = true,
                stars = bestStars,
                bestMoves = bestMoves,
                bestTimeSeconds = bestTime,
                hintsUsed = current.hintsUsed + hintsUsed
            )
        )

        // Unlock next level if available
        if (levelId < LevelCatalog.TOTAL_LEVELS) {
            val next = levelDao.getLevelSync(levelId + 1) ?: LevelProgressEntity(levelId = levelId + 1)
            if (!next.isUnlocked) {
                levelDao.saveLevel(next.copy(isUnlocked = true))
            }
        }

        // Check trophy updates
        unlockTrophy("first_escape")
        if (levelId == 42) unlockTrophy("heart_breaker")
        if (livesRemaining == 3 && hintsUsed == 0) unlockTrophy("flawless")
        if (timeSeconds < 25L) unlockTrophy("speedster")

        // Check aggregate progress
        val all = levelDao.getAllLevels().first()
        val completedCount = all.count { it.isCompleted }
        val totalStars = all.sumOf { it.stars }

        updateTrophyProgress("tutorial_grad", minOf(10, all.take(10).count { it.isCompleted }))
        updateTrophyProgress("easy_champ", minOf(30, all.take(30).count { it.isCompleted }))
        updateTrophyProgress("star_50", totalStars)
        updateTrophyProgress("star_100", totalStars)
        updateTrophyProgress("century_club", completedCount)
    }

    suspend fun getDailyChallenge(dateKey: String): DailyChallengeEntity? {
        return dailyDao.getChallengeSync(dateKey)
    }

    fun getDailyChallengeFlow(dateKey: String): Flow<DailyChallengeEntity?> {
        return dailyDao.getChallenge(dateKey)
    }

    suspend fun completeDailyChallenge(dateKey: String, stars: Int) {
        val challenge = dailyDao.getChallengeSync(dateKey) ?: DailyChallengeEntity(dateKey = dateKey)
        if (!challenge.isCompleted) {
            dailyDao.saveChallenge(
                challenge.copy(
                    isCompleted = true,
                    stars = stars,
                    completedAtTimestamp = System.currentTimeMillis()
                )
            )
            // Increment daily challenge streak trophies
            val allChallenges = dailyDao.getAllChallenges().first()
            val totalDailyCompleted = allChallenges.count { it.isCompleted }
            updateTrophyProgress("daily_streak_3", totalDailyCompleted)
            updateTrophyProgress("daily_streak_7", totalDailyCompleted)
        }
    }

    suspend fun unlockTrophy(id: String) {
        val trophy = trophyDao.getTrophySync(id) ?: return
        if (!trophy.isUnlocked) {
            trophyDao.saveTrophy(trophy.copy(isUnlocked = true, progress = trophy.maxProgress))
        }
    }

    suspend fun updateTrophyProgress(id: String, progress: Int) {
        val trophy = trophyDao.getTrophySync(id) ?: return
        val newProgress = minOf(trophy.maxProgress, maxOf(trophy.progress, progress))
        val isNowUnlocked = newProgress >= trophy.maxProgress
        trophyDao.saveTrophy(
            trophy.copy(
                progress = newProgress,
                isUnlocked = trophy.isUnlocked || isNowUnlocked
            )
        )
    }

    suspend fun resetAllProgress() {
        levelDao.clearAll()
        dailyDao.clearAll()
        trophyDao.clearAll()
        initializeIfNeeded()
    }

    suspend fun setSound(enabled: Boolean) = preferencesRepository.setSoundEnabled(enabled)
    suspend fun setMusic(enabled: Boolean) = preferencesRepository.setMusicEnabled(enabled)
    suspend fun setVibration(enabled: Boolean) = preferencesRepository.setVibrationEnabled(enabled)
    suspend fun addHints(amount: Int) = preferencesRepository.addHints(amount)
    suspend fun useHint(): Boolean = preferencesRepository.useHint()
}
