package com.viral32111.bedteleport.config

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExperienceCost(
	@Required @SerialName( "enabled" ) val enabled: Boolean = true,
	@Required @SerialName( "distance-multiplier" ) val distanceMultiplier: Float = 1.0f,
)
