package app.yuki.core.database

import androidx.room.TypeConverter
import java.time.Instant

internal class InstantConverter {
    @TypeConverter
    fun toEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun fromEpochMilli(epochMilli: Long?): Instant? = epochMilli?.let(Instant::ofEpochMilli)
}
