package com.blockko.app.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromRuleType(type: CustomRuleType): String = type.name

    @TypeConverter
    fun toRuleType(value: String): CustomRuleType = CustomRuleType.valueOf(value)
}
