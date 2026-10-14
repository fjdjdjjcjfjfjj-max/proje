package com.proje.sosyal.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SystemStatusDto(
    val status: String,
    val apiVersion: String,
    val serverTime: String,
)
