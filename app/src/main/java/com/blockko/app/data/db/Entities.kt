package com.blockko.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A domain coming from the bundled or remotely-updated blocklist. */
@Entity(tableName = "blocklist_domains")
data class BlocklistDomainEntity(
    @PrimaryKey val domain: String
)

enum class CustomRuleType { BLOCK, ALLOW }

/** User-managed custom rule. An ALLOW rule always overrides the blocklist. */
@Entity(tableName = "custom_rules")
data class CustomRuleEntity(
    @PrimaryKey val domain: String,
    val type: CustomRuleType
)

/** A single DNS query that was blocked, used to drive the stats dashboard. */
@Entity(tableName = "block_events")
data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val timestampMillis: Long
)

data class DomainCount(
    val domain: String,
    val count: Int
)

data class DayCount(
    val dayEpoch: Long,
    val count: Int
)
