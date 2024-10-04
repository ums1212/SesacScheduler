package com.sesac.sesacscheduler.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sesac.sesacscheduler.dao.ScheduleDAO
import com.sesac.sesacscheduler.model.ScheduleInfo

@Database(entities = [ScheduleInfo::class], version = 1, exportSchema = false)
abstract class ScheduleRoomDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDAO

    companion object {
        private lateinit var INSTANCE: ScheduleRoomDatabase
        internal fun getDatabase(context: Context): ScheduleRoomDatabase {
            if (!this::INSTANCE.isInitialized) {
                synchronized(ScheduleRoomDatabase::class.java) {
                    INSTANCE =
                        Room.databaseBuilder(
                            context.applicationContext,
                            ScheduleRoomDatabase::class.java,
                            "product_database"
                        ).build()
                }
            }
            return INSTANCE
        }

    }
}