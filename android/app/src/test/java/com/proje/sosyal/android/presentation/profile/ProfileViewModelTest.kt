package com.proje.sosyal.android.presentation.profile

import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.model.ProfileUpdate
import com.proje.sosyal.android.domain.model.UserProfile
import com.proje.sosyal.android.domain.repository.AuthRepository
import com.proje.sosyal.android.domain.repository.UserRepository
import com.proje.sosyal.android.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val users = mockk<UserRepository>()
    private val auth = mockk<AuthRepository>(relaxed = true)

    private val profile = UserProfile(
        id = "id", username = "ayse", displayName = "Ayşe", email = "a@example.com", bio = null,
        isMinor = false, isAgeVerified = false, isProfilePrivate = false, postCount = 3, likesReceived = 1362,
    )

    @Test
    fun `profil yuklenir`() = runTest {
        coEvery { users.getMe() } returns AppResult.Success(profile)
        val vm = ProfileViewModel(users, auth)
        advanceUntilIdle()
        assertEquals(ProfileUiState.Loaded(profile), vm.state.value)
    }

    @Test
    fun `basarili kayitta profil guncellenir ve duzenleyici kapanir`() = runTest {
        val updated = profile.copy(bio = "Merhaba")
        coEvery { users.getMe() } returns AppResult.Success(profile)
        coEvery { users.updateMe(any()) } returns AppResult.Success(updated)
        val vm = ProfileViewModel(users, auth)
        advanceUntilIdle()

        vm.openEditor()
        assertTrue((vm.state.value as ProfileUiState.Loaded).edit.isOpen)
        vm.save(ProfileUpdate("Ayşe", "Merhaba", isPrivate = false))
        advanceUntilIdle()

        val state = vm.state.value as ProfileUiState.Loaded
        assertEquals("Merhaba", state.profile.bio)
        assertFalse(state.edit.isOpen)
    }

    @Test
    fun `basarisiz kayitta duzenleyici acik kalir ve hata gosterilir`() = runTest {
        coEvery { users.getMe() } returns AppResult.Success(profile)
        coEvery { users.updateMe(any()) } returns AppResult.Failure(AppError.NETWORK)
        val vm = ProfileViewModel(users, auth)
        advanceUntilIdle()

        vm.openEditor()
        vm.save(ProfileUpdate("Ayşe", "x", isPrivate = true))
        advanceUntilIdle()

        val edit = (vm.state.value as ProfileUiState.Loaded).edit
        assertTrue(edit.isOpen)
        assertFalse(edit.isSaving)
        assertEquals(AppError.NETWORK, edit.error)
    }

    @Test
    fun `cikis yapilir`() = runTest {
        coEvery { users.getMe() } returns AppResult.Success(profile)
        val vm = ProfileViewModel(users, auth)
        vm.logout()
        advanceUntilIdle()
        coVerify { auth.logout() }
    }
}
