package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelProgressDao {
    @Query("SELECT * FROM level_progress ORDER BY levelId ASC")
    fun getAllLevels(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId")
    fun getLevel(levelId: Int): Flow<LevelProgressEntity?>

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId")
    suspend fun getLevelSync(levelId: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLevel(entity: LevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(entities: List<LevelProgressEntity>)

    @Query("DELETE FROM level_progress")
    suspend fun clearAll()
}

@Dao
interface DailyChallengeDao {
    @Query("SELECT * FROM daily_challenges WHERE dateKey = :dateKey")
    fun getChallenge(dateKey: String): Flow<DailyChallengeEntity?>

    @Query("SELECT * FROM daily_challenges WHERE dateKey = :dateKey")
    suspend fun getChallengeSync(dateKey: String): DailyChallengeEntity?

    @Query("SELECT * FROM daily_challenges ORDER BY dateKey DESC")
    fun getAllChallenges(): Flow<List<DailyChallengeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveChallenge(entity: DailyChallengeEntity)

    @Query("DELETE FROM daily_challenges")
    suspend fun clearAll()
}

@Dao
interface TrophyDao {
    @Query("SELECT * FROM trophies ORDER BY id ASC")
    fun getAllTrophies(): Flow<List<TrophyEntity>>

    @Query("SELECT * FROM trophies WHERE id = :id")
    suspend fun getTrophySync(id: String): TrophyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTrophy(entity: TrophyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(entities: List<TrophyEntity>)

    @Query("DELETE FROM trophies")
    suspend fun clearAll()
}
