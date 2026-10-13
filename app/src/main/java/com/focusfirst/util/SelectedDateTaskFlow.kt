package com.focusfirst.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate

/** Switches the observed task query whenever the selected calendar date changes. */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> selectedDateTaskFlow(
    selectedDate: Flow<LocalDate>,
    queryTasksForDate: (LocalDate) -> Flow<T>,
): Flow<T> = selectedDate.distinctUntilChanged().flatMapLatest(queryTasksForDate)
