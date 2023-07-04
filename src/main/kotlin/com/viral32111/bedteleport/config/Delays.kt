package com.viral32111.bedteleport.config

import kotlinx.serialization.Serializable

@Serializable
data class Delays(
	val activation: Int = 5,
	val cooldown: Int = 120
)
