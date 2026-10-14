package com.proje.sosyal.android.domain.model

import java.time.LocalDate

data class RegistrationForm(
    val username: String,
    val displayName: String,
    val email: String,
    val password: String,
    val birthDate: LocalDate,
    val privacyNoticeAccepted: Boolean,
)
