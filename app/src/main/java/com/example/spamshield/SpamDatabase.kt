package com.example.spamshield

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [MessageRecord::class, BlockedThread::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SpamDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun blockedThreadDao(): BlockedThreadDao
}

class Converters {
    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return list?.joinToString(separator = "||") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("||").filter { it.isNotBlank() }
    }
}
