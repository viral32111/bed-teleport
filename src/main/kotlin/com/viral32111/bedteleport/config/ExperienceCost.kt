package com.viral32111.bedteleport.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExperienceCost(
	val enabled: Boolean = true,
	@SerialName( "per-blocks" ) val perBlocks: Int = 10,
	@SerialName( "distance-free" ) val distanceFree: Int = 50
)
