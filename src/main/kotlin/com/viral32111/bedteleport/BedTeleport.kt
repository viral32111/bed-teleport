package com.viral32111.bedteleport

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
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
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
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
		private val LOGGER: Logger = LoggerFactory.getLogger( "com.viral32111.$MOD_ID" )

		private const val CONFIGURATION_DIRECTORY_NAME = "viral32111"
		private const val CONFIGURATION_FILE_NAME = "$MOD_ID.json"

		@OptIn( ExperimentalSerializationApi::class )
		val JSON = Json {
			prettyPrint = true
			prettyPrintIndent = "\t"
			ignoreUnknownKeys = true
		}
	}

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

	private fun getModVersion(): String =
		FabricLoader.getInstance().getModContainer( MOD_ID ).orElseThrow {
			throw IllegalStateException( "Mod container not found" )
		}.metadata.version.friendlyString

	override fun onInitializeServer() {
		LOGGER.info( "Bed Teleport v${ getModVersion() } initialized on the server." )

		val configuration = loadConfigurationFile()

		CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
			LOGGER.debug( "Registering chat command '/${ configuration.commandName }'..." )

			dispatcher.register( literal( configuration.commandName ).executes {
				chatCommand( it, configuration )
			} )
		}

		ServerLifecycleEvents.SERVER_STOPPING.register {
			LOGGER.debug( "Cancelling coroutines..." )
			coroutineScope.cancel()
		}
	}

	private fun chatCommand( context: CommandContext<ServerCommandSource>, configuration: Configuration ): Int {
		val player = context.source.player ?: throw SimpleCommandExceptionType( Text.literal( "Command only usable by players." ) ).create()

		val cooldownDelay = configuration.delays.cooldown
		if ( cooldownDelay > 0 ) {
			val lastTeleportTime = playerLastTeleportTimes[ player.uuid ]
			if ( lastTeleportTime != null ) {
				val remainingSeconds = ( lastTeleportTime.plus( cooldownDelay ) - currentMonotonicSecond() ).coerceAtLeast( 0 )
				LOGGER.debug( "Player '${ player.name.string }' (${ player.uuidAsString }) has $remainingSeconds second(s) remaining on their cooldown." )

				if ( remainingSeconds > 0 ) {
					player.sendMessage( Text.literal( "You must wait $remainingSeconds second(s) before teleporting again!" ) )
					LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) attempted teleport while in cooldown (${ remainingSeconds }/${ cooldownDelay } second(s) remaining)." )
					return 0
				}
			}
		}

		val world = context.source.world
		val playerDimension = player.world.dimensionKey
		val bedPosition = player.spawnPointPosition
		val bedDimension = player.spawnPointDimension
		LOGGER.debug( "Player '${ player.name.string }' (${ player.uuidAsString }) in dimension '${ playerDimension.value.namespace }:${ playerDimension.value.path }' and bed [ ${ bedPosition?.x }, ${ bedPosition?.y }, ${ bedPosition?.z } ] in dimension '${ bedDimension.value.namespace }:${ bedDimension.value.path }'." )

		if ( playerDimension.value.path != bedDimension.value.path ) {
			player.sendMessage( Text.literal( "Your bed is in a different dimension!" )  )
			LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) in dimension '${ playerDimension.value.namespace }:${ playerDimension.value.path }' attempted teleport to bed in dimension '${ bedDimension.value.namespace }:${ bedDimension.value.path }'." )
			return 0
		}

		if ( bedPosition == null ) {
			player.sendMessage( Text.literal( "You have not set your respawn point in a bed!" ) )
			LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) attempted teleport with no bed." )
			return 0
		}

		if ( !bedBlocks.contains( world.getBlockState( bedPosition ).block ) ) {
			player.sendMessage( Text.literal( "Your bed is destroyed!" )  )
			LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) attempted teleport to destroyed bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ]." )
			return 0
		}

		val safeTeleportPosition = findSafePosition( world, bedPosition )
		LOGGER.debug( "Player '${ player.name.string }' (${ player.uuidAsString }) has safe teleport position [ ${ safeTeleportPosition?.x }, ${ safeTeleportPosition?.y }, ${ safeTeleportPosition?.z } ] for bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ]." )
		if ( safeTeleportPosition == null ) {
			player.sendMessage( Text.literal( "Your bed is obstructed! Unable to find a safe teleport destination around your bed." ) )
			LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) attempted teleport to obstructed bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ]." )
			return 0
		}

		val distance = sqrt( bedPosition.getSquaredDistance( player.blockPos ) ).toInt()
		val shouldChargeExperience = configuration.experienceCost.enabled
		val distanceFree = configuration.experienceCost.distanceFree
		val experienceCost = if ( shouldChargeExperience && distance > distanceFree ) distance / configuration.experienceCost.perBlocks else 0
		LOGGER.debug( "Player '${ player.name.string }' (${ player.uuidAsString}) requires $experienceCost experience to teleport to bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ] ($distance block(s))." )

		if ( experienceCost > 0 ) {
			val requiredExperience = ( experienceCost - player.totalExperience ).coerceAtLeast( 0 )
			LOGGER.debug( "Player '${ player.name.string }' (${ player.uuidAsString}) needs $requiredExperience/$experienceCost experience to teleport to bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ] ($distance block(s))." )

			if ( requiredExperience > 0 ) {
				player.sendMessage( Text.literal( "You do not have enough experience to teleport $distance block(s)! You need $requiredExperience more experience." ) )
				LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) at [ ${ player.x.toInt() }, ${ player.y.toInt() }, ${ player.z.toInt() } ] attempted teleport to bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ] (${ distance } block(s)) with only ${ player.totalExperience }/${ experienceCost } (needs $requiredExperience)." )
				return 0
			}
		}

		coroutineScope.launch {
			LOGGER.debug( "Entered coroutine scope..." )

			val activationDelay = configuration.delays.activation
			if ( activationDelay > 0 ) {
				player.sendMessage( Text.literal( "Do not move. Teleporting in $activationDelay second(s)..." ) )

				if ( delayUnlessMovement( player, activationDelay ) ) {
					player.sendMessage( Text.literal( "Teleportation cancelled due to movement!" ) )
					LOGGER.warn( "Player '${ player.name.string }' (${ player.uuidAsString }) attempted teleport to bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ] ($distance block(s)) but moved." )
					return@launch
				}
			}

			val centeredTeleportPosition = Vec3d.add( Vec3i( safeTeleportPosition.x, safeTeleportPosition.y, safeTeleportPosition.z ), 0.5, 0.0, 0.5 )
			LOGGER.debug( "Player '${ player.name.string }' (${ player.uuidAsString}) will be teleported to [ ${ centeredTeleportPosition.x }, ${ centeredTeleportPosition.y }, ${ centeredTeleportPosition.z } ]." )

			player.serverWorld.server.execute {
				LOGGER.debug( "Entered main server thread." )

				player.teleport( player.serverWorld, centeredTeleportPosition.x, centeredTeleportPosition.y, centeredTeleportPosition.z, player.yaw, player.pitch )
				player.refreshPositionAfterTeleport( centeredTeleportPosition )

				LOGGER.debug( "Player '${ player.name.string }' (${ player.uuidAsString}) was (hopefully) teleported to [ ${ centeredTeleportPosition.x }, ${ centeredTeleportPosition.y }, ${ centeredTeleportPosition.z } ]." )
			}

			LOGGER.debug( "Waiting 1 second for player to arrive at destination..." )
			delay( 1000 )

			if ( hasPlayerTeleported( player, centeredTeleportPosition ) ) {
				LOGGER.debug( "Player '${ player.name.string }' (${ player.uuidAsString}) will be teleported to [ ${ centeredTeleportPosition.x }, ${ centeredTeleportPosition.y }, ${ centeredTeleportPosition.z } ]." )

				if ( experienceCost > 0 ) {
					player.serverWorld.server.execute {
						LOGGER.debug( "Entered main server thread." )

						player.addExperience( -experienceCost )
						world.playSound( null, safeTeleportPosition, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.0f )

						LOGGER.debug( "Player '${ player.name.string }' (${ player.uuidAsString}) was charged $experienceCost experience." )
					}
				}

				if ( cooldownDelay > 0 ) playerLastTeleportTimes[ player.uuid ] = currentMonotonicSecond()

				player.sendMessage( Text.literal( "Teleported $distance block(s) back to your bed for ${ if ( experienceCost > 0 ) "$experienceCost experience" else "free" }." ) )
				LOGGER.info( "Player '${ player.name.string }' (${ player.uuidAsString }) at [ ${ player.x.toInt() }, ${ player.y.toInt() }, ${ player.z.toInt() } ] teleported to bed [ ${ bedPosition.x }, ${ bedPosition.y }, ${ bedPosition.z } ] ($distance block(s)) for $experienceCost experience." )
			} else {
				player.sendMessage( Text.literal( "Unable to teleport $distance block(s) back to your bed." ) )
				LOGGER.error( "Player '${ player.name.string }' (${ player.uuidAsString}) was not teleported to bed [ ${ centeredTeleportPosition.x }, ${ centeredTeleportPosition.y }, ${ centeredTeleportPosition.z } ] ($distance block(s))." )
			}
		}

		return Command.SINGLE_SUCCESS
	}

	private suspend fun delayUnlessMovement( player: ServerPlayerEntity, delay: Int ): Boolean {
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

	private fun hasPlayerTeleported( player: ServerPlayerEntity, destination: Vec3d ) = sqrt( player.squaredDistanceTo( destination ) ).toInt() <= 10

	private fun currentMonotonicSecond(): Long = System.nanoTime().div( 1000 * 1000 * 1000 ) // nano -> micro -> milli -> second

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
