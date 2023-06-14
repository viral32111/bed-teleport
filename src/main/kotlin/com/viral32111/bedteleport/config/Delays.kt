package com.viral32111.bedteleport.config

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Delays(
	@Required @SerialName( "activation" ) val activation: Float = 5.0f,
	@Required @SerialName( "cooldown" ) val cooldown: Float = 120.0f
)
