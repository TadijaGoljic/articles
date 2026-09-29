package com.example.articles.testing

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FakeFirebaseAuth(uid: String? = "uid-a", email: String? = "a@example.com") {

    val user: FirebaseUser? = uid?.let {
        mockk(relaxed = true) {
            every { this@mockk.uid } returns uid
            every { this@mockk.email } returns email
        }
    }

    val auth: FirebaseAuth = mockk(relaxed = true) {
        every { currentUser } returns user
    }

    init {
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth
    }

    fun release() = unmockkStatic(FirebaseAuth::class)
}

fun fakeContext(): Context = mockk(relaxed = true) {
    every { getString(any()) } answers { stringRes(firstArg()) }
    every { getString(any(), *anyVararg()) } answers {
        "${stringRes(firstArg())}:${secondArg<Array<Any?>>().joinToString()}"
    }
}

fun stringRes(id: Int) = "string:$id"

fun <T : Any> savedStateHandleFor(route: T): SavedStateHandle {
    val handle = SavedStateHandle()
    mockkStatic(SAVED_STATE_HANDLE_KT)
    @Suppress("UNCHECKED_CAST")
    every { handle.toRoute(route::class as KClass<T>, any()) } returns route
    return handle
}

fun releaseRoute() = unmockkStatic(SAVED_STATE_HANDLE_KT)

private const val SAVED_STATE_HANDLE_KT = "androidx.navigation.SavedStateHandleKt"
