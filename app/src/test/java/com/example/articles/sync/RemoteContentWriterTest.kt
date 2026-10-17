package com.example.articles.sync

import com.example.articles.models.UserPreferences
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RemoteContentWriterTest {

    private val document = mockk<DocumentReference> {
        every { set(any()) } returns Tasks.forResult(null)
        every { delete() } returns Tasks.forResult(null)
    }

    private fun writer(syncEnabled: Boolean) = RemoteContentWriter(
        mockk<UserPreferences> { every { syncDataEnabled } returns syncEnabled }
    )

    @Test
    fun `writes and deletes reach Firestore when sync is enabled`() = runTest {
        val writer = writer(syncEnabled = true)

        writer.set(document, mapOf("name" to "Milk"))
        writer.delete(document)

        verify(exactly = 1) { document.set(mapOf("name" to "Milk")) }
        verify(exactly = 1) { document.delete() }
    }

    @Test
    fun `nothing is sent to Firestore when sync is disabled`() = runTest {
        val writer = writer(syncEnabled = false)

        writer.set(document, mapOf("name" to "Milk"))
        writer.delete(document)
        writer.deleteAll(listOf(document))

        verify(exactly = 0) { document.set(any()) }
        verify(exactly = 0) { document.delete() }
    }

    @Test
    fun `deleting an empty list sends nothing even with sync enabled`() = runTest {
        writer(syncEnabled = true).deleteAll(emptyList())

        verify(exactly = 0) { document.delete() }
    }
}
