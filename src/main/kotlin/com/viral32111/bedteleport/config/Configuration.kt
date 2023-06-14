package com.viral32111.bedteleport.config

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
	@Required @SerialName( "experience-cost" ) val experienceCost: ExperienceCost = ExperienceCost(),
	@Required @SerialName( "delays" ) val delays: Delays = Delays()
)
