package com.viral32111.bedteleport.config

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExperienceCost(
	@Required @SerialName( "enabled" ) val enabled: Boolean = true,
	@Required @SerialName( "per-blocks" ) val perBlocks: Int = 10,
	@Required @SerialName( "distance-free" ) val distanceFree: Int = 50
)
