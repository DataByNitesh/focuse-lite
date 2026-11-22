package com.focuslite.data

import com.focuslite.data.db.BlockEventDao
import com.focuslite.data.db.BlockEventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

class StatsRepository(private val dao: BlockEventDao) {

    suspend fun recordBlock(packageName: String, targetId: String, timestampMillis: Long) {
        dao.insert(BlockEventEntity(timestampMillis = timestampMillis, packageName = packageName, targetId = targetId))
    }

    fun countToday(nowMillis: Long = System.currentTimeMillis()): Flow<Int> = dao.countSince(startOfDay(nowMillis))

    fun countsByTargetToday(nowMillis: Long = System.currentTimeMillis()): Flow<Map<String, Int>> =
        dao.countsByTargetSince(startOfDay(nowMillis)).map { list -> list.associate { it.targetId to it.count } }

    fun countAll(): Flow<Int> = dao.countAll()

    suspend fun deleteAll() = dao.deleteAll()

    companion object {
        fun startOfDay(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
            Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
