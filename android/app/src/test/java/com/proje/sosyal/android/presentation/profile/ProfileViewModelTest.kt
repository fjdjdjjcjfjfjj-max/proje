package com.proje.sosyal.android.presentation.profile

import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.model.UserProfile
import com.proje.sosyal.android.domain.repository.AuthRepository
import com.proje.sosyal.android.domain.repository.UserRepository
import com.proje.sosyal.android.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val me = MutableStateFlow<UserProfile?>(null)
    private val users = mockk<UserRepository> { every { me } returns this@ProfileViewModelTest.me }
    private val auth = mockk<AuthRepository>(relaxed = true)

    private val profile = UserProfile(
        id = "id", username = "ayse", displayName = "Ayşe", email = "a@example.com", bio = null,
        isMinor = false, isAgeVerified = false, isProfilePrivate = false, postCount = 3, likesReceived = 1362,
    )

    @Test
    fun `depodaki profil gosterilir ve guncellenince ekran da guncellenir`() = runTest {
        coEvery { users.getMe() } coAnswers { me.value = profile; AppResult.Success(profile) }
        val vm = ProfileViewModel(users, auth)
        advanceUntilIdle()
        assertEquals(ProfileUiState.Loaded(profile), vm.state.value)

        // Düzenleme ekranı depoyu güncellediğinde profil ekranı da değişir
        me.value = profile.copy(bio = "Yeni")
        advanceUntilIdle()
        assertEquals("Yeni", (vm.state.value as ProfileUiState.Loaded).profile.bio)
    }

    @Test
    fun `yukleme hatasinda hata gosterilir`() = runTest {
        coEvery { users.getMe() } returns AppResult.Failure(AppError.NETWORK)
        val vm = ProfileViewModel(users, auth)
        advanceUntilIdle()
        assertEquals(ProfileUiState.Error(AppError.NETWORK), vm.state.value)
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
