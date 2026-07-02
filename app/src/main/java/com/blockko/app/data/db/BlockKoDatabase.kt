package com.blockko.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [BlocklistDomainEntity::class, CustomRuleEntity::class, BlockEventEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BlockKoDatabase : RoomDatabase() {
    abstract fun blocklistDomainDao(): BlocklistDomainDao
    abstract fun customRuleDao(): CustomRuleDao
    abstract fun blockEventDao(): BlockEventDao

    companion object {
        @Volatile private var instance: BlockKoDatabase? = null

        fun getInstance(context: Context): BlockKoDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BlockKoDatabase::class.java,
                    "blockko.db"
                ).build().also { instance = it }
            }
    }
}
