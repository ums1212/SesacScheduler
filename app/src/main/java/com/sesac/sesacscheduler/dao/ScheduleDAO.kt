package com.sesac.sesacscheduler.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sesac.sesacscheduler.model.ScheduleInfo

@Dao
interface ScheduleDAO {

    @Insert
    fun insertSchedule(schedule: ScheduleInfo)

    @Update
    fun updateSchedule(schedule: ScheduleInfo)

    @Delete
    fun deleteSchedule(id: Int)

    //id값으로 스케줄 가져오기(수정페이지)
    @Query("SELECT * FROM schedule WHERE scheduleId = :id")
    fun getSchedule(id: Int): ScheduleInfo

    //해당 날짜 스케줄 가져오기
    @Query("SELECT * FROM schedule WHERE startDate = :date")
    fun getScheduleDate(date: String): MutableList<ScheduleInfo>

    //해당 월 스케줄 가져오기
    @Query("SELECT * FROM schedule WHERE startDate LIKE :month || '%'")
    fun getScheduleMonth(month: String): MutableList<ScheduleInfo>
}