package com.blockko.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockEventDao {
    @Insert
    suspend fun insert(event: BlockEventEntity)

    @Query("SELECT COUNT(*) FROM block_events WHERE timestampMillis >= :sinceMillis")
    fun countSince(sinceMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_events")
    fun countAll(): Flow<Int>

    @Query(
        "SELECT domain, COUNT(*) as count FROM block_events " +
            "WHERE timestampMillis >= :sinceMillis GROUP BY domain ORDER BY count DESC LIMIT :limit"
    )
    fun topDomainsSince(sinceMillis: Long, limit: Int): Flow<List<DomainCount>>

    @Query(
        "SELECT (timestampMillis / 86400000) as dayEpoch, COUNT(*) as count FROM block_events " +
            "WHERE timestampMillis >= :sinceMillis GROUP BY dayEpoch ORDER BY dayEpoch ASC"
    )
    fun dailyCountsSince(sinceMillis: Long): Flow<List<DayCount>>

    @Query("DELETE FROM block_events WHERE timestampMillis < :cutoffMillis")
    suspend fun pruneOlderThan(cutoffMillis: Long)
}
