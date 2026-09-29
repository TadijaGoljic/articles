package com.example.articles.viewmodels

import com.example.articles.db.ArticleDatabase
import com.example.articles.fcm.SessionManager
import com.example.articles.models.UserPreferences
import com.example.articles.repositories.AccountRepository
import com.example.articles.repositories.InvitationRepository
import com.example.articles.testing.FakeFirebaseAuth
import com.example.articles.testing.MainDispatcherRule
import com.example.articles.testing.fakeContext
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestoreException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeAuth: FakeFirebaseAuth

    private val database = mockk<ArticleDatabase>(relaxed = true)
    private val accountRepository = mockk<AccountRepository>(relaxed = true)
    private val userPreferences = mockk<UserPreferences>(relaxed = true) {
        every { syncDataEnabled } returns true
        every { confirmNonDestructiveAgentActions } returns true
    }

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        fakeAuth = FakeFirebaseAuth(uid = "uid-a", email = "a@example.com")
        viewModel = SettingsViewModel(
            database = database,
            accountRepository = accountRepository,
            groupRepository = mockk(relaxed = true),
            articleRepository = mockk(relaxed = true),
            invitationRepository = mockk<InvitationRepository> {
                every { observePendingInvitations(any()) } returns flowOf(emptyList())
            },
            userPreferences = userPreferences,
            triggerSync = mockk(relaxed = true),
            fileManager = mockk(relaxed = true),
            sessionManager = mockk<SessionManager>(relaxed = true),
            context = fakeContext()
        )
    }

    @After
    fun tearDown() = fakeAuth.release()

    private fun awaitDeleteFinished() = runBlocking {
        withContext(Dispatchers.Default) {
            withTimeout(5_000) { viewModel.uiState.first { !it.deleteAccountLoading } }
        }
    }

    @Test
    fun `shows the signed-in email and stored preferences`() {
        with(viewModel.uiState.value) {
            assertEquals("a@example.com", userEmail)
            assertTrue(syncData)
            assertTrue(confirmAgentActions)
        }
    }

    @Test
    fun `deleting the account removes cloud data, then the account, then local data`() {
        every { fakeAuth.user!!.delete() } returns Tasks.forResult(null)

        viewModel.handleAction(SettingsAction.DeleteAccount)
        awaitDeleteFinished()

        coVerifyOrder {
            accountRepository.deleteUserData("uid-a")
            fakeAuth.user!!.delete()
            database.clearAllTables()
        }
        assertTrue(viewModel.uiState.value.deleteOk)
    }

    @Test
    fun `a refused account deletion keeps local data and shows why`() {
        every { fakeAuth.user!!.delete() } returns Tasks.forException(
            IllegalStateException("This operation requires recent authentication.")
        )

        viewModel.handleAction(SettingsAction.DeleteAccount)
        awaitDeleteFinished()

        verify(exactly = 0) { database.clearAllTables() }
        assertFalse(viewModel.uiState.value.deleteOk)
        assertEquals("This operation requires recent authentication.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `a failing cloud cleanup does not delete the account or crash`() {
        coEvery { accountRepository.deleteUserData(any()) } throws FirebaseFirestoreException(
            "PERMISSION_DENIED", FirebaseFirestoreException.Code.PERMISSION_DENIED
        )

        viewModel.handleAction(SettingsAction.DeleteAccount)
        awaitDeleteFinished()

        verify(exactly = 0) { fakeAuth.user!!.delete() }
        verify(exactly = 0) { database.clearAllTables() }
        assertEquals("PERMISSION_DENIED", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `toggles are persisted`() {
        viewModel.handleAction(SettingsAction.UpdateConfirmAgentActionsStatus(false))
        viewModel.handleAction(SettingsAction.UpdateSyncDataStatus(false))

        verify { userPreferences.confirmNonDestructiveAgentActions = false }
        verify { userPreferences.syncDataEnabled = false }
        assertFalse(viewModel.uiState.value.confirmAgentActions)
        assertFalse(viewModel.uiState.value.syncData)
        coVerify(exactly = 0) { accountRepository.deleteUserData(any()) }
    }
}
