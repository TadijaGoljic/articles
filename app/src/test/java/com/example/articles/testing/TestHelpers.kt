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

/** Routes `viewModelScope` (Dispatchers.Main) to a test dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/**
 * Stubs the static `Firebase.auth` accessor the ViewModels use, signed in as [uid] / [email]
 * (or signed out when [uid] is null). Call [release] in `@After`.
 */
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
        // `Firebase.auth` delegates to FirebaseAuth.getInstance(). Mocking the Java static avoids
        // mockk reflecting over AuthKt, which references classes missing from the unit-test classpath.
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns auth
    }

    fun release() = unmockkStatic(FirebaseAuth::class)
}

/** A Context whose strings are "string:<id>" / "string:<id>:<args>", so tests can assert which resource was used. */
fun fakeContext(): Context = mockk(relaxed = true) {
    every { getString(any()) } answers { stringRes(firstArg()) }
    every { getString(any(), *anyVararg()) } answers {
        "${stringRes(firstArg())}:${secondArg<Array<Any?>>().joinToString()}"
    }
}

fun stringRes(id: Int) = "string:$id"

/**
 * `SavedStateHandle.toRoute()` decodes through android.os.Bundle, which is only a stub in local
 * unit tests. Stub the navigation helper to hand the ViewModel [route] directly.
 * Call [releaseRoute] in `@After`.
 */
fun <T : Any> savedStateHandleFor(route: T): SavedStateHandle {
    val handle = SavedStateHandle()
    mockkStatic(SAVED_STATE_HANDLE_KT)
    @Suppress("UNCHECKED_CAST")
    every { handle.toRoute(route::class as KClass<T>, any()) } returns route
    return handle
}

fun releaseRoute() = unmockkStatic(SAVED_STATE_HANDLE_KT)

private const val SAVED_STATE_HANDLE_KT = "androidx.navigation.SavedStateHandleKt"
