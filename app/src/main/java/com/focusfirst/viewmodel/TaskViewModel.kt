package com.focusfirst.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfirst.billing.BillingManager
import com.focusfirst.data.db.TaskDao
import com.focusfirst.data.db.TagDao
import com.focusfirst.data.model.TaskEntity
import com.focusfirst.data.model.TagEntity
import com.focusfirst.util.TaskDraft
import com.focusfirst.util.selectedDateTaskFlow
import com.focusfirst.util.TaskLinkResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val tagDao: TagDao,
    private val billingManager: BillingManager,
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val activeTasks = taskDao.observeActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeCount = taskDao.observeActiveCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isPro = billingManager.isPro
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val canAddTask: Boolean
        get() = isPro.value || activeCount.value < FREE_TASK_LIMIT

    val tags = tagDao.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Tasks for the currently selected calendar date. */
    val tasksForSelectedDate = selectedDateTaskFlow(_selectedDate) { date ->
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        taskDao.observeTasksForDate(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Incomplete tasks from previous days. */
    val overdueTasks = taskDao.observeOverdueTasks(
        java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** ID of the task currently linked to the running timer session. */
    var selectedTaskId by mutableStateOf<Int?>(null)

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    /** Resolves a linked task against Room's current active task list. */
    suspend fun resolveDeepLinkTaskId(taskId: String?, taskName: String?): Int? {
        if (taskId == null && taskName == null) return null
        return try {
            TaskLinkResolver.resolve(taskId, taskName, taskDao.observeActiveTasks().first())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("TaskViewModel", "Failed to resolve task from timer link", error)
            null
        }
    }

    fun addTask(
        title: String,
        description: String = "",
        status: String = "TODO",
        date: java.time.LocalDate? = null,
        timeMillis: Long? = null,
        target: Int = 4,
        durationMinutes: Int = 25,
        tagName: String = "Focus"
    ) {
        val task = TaskDraft(
            title = title,
            description = description,
            status = status,
            date = date,
            timeMillis = timeMillis,
            targetPomodoros = target,
            durationMinutes = durationMinutes,
            tagName = tagName,
        ).toTaskEntity() ?: return
        viewModelScope.launch {
            taskDao.insert(task)
        }
    }

    fun addTag(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            tagDao.insertTag(TagEntity(name = name, colorHex = colorHex))
        }
    }

    fun updateTaskStatus(task: TaskEntity, newStatus: String) {
        viewModelScope.launch {
            taskDao.update(task.copy(status = newStatus, isCompleted = newStatus == "DONE"))
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { taskDao.delete(task) }
    }

    fun completeTask(task: TaskEntity) {
        viewModelScope.launch { 
            taskDao.update(task.copy(status = "DONE", isCompleted = true)) 
        }
    }

    fun incrementPomodoro(taskId: Int) {
        viewModelScope.launch { taskDao.incrementPomodoro(taskId) }
    }

    companion object {
        const val FREE_TASK_LIMIT = 10
    }
}
