package com.blockko.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: CustomRuleEntity)

    @Query("DELETE FROM custom_rules WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)

    @Query("SELECT * FROM custom_rules ORDER BY domain ASC")
    fun observeAll(): Flow<List<CustomRuleEntity>>

    @Query("SELECT * FROM custom_rules WHERE domain LIKE '%' || :query || '%' ORDER BY domain ASC")
    fun search(query: String): Flow<List<CustomRuleEntity>>

    @Query("SELECT * FROM custom_rules")
    suspend fun getAllOnce(): List<CustomRuleEntity>
}
