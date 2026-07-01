package com.tokenmonitor.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiKeyEntry(
    val name: String = "",
    val created_at: String = "",
    val key: String = ""
)

@Serializable
data class PlatformKeys(
    val keys: List<ApiKeyEntry> = emptyList()
)

@Serializable
data class PlatformsData(
    val platforms: Map<String, PlatformKeys> = emptyMap()
)
