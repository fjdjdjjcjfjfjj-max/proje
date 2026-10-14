package com.proje.sosyal.android.presentation.home

import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.model.ServerStatus
import com.proje.sosyal.android.domain.usecase.GetServerStatusUseCase
import com.proje.sosyal.android.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getServerStatus = mockk<GetServerStatusUseCase>()

    @Test
    fun `basarili yanitta Connected durumuna gecer`() = runTest {
        coEvery { getServerStatus() } returns AppResult.Success(ServerStatus(isUp = true, apiVersion = "v1"))

        val viewModel = HomeViewModel(getServerStatus)
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()
        assertEquals(HomeUiState.Connected("v1"), viewModel.uiState.value)
    }

    @Test
    fun `ag hatasinda Error durumuna gecer`() = runTest {
        coEvery { getServerStatus() } returns AppResult.Failure(AppError.NETWORK)

        val viewModel = HomeViewModel(getServerStatus)
        advanceUntilIdle()

        assertEquals(HomeUiState.Error(AppError.NETWORK), viewModel.uiState.value)
    }

    @Test
    fun `sunucu UP degilse sunucu hatasi gosterilir`() = runTest {
        coEvery { getServerStatus() } returns AppResult.Success(ServerStatus(isUp = false, apiVersion = "v1"))

        val viewModel = HomeViewModel(getServerStatus)
        advanceUntilIdle()

        assertEquals(HomeUiState.Error(AppError.SERVER), viewModel.uiState.value)
    }

    @Test
    fun `tekrar dene istegi yeniden yukler`() = runTest {
        coEvery { getServerStatus() } returnsMany listOf(
            AppResult.Failure(AppError.NETWORK),
            AppResult.Success(ServerStatus(isUp = true, apiVersion = "v1")),
        )

        val viewModel = HomeViewModel(getServerStatus)
        advanceUntilIdle()
        viewModel.retry()
        assertEquals(HomeUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(HomeUiState.Connected("v1"), viewModel.uiState.value)
        coVerify(exactly = 2) { getServerStatus() }
    }
}
