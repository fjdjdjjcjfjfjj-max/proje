package com.proje.sosyal.android.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proje.sosyal.android.core.session.SessionManager
import com.proje.sosyal.android.core.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val session: SessionManager,
) : ViewModel() {
    val state: StateFlow<SessionState> = session.state

    init {
        viewModelScope.launch { session.restore() }
    }
}
