package com.sesac.sesacscheduler.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sesac.sesacscheduler.common.ScheduleResult
import com.sesac.sesacscheduler.common.logE
import com.sesac.sesacscheduler.common.toastShort
import com.sesac.sesacscheduler.model.ScheduleInfo
import com.sesac.sesacscheduler.repo.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScheduleViewModel : ViewModel() {

    private val repository: ScheduleRepository by lazy {
        ScheduleRepository()
    }
    private var _scheduleList = MutableStateFlow<ScheduleResult<MutableList<ScheduleInfo>>>(ScheduleResult.NoConstructor)
    val scheduleList get() = _scheduleList.asStateFlow()

    private var _removeSchedule = MutableStateFlow<ScheduleResult<Int>>(ScheduleResult.NoConstructor)
    val removeSchedule get() = _removeSchedule.asStateFlow()

    private val ioDispatchers = CoroutineScope(Dispatchers.IO)

    fun insertSchedule(newSchedule: ScheduleInfo){
        viewModelScope.launch {
            withContext(ioDispatchers.coroutineContext){
                repository.insertSchedule(newSchedule).collectLatest{
                    when(it){
                        is ScheduleResult.Success -> {
                            viewModelScope.launch {
                                toastShort("스케줄 저장")
                                logE("스케줄 저장(성공)",it.toString())
                            }
                            findAllSchedule()
                        }
                        is ScheduleResult.Loading -> {
                            logE("스케줄 저장(로딩)",it.toString())
                        }
                        is ScheduleResult.RoomDBError -> {
                            logE("스케줄 저장(DB에러)",it.exception.toString())
                        }
                        else -> {
                            logE("스케줄 저장(너가 왜뜨니?)",it.toString())
                        }
                    }
                }
            }
        }
    }
    fun getSchedule(id: Int){
        viewModelScope.launch {
            withContext(ioDispatchers.coroutineContext) {
                repository.getSchedule(id).collectLatest {
                    // _scheduleList.value = it
                }
            }
        }
    }
    fun deleteSchedule(id: Int) {
        viewModelScope.launch {
            withContext(ioDispatchers.coroutineContext) {
                repository.deleteSchedule(id).collectLatest {
                    //_removeSchedule.value = it
                }
            }
        }
    }
    fun findAllSchedule() = viewModelScope.launch {
        withContext(ioDispatchers.coroutineContext) {
            repository.findAllSchedule().collectLatest {
                _scheduleList.value = it
            }
        }
    }
}