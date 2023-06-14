package com.viral32111.bedteleport

import com.mojang.brigadier.Command
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.viral32111.bedteleport.config.Configuration
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.Blocks
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.io.path.createDirectory
import kotlin.io.path.notExists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.math.sqrt

@Suppress( "UNUSED" )
class BedTeleport: DedicatedServerModInitializer {

	companion object {
		private const val MOD_ID = "bedteleport"
		val LOGGER: Logger = LoggerFactory.getLogger( "com.viral32111.$MOD_ID" )

		@OptIn( ExperimentalSerializationApi::class )
		val JSON = Json {
			prettyPrint = true
			prettyPrintIndent = "\t"
			ignoreUnknownKeys = true
		}

		const val CONFIGURATION_DIRECTORY_NAME = "viral32111"
		const val CONFIGURATION_FILE_NAME = "$MOD_ID.json"

		var configuration = Configuration()

		/**
		 * Gets the current version of this mod.
		 * @since 2.0.0
		 */
		fun getModVersion(): String =
			FabricLoader.getInstance().getModContainer( MOD_ID ).orElseThrow {
				throw IllegalStateException( "Mod container not found" )
			}.metadata.version.friendlyString

		private val coroutineScope = CoroutineScope( Dispatchers.Default )

		private val playerLastTeleportTimes = mutableMapOf<UUID, Long>()

		private val bedBlocks = setOf(
			Blocks.WHITE_BED,
			Blocks.BLACK_BED,
			Blocks.BLUE_BED,
			Blocks.BROWN_BED,
			Blocks.CYAN_BED,
			Blocks.GREEN_BED,
			Blocks.LIGHT_BLUE_BED,
			Blocks.LIGHT_GRAY_BED,
			Blocks.LIME_BED,
			Blocks.GRAY_BED,
			Blocks.PINK_BED,
			Blocks.ORANGE_BED,
			Blocks.MAGENTA_BED,
			Blocks.PURPLE_BED,
			Blocks.YELLOW_BED,
			Blocks.RED_BED
		)
	}

