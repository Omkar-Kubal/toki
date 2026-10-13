package com.focusfirst.util

import com.focusfirst.data.model.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskLinkResolverTest {

    private val tasks = listOf(
        TaskEntity(id = 4, title = "Physics"),
        TaskEntity(id = 8, title = "Physics"),
        TaskEntity(id = 11, title = "Archived", isCompleted = true),
    )

    @Test
    fun taskIdLinksResolveTheExactActiveTaskAndLegacyNamesRequireOneMatch() {
        assertEquals(8, TaskLinkResolver.resolve("8", "Physics", tasks))
        assertNull(TaskLinkResolver.resolve(null, "Physics", tasks))
    }

    @Test
    fun invalidOrCompletedTaskLinksDoNotSelectAnotherTask() {
        assertNull(TaskLinkResolver.resolve("11", "Physics", tasks))
        assertNull(TaskLinkResolver.resolve("invalid", "Physics", tasks))
    }

    @Test
    fun legacyNameResolvesOnlyWhenExactlyOneActiveTaskMatches() {
        assertEquals(4, TaskLinkResolver.resolve(null, "Physics", listOf(tasks[0])))
        assertNull(TaskLinkResolver.resolve(null, "Physics", tasks))
        assertNull(TaskLinkResolver.resolve(null, "Missing", tasks))
        assertNull(TaskLinkResolver.resolve(null, null, tasks))
    }

    @Test
    fun invalidIdDoesNotFallBackToAnOtherwiseUniqueLegacyName() {
        assertNull(TaskLinkResolver.resolve("invalid", "Physics", listOf(tasks[0])))
    }
}
