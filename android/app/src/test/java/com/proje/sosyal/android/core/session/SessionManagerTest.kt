package com.proje.sosyal.android.core.session

import com.proje.sosyal.android.testutil.FakeTokenStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionManagerTest {

    @Test
    fun `kayitli belirtec yoksa oturum kapali baslar`() = runTest {
        val manager = SessionManager(FakeTokenStore())
        assertEquals(SessionState.Loading, manager.state.value)
        manager.restore()
        assertEquals(SessionState.LoggedOut, manager.state.value)
    }

    @Test
    fun `kayitli belirtec varsa oturum acik baslar`() = runTest {
        val manager = SessionManager(FakeTokenStore(AuthTokens("a", "r")))
        manager.restore()
        assertEquals(SessionState.LoggedIn, manager.state.value)
        assertEquals("a", manager.currentTokens()?.accessToken)
    }

    @Test
    fun `kaydetme ve temizleme hem diski hem bellegi gunceller`() = runTest {
        val store = FakeTokenStore()
        val manager = SessionManager(store)
        manager.restore()

        manager.save(AuthTokens("a", "r"))
        assertEquals(SessionState.LoggedIn, manager.state.value)
        assertEquals("r", store.stored?.refreshToken)

        manager.clear()
        assertEquals(SessionState.LoggedOut, manager.state.value)
        assertNull(store.stored)
        assertNull(manager.currentTokens())
    }
}
