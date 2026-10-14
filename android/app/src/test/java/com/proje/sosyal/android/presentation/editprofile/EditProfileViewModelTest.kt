package com.proje.sosyal.android.presentation.editprofile

import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.core.media.ImageCompressor
import com.proje.sosyal.android.domain.model.ProfileImageKind
import com.proje.sosyal.android.domain.model.UserProfile
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profile = UserProfile(
        id = "id", username = "ayse", displayName = "Ayşe", email = "a@example.com", bio = "eski",
        isMinor = false, isAgeVerified = false, isProfilePrivate = false,
    )
    private val users = mockk<UserRepository> { every { me } returns MutableStateFlow(profile) }
    private val compressor = mockk<ImageCompressor>()

    @Test
    fun `degisiklik yoksa kaydetmek sunucuya gitmeden kapatir`() = runTest {
        val vm = EditProfileViewModel(users, compressor)
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.saved)
        coVerify(exactly = 0) { users.updateMe(any()) }
    }

    @Test
    fun `degisiklik kaydedilir`() = runTest {
        coEvery { users.updateMe(any()) } returns AppResult.Success(profile.copy(bio = "yeni"))
        val vm = EditProfileViewModel(users, compressor)
        vm.onBioChange("yeni")
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.state.value.saved)
        coVerify { users.updateMe(match { it.bio == "yeni" && it.displayName == "Ayşe" }) }
    }

    @Test
    fun `bos gorunen adla kaydedilemez`() = runTest {
        val vm = EditProfileViewModel(users, compressor)
        vm.onDisplayNameChange("   ")
        assertFalse(vm.state.value.nameValid)
        vm.save()
        advanceUntilIdle()
        assertFalse(vm.state.value.saved)
    }

    @Test
    fun `18 yas alti kullanici gizli profili kapatamaz`() = runTest {
        val minorUsers = mockk<UserRepository> {
            every { me } returns MutableStateFlow(profile.copy(isMinor = true, isProfilePrivate = true))
        }
        val vm = EditProfileViewModel(minorUsers, compressor)
        vm.onPrivateChange(false)
        assertTrue(vm.state.value.isPrivate)
    }

    @Test
    fun `fotograf kaldirma hatasinda yazilan metin korunur`() = runTest {
        coEvery { users.removeImage(ProfileImageKind.AVATAR) } returns AppResult.Failure(AppError.NETWORK)
        val vm = EditProfileViewModel(users, compressor)
        vm.onBioChange("kaydedilmemiş metin")
        vm.removeImage(ProfileImageKind.AVATAR)
        advanceUntilIdle()
        assertEquals("kaydedilmemiş metin", vm.state.value.bio)
        assertEquals(AppError.NETWORK, vm.state.value.error)
        assertNull(vm.state.value.uploading)
    }
}
