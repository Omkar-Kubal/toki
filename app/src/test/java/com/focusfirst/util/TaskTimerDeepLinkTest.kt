package com.focusfirst.util

import com.focusfirst.data.model.TaskEntity
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskTimerDeepLinkTest {

    @Test
    fun taskLinkCarriesDurationIdentityAndEncodedTitleToTheTimerRoute() {
        val task = TaskEntity(
            id = 42,
            title = "Review API & UI / notes",
            durationMinutes = 45,
        )

        val uri = URI(TaskTimerDeepLink.forTask(task))
        val query = uri.rawQuery.split('&').associate { pair ->
            val (key, value) = pair.split('=', limit = 2)
            key to URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }

        assertEquals("toki", uri.scheme)
        assertEquals("timer", uri.host)
        assertEquals("/start", uri.path)
        assertEquals("45", query["duration"])
        assertEquals("42", query["taskId"])
        assertEquals("Review API & UI / notes", query["task"])
    }
}
