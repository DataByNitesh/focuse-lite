package com.focuslite.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Room = Android's SQLite wrapper. @Entity is a table, @Dao holds typed SQL queries,
 * and queries returning Flow re-emit automatically whenever the table changes
 * (similar to a live query / subscription).
 */
@Entity(tableName = "block_events", indices = [Index("timestampMillis")])
data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val packageName: String,
    val targetId: String,
)

data class TargetCount(val targetId: String, val count: Int)

@Dao
interface BlockEventDao {
    @Insert
    suspend fun insert(event: BlockEventEntity)

    @Query("SELECT COUNT(*) FROM block_events WHERE timestampMillis >= :fromMillis")
    fun countSince(fromMillis: Long): Flow<Int>

    @Query("SELECT targetId, COUNT(*) AS count FROM block_events WHERE timestampMillis >= :fromMillis GROUP BY targetId")
    fun countsByTargetSince(fromMillis: Long): Flow<List<TargetCount>>

    @Query("SELECT COUNT(*) FROM block_events")
    fun countAll(): Flow<Int>

    @Query("DELETE FROM block_events")
    suspend fun deleteAll()
}

@Database(entities = [BlockEventEntity::class], version = 1, exportSchema = true)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun blockEventDao(): BlockEventDao
}
