package com.blockko.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlocklistDomainDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(domains: List<BlocklistDomainEntity>)

    @Query("DELETE FROM blocklist_domains")
    suspend fun clearAll()

    @Query("SELECT domain FROM blocklist_domains")
    suspend fun getAllDomains(): List<String>

    @Query("SELECT COUNT(*) FROM blocklist_domains")
    suspend fun count(): Int
}
