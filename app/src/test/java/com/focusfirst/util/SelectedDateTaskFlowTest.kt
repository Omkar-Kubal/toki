package com.focusfirst.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SelectedDateTaskFlowTest {

    @Test
    fun changingSelectedDateQueriesTasksForTheNewDate() = runBlocking {
        val selectedDate = MutableStateFlow(LocalDate.of(2026, 10, 10))
        val tasks = selectedDateTaskFlow(selectedDate) { date -> flowOf(date) }
        val receivedDates = Channel<LocalDate>(Channel.UNLIMITED)

        val collector = launch { tasks.take(2).collect { receivedDates.send(it) } }
        assertEquals(
            LocalDate.of(2026, 10, 10),
            withTimeout(5_000) { receivedDates.receive() },
        )
        selectedDate.value = LocalDate.of(2026, 10, 11)
        assertEquals(
            LocalDate.of(2026, 10, 11),
            withTimeout(5_000) { receivedDates.receive() },
        )
        collector.join()
    }
}
