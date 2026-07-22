package com.miruronative.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class KonohaEpisode(
    val title: String? = null,
    val image: String? = null,
    val episodeNumber: Double? = null,
    val anilistId: Int? = null,
    val malId: Int? = null,
    val filler: Boolean? = null,
)
