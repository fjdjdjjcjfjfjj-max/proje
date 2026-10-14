package com.proje.sosyal.android.presentation.auth

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = mockk<AuthRepository>()

    @Test
    fun `bos alanlarla gonderim yapilmaz`() = runTest {
        val vm = LoginViewModel(auth)
        assertFalse(vm.state.value.canSubmit)
        vm.submit()
        advanceUntilIdle()
        coVerify(exactly = 0) { auth.login(any(), any()) }
    }

    @Test
    fun `basarili giriste parola alani temizlenir`() = runTest {
        coEvery { auth.login("ayse", "GucluParola123") } returns AppResult.Success(Unit)
        val vm = LoginViewModel(auth)
        vm.onIdentifierChange("ayse")
        vm.onPasswordChange("GucluParola123")
        vm.submit()
        advanceUntilIdle()

        assertEquals("", vm.state.value.password)
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `hatali giriste hata ve sunucu mesaji gosterilir, alan degisince temizlenir`() = runTest {
        coEvery { auth.login(any(), any()) } returns AppResult.Failure(AppError.INVALID_CREDENTIALS, "Hatalı")
        val vm = LoginViewModel(auth)
        vm.onIdentifierChange("ayse")
        vm.onPasswordChange("yanlis-parola")
        vm.submit()
        advanceUntilIdle()

        assertEquals(AppError.INVALID_CREDENTIALS, vm.state.value.error)
        assertEquals("Hatalı", vm.state.value.serverMessage)

        vm.onPasswordChange("yeni")
        assertNull(vm.state.value.error)
    }
}
