package com.proje.sosyal.android.presentation.auth

import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.repository.AuthRepository
import com.proje.sosyal.android.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = mockk<AuthRepository>()

    private fun RegisterViewModel.fillValid() {
        onUsernameChange("ayse.yilmaz")
        onDisplayNameChange("Ayşe Yılmaz")
        onEmailChange("ayse@example.com")
        onPasswordChange("GucluParola123")
        onBirthDateChange(LocalDate.of(2000, 1, 1))
        onPrivacyAcceptedChange(true)
    }

    @Test
    fun `gecersiz form gonderilmez ve tum alan hatalari gosterilir`() = runTest {
        val vm = RegisterViewModel(auth)
        vm.onUsernameChange("a!")
        vm.onPasswordChange("kisa")
        vm.submit()
        advanceUntilIdle()

        val errors = vm.state.value.fieldErrors
        assertEquals(RegisterField.entries.toSet(), errors.keys)
        assertEquals(R.string.validation_password, errors[RegisterField.PASSWORD])
        coVerify(exactly = 0) { auth.register(any()) }
    }

    @Test
    fun `gecerli form gonderilir`() = runTest {
        coEvery { auth.register(any()) } returns AppResult.Success(Unit)
        val vm = RegisterViewModel(auth)
        vm.fillValid()
        vm.submit()
        advanceUntilIdle()

        coVerify { auth.register(match { it.username == "ayse.yilmaz" && it.birthDate == LocalDate.of(2000, 1, 1) }) }
        assertTrue(vm.state.value.fieldErrors.isEmpty())
    }

    @Test
    fun `sunucu kullanici adi alinmis derse hata ilgili alana yazilir`() = runTest {
        coEvery { auth.register(any()) } returns AppResult.Failure(AppError.USERNAME_TAKEN, "Bu kullanıcı adı kullanılıyor.")
        val vm = RegisterViewModel(auth)
        vm.fillValid()
        vm.submit()
        advanceUntilIdle()

        assertEquals(R.string.error_username_taken, vm.state.value.fieldErrors[RegisterField.USERNAME])

        vm.onUsernameChange("baska_ad")
        assertTrue(RegisterField.USERNAME !in vm.state.value.fieldErrors)
    }
}