	override fun onInitializeServer() {
		LOGGER.info( "Bed Teleport v${ getModVersion() } initialized on the server." )

		configuration = loadConfigurationFile()

		ServerLifecycleEvents.SERVER_STOPPING.register { _ ->
			coroutineScope.cancel()
		}

		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			dispatcher.register( literal( "bed" ).executes { context ->
				val player = context.source.player ?: throw SimpleCommandExceptionType( Text.literal( "Command only usable by players." ) ).create()

				val cooldownDelay = configuration.delays.cooldown
				if ( cooldownDelay > 0 ) {
					val lastTeleportTime = playerLastTeleportTimes[ player.uuid ]
					if ( lastTeleportTime != null ) {
						val remainingSeconds = ( lastTeleportTime.plus( cooldownDelay ) - currentMonotonicSecond() ).coerceAtLeast( 0 )

						if ( remainingSeconds > 0 ) {
							player.sendMessage( Text.literal( "You must wait $remainingSeconds second(s) before teleporting again!" ) )
							LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) attempted teleport while in cooldown (${ remainingSeconds }/${ cooldownDelay } second(s) remaining)." )
							return@executes 0
						}
					}
				}

				val world = context.source.world
				val bedPosition = player.spawnPointPosition

				if ( bedPosition == null ) {
					player.sendMessage( Text.literal( "You have not set your respawn point in a bed!" ) )
					LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) attempted teleport with no bed." )
					return@executes 0
				} else if ( !bedBlocks.contains( world.getBlockState( bedPosition ).block ) ) {
					player.sendMessage( Text.literal( "Your bed is destroyed!" )  )
					LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) attempted teleport to destroyed bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ]." )
					return@executes 0
				} else if ( player.world.registryKey != player.spawnPointDimension ) {
					player.sendMessage( Text.literal( "Your bed is in a different dimension!" )  )
					LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) in dimension '${ player.world.registryKey.value.namespace }:${ player.world.registryKey.value.path }' attempted teleport to bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ] in dimension '${ player.spawnPointDimension.value.namespace }:${ player.spawnPointDimension.value.path }'." )
					return@executes 0
				}

				val safeTeleportPosition = findSafePosition( world, bedPosition )
				if ( safeTeleportPosition == null ) {
					player.sendMessage( Text.literal( "Your bed is obstructed! Unable to find a safe teleport destination around your bed." ) )
					LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) attempted teleport to obstructed bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ]." )
					return@executes 0
				}
				//LOGGER.info( "safeTeleportPosition: [ ${ safeTeleportPosition.x }, ${ safeTeleportPosition.y }, ${ safeTeleportPosition.z } ]" )

				val distance = sqrt( bedPosition.getSquaredDistance( player.blockPos ) ).toInt()
				val shouldChargeExperience = configuration.experienceCost.enabled
				val distanceFree = configuration.experienceCost.distanceFree
				val experienceCost = if ( shouldChargeExperience && distance > distanceFree ) distance / configuration.experienceCost.perBlocks else 0
				//LOGGER.info( "experienceCost: $experienceCost | player.totalExperience: ${ player.totalExperience } | distance: $distance | distanceFree: $distanceFree" )

				if ( experienceCost > 0 ) {
					val requiredExperience = ( experienceCost - player.totalExperience ).coerceAtLeast( 0 )
					//LOGGER.info( "requiredExperience: $requiredExperience" )

					if ( requiredExperience > 0 ) {
						player.sendMessage( Text.literal( "You do not have enough experience to teleport $distance block(s)! You need $requiredExperience more experience." ) )
						LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) at [ ${ player.x.toInt() }, ${ player.y.toInt() }, ${ player.z.toInt() } ] attempted teleport to bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ] (${ distance } blocks) with only ${ player.totalExperience }/${ experienceCost } (needs $requiredExperience)." )
						return@executes 0
					}
				}

				val destinationPositionX = safeTeleportPosition.x.toDouble() + 0.5
				val destinationPositionY = safeTeleportPosition.y.toDouble()
				val destinationPositionZ = safeTeleportPosition.z.toDouble() + 0.5

				val activationDelay = configuration.delays.activation
				if ( activationDelay > 0 ) {
					player.sendMessage( Text.literal( "Do not move. Teleporting in $activationDelay second(s)..." ) )

					coroutineScope.launch {
						if ( delayUnlessMovement( player, activationDelay ) ) {
							player.sendMessage( Text.literal( "You moved! Teleportation cancelled." ) )
							LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) attempted teleport to bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ] but moved." )
							return@launch
						}

						if ( experienceCost > 0 ) {
							player.addExperience( -experienceCost )
							player.playSound( SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f )
							//world.playSound( null, player.blockPos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f )
							//world.playSound( null, safeTeleportPosition, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f )
						}

						player.teleport( destinationPositionX, destinationPositionY, destinationPositionZ, true )
						if ( cooldownDelay > 0 ) playerLastTeleportTimes[ player.uuid ] = currentMonotonicSecond()

						player.sendMessage( Text.literal( "Teleported $distance block(s) back to your bed for ${ if ( experienceCost > 0 ) "$experienceCost experience" else "free" }." ) )
						LOGGER.info( "Player '${ player.name.string }' (${ player.uuidAsString }) at [ ${ player.x.toInt() }, ${ player.y.toInt() }, ${ player.z.toInt() } ] teleported to bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ] (${ distance } blocks) for $experienceCost experience." )
					}
				} else {
					if ( experienceCost > 0 ) {
						player.addExperience( -experienceCost )
						player.playSound( SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f )
						//world.playSound( null, player.blockPos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f )
						//world.playSound( null, safeTeleportPosition, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f )
					}

					player.teleport( destinationPositionX, destinationPositionY, destinationPositionZ, true )
					if ( cooldownDelay > 0 ) playerLastTeleportTimes[ player.uuid ] = currentMonotonicSecond()

					player.sendMessage( Text.literal( "Teleported $distance block(s) back to your bed for ${ if ( experienceCost > 0 ) "$experienceCost experience" else "free" }." ) )
					LOGGER.info( "Player '${ player.name.string }' (${ player.uuidAsString }) at [ ${ player.x.toInt() }, ${ player.y.toInt() }, ${ player.z.toInt() } ] teleported to bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ] (${ distance } blocks) for $experienceCost experience." )
				}

				return@executes Command.SINGLE_SUCCESS
			} )
		}
	}

	private fun currentMonotonicSecond(): Long = System.nanoTime().div( 1000 * 1000 * 1000 ) // nano -> micro -> milli -> second

	private suspend fun delayUnlessMovement( player: ServerPlayerEntity, delay: Int = configuration.delays.activation ): Boolean {
		val initialPosition = player.blockPos

		repeat( delay ) { remainingSeconds ->
			delay( 1000 ) // 1s

			if ( player.blockPos != initialPosition ) return true

			player.sendMessage( Text.literal( "Teleporting in ${ delay - remainingSeconds } second(s)..." ), true )
		}

		delay( 1000 ) // 1s

		return false
	}

	private fun findSafePosition( world: ServerWorld, position: BlockPos, minimumRange: Int = -1, maximumRange: Int = 1 ): BlockPos? =
		sequence {
			for ( y in 0..1 ) { // On the ground & one block above
				for ( x in minimumRange..maximumRange ) {
					for ( z in minimumRange..maximumRange ) {
						yield( position.add( x, y, z ) )
					}
				}
			}
		}.firstOrNull { isSafeToStand( world, it ) }

	private fun isSafeToStand( world: ServerWorld, position: BlockPos ): Boolean {
		val block = world.getBlockState( position )
		val blockAbove = world.getBlockState( position.up() )
		val blockBelow = world.getBlockState( position.down() )

		return ( !blockBelow.isAir || bedBlocks.contains( blockBelow.block ) ) && block.isAir && blockAbove.isAir
	}

	private fun loadConfigurationFile(): Configuration {
		val serverConfigurationDirectory = FabricLoader.getInstance().configDir
		val configurationDirectory = serverConfigurationDirectory.resolve( CONFIGURATION_DIRECTORY_NAME )
		val configurationFile = configurationDirectory.resolve( CONFIGURATION_FILE_NAME )

		if ( configurationDirectory.notExists() ) {
			configurationDirectory.createDirectory()
			LOGGER.info( "Created directory '${ configurationDirectory }' for configuration files." )
		}

		if ( configurationFile.notExists() ) {
			val configAsJSON = JSON.encodeToString( Configuration() )

			configurationFile.writeText( configAsJSON, options = arrayOf(
				StandardOpenOption.CREATE_NEW,
				StandardOpenOption.WRITE
			) )

			LOGGER.info( "Created configuration file '${ configurationFile }'." )
		}

		val configAsJSON = configurationFile.readText()
		val config = JSON.decodeFromString<Configuration>( configAsJSON )
		LOGGER.info( "Loaded configuration from file '${ configurationFile }'" )

		return config
	}

}
