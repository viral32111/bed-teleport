package com.viral32111.bedteleport.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
	@SerialName( "command-name" ) val commandName: String = "bed",
	@SerialName( "experience-cost" ) val experienceCost: ExperienceCost = ExperienceCost(),
	val delays: Delays = Delays()
)
