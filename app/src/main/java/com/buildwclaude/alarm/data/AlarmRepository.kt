package com.buildwclaude.alarm.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Thin wrapper over [AlarmDao]. A single instance is exposed via [get] so every screen,
 * receiver and service reads/writes the same database.
 */
class AlarmRepository private constructor(private val dao: AlarmDao) {

    fun observeAll(): Flow<List<AlarmEntity>> = dao.observeAll()
    suspend fun getEnabled(): List<AlarmEntity> = dao.getEnabled()
    suspend fun getById(id: Int): AlarmEntity? = dao.getById(id)
    suspend fun upsert(alarm: AlarmEntity): Int = dao.upsert(alarm).toInt()
    suspend fun update(alarm: AlarmEntity) = dao.update(alarm)
    suspend fun delete(alarm: AlarmEntity) = dao.delete(alarm)
    suspend fun setEnabled(id: Int, enabled: Boolean) = dao.setEnabled(id, enabled)

    companion object {
        @Volatile private var INSTANCE: AlarmRepository? = null

        fun get(context: Context): AlarmRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlarmRepository(AlarmDatabase.get(context).alarmDao())
                    .also { INSTANCE = it }
            }
    }
}
