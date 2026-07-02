package com.blockko.app.data.blocklist

import com.blockko.app.data.db.BlockEventEntity
import com.blockko.app.data.db.BlockKoDatabase
import com.blockko.app.data.db.DayCount
import com.blockko.app.data.db.DomainCount
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow

private val PRUNE_AFTER_MILLIS = TimeUnit.DAYS.toMillis(30)

class StatsRepository(private val db: BlockKoDatabase) {

    suspend fun recordBlock(domain: String, timestampMillis: Long = System.currentTimeMillis()) {
        db.blockEventDao().insert(BlockEventEntity(domain = domain, timestampMillis = timestampMillis))
    }

    fun blockedSince(sinceMillis: Long): Flow<Int> = db.blockEventDao().countSince(sinceMillis)

    fun blockedAllTime(): Flow<Int> = db.blockEventDao().countAll()

    fun topBlockedDomains(sinceMillis: Long, limit: Int = 10): Flow<List<DomainCount>> =
        db.blockEventDao().topDomainsSince(sinceMillis, limit)

    fun dailyCounts(sinceMillis: Long): Flow<List<DayCount>> = db.blockEventDao().dailyCountsSince(sinceMillis)

    suspend fun pruneOldEvents(nowMillis: Long = System.currentTimeMillis()) {
        db.blockEventDao().pruneOlderThan(nowMillis - PRUNE_AFTER_MILLIS)
    }
}
