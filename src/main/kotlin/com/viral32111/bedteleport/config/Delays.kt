package com.viral32111.bedteleport.config

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Delays(
	@Required @SerialName( "activation" ) val activation: Int = 5,
	@Required @SerialName( "cooldown" ) val cooldown: Int = 120
)
